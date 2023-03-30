package br.com.anteros.keycloak.service;

import br.com.anteros.keycloak.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;


import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.keycloak.admin.client.CreatedResponseUtil.getCreatedId;

public class KeycloakUserAdminService {

	private final UsersResource usersResource;
	private final ClientsResource clientsResource;
	private final RolesResource rolesResource;

	public KeycloakUserAdminService(UsersResource usersResource, ClientsResource clientsResource, RolesResource rolesResource) {
		this.usersResource = usersResource;
		this.clientsResource = clientsResource;
		this.rolesResource = rolesResource;
	}

	public Page<User> getUsers(GetUsersRequest request) {
		Pageable pageable = request.getPageable();
		String clientId = request.getClientId().orElse(null);

		return retrieveUsers(
				() -> usersResource.list(getPage(pageable), getPageSize(pageable)),
				usersResource::count,
				clientId,
				pageable
		);
	}

	public Page<User> searchUsers(SearchUserRequest request) {
		Pageable pageable = request.getPageable();
		String clientId = request.getClientId().orElse(null);

		return retrieveUsers(
				() -> usersResource.search(request.getSearch(), getPage(pageable), getPageSize(pageable)),
				() -> usersResource.count(request.getSearch()),
				clientId,
				pageable
		);


	}

	public Set<User> searchUsersByRealmRole(SearchUserByRealmRoleRequest request) {
		int page = 0;
		int pageSize = 100;
		int currentPageSize;
		Set<UserRepresentation> roleUserMembers = new HashSet<>();

		do {
			Pageable pageable = PageRequest.of(page, pageSize);
			Set<UserRepresentation> currentPage = rolesResource.get(request.getRoleName()).getRoleUserMembers(getPage(pageable), getPageSize(pageable));
			roleUserMembers.addAll(currentPage);
			currentPageSize = currentPage.size();
			page++;
		} while(currentPageSize == pageSize);

		return roleUserMembers.stream()
				.map(this::enhanceWithRealmRoles)
				.map(roleUserMember -> new User(roleUserMember, null))
				.collect(Collectors.toSet());
	}

	public User getUser(GetUserRequest request) {
		String clientId = request.getClientId().orElse(null);
		UserResource userResource = usersResource.get(request.getUserId());
		UserRepresentation userRepresentation = enhanceWithClientRoles(userResource.toRepresentation(), clientId);

		return new User(userRepresentation, clientId);
	}

	public String createUser(CreateUserCommand command) {
		CredentialRepresentation credentials = new CredentialRepresentation();
		credentials.setType(CredentialRepresentation.PASSWORD);
		credentials.setValue(command.getPassword());
		credentials.setTemporary(false);
		UserRepresentation userRepresentation = new UserRepresentation();
		userRepresentation.setEnabled(command.isEnabled());
		userRepresentation.setUsername(command.getUsername());
		userRepresentation.setFirstName(command.getFirstName());
		userRepresentation.setLastName(command.getLastName());
		userRepresentation.setEmail(command.getEmail());
		userRepresentation.setAttributes(getPersonalData(command));
		userRepresentation.setRequiredActions(getActions(command.getRequiredUserActions()));
		userRepresentation.setCredentials(Collections.singletonList(credentials));

		return getCreatedId(usersResource.create(userRepresentation));
	}

	/**
	 * Does a PATCH of an existing CadeUser.
	 * Only following attributes will be overridden:
	 * - pictureUrl
	 * - personalData
	 * - requiredUserActions
	 * <p>
	 * Username can only be updated when enabled in the realm (editUsernameAllowed).
	 * Otherwise this will be silently discarded, no update on this field.
	 */
	public void updateUser(UpdateUserCommand command) {
		Optional<CredentialRepresentation> credentials = Optional.empty();
		if (command.getPassword().isPresent()) {
			credentials = Optional.of(new CredentialRepresentation());
			credentials.get().setType(CredentialRepresentation.PASSWORD);
			credentials.get().setValue(command.getPassword().get());
			credentials.get().setTemporary(false);
		}
		UserRepresentation userRepresentation = new UserRepresentation();
		command.getEnabled().ifPresent(userRepresentation::setEnabled);
		command.getUsername().ifPresent(userRepresentation::setUsername);
		command.getFirstName().ifPresent(userRepresentation::setFirstName);
		command.getLastName().ifPresent(userRepresentation::setLastName);
		command.getEmail().ifPresent(userRepresentation::setEmail);
		userRepresentation.setAttributes(getPersonalData(command));
		userRepresentation.setRequiredActions(getActions(command.getRequiredUserActions()));
		if (credentials.isPresent()) {
			userRepresentation.setCredentials(Collections.singletonList(credentials.get()));
		}

		UserResource userResource = usersResource.get(command.getUserId());

		userResource.update(userRepresentation);


	}

