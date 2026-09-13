package com.maestria.kaffehausv4.Shared.Infrastructure.Security.Keycloak;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.maestria.kaffehausv4.Shared.Application.Security.IIdentidadExternaDatasource;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;
import com.maestria.kaffehausv4.Shared.Domain.Security.KeycloakId;
import com.maestria.kaffehausv4.Shared.Domain.Security.RolDeSistema;

/**
 * Adaptador del puerto IIdentidadExternaDatasource contra la Admin REST API.
 *
 * Anotado con @Repository por coherencia con EntidadDatasourceImpl y
 * ClienteDatasourceImpl: para la capa Application es una fuente de datos mas, solo
 * que esta termina en HTTP en lugar de en JPA.
 *
 * CAPA ANTICORRUPCION: ningun tipo ni error propio de Keycloak sale de esta clase.
 * Los 409 se traducen a DomainException y los fallos de red o de configuracion a
 * SystemException, de modo que GlobalExceptionHandler los serializa con el mismo
 * formato ErrorResponse que el resto de la API. Si manana se sustituye Keycloak por
 * otro proveedor, solo cambia este paquete.
 */
@Repository
public class IdentidadExternaDatasourceImpl implements IIdentidadExternaDatasource {

    private final KeycloakAdminProperties properties;
    private final KeycloakAdminTokenProvider tokenProvider;
    private final RestClient restClient;

    public IdentidadExternaDatasourceImpl(
            KeycloakAdminProperties properties,
            KeycloakAdminTokenProvider tokenProvider) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    // ------------------------------------------------------------------
    // Usuarios
    // ------------------------------------------------------------------

