package br.com.anteros.keycloak.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.*;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

public class User {

	public static final String PICTURE_URL_ATTRIBUTE = "pictureUrl";

	private final String id;
	private final String username;
	private final String firstName;
	private final String lastName;
	private final String email;
	private final boolean enabled;
	private final Map<String, List<String>> attributes;
	private final List<String> realmRoles;
	private Set<String> clientRoles;

	public User(UserRepresentation userRepresentation, String clientId) {
		this(userRepresentation);
		this.clientRoles = getClientRoles(userRepresentation.getClientRoles(), clientId);
	}

	public User(UserRepresentation userRepresentation) {
		this.id = userRepresentation.getId();
		this.username = userRepresentation.getUsername();
		this.firstName = userRepresentation.getFirstName();
		this.lastName = userRepresentation.getLastName();
		this.email = userRepresentation.getEmail();
		this.enabled = userRepresentation.isEnabled();
		this.attributes = userRepresentation.getAttributes();
		this.realmRoles = userRepresentation.getRealmRoles();
	}

	public String getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public List<String> getRealmRoles() {
		return realmRoles;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getPictureUrl() {
		return this.getSingleAttribute(PICTURE_URL_ATTRIBUTE)
				.orElse(null);
	}

	public Set<String> getClientRoles() {
		return unmodifiableSet(clientRoles);
	}

	public Optional<String> getSingleAttribute(String key) {
		if (attributes != null && attributes.containsKey(key)) {
			return Optional.of(attributes.get(key).get(0));
		}

		return Optional.empty();
	}

	@Override
	public boolean equals(Object o) {
		return EqualsBuilder.reflectionEquals(this, o);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	private Set<String> getClientRoles(Map<String, List<String>> clientRoles, String clientId) {
		if (StringUtils.isBlank(clientId)) {
			return emptySet();
		}

		return new HashSet<>(clientRoles.get(clientId));
	}
}
