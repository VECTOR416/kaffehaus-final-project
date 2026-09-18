package com.maestria.kaffehausv4.Shared.Presentation.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
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
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.AutenticacionResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.IniciarSesionConGoogleRequest;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.IniciarSesionRequest;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.RenovarTokenRequest;

/**
 * Pruebas de los tres flujos de autenticacion contra la base real.
 *
 * Comprueban ademas la premisa del diseno: el servicio no tiene persistencia propia, lee los
 * mismos datos que escriben los CRUD de Usuario, Persona y Rol. Por eso la siembra se hace con
 * los JpaRepository de esos agregados y el rol que acaba en el token es, literalmente, la fila
 * de la tabla 'rol'.
 *
 * El verificador de Google se sustituye por un doble: la prueba no puede firmar un id_token
 * valido, y lo que interesa verificar no es la criptografia de Google (eso lo cubre su
 * libreria) sino la regla del proyecto: que un correo verificado sin usuario registrado termine
 * en 401 y no provisione a nadie.
 */
@SpringBootTest
class AutenticacionServiceIntegracionTest {

    /** Correo que el doble de Google devolvera como verificado en cada prueba. */
    static final ThreadLocal<String> CORREO_DE_GOOGLE = new ThreadLocal<>();

    @TestConfiguration
    static class VerificadorDeGoogleDePrueba {
        @Bean
        @Primary
        VerificadorDeTokenGoogle verificadorDeTokenGoogleDePrueba() {
            return new VerificadorDeTokenGoogle("") {
                @Override
                public String obtenerCorreoVerificado(String googleToken) {
                    if (!"token-valido".equals(googleToken)) {
                        throw AutorizacionException.throwException(401, "El token de Google no es valido");
                    }
                    return CORREO_DE_GOOGLE.get();
                }
            };
        }
    }

    @Autowired private AutenticacionService autenticacionService;
    @Autowired private JwtService jwtService;
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

    /** Reutiliza el rol si ya existe con ese nombre: los roles son datos del sistema. */
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

    private AutenticacionResponse login(String email, String password) {
        return autenticacionService.iniciarSesion(new IniciarSesionRequest(email, password));
    }

    // ------------------------------------------------------------------ login clasico

    @Test
    void elLoginDevuelveLosDatosDelUsuarioYLosDosTokens() {
        AutenticacionResponse respuesta = login(correo, CONTRASENA);

        assertEquals(idUsuario.toString(), respuesta.usuario().usuarioId());
        assertEquals("Ana Perez Lopez", respuesta.usuario().nombreCompleto());
        assertEquals(idRolOperador.toString(), respuesta.usuario().rolId());
        assertEquals("Operador", respuesta.usuario().rolNombre());
        assertNotNull(respuesta.accessToken());
        assertNotNull(respuesta.refreshToken());
        assertNotEquals(respuesta.accessToken(), respuesta.refreshToken());
    }

    @Test
    void elRolDelTokenEsLaFilaDeLaTablaRol() {
        AutenticacionResponse respuesta = login(correo, CONTRASENA);

        UsuarioAutenticado autenticado = jwtService.verificarAccessToken(respuesta.accessToken());

        RolJpaModel rolEnLaBase = rolJpaRepository.findById(idRolOperador).orElseThrow();
        assertEquals(rolEnLaBase.getId().toString(), autenticado.rolId());
        assertEquals(rolEnLaBase.getNombre(), autenticado.rolNombre());
    }

    @Test
    void elAccessTokenDelLoginIdentificaAlUsuarioYSuRol() {
        AutenticacionResponse respuesta = login(correo, CONTRASENA);

        UsuarioAutenticado autenticado = jwtService.verificarAccessToken(respuesta.accessToken());

        assertEquals(respuesta.usuario(), autenticado);
    }

    @Test
    void elLoginRechazaUnaContrasenaIncorrecta() {
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> login(correo, "contrasena-equivocada"));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elLoginRechazaUnCorreoInexistenteConElMismoMensaje() {
        AutorizacionException inexistente = assertThrows(AutorizacionException.class,
                () -> login("nadie@test.local", CONTRASENA));
        AutorizacionException contrasenaMala = assertThrows(AutorizacionException.class,
                () -> login(correo, "contrasena-equivocada"));

        assertEquals(401, inexistente.getCodigo());
        // Mismo mensaje: el login no debe revelar que correos estan registrados.
        assertEquals(contrasenaMala.getMensaje(), inexistente.getMensaje());
    }

