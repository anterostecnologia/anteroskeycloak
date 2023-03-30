package br.com.anteros.keycloak.domain;

import java.util.List;

public interface CreatePublicClient {

	String getClientId();
	String getRootUrl();
	List<String> getRedirectUris();
	List<String> getWebOrigins();
}
