package br.com.anteros.keycloak.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserPersonalDataCommand {

	Optional<String> getPictureUrl();

	Map<String, List<String>> getPersonalData();
}
