package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.AutenticacionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.IniciarSesionConGoogleRequest;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.IniciarSesionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.RenovarTokenRequest;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Tokens.IGeneradorDeTokens;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Tokens.IVerificadorDeTokenGoogle;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.UseCases.IniciarSesionConGoogleUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.UseCases.IniciarSesionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.UseCases.RenovarAccessTokenUseCase;
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

/**
 * Pruebas de los tres flujos de autenticacion contra la base real.
 *
 * El verificador de Google se sustituye por un doble: la prueba no puede firmar un id_token
 * valido de Google, y lo que interesa verificar aqui no es la criptografia de Google (eso lo
 * cubre su libreria) sino la regla del proyecto: que un correo verificado sin usuario
 * registrado termine en 401 y no provisione a nadie.
 */
@SpringBootTest
class AutenticacionIntegracionTest {

    /** Correo que el doble de Google devolvera como verificado en cada prueba. */
    static final ThreadLocal<String> CORREO_DE_GOOGLE = new ThreadLocal<>();

    @TestConfiguration
    static class VerificadorDeGoogleDePrueba {
        @Bean
        @Primary
        IVerificadorDeTokenGoogle verificadorDeTokenGoogleDePrueba() {
            return googleToken -> {
                if (!"token-valido".equals(googleToken)) {
                    throw AutorizacionException.throwException(401, "El token de Google no es valido");
                }
                return CORREO_DE_GOOGLE.get();
            };
        }
    }

    @Autowired private IniciarSesionUseCase iniciarSesionUseCase;
    @Autowired private IniciarSesionConGoogleUseCase iniciarSesionConGoogleUseCase;
    @Autowired private RenovarAccessTokenUseCase renovarAccessTokenUseCase;
    @Autowired private IGeneradorDeTokens generadorDeTokens;
    @Autowired private IHasheadorDeContrasena hasheadorDeContrasena;

    @Autowired private UsuarioJpaRepository usuarioJpaRepository;
    @Autowired private PersonaJpaRepository personaJpaRepository;
    @Autowired private RolJpaRepository rolJpaRepository;

    private static final String CONTRASENA = "Contrasena-de-prueba-123";

    private UUID idRolOperador;
    private UUID idRolAdministrador;
    private UUID idPersona;
    private UUID idUsuario;
    private String correo;

    @BeforeEach
    void sembrarDatos() {
        idRolOperador = guardarRol("Operador");
        idRolAdministrador = guardarRol("Administrador");

        idPersona = UUID.randomUUID();
        PersonaJpaModel persona = new PersonaJpaModel();
        persona.setId(idPersona);
        persona.setNombre("Ana");
        persona.setApellidos("Perez Lopez");
        persona.setTelefono("70000000");
        persona.setDni("TEST-" + idPersona.toString().substring(0, 8));
        persona.setFecC(OffsetDateTime.now());
        persona.setBorrado(false);
        personaJpaRepository.save(persona);

        idUsuario = UUID.randomUUID();
        correo = idUsuario + "@test.local";
        guardarUsuario(idUsuario, correo, idRolOperador, false);

        CORREO_DE_GOOGLE.set(correo);
    }

    /** Reutiliza el rol si ya existe con ese nombre: los dos roles del sistema son datos fijos. */
    private UUID guardarRol(String nombre) {
        return rolJpaRepository.findAll().stream()
                .filter(rol -> nombre.equals(rol.getNombre()) && !Boolean.TRUE.equals(rol.getBorrado()))
                .map(RolJpaModel::getId)
                .findFirst()
                .orElseGet(() -> {
                    UUID id = UUID.randomUUID();
                    RolJpaModel rol = new RolJpaModel();
                    rol.setId(id);
                    rol.setNombre(nombre);
                    rol.setFecC(OffsetDateTime.now());
                    rol.setBorrado(false);
                    rolJpaRepository.save(rol);
                    return id;
                });
    }

    private void guardarUsuario(UUID id, String correoUsuario, UUID idRol, boolean borrado) {
        UsuarioJpaModel usuario = new UsuarioJpaModel();
        usuario.setId(id);
        usuario.setIdPersona(idPersona);
        usuario.setCorreo(correoUsuario);
        usuario.setContrasena(hasheadorDeContrasena.hashear(CONTRASENA));
        usuario.setIdRol(idRol);
        usuario.setFecC(OffsetDateTime.now());
        usuario.setBorrado(borrado);
        usuarioJpaRepository.save(usuario);
    }

