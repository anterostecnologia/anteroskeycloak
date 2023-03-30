package br.com.anteros.keycloak.domain;

public class ClientRole {

	private final String roleName;
	private final String roleId;

	public ClientRole(String roleName, String roleId) {
		this.roleName = roleName;
		this.roleId = roleId;
	}

	public String getRoleId() {
		return roleId;
	}

	public String getRoleName() {
		return roleName;
	}
}
