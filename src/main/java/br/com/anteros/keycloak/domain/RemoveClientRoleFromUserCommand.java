package br.com.anteros.keycloak.domain;

public interface RemoveClientRoleFromUserCommand {

	String getUserId();

	String getClientId();

	String getRoleName();
}