    @AfterEach
    void limpiarDatos() {
        usuarioJpaRepository.findAll().stream()
                .filter(usuario -> usuario.getCorreo() != null && usuario.getCorreo().endsWith("@test.local"))
                .forEach(usuario -> usuarioJpaRepository.deleteById(usuario.getId()));
        personaJpaRepository.deleteById(idPersona);
        CORREO_DE_GOOGLE.remove();
    }

    private IniciarSesionRequest credenciales(String email, String password) {
        return IniciarSesionRequest.createFromRequest(Map.of("email", email, "password", password));
    }

    // ------------------------------------------------------------------ login clasico

    @Test
    void elLoginClasicoDevuelveUsuarioYLosDosTokens() {
        AutenticacionResponse respuesta = iniciarSesionUseCase.execute(credenciales(correo, CONTRASENA));

        assertEquals(idUsuario.toString(), respuesta.getUsuario().getId());
        assertEquals(correo, respuesta.getUsuario().getEmail());
        assertEquals("Ana Perez Lopez", respuesta.getUsuario().getNombreCompleto());
        assertEquals(1, respuesta.getUsuario().getRol().getId());
        assertEquals("Operador", respuesta.getUsuario().getRol().getNombre());
        assertNotNull(respuesta.getAccessToken());
        assertNotNull(respuesta.getRefreshToken());
        assertNotEquals(respuesta.getAccessToken(), respuesta.getRefreshToken());
    }

    @Test
    void elLoginClasicoRechazaUnaContrasenaIncorrecta() {
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> iniciarSesionUseCase.execute(credenciales(correo, "contrasena-equivocada")));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elLoginClasicoRechazaUnCorreoInexistenteConElMismoMensaje() {
        AutorizacionException inexistente = assertThrows(AutorizacionException.class,
                () -> iniciarSesionUseCase.execute(credenciales("nadie@test.local", CONTRASENA)));
        AutorizacionException contrasenaMala = assertThrows(AutorizacionException.class,
                () -> iniciarSesionUseCase.execute(credenciales(correo, "contrasena-equivocada")));

        assertEquals(401, inexistente.getCodigo());
        // Mismo mensaje: el login no debe revelar que correos estan registrados.
        assertEquals(contrasenaMala.getMensaje(), inexistente.getMensaje());
    }

    @Test
    void elLoginClasicoRechazaAUnUsuarioDadoDeBaja() {
        UUID idBorrado = UUID.randomUUID();
        String correoBorrado = idBorrado + "@test.local";
        guardarUsuario(idBorrado, correoBorrado, idRolOperador, true);

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> iniciarSesionUseCase.execute(credenciales(correoBorrado, CONTRASENA)));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elAccessTokenDelLoginIdentificaAlUsuarioYSuRol() {
        AutenticacionResponse respuesta = iniciarSesionUseCase.execute(credenciales(correo, CONTRASENA));

        var autenticado = generadorDeTokens.verificarAccessToken(respuesta.getAccessToken());

        assertEquals(idUsuario.toString(), autenticado.getUsuarioId());
        assertEquals("Ana Perez Lopez", autenticado.getNombreCompleto());
        assertEquals(1, autenticado.getRolId());
        assertEquals("Operador", autenticado.getRolNombre());
    }

    @Test
    void elLoginDeUnAdministradorLlevaRolId2() {
        UUID idAdmin = UUID.randomUUID();
        String correoAdmin = idAdmin + "@test.local";
        guardarUsuario(idAdmin, correoAdmin, idRolAdministrador, false);

        AutenticacionResponse respuesta = iniciarSesionUseCase.execute(credenciales(correoAdmin, CONTRASENA));

        assertEquals(2, respuesta.getUsuario().getRol().getId());
        assertEquals("Administrador", respuesta.getUsuario().getRol().getNombre());
    }

    // ------------------------------------------------------------------ login con Google

