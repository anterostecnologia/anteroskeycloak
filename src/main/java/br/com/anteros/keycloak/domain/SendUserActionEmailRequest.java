package br.com.anteros.keycloak.domain;

import java.util.Set;

public interface SendUserActionEmailRequest {

	String getUserId();

	Set<UserAction> getUserActions();
}
