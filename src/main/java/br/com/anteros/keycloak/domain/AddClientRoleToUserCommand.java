package br.com.anteros.keycloak.domain;

public interface AddClientRoleToUserCommand {

	String getUserId();

	String getClientId();

	String getRoleName();
}
