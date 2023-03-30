package br.com.anteros.keycloak.domain;

public interface AddRealmRoleToUserCommand {

	String getUserId();

	String getRoleName();
}
