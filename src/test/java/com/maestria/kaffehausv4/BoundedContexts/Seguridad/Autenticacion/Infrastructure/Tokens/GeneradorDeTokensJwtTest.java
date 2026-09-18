package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.UsuarioAutenticado;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;

/**
 * Pruebas del emisor de tokens. No levantan Spring: la clase se construye con sus parametros,
 * que es justo lo que permite fijar vigencias absurdas y comprobar que caducan.
 */
class GeneradorDeTokensJwtTest {

    private static final String SECRETO_ACCESO = "secreto-de-acceso-para-pruebas-con-mas-de-32-caracteres";
    private static final String SECRETO_REFRESCO = "secreto-de-refresco-para-pruebas-con-mas-de-32-caracteres";
    private static final String EMISOR = "kaffehausv4-test";

    private static final String ID_USUARIO = "33333333-3333-3333-3333-333333333333";

    private GeneradorDeTokensJwt generador() {
        return new GeneradorDeTokensJwt(SECRETO_ACCESO, SECRETO_REFRESCO, EMISOR, 15, 7);
    }

    private UsuarioAutenticado operador() {
        return new UsuarioAutenticado(ID_USUARIO, "Ana Perez", 1, "Operador");
    }

    /** Lee el payload sin verificar firma: sirve para inspeccionar que claims viajan. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> payloadDe(String token) throws Exception {
        String cuerpo = token.split("\\.")[1];
        byte[] json = Base64.getUrlDecoder().decode(cuerpo);
        return new ObjectMapper().readValue(new String(json, StandardCharsets.UTF_8), Map.class);
    }

    @Test
    void elAccessTokenLlevaExactamenteElPayloadAcordado() throws Exception {
        String token = generador().emitirAccessToken(operador());

        Map<String, Object> payload = payloadDe(token);

        assertEquals(ID_USUARIO, payload.get("sub"));
        assertEquals("Ana Perez", payload.get("nombreCompleto"));
        assertEquals(1, payload.get("rolId"));
        assertEquals("Operador", payload.get("rolNombre"));

        // Ni un dato de negocio de mas: solo los cuatro campos acordados y los de control del JWT.
        assertEquals(Set.of("sub", "nombreCompleto", "rolId", "rolNombre", "tipo", "iss", "iat", "exp"),
                payload.keySet());
    }

    @Test
    void elAccessTokenVence15MinutosDespuesDeEmitirse() throws Exception {
        Map<String, Object> payload = payloadDe(generador().emitirAccessToken(operador()));

        long emitido = ((Number) payload.get("iat")).longValue();
        long expira = ((Number) payload.get("exp")).longValue();

        assertEquals(Duration.ofMinutes(15).toSeconds(), expira - emitido);
    }

    @Test
    void elRefreshTokenVence7DiasDespuesDeEmitirse() throws Exception {
        Map<String, Object> payload = payloadDe(generador().emitirRefreshToken(ID_USUARIO));

        long emitido = ((Number) payload.get("iat")).longValue();
        long expira = ((Number) payload.get("exp")).longValue();

        assertEquals(Duration.ofDays(7).toSeconds(), expira - emitido);
    }

    @Test
    void elRefreshTokenNoLlevaDatosDeNegocio() throws Exception {
        Map<String, Object> payload = payloadDe(generador().emitirRefreshToken(ID_USUARIO));

        assertEquals(ID_USUARIO, payload.get("sub"));
        assertEquals(Set.of("sub", "tipo", "iss", "iat", "exp"), payload.keySet());
    }

    @Test
    void verificarDevuelveAlMismoUsuarioQueSeEmitio() {
        GeneradorDeTokensJwt generador = generador();

        UsuarioAutenticado verificado = generador.verificarAccessToken(generador.emitirAccessToken(operador()));

        assertEquals(ID_USUARIO, verificado.getUsuarioId());
        assertEquals("Ana Perez", verificado.getNombreCompleto());
        assertEquals(1, verificado.getRolId());
        assertEquals("Operador", verificado.getRolNombre());
    }

    @Test
    void rechazaUnTokenAlterado() {
        GeneradorDeTokensJwt generador = generador();
        String token = generador.emitirAccessToken(operador());

        // Se manipula el payload para escalar de Operador a Administrador: la firma deja de cuadrar.
        String[] partes = token.split("\\.");
        String payloadAlterado = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                        .replace("\"rolId\":1", "\"rolId\":2")
                        .getBytes(StandardCharsets.UTF_8));
        String falsificado = partes[0] + "." + payloadAlterado + "." + partes[2];

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador.verificarAccessToken(falsificado));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void rechazaUnTokenExpirado() {
        // Vigencia de cero minutos: nace vencido.
        GeneradorDeTokensJwt generadorExpirado =
                new GeneradorDeTokensJwt(SECRETO_ACCESO, SECRETO_REFRESCO, EMISOR, 0, 7);

        String token = generadorExpirado.emitirAccessToken(operador());

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generadorExpirado.verificarAccessToken(token));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void rechazaUnTokenFirmadoConOtroSecreto() {
        GeneradorDeTokensJwt intruso = new GeneradorDeTokensJwt(
                "otro-secreto-de-acceso-distinto-con-mas-de-32-caracteres", SECRETO_REFRESCO, EMISOR, 15, 7);

        String tokenAjeno = intruso.emitirAccessToken(operador());

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador().verificarAccessToken(tokenAjeno));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void unRefreshTokenNoSirveComoAccessToken() {
        GeneradorDeTokensJwt generador = generador();
        String refresh = generador.emitirRefreshToken(ID_USUARIO);

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador.verificarAccessToken(refresh));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void unAccessTokenNoSirveComoRefreshToken() {
        GeneradorDeTokensJwt generador = generador();
        String access = generador.emitirAccessToken(operador());

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador.verificarRefreshToken(access));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void rechazaUnaCabeceraSinToken() {
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador().verificarAccessToken("   "));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void noArrancaConUnSecretoDemasiadoCorto() {
        SystemException error = assertThrows(SystemException.class,
                () -> new GeneradorDeTokensJwt("corto", SECRETO_REFRESCO, EMISOR, 15, 7));

        assertEquals(500, error.getCodigo());
    }

    @Test
    void noArrancaSiLosDosSecretosSonElMismo() {
        SystemException error = assertThrows(SystemException.class,
                () -> new GeneradorDeTokensJwt(SECRETO_ACCESO, SECRETO_ACCESO, EMISOR, 15, 7));

        assertEquals(500, error.getCodigo());
        assertTrue(error.getMensaje().contains("distintos"));
    }

    @Test
    void rechazaUnTokenDeOtroEmisor() {
        GeneradorDeTokensJwt otroEmisor =
                new GeneradorDeTokensJwt(SECRETO_ACCESO, SECRETO_REFRESCO, "otro-sistema", 15, 7);

        String token = otroEmisor.emitirAccessToken(operador());

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador().verificarAccessToken(token));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elInstanteDeEmisionEsElActual() throws Exception {
        Instant antes = Instant.now().minusSeconds(5);

        Map<String, Object> payload = payloadDe(generador().emitirAccessToken(operador()));
        long emitido = ((Number) payload.get("iat")).longValue();

        assertTrue(emitido >= antes.getEpochSecond());
        assertTrue(emitido <= Instant.now().plusSeconds(5).getEpochSecond());
    }
}
