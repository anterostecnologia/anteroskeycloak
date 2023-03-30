package br.com.anteros.keycloak.domain;

import java.util.Optional;

public interface GetUserRequest {

	String getUserId();

	Optional<String> getClientId();
}