    @Override
    public KeycloakId crearUsuario(String correo, String nombres, String apellidos, String contrasenaTemporal) {
        Map<String, Object> credencial = new HashMap<>();
        credencial.put("type", "password");
        credencial.put("value", contrasenaTemporal);
        credencial.put("temporary", true);

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("username", correo);
        usuario.put("email", correo);
        usuario.put("firstName", nombres);
        usuario.put("lastName", apellidos);
        usuario.put("enabled", true);
        usuario.put("emailVerified", false);
        usuario.put("credentials", List.of(credencial));

        URI ubicacion = ejecutar(() -> restClient.post()
                .uri(url("/users"))
                .headers(this::autorizar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(usuario)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getLocation(),
                "No se pudo crear el usuario '" + correo + "' en el proveedor de identidad",
                "Ya existe un usuario con el correo '" + correo + "'");

        if (ubicacion == null) {
            throw SystemException.throwException(502,
                    "Keycloak no devolvio el identificador del usuario creado");
        }

        return new KeycloakId(extraerIdDeLaUbicacion(ubicacion));
    }

    @Override
    public void actualizarUsuario(KeycloakId usuarioId, String correo, String nombres, String apellidos) {
        Map<String, Object> cambios = new HashMap<>();
        if (correo != null) {
            cambios.put("username", correo);
            cambios.put("email", correo);
        }
        if (nombres != null) cambios.put("firstName", nombres);
        if (apellidos != null) cambios.put("lastName", apellidos);

        if (cambios.isEmpty()) return;

        ejecutarSinRespuesta(() -> restClient.put()
                .uri(url("/users/" + valor(usuarioId)))
                .headers(this::autorizar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cambios)
                .retrieve()
                .toBodilessEntity(),
                "No se pudo actualizar el usuario en el proveedor de identidad",
                "Ya existe otro usuario con el correo '" + correo + "'");
    }

    @Override
    public void cambiarContrasena(KeycloakId usuarioId, String nuevaContrasena) {
        Map<String, Object> credencial = new HashMap<>();
        credencial.put("type", "password");
        credencial.put("value", nuevaContrasena);
        credencial.put("temporary", false);

        ejecutarSinRespuesta(() -> restClient.put()
                .uri(url("/users/" + valor(usuarioId) + "/reset-password"))
                .headers(this::autorizar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(credencial)
                .retrieve()
                .toBodilessEntity(),
                "No se pudo cambiar la contrasena en el proveedor de identidad",
                null);
    }

    @Override
    public void habilitarUsuario(KeycloakId usuarioId) {
        cambiarEstadoHabilitado(usuarioId, true);
    }

    @Override
    public void deshabilitarUsuario(KeycloakId usuarioId) {
        cambiarEstadoHabilitado(usuarioId, false);
    }

    @Override
    public void eliminarUsuario(KeycloakId usuarioId) {
        ejecutarSinRespuesta(() -> restClient.delete()
                .uri(url("/users/" + valor(usuarioId)))
                .headers(this::autorizar)
                .retrieve()
                .toBodilessEntity(),
                "No se pudo eliminar el usuario en el proveedor de identidad",
                null);
    }

    @Override
    public void asignarRol(KeycloakId usuarioId, String nombreRol) {
        Map<String, Object> rol = obtenerRepresentacionDeRol(nombreRol);

        ejecutarSinRespuesta(() -> restClient.post()
                .uri(url("/users/" + valor(usuarioId) + "/role-mappings/realm"))
                .headers(this::autorizar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(rol))
                .retrieve()
                .toBodilessEntity(),
                "No se pudo asignar el rol '" + nombreRol + "' al usuario",
                null);
    }

    @Override
    public void quitarRol(KeycloakId usuarioId, String nombreRol) {
        Map<String, Object> rol = obtenerRepresentacionDeRol(nombreRol);

        ejecutarSinRespuesta(() -> restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(url("/users/" + valor(usuarioId) + "/role-mappings/realm"))
                .headers(this::autorizar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(rol))
                .retrieve()
                .toBodilessEntity(),
                "No se pudo quitar el rol '" + nombreRol + "' al usuario",
                null);
    }

    // ------------------------------------------------------------------
    // Roles de realm
    // ------------------------------------------------------------------

    @Override
    public void crearRol(String nombre) {
        String normalizado = RolDeSistema.normalizar(nombre);

        Map<String, Object> rol = new HashMap<>();
        rol.put("name", normalizado);

        ejecutarSinRespuesta(() -> restClient.post()
                .uri(url("/roles"))
                .headers(this::autorizar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(rol)
                .retrieve()
                .toBodilessEntity(),
                "No se pudo crear el rol '" + normalizado + "' en el proveedor de identidad",
                "Ya existe un rol con el nombre '" + normalizado + "'");
    }

    @Override
    public void renombrarRol(String nombreActual, String nombreNuevo) {
        String actual = RolDeSistema.normalizar(nombreActual);
        String nuevo = RolDeSistema.normalizar(nombreNuevo);

        Map<String, Object> rol = new HashMap<>();
        rol.put("name", nuevo);

        ejecutarSinRespuesta(() -> restClient.put()
                .uri(url("/roles/" + codificar(actual)))
                .headers(this::autorizar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(rol)
                .retrieve()
                .toBodilessEntity(),
                "No se pudo renombrar el rol '" + actual + "'",
                "Ya existe un rol con el nombre '" + nuevo + "'");
    }

    @Override
    public void eliminarRol(String nombre) {
        String normalizado = RolDeSistema.normalizar(nombre);

        ejecutarSinRespuesta(() -> restClient.delete()
                .uri(url("/roles/" + codificar(normalizado)))
                .headers(this::autorizar)
                .retrieve()
                .toBodilessEntity(),
                "No se pudo eliminar el rol '" + normalizado + "'",
                null);
    }

    @Override
    public boolean existeRol(String nombre) {
        String normalizado = RolDeSistema.normalizar(nombre);

        try {
            restClient.get()
                    .uri(url("/roles/" + codificar(normalizado)))
                    .headers(this::autorizar)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false;
            }
            throw traducir(e, "No se pudo consultar el rol '" + normalizado + "'", null);
        } catch (RestClientException e) {
            throw SystemException.throwException(503,
                    "El servicio de identidad no esta disponible");
        }
    }

    // ------------------------------------------------------------------
    // Soporte
    // ------------------------------------------------------------------

    private void cambiarEstadoHabilitado(KeycloakId usuarioId, boolean habilitado) {
        Map<String, Object> cambio = new HashMap<>();
        cambio.put("enabled", habilitado);

        ejecutarSinRespuesta(() -> restClient.put()
                .uri(url("/users/" + valor(usuarioId)))
                .headers(this::autorizar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cambio)
                .retrieve()
                .toBodilessEntity(),
                "No se pudo cambiar el estado del usuario en el proveedor de identidad",
                null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> obtenerRepresentacionDeRol(String nombreRol) {
        String normalizado = RolDeSistema.normalizar(nombreRol);

        Map<String, Object> rol = ejecutar(() -> restClient.get()
                .uri(url("/roles/" + codificar(normalizado)))
                .headers(this::autorizar)
                .retrieve()
                .body(Map.class),
                "No se pudo obtener el rol '" + normalizado + "' del proveedor de identidad",
                null);

        if (rol == null) {
            throw DomainException.throwException(404,
                    "El rol '" + normalizado + "' no existe en el proveedor de identidad");
        }

        // Keycloak exige id y name en el mapeo de roles; se reenvia solo lo necesario.
        Map<String, Object> representacion = new HashMap<>();
        representacion.put("id", rol.get("id"));
        representacion.put("name", rol.get("name"));
        return representacion;
    }

    private void autorizar(HttpHeaders headers) {
        headers.setBearerAuth(tokenProvider.obtenerToken());
    }

    private String url(String ruta) {
        return properties.urlDeAdmin() + ruta;
    }

    private static String valor(KeycloakId id) {
        if (id == null || id.getValue() == null || id.getValue().trim().isEmpty()) {
            throw DomainException.throwException(400,
                    "El identificador del usuario en el proveedor de identidad es obligatorio");
        }
        return id.getValue();
    }

    private static String codificar(String segmento) {
        return java.net.URLEncoder.encode(segmento, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static String extraerIdDeLaUbicacion(URI ubicacion) {
        String ruta = ubicacion.getPath();
        return ruta.substring(ruta.lastIndexOf('/') + 1);
    }

    private <T> T ejecutar(java.util.function.Supplier<T> llamada, String mensajeGenerico, String mensajeConflicto) {
        try {
            return llamada.get();
        } catch (RestClientResponseException e) {
            throw traducir(e, mensajeGenerico, mensajeConflicto);
        } catch (RestClientException e) {
            throw SystemException.throwException(503,
                    "El servicio de identidad no esta disponible");
        }
    }

    private void ejecutarSinRespuesta(Runnable llamada, String mensajeGenerico, String mensajeConflicto) {
        List<Object> ignorado = new ArrayList<>();
        ejecutar(() -> {
            llamada.run();
            return ignorado;
        }, mensajeGenerico, mensajeConflicto);
    }

    /**
     * Traduce el error HTTP de Keycloak a la jerarquia de errores del proyecto, para
     * que la respuesta al cliente conserve el formato ErrorResponse.
     */
    private RuntimeException traducir(RestClientResponseException e, String mensajeGenerico, String mensajeConflicto) {
        int codigo = e.getStatusCode().value();

        if (codigo == 409) {
            return DomainException.throwException(409,
                    mensajeConflicto != null ? mensajeConflicto : mensajeGenerico);
        }

        if (codigo == 404) {
            return DomainException.throwException(404, mensajeGenerico);
        }

        if (codigo == 400) {
            return DomainException.throwException(400, mensajeGenerico);
        }

        if (codigo == 401 || codigo == 403) {
            // El token de servicio pudo caducar o perder permisos: se fuerza su renovacion.
            tokenProvider.invalidar();
            return SystemException.throwException(502,
                    "El backend no tiene permisos suficientes sobre el proveedor de identidad");
        }

        return SystemException.throwException(502, mensajeGenerico);
    }
}
