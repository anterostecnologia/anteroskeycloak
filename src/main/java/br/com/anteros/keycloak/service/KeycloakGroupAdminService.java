package br.com.anteros.keycloak.service;


import br.com.anteros.keycloak.domain.GetGroupMembersRequest;
import br.com.anteros.keycloak.domain.User;
import org.keycloak.admin.client.resource.GroupsResource;

import java.util.Set;
import java.util.stream.Collectors;

public class KeycloakGroupAdminService {

	private final GroupsResource groupsResource;

	public KeycloakGroupAdminService(GroupsResource groupsResource) {
		this.groupsResource = groupsResource;
	}

	public Set<User> getMembers(GetGroupMembersRequest request) {
		return groupsResource.group(request.getGroupName()).members().stream()
				.map(userRepresentation -> new User(userRepresentation, null))
				.collect(Collectors.toSet());
	}
}
