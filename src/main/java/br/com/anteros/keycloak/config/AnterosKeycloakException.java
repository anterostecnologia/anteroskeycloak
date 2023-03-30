package br.com.anteros.keycloak.config;

public class AnterosKeycloakException extends RuntimeException{

    public AnterosKeycloakException() {
    }

    public AnterosKeycloakException(String message) {
        super(message);
    }

    public AnterosKeycloakException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnterosKeycloakException(Throwable cause) {
        super(cause);
    }

    public AnterosKeycloakException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
