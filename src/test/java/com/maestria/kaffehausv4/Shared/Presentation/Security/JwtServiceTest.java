package com.maestria.kaffehausv4.Shared.Presentation.Security;

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

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;

import tools.jackson.databind.ObjectMapper;

/**
 * Pruebas del emisor de tokens. No levantan Spring: la clase se construye con sus parametros,
 * que es justo lo que permite fijar vigencias absurdas y comprobar que caducan.
 */
class JwtServiceTest {

    private static final String SECRETO_ACCESO = "secreto-de-acceso-para-pruebas-con-mas-de-32-caracteres";
    private static final String SECRETO_REFRESCO = "secreto-de-refresco-para-pruebas-con-mas-de-32-caracteres";
    private static final String EMISOR = "kaffehausv4-test";

    private static final String ID_USUARIO = "33333333-3333-3333-3333-333333333333";
    private static final String ID_ROL = "11111111-1111-1111-1111-111111111111";

    private JwtService generador() {
        return new JwtService(SECRETO_ACCESO, SECRETO_REFRESCO, EMISOR, 15, 7);
    }

    private UsuarioAutenticado operador() {
        return new UsuarioAutenticado(ID_USUARIO, "Ana Perez", ID_ROL, "Operador");
    }

    /** Lee el payload sin verificar firma: sirve para inspeccionar que claims viajan. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> payloadDe(String token) {
        String cuerpo = token.split("\\.")[1];
        byte[] json = Base64.getUrlDecoder().decode(cuerpo);
        return new ObjectMapper().readValue(new String(json, StandardCharsets.UTF_8), Map.class);
    }

    @Test
    void elAccessTokenLlevaExactamenteElPayloadAcordado() {
        Map<String, Object> payload = payloadDe(generador().emitirAccessToken(operador()));

        assertEquals(ID_USUARIO, payload.get("sub"));
        assertEquals("Ana Perez", payload.get("nombreCompleto"));
        assertEquals(ID_ROL, payload.get("rolId"));
        assertEquals("Operador", payload.get("rolNombre"));

        // Ni un dato de negocio de mas: solo los cuatro campos acordados y los de control del JWT.
        assertEquals(Set.of("sub", "nombreCompleto", "rolId", "rolNombre", "tipo", "iss", "iat", "exp"),
                payload.keySet());
    }

    @Test
    void elAccessTokenVence15MinutosDespuesDeEmitirse() {
        Map<String, Object> payload = payloadDe(generador().emitirAccessToken(operador()));

        long emitido = ((Number) payload.get("iat")).longValue();
        long expira = ((Number) payload.get("exp")).longValue();

        assertEquals(Duration.ofMinutes(15).toSeconds(), expira - emitido);
    }

    @Test
    void elRefreshTokenVence7DiasDespuesDeEmitirse() {
        Map<String, Object> payload = payloadDe(generador().emitirRefreshToken(ID_USUARIO));

        long emitido = ((Number) payload.get("iat")).longValue();
        long expira = ((Number) payload.get("exp")).longValue();

        assertEquals(Duration.ofDays(7).toSeconds(), expira - emitido);
    }

    @Test
    void elRefreshTokenNoLlevaDatosDeNegocio() {
        Map<String, Object> payload = payloadDe(generador().emitirRefreshToken(ID_USUARIO));

        assertEquals(ID_USUARIO, payload.get("sub"));
        assertEquals(Set.of("sub", "tipo", "iss", "iat", "exp"), payload.keySet());
    }

    @Test
    void verificarDevuelveAlMismoUsuarioQueSeEmitio() {
        JwtService generador = generador();

        UsuarioAutenticado verificado = generador.verificarAccessToken(generador.emitirAccessToken(operador()));

        assertEquals(operador(), verificado);
    }

    @Test
    void rechazaUnTokenAlterado() {
        JwtService generador = generador();
        String token = generador.emitirAccessToken(operador());

        // Se manipula el payload para cambiar de rol: la firma deja de cuadrar.
        String[] partes = token.split("\\.");
        String payloadAlterado = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                        .replace("\"rolNombre\":\"Operador\"", "\"rolNombre\":\"Administrad\"")
                        .getBytes(StandardCharsets.UTF_8));
        String falsificado = partes[0] + "." + payloadAlterado + "." + partes[2];

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador.verificarAccessToken(falsificado));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void rechazaUnTokenExpirado() {
        // Vigencia de cero minutos: nace vencido.
        JwtService generadorExpirado = new JwtService(SECRETO_ACCESO, SECRETO_REFRESCO, EMISOR, 0, 7);

        String token = generadorExpirado.emitirAccessToken(operador());

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generadorExpirado.verificarAccessToken(token));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void rechazaUnTokenFirmadoConOtroSecreto() {
        JwtService intruso = new JwtService(
                "otro-secreto-de-acceso-distinto-con-mas-de-32-caracteres", SECRETO_REFRESCO, EMISOR, 15, 7);

        String tokenAjeno = intruso.emitirAccessToken(operador());

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador().verificarAccessToken(tokenAjeno));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void unRefreshTokenNoSirveComoAccessToken() {
        JwtService generador = generador();
        String refresh = generador.emitirRefreshToken(ID_USUARIO);

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador.verificarAccessToken(refresh));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void unAccessTokenNoSirveComoRefreshToken() {
        JwtService generador = generador();
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
                () -> new JwtService("corto", SECRETO_REFRESCO, EMISOR, 15, 7));

        assertEquals(500, error.getCodigo());
    }

    @Test
    void noArrancaSiLosDosSecretosSonElMismo() {
        SystemException error = assertThrows(SystemException.class,
                () -> new JwtService(SECRETO_ACCESO, SECRETO_ACCESO, EMISOR, 15, 7));

        assertEquals(500, error.getCodigo());
        assertTrue(error.getMensaje().contains("distintos"));
    }

    @Test
    void rechazaUnTokenDeOtroEmisor() {
        JwtService otroEmisor = new JwtService(SECRETO_ACCESO, SECRETO_REFRESCO, "otro-sistema", 15, 7);

        String token = otroEmisor.emitirAccessToken(operador());

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador().verificarAccessToken(token));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elInstanteDeEmisionEsElActual() {
        Instant antes = Instant.now().minusSeconds(5);

        Map<String, Object> payload = payloadDe(generador().emitirAccessToken(operador()));
        long emitido = ((Number) payload.get("iat")).longValue();

        assertTrue(emitido >= antes.getEpochSecond());
        assertTrue(emitido <= Instant.now().plusSeconds(5).getEpochSecond());
    }

    // -------------------------------------------------- lectura de claims

    @Test
    void extraerUsuarioIdDevuelveElSubjectDelToken() {
        JwtService generador = generador();

        assertEquals(ID_USUARIO, generador.extraerUsuarioId(generador.emitirAccessToken(operador())));
    }

    @Test
    void extraerExpiracionDevuelveElVencimientoDelToken() {
        JwtService generador = generador();
        Instant antesDeEmitir = Instant.now();

        Instant expiracion = generador.extraerExpiracion(generador.emitirAccessToken(operador()));

        assertTrue(expiracion.isAfter(antesDeEmitir.plus(Duration.ofMinutes(14))));
        assertTrue(expiracion.isBefore(antesDeEmitir.plus(Duration.ofMinutes(16))));
    }

    @Test
    void leerLosClaimsDeUnTokenIlegibleTambienEs401() {
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> generador().extraerUsuarioId("esto-no-es-un-jwt"));

        assertEquals(401, error.getCodigo());
    }

    // -------------------------------------------------- roles leidos de la base

    @Test
    void elRolViajaComoLoGuardaLaBaseSinCatalogoEnElCodigo() {
        // Un rol que no existe hoy en el sistema viaja igual: no hay enum que lo limite.
        UsuarioAutenticado jefeDeTurno = new UsuarioAutenticado(
                ID_USUARIO, "Ana Perez", "99999999-9999-9999-9999-999999999999", "Jefe de turno");

        JwtService generador = generador();
        UsuarioAutenticado verificado = generador.verificarAccessToken(generador.emitirAccessToken(jefeDeTurno));

        assertEquals("Jefe de turno", verificado.rolNombre());
        assertTrue(verificado.tieneAlgunRol("jefe de turno"));
    }
}
