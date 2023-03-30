package br.com.anteros.keycloak.domain;

public class Client {

	private final String id;
	private final String clientName;
	private final String clientId;

	public Client(String id, String clientName, String clientId) {
		this.id = id;
		this.clientName = clientName;
		this.clientId = clientId;
	}

	public String getId() {
		return id;
	}

	public String getClientName() {
		return clientName;
	}

	public String getClientId() {
		return clientId;
	}
}