	/**
	 * Sends emails for the specified user actions to the CadeUser.
	 *
	 * @throws javax.ws.rs.InternalServerErrorException when SMTP is not configured or accessible.
	 */
	public void sendUserActionEmails(SendUserActionEmailRequest request) {
		UserResource userResource = usersResource.get(request.getUserId());

		userResource.executeActionsEmail(getActions(request.getUserActions()));
	}

	public void addRealmRoleToUser(AddRealmRoleToUserCommand command) {
		RoleRepresentation realmRole = rolesResource.get(command.getRoleName())
				.toRepresentation();
		usersResource.get(command.getUserId()).roles().realmLevel().add(singletonList(realmRole));
	}

	public void addClientRoleToUser(AddClientRoleToUserCommand command) {

		ClientResource clientResource = clientsResource.get(command.getClientId());
		RolesResource roles = clientResource.roles();
		RoleResource roleResource = roles.get(command.getRoleName());

		RoleRepresentation clientRole = roleResource.toRepresentation();

		usersResource.get(command.getUserId())
				.roles()
				.clientLevel(command.getClientId())
				.add(singletonList(clientRole));
	}

	public void removeClientRoleFromUser(RemoveClientRoleFromUserCommand command) {
		RoleRepresentation clientRole = clientsResource.get(command.getClientId())
				.roles()
				.get(command.getRoleName())
				.toRepresentation();

		usersResource.get(command.getUserId())
				.roles()
				.clientLevel(command.getClientId())
				.remove(singletonList(clientRole));
	}

	public void deleteUser(String userId) {
		UserResource user = usersResource.get(userId);

		user.remove();
	}

	private int getPage(Pageable pageable) {
		return pageable.isPaged()
				? (int) pageable.getOffset()
				: 0;
	}

	private int getPageSize(Pageable pageable) {
		return pageable.isPaged()
				? pageable.getPageSize()
				: Integer.MAX_VALUE;
	}

	private Page<User> retrieveUsers(Supplier<List<UserRepresentation>> userRepresentations, IntSupplier userCount, String clientId, Pageable pageable) {
		List<User> users = userRepresentations.get()
				.stream()
				.map(userRepresentation -> enhanceWithClientRoles(userRepresentation, clientId))
				.map(userRepresentation -> new User(userRepresentation, clientId))
				.collect(toList());
		int total = userCount.getAsInt();

		return new PageImpl<>(users, pageable, total);
	}

	private UserRepresentation enhanceWithRealmRoles(UserRepresentation userRepresentation) {
		UserResource userResource = usersResource.get(userRepresentation.getId());
		List<RoleRepresentation> roleRepresentations = userResource.roles().realmLevel().listEffective();
		UserRepresentation userWithRoles = userResource.toRepresentation();

		userWithRoles.setRealmRoles(
				roleRepresentations.stream()
						.map((RoleRepresentation::getName))
						.collect(Collectors.toUnmodifiableList())
		);

		return userWithRoles;
	}


	private UserRepresentation enhanceWithClientRoles(UserRepresentation userRepresentation, String clientId) {
		if (StringUtils.isBlank(clientId)) {
			return userRepresentation;
		}

		UserResource userResource = usersResource.get(userRepresentation.getId());
		List<RoleRepresentation> clientRoles = userResource.roles().clientLevel(clientId).listEffective();
		UserRepresentation userWithRoles = userResource.toRepresentation();

		userWithRoles.setClientRoles(
				Map.of(clientId, clientRoles.stream()
						.map(RoleRepresentation::getName)
						.collect(Collectors.toList()))
		);

		return userWithRoles;
	}

	private Map<String, List<String>> getPersonalData(UserPersonalDataCommand command) {
		Map<String, List<String>> personalData = new HashMap<>();

		if (command.getPersonalData() != null) {
			personalData.putAll(command.getPersonalData());
		}

		command.getPictureUrl()
				.ifPresent(pictureUrl -> personalData.put(User.PICTURE_URL_ATTRIBUTE, singletonList(pictureUrl)));

		return personalData;
	}

	private List<String> getActions(Set<UserAction> userActions) {
		if (userActions == null) {
			return emptyList();
		}

		return userActions
				.stream()
				.map(UserAction::name)
				.collect(toList());
	}
}
