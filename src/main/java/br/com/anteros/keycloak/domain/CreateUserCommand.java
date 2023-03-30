package br.com.anteros.keycloak.domain;

import java.util.Set;

public interface CreateUserCommand extends UserPersonalDataCommand {

	String getUsername();

	String getFirstName();

	String getLastName();

	String getEmail();

	String getPassword();

	boolean isEnabled();

	Set<UserAction> getRequiredUserActions();
}
