package br.com.anteros.keycloak.config;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterConfigurer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.*;

/**
 * JwtAccessTokenCustomizer é para ler funções e user_name no token de acesso.
 * <p>
 * Esta classe assume que você definiu um Mapeador de Protocolo no Keycloack para mapear a propriedade do usuário 'username'
 * para uma declaração chamada 'user_name' no acesso
 * token
 * </p>
 */
public class KeycloakJwtAccessTokenCustomizer extends DefaultAccessTokenConverter implements JwtAccessTokenConverterConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakJwtAccessTokenCustomizer.class);

    private static final String CLIENT_NAME_ELEMENT_IN_JWT = "resource_access";

    private static final String ROLE_ELEMENT_IN_JWT = "roles";

    private ObjectMapper mapper;

    /* Usando injeção de construtor, @Autowired é implícito */
    public KeycloakJwtAccessTokenCustomizer(ObjectMapper mapper) {
        this.mapper = mapper;
        LOG.info("Initialized {}", KeycloakJwtAccessTokenCustomizer.class.getSimpleName());
    }

    @Override
    public void configure(JwtAccessTokenConverter converter) {
        converter.setAccessTokenConverter(this);
        LOG.info("Configured {}", JwtAccessTokenConverter.class.getSimpleName());
    }

    /**
     * Spring oauth2 espera funções no elemento de autoridades no tokenMap, mas o keycloak o fornece em resource_access. Daí extrairAutenticação
     * é substituído para extrair funções de resource_access.
     *
     * @return OAuth2Authentication com as autoridades para determinada aplicação
     */
    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> tokenMap) {
        LOG.debug("Iniciar extração Autenticação: tokenMap = {}", tokenMap);
        Map<String,Object> newMap = new HashMap<>();
        newMap.putAll(tokenMap);
        if (newMap.containsKey("preferred_username")){
            Object preferred_username = newMap.get("preferred_username");
            newMap.put("user_name", preferred_username);
        }
        JsonNode token = mapper.convertValue(newMap, JsonNode.class);
        Set<String> audienceList = extractClients(token); // extraindo nomes de clientes
        List<GrantedAuthority> authorities = extractRoles(token); // extraindo funções de cliente

        OAuth2Authentication authentication = super.extractAuthentication(newMap);
        OAuth2Request oAuth2Request = authentication.getOAuth2Request();

        OAuth2Request request =
                new OAuth2Request(oAuth2Request.getRequestParameters(), oAuth2Request.getClientId(), authorities, true, oAuth2Request.getScope(),
                        audienceList, null, null, null);

        Authentication usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), "N/A", authorities);
        LOG.debug("Finalizar extração Autenticação");
        return new OAuth2Authentication(request, usernamePasswordAuthentication);
    }

    private List<GrantedAuthority> extractRoles(JsonNode jwt) {
        LOG.debug("Comece extrair Roles: jwt = {}", jwt);
        Set<String> rolesWithPrefix = new HashSet<>();

        jwt.path(CLIENT_NAME_ELEMENT_IN_JWT)
                .elements()
                .forEachRemaining(e -> e.path(ROLE_ELEMENT_IN_JWT)
                        .elements()
                        .forEachRemaining(r -> rolesWithPrefix.add("ROLE_" + r.asText())));

        final List<GrantedAuthority> authorityList = AuthorityUtils.createAuthorityList(rolesWithPrefix.toArray(new String[0]));
        LOG.debug("Finalizar extração Roles: roles = {}", authorityList);
        return authorityList;
    }

    private Set<String> extractClients(JsonNode jwt) {
        LOG.debug("Comece a extrair Clientes: jwt = {}", jwt);
        if (jwt.has(CLIENT_NAME_ELEMENT_IN_JWT)) {
            JsonNode resourceAccessJsonNode = jwt.path(CLIENT_NAME_ELEMENT_IN_JWT);
            final Set<String> clientNames = new HashSet<>();
            resourceAccessJsonNode.fieldNames()
                    .forEachRemaining(clientNames::add);

            LOG.debug("Finalizar extração Clientes: clients = {}", clientNames);
            return clientNames;

        } else {
            throw new IllegalArgumentException("Elemento esperado " + CLIENT_NAME_ELEMENT_IN_JWT + " não encontrado no token");
        }

    }

}