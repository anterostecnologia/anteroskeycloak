package br.com.anteros.keycloak.domain;

import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface GetUsersRequest {

	Optional<String> getClientId();

	Pageable getPageable();
}