    @Test
    void elLoginRechazaAUnUsuarioDadoDeBaja() {
        UUID idBorrado = UUID.randomUUID();
        String correoBorrado = idBorrado + "@test.local";
        guardarUsuario(idBorrado, correoBorrado, idRolOperador, true);

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> login(correoBorrado, CONTRASENA));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elLoginExigeLasCredencialesCompletas() {
        DomainException error = assertThrows(DomainException.class, () -> login(null, CONTRASENA));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void elLoginDeUnAdministradorLlevaSuPropioRol() {
        UUID idAdmin = UUID.randomUUID();
        String correoAdmin = idAdmin + "@test.local";
        guardarUsuario(idAdmin, correoAdmin, idRolAdministrador, false);

        AutenticacionResponse respuesta = login(correoAdmin, CONTRASENA);

        assertEquals(idRolAdministrador.toString(), respuesta.usuario().rolId());
        assertEquals("Administrador", respuesta.usuario().rolNombre());
    }

    // ------------------------------------------------------------------ login con Google

    @Test
    void elLoginConGoogleDevuelveLaMismaEstructuraQueElClasico() {
        AutenticacionResponse porGoogle = autenticacionService.iniciarSesionConGoogle(
                new IniciarSesionConGoogleRequest("token-valido"));
        AutenticacionResponse clasico = login(correo, CONTRASENA);

        assertEquals(clasico.usuario(), porGoogle.usuario());
        assertNotNull(porGoogle.accessToken());
        assertNotNull(porGoogle.refreshToken());
    }

    @Test
    void elLoginConGoogleRechazaUnCorreoQueNoEstaRegistradoYNoCreaUsuarios() {
        CORREO_DE_GOOGLE.set("desconocido@gmail.com");
        long usuariosAntes = usuarioJpaRepository.count();

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> autenticacionService.iniciarSesionConGoogle(new IniciarSesionConGoogleRequest("token-valido")));

        assertEquals(401, error.getCodigo());
        // La regla estricta: no se provisiona nadie.
        assertEquals(usuariosAntes, usuarioJpaRepository.count());
    }

    @Test
    void elLoginConGoogleRechazaUnTokenInvalido() {
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> autenticacionService.iniciarSesionConGoogle(new IniciarSesionConGoogleRequest("token-falso")));

        assertEquals(401, error.getCodigo());
    }

    // ------------------------------------------------------------------ refresh

    @Test
    void elRefreshDevuelveUnAccessTokenNuevoYValido() {
        AutenticacionResponse login = login(correo, CONTRASENA);

        String nuevoAccessToken = autenticacionService
                .renovarAccessToken(new RenovarTokenRequest(login.refreshToken()))
                .accessToken();

        assertNotNull(nuevoAccessToken);
        assertEquals(login.usuario(), jwtService.verificarAccessToken(nuevoAccessToken));
    }

    @Test
    void elRefreshRechazaUnAccessTokenEnLugarDelRefreshToken() {
        AutenticacionResponse login = login(correo, CONTRASENA);

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> autenticacionService.renovarAccessToken(new RenovarTokenRequest(login.accessToken())));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elRefreshDejaDeFuncionarSiElUsuarioEsDadoDeBaja() {
        AutenticacionResponse login = login(correo, CONTRASENA);

        UsuarioJpaModel usuario = usuarioJpaRepository.findById(idUsuario).orElseThrow();
        usuario.setBorrado(true);
        usuarioJpaRepository.save(usuario);

        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> autenticacionService.renovarAccessToken(new RenovarTokenRequest(login.refreshToken())));

        assertEquals(401, error.getCodigo());
    }

    @Test
    void elRefreshRecogeElCambioDeRolSinEsperarAQueCaduque() {
        AutenticacionResponse login = login(correo, CONTRASENA);
        assertEquals("Operador", jwtService.verificarAccessToken(login.accessToken()).rolNombre());

        UsuarioJpaModel usuario = usuarioJpaRepository.findById(idUsuario).orElseThrow();
        usuario.setIdRol(idRolAdministrador);
        usuarioJpaRepository.save(usuario);

        String nuevoAccessToken = autenticacionService
                .renovarAccessToken(new RenovarTokenRequest(login.refreshToken()))
                .accessToken();

        UsuarioAutenticado autenticado = jwtService.verificarAccessToken(nuevoAccessToken);
        assertEquals(idRolAdministrador.toString(), autenticado.rolId());
        assertEquals("Administrador", autenticado.rolNombre());
    }
}
