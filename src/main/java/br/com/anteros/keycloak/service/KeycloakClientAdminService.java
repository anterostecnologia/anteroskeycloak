package br.com.anteros.keycloak.service;

import br.com.anteros.keycloak.domain.*;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class KeycloakClientAdminService {

	private final ClientsResource clientsResource;

	public KeycloakClientAdminService(ClientsResource clientsResource) {
		this.clientsResource = clientsResource;
	}

	public Set<Client> getClients() {
		return clientsResource.findAll().stream()
				.map(clientRepresentation -> new Client(clientRepresentation.getId(), clientRepresentation.getName(), clientRepresentation.getClientId()))
				.collect(Collectors.toSet());
	}

	public Optional<Client> getClient(String clientId) {
		return clientsResource.findByClientId(clientId).stream()
				.findFirst()
				.map(clientRepresentation -> new Client(clientRepresentation.getId(), clientRepresentation.getName(), clientRepresentation.getClientId()));
	}

	public Set<ClientRole> getClientRoles(Client client) {
		return clientsResource.get(client.getId()).roles().list().stream()
				.map(roleRepresentation -> new ClientRole(roleRepresentation.getName(), roleRepresentation.getId()))
				.collect(Collectors.toSet());
	}

	public void createPublicClient(CreatePublicClient createPublicClient) {
		ClientRepresentation clientRepresentation = new ClientRepresentation();
		clientRepresentation.setClientId(createPublicClient.getClientId());
		clientRepresentation.setProtocol("openid-connect");
		clientRepresentation.setEnabled(true);
		clientRepresentation.setRootUrl(createPublicClient.getRootUrl());
		clientRepresentation.setPublicClient(true);
		clientRepresentation.setRedirectUris(createPublicClient.getRedirectUris());
		clientRepresentation.setWebOrigins(createPublicClient.getWebOrigins());
		clientRepresentation.setStandardFlowEnabled(true);
		clientRepresentation.setImplicitFlowEnabled(false);
		clientRepresentation.setDirectAccessGrantsEnabled(false);
		clientsResource.create(clientRepresentation);
	}

	public void createClientRole(CreateClientRole createClientRole) {
		RoleRepresentation roleRepresentation = new RoleRepresentation();
		roleRepresentation.setName(createClientRole.getRoleName());
		roleRepresentation.setClientRole(true);
		roleRepresentation.setContainerId(createClientRole.getClientContainerId());
		clientsResource.get(createClientRole.getClientContainerId()).roles().create(roleRepresentation);
	}

	public void removeClientRole(RemoveClientRole removeClientRole) {
		clientsResource.get(removeClientRole.getClientContainerId()).roles()
				.deleteRole(removeClientRole.getRoleName());
	}



	public static void main(String[] args) {

		String serverUrl = "https://auth.relevantsolutions.com.br:15001";
		String realm = "cade-moto";
		// idm-client needs to allow "Direct Access Grants: Resource Owner Password Credentials Grant"
		String clientId = "cade-rest-api";
		String username = "cade-admin-api";
		String clientSecret = "vuY4XBrKV5Utin6JWFL6Pgtg6jtzGMoA";
		String password = "Anteros@727204567890";

		// CadeUser "idm-admin" needs at least "manage-users, view-clients, view-realm, view-users" roles for "realm-management"
		Keycloak keycloak = KeycloakBuilder.builder() //
				.serverUrl(serverUrl) //
				.realm(realm) //
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS) //
				.clientId(clientId) //
				.clientSecret(clientSecret) //
				.username(username) //
				.password(password) //
				.build();




		ClientRepresentation clientRep = keycloak
				.realm(realm)
				.clients()
				.findByClientId("cade-moto")
				.get(0);

		RoleRepresentation role = keycloak.realm(realm)
				.clients()
				.get(clientRep.getId())
				.roles().get("cade-store-owner").toRepresentation();



		List<UserRepresentation> users = keycloak
				.realm(realm)
						.users().search("murilogustavo@hotmail.com");

		UserResource userResource = keycloak
				.realm(realm)
				.users()
				.get(users.iterator().next().getId());

		userResource.roles().clientLevel(clientRep.getId()).add(Collections.singletonList(role));

		System.out.println(clientRep);

	}
}
