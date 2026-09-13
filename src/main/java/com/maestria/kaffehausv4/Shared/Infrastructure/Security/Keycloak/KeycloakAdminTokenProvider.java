package com.maestria.kaffehausv4.Shared.Infrastructure.Security.Keycloak;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;

/**
 * Obtiene y cachea el token de servicio con el que el backend llama a la Admin API.
 *
 * Usa el flujo client_credentials del client confidencial configurado. El token se
 * reutiliza hasta poco antes de expirar: pedir uno nuevo en cada peticion es el
 * error de rendimiento clasico en esta integracion.
 */
@Component
public class KeycloakAdminTokenProvider {

    private final KeycloakAdminProperties properties;
    private final RestClient restClient;

    private volatile String tokenEnCache;
    private volatile Instant expiraEn = Instant.EPOCH;

    public KeycloakAdminTokenProvider(KeycloakAdminProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public synchronized String obtenerToken() {
        if (!properties.estaConfigurado()) {
            throw SystemException.throwException(503,
                    "La integracion administrativa con Keycloak no esta configurada");
        }

        if (tokenEnCache != null && Instant.now().isBefore(expiraEn)) {
            return tokenEnCache;
        }

        return solicitarTokenNuevo();
    }

    /** Fuerza la renovacion en el siguiente uso, por ejemplo tras recibir un 401. */
    public synchronized void invalidar() {
        this.tokenEnCache = null;
        this.expiraEn = Instant.EPOCH;
    }

    @SuppressWarnings("unchecked")
    private String solicitarTokenNuevo() {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "client_credentials");
        formulario.add("client_id", properties.getClientId());
        formulario.add("client_secret", properties.getClientSecret());

        Map<String, Object> respuesta;
        try {
            respuesta = restClient.post()
                    .uri(properties.urlDeToken())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw SystemException.throwException(503,
                    "No se pudo obtener el token de servicio de Keycloak");
        }

        if (respuesta == null || !(respuesta.get("access_token") instanceof String)) {
            throw SystemException.throwException(503,
                    "Keycloak devolvio una respuesta de token no valida");
        }

        this.tokenEnCache = (String) respuesta.get("access_token");
        this.expiraEn = calcularExpiracion(respuesta.get("expires_in"));

        return tokenEnCache;
    }

    private Instant calcularExpiracion(Object expiresIn) {
        long segundos = (expiresIn instanceof Number) ? ((Number) expiresIn).longValue() : 60L;
        long margen = properties.getMargenRenovacionToken().getSeconds();
        long vigencia = Math.max(segundos - margen, 5L);

        return Instant.now().plusSeconds(vigencia);
    }
}
