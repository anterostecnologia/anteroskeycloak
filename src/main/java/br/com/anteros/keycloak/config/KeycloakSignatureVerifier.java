package br.com.anteros.keycloak.config;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.security.jwt.codec.Codecs;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;

public class KeycloakSignatureVerifier implements SignatureVerifier {
    private List<String> allowedIsses;
    private RSAPublicKey publicKey;

    public KeycloakSignatureVerifier(List<String> allowedIsses) {
        this.allowedIsses = allowedIsses;
    }

    private String getKeycloakCertificateUrl(DecodedJWT token) {
        return token.getIssuer() + "/protocol/openid-connect/certs";
    }

    private RSAPublicKey loadPublicKey(DecodedJWT token) throws JwkException, MalformedURLException {

        final String url = getKeycloakCertificateUrl(token);
        JwkProvider provider = new UrlJwkProvider(new URL(url));

        return (RSAPublicKey) provider.get(token.getKeyId()).getPublicKey();
    }

    @Override
    public void verify(byte[] content, byte[] sig) {
        byte[] bytes = Codecs.b64UrlEncode(sig);
        StringBuffer token = new StringBuffer();
        token.append(new String(content));
        token.append(".");
        token.append(new String(bytes));

        final DecodedJWT jwt = JWT.decode(token.toString());

        if (!allowedIsses.contains(jwt.getIssuer())) {
            throw new InvalidParameterException(String.format("Issuer desconhecido %s", jwt.getIssuer()));
        }

        if (publicKey == null) {
            try {
                publicKey = loadPublicKey(jwt);
            } catch (Exception e) {
                throw new AnterosKeycloakException("Erro lendo chave pública.");
            }
        }

        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(jwt.getIssuer())
                .acceptExpiresAt(999999999)
                .build();

        verifier.verify(token.toString());
    }

    @Override
    public String algorithm() {
        return "RS256";
    }
}