    @Test
    void elLoginConGoogleDevuelveLaMismaEstructuraQueElClasico() {
        AutenticacionResponse porGoogle = iniciarSesionConGoogleUseCase.execute(
                IniciarSesionConGoogleRequest.createFromRequest(Map.of("googleToken", "token-valido")));
        AutenticacionResponse clasico = iniciarSesionUseCase.execute(credenciales(correo, CONTRASENA));

        assertEquals(clasico.getUsuario().getId(), porGoogle.getUsuario().getId());
        assertEquals(clasico.getUsuario().getEmail(), porGoogle.getUsuario().getEmail());
        assertEquals(clasico.getUsuario().getNombreCompleto(), porGoogle.getUsuario().getNombreCompleto());
        assertEquals(clasico.getUsuario().getRol().getId(), porGoogle.getUsuario().getRol().getId());
        assertNotNull(porGoogle.getAccessToken());
        assertNotNull(porGoogle.getRefreshToken());
    }

    @Test
    void elLoginConGoogleRechazaUnCorreoQueNoEstaRegistradoYNoCreaUsuarios() {
        CORREO_DE_GOOGLE.set("desconocido@gmail.com");
        long usuariosAntes = usuarioJpaRepository.count();

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> iniciarSesionConGoogleUseCase.execute(
                        IniciarSesionConGoogleRequest.createFromRequest(Map.of("googleToken", "token-valido"))));

        assertEquals(401, error.getCodigo());
        // La regla estricta: no se provisiona nadie.
        assertEquals(usuariosAntes, usuarioJpaRepository.count());
    }

    @Test
    void elLoginConGoogleRechazaUnTokenInvalido() {
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> iniciarSesionConGoogleUseCase.execute(
                        IniciarSesionConGoogleRequest.createFromRequest(Map.of("googleToken", "token-falso"))));

        assertEquals(401, error.getCodigo());
    }

    // ------------------------------------------------------------------ refresh

    @Test
    void elRefreshDevuelveUnAccessTokenNuevoYValido() {
        AutenticacionResponse login = iniciarSesionUseCase.execute(credenciales(correo, CONTRASENA));

        String nuevoAccessToken = renovarAccessTokenUseCase.execute(
                RenovarTokenRequest.createFromRequest(Map.of("refreshToken", login.getRefreshToken())))
                .getAccessToken();

        assertNotNull(nuevoAccessToken);
        var autenticado = generadorDeTokens.verificarAccessToken(nuevoAccessToken);
        assertEquals(idUsuario.toString(), autenticado.getUsuarioId());
        assertEquals(1, autenticado.getRolId());
    }

    @Test
    void elRefreshRechazaUnAccessTokenEnLugarDelRefreshToken() {
        AutenticacionResponse login = iniciarSesionUseCase.execute(credenciales(correo, CONTRASENA));

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> renovarAccessTokenUseCase.execute(
                        RenovarTokenRequest.createFromRequest(Map.of("refreshToken", login.getAccessToken()))));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elRefreshDejaDeFuncionarSiElUsuarioEsDadoDeBaja() {
        AutenticacionResponse login = iniciarSesionUseCase.execute(credenciales(correo, CONTRASENA));

        UsuarioJpaModel usuario = usuarioJpaRepository.findById(idUsuario).orElseThrow();
        usuario.setBorrado(true);
        usuarioJpaRepository.save(usuario);

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> renovarAccessTokenUseCase.execute(
                        RenovarTokenRequest.createFromRequest(Map.of("refreshToken", login.getRefreshToken()))));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elRefreshRecogeElCambioDeRolSinEsperarAQueCaduque() {
        AutenticacionResponse login = iniciarSesionUseCase.execute(credenciales(correo, CONTRASENA));
        assertEquals(1, generadorDeTokens.verificarAccessToken(login.getAccessToken()).getRolId());

        UsuarioJpaModel usuario = usuarioJpaRepository.findById(idUsuario).orElseThrow();
        usuario.setIdRol(idRolAdministrador);
        usuarioJpaRepository.save(usuario);

        String nuevoAccessToken = renovarAccessTokenUseCase.execute(
                RenovarTokenRequest.createFromRequest(Map.of("refreshToken", login.getRefreshToken())))
                .getAccessToken();

        assertEquals(2, generadorDeTokens.verificarAccessToken(nuevoAccessToken).getRolId());
    }
}
