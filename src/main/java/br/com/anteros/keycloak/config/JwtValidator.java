package br.com.anteros.keycloak.config;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JwtValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidator.class);
    private List<String> allowedIsses;

    public JwtValidator(List<String> allowedIsses) {
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

    /**
     * Validate a JWT token
     * @param token
     * @return decoded token
     */
    public DecodedJWT validate(String token) {
        try {
            final DecodedJWT jwt = JWT.decode(token);

            if (!allowedIsses.contains(jwt.getIssuer())) {
                throw new InvalidParameterException(String.format("Unknown Issuer %s", jwt.getIssuer()));
            }

            RSAPublicKey publicKey = loadPublicKey(jwt);

            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(jwt.getIssuer())
                    .build();

            verifier.verify(token);
            return jwt;

        } catch (Exception e) {
            logger.error("Failed to validate JWT", e);
            throw new InvalidParameterException("JWT validation failed: " + e.getMessage());
        }
    }


    public static void main( String[] args )
    {

        final JwtValidator validator = new JwtValidator(Arrays.asList("https://auth.relevantsolutions.com.br:15001/realms/cade-moto"));

        try {
            DecodedJWT token = validator.validate("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJRQnFLbmhsZ1ZVNWtvR0VaNDVaNEN5QXhTUkVVQmg0ZEp1RmFOaDY3dWgwIn0.eyJleHAiOjE2NTc2MzU3MjMsImlhdCI6MTY1NzYzNTQyMywianRpIjoiMmQzZDExYjMtNmVhZi00MWE1LWJhZjgtNDU4OGQ3OGU5NGMyIiwiaXNzIjoiaHR0cHM6Ly9hdXRoLnJlbGV2YW50c29sdXRpb25zLmNvbS5icjoxNTAwMS9yZWFsbXMvY2FkZS1tb3RvIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjA5ZjYxMzk4LWQ4NzEtNDk1Ny04ODRkLTI2YmQ4ZWY0Yjc5NCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImNhZGUtbW90byIsInNlc3Npb25fc3RhdGUiOiJiNTc2MDg4Yi0xYjRlLTQ4NzUtOTExMi0zYmU5ZjA1NDhkZDQiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbIioiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImRlZmF1bHQtcm9sZXMtY2FkZS1tb3RvIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsInNpZCI6ImI1NzYwODhiLTFiNGUtNDg3NS05MTEyLTNiZTlmMDU0OGRkNCIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJuYW1lIjoiRURTT04gTUFSVElOUyIsImF2YXRhciI6Imh0dHBzOi8vbmV4dGNsb3VkLnJlbGV2YW50c29sdXRpb25zLmNvbS5ici9pbmRleC5waHAvcy9tTW5paWo0ZEtKbkpQcmEvcHJldmlldyIsInByZWZlcnJlZF91c2VybmFtZSI6ImNhZGUiLCJnaXZlbl9uYW1lIjoiRURTT04iLCJmYW1pbHlfbmFtZSI6Ik1BUlRJTlMiLCJlbWFpbCI6InZlcnNhdGlsQHJlbGV2YW50c29sdXRpb25zLmNvbS5iciJ9.HbcbTppSXOBQ6g0Zt2otC4V9Iqkod6qonr45YPPMIDL8BDd9-omYrEp1Tk0CX6h5fvYO73K9BqXR_va7NZ4KDm6s1sOhG1Asx_ES4XWTJwCP7Eby2Pz_mSxUnnQQ-JGREFkIhgsmJ1fm1c5uDUuetrrIjAcsXXlYtz_cmb-p0S1Q7kRvJCUYpFhe7hkrbz0EbPyItK-9i8WxVsE6dt5ifNvngEfwBou8bU3j3N1FDSPIkQk-rVWQeBsW5Up3cwE2rFEJmgnKtvgKX2u3ZSLcYjeIEp5INbVvdk97nfTBCUKrVwYRp6-Uku-bZ0qVCe531rmy9PqCUNGFRM4lNbVbqA");
            System.out.println( "Jwt is valid" );
        } catch (InvalidParameterException e) {
            System.out.println( "Jwt is invalid" );
            e.printStackTrace();
        }

    }
}