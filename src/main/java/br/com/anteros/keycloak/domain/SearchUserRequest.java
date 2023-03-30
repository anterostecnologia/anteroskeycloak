package br.com.anteros.keycloak.domain;

import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SearchUserRequest {

	/**
	 * @return Search term that will be used to search (partially) in:
	 * username,
	 * firstName,
	 * lastName,
	 * or email fields
	 */
	String getSearch();

	Optional<String> getClientId();

	Pageable getPageable();
}
