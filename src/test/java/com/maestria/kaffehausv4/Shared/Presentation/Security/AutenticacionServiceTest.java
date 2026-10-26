package com.maestria.kaffehausv4.Shared.Presentation.Security;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_PERSONA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_ROL;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_USUARIO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.AccessTokenResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.AutenticacionResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.IniciarSesionConGoogleRequest;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.IniciarSesionRequest;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.RenovarTokenRequest;

/**
 * Pruebas unitarias del servicio de autenticacion: inicio de sesion con contrasena, con Google
 * y renovacion del access token. Repositorios, hasheador, verificador de Google y emisor de
 * tokens son dobles de Mockito; aqui solo se prueba la decision de dejar entrar o no.
 */
@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    private static final String CORREO = "ana@kaffe.com";

    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IPersonaRepository personaRepository;
    @Mock private IRolRepository rolRepository;
    @Mock private IHasheadorDeContrasena hasheadorDeContrasena;
    @Mock private VerificadorDeTokenGoogle verificadorDeTokenGoogle;
    @Mock private JwtService jwtService;

    private AutenticacionService servicio;

    @BeforeEach
    void prepararUsuarioRegistrado() {
        servicio = new AutenticacionService(usuarioRepository, personaRepository, rolRepository, hasheadorDeContrasena,
                verificadorDeTokenGoogle, jwtService);

        lenient().when(usuarioRepository.obtenerPorCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(Persona.restaurar(ID_PERSONA, "Ana", "Rojas", null, "123", false)));
        lenient().when(rolRepository.obtenerPorId(any())).thenReturn(Optional.of(Rol.restaurar(ID_ROL, "Administrador", false)));
        lenient().when(hasheadorDeContrasena.coincide("secreta", "hash")).thenReturn(true);
        lenient().when(jwtService.emitirAccessToken(any())).thenReturn("access-token");
        lenient().when(jwtService.emitirRefreshToken(anyString())).thenReturn("refresh-token");
    }

    private static Usuario dadoDeBaja() {
        return Usuario.restaurar(ID_USUARIO, ID_PERSONA, CORREO, "hash", true, new RolId(ID_ROL));
    }

    // ------------------------------------------------------------------ correo y contrasena

    @Test
    void conCredencialesValidasDevuelveLaIdentidadYLosDosTokens() {
        // Act
        AutenticacionResponse sesion = servicio.iniciarSesion(new IniciarSesionRequest(CORREO, "secreta"));

        // Assert
        assertEquals(ID_USUARIO, sesion.usuario().usuarioId());
        assertEquals("Ana Rojas", sesion.usuario().nombreCompleto());
        assertEquals(ID_ROL, sesion.usuario().rolId());
        assertEquals("Administrador", sesion.usuario().rolNombre());
        assertEquals("access-token", sesion.accessToken());
        assertEquals("refresh-token", sesion.refreshToken());
    }

    @Test
    void elAccessTokenSeEmiteConLaMismaIdentidadQueSeDevuelve() {
        // Arrange
        ArgumentCaptor<UsuarioAutenticado> firmada = ArgumentCaptor.forClass(UsuarioAutenticado.class);

        // Act
        AutenticacionResponse sesion = servicio.iniciarSesion(new IniciarSesionRequest(CORREO, "secreta"));

        // Assert
        verify(jwtService).emitirAccessToken(firmada.capture());
        assertEquals(sesion.usuario(), firmada.getValue());
        verify(jwtService).emitirRefreshToken(ID_USUARIO);
    }

    @Test
    void unaContrasenaIncorrectaNoEmiteTokens() {
        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> servicio.iniciarSesion(new IniciarSesionRequest(CORREO, "otra")));

        // Assert
        assertEquals(401, error.getCodigo());
        verifyNoInteractions(jwtService);
    }

    @Test
    void unCorreoDesconocidoRespondeIgualQueUnaContrasenaIncorrecta() {
        // Arrange
        when(usuarioRepository.obtenerPorCorreo("nadie@kaffe.com")).thenReturn(Optional.empty());

        // Act
        AutorizacionException porCorreo = assertThrows(AutorizacionException.class,
                () -> servicio.iniciarSesion(new IniciarSesionRequest("nadie@kaffe.com", "secreta")));
        AutorizacionException porContrasena = assertThrows(AutorizacionException.class,
                () -> servicio.iniciarSesion(new IniciarSesionRequest(CORREO, "otra")));

        // Assert: el mensaje no revela cual de los dos datos fallo.
        assertEquals(401, porCorreo.getCodigo());
        assertEquals(porContrasena.getMensaje(), porCorreo.getMensaje());
        verifyNoInteractions(jwtService);
    }

    @Test
    void unUsuarioDadoDeBajaNoPuedeEntrarAunqueSuContrasenaCoincida() {
        // Arrange
        when(usuarioRepository.obtenerPorCorreo(CORREO)).thenReturn(Optional.of(dadoDeBaja()));

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> servicio.iniciarSesion(new IniciarSesionRequest(CORREO, "secreta")));

        // Assert
        assertEquals(401, error.getCodigo());
        verify(hasheadorDeContrasena, never()).coincide(anyString(), anyString());
        verifyNoInteractions(jwtService);
    }

    @Test
    void sinPersonaAsociadaNoSeIniciaSesion() {
        // Arrange
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> servicio.iniciarSesion(new IniciarSesionRequest(CORREO, "secreta")));

        // Assert
        assertEquals(401, error.getCodigo());
        verifyNoInteractions(jwtService);
    }

    @Test
    void unUsuarioSinRolValidoRecibeProhibido() {
        // Arrange
        when(rolRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> servicio.iniciarSesion(new IniciarSesionRequest(CORREO, "secreta")));

        // Assert
        assertEquals(403, error.getCodigo());
        verifyNoInteractions(jwtService);
    }

    @Test
    void laPeticionExigeCorreoYContrasena() {
        // Act
        DomainException sinPeticion = assertThrows(DomainException.class, () -> servicio.iniciarSesion(null));
        DomainException sinCorreo = assertThrows(DomainException.class, () -> servicio.iniciarSesion(new IniciarSesionRequest(" ", "secreta")));
        DomainException sinContrasena = assertThrows(DomainException.class, () -> servicio.iniciarSesion(new IniciarSesionRequest(CORREO, null)));

        // Assert
        assertEquals(400, sinPeticion.getCodigo());
        assertEquals(400, sinCorreo.getCodigo());
        assertEquals(400, sinContrasena.getCodigo());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void elNombreCompletoNoArrastraEspaciosSiFaltanApellidos() {
        // Arrange
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(Persona.restaurar(ID_PERSONA, "Ana", "", null, "123", false)));

        // Act
        AutenticacionResponse sesion = servicio.iniciarSesion(new IniciarSesionRequest(CORREO, "secreta"));

        // Assert
        assertEquals("Ana", sesion.usuario().nombreCompleto());
    }

    // ------------------------------------------------------------------ Google

    @Test
    void conUnTokenDeGoogleDeUnCorreoRegistradoSeIniciaSesionSinContrasena() {
        // Arrange
        when(verificadorDeTokenGoogle.obtenerCorreoVerificado("token-google")).thenReturn(CORREO);

        // Act
        AutenticacionResponse sesion = servicio.iniciarSesionConGoogle(new IniciarSesionConGoogleRequest("token-google"));

        // Assert
        assertEquals(ID_USUARIO, sesion.usuario().usuarioId());
        assertEquals("access-token", sesion.accessToken());
        verifyNoInteractions(hasheadorDeContrasena);
    }

    @Test
    void unaCuentaDeGoogleQueNoEsUsuarioDelSistemaNoEntra() {
        // Arrange
        when(verificadorDeTokenGoogle.obtenerCorreoVerificado("token-google")).thenReturn("externo@gmail.com");
        when(usuarioRepository.obtenerPorCorreo("externo@gmail.com")).thenReturn(Optional.empty());

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> servicio.iniciarSesionConGoogle(new IniciarSesionConGoogleRequest("token-google")));

        // Assert
        assertEquals(401, error.getCodigo());
        verifyNoInteractions(jwtService);
    }

    @Test
    void unUsuarioDadoDeBajaTampocoEntraConGoogle() {
        // Arrange
        when(verificadorDeTokenGoogle.obtenerCorreoVerificado("token-google")).thenReturn(CORREO);
        when(usuarioRepository.obtenerPorCorreo(CORREO)).thenReturn(Optional.of(dadoDeBaja()));

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> servicio.iniciarSesionConGoogle(new IniciarSesionConGoogleRequest("token-google")));

        // Assert
        assertEquals(401, error.getCodigo());
    }

    @Test
    void siGoogleRechazaElTokenElErrorSePropagaSinConsultarUsuarios() {
        // Arrange
        when(verificadorDeTokenGoogle.obtenerCorreoVerificado("token-falso"))
                .thenThrow(AutorizacionException.throwException(401, "El token de Google no es valido"));

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> servicio.iniciarSesionConGoogle(new IniciarSesionConGoogleRequest("token-falso")));

        // Assert
        assertEquals(401, error.getCodigo());
        verifyNoInteractions(usuarioRepository, jwtService);
    }

    @Test
    void elInicioConGoogleExigeElToken() {
        // Act
        DomainException sinPeticion = assertThrows(DomainException.class, () -> servicio.iniciarSesionConGoogle(null));
        DomainException sinToken = assertThrows(DomainException.class,
                () -> servicio.iniciarSesionConGoogle(new IniciarSesionConGoogleRequest("")));

        // Assert
        assertEquals(400, sinPeticion.getCodigo());
        assertEquals(400, sinToken.getCodigo());
        verifyNoInteractions(verificadorDeTokenGoogle);
    }

    // ------------------------------------------------------------------ renovacion

    @Test
    void unRefreshTokenValidoDevuelveUnAccessTokenNuevoConElRolActual() {
        // Arrange: el rol se vuelve a leer, asi que un cambio de rol se refleja al renovar.
        when(jwtService.verificarRefreshToken("refresh-token")).thenReturn(ID_USUARIO);
        when(rolRepository.obtenerPorId(any())).thenReturn(Optional.of(Rol.restaurar(ID_ROL, "Operador", false)));
        ArgumentCaptor<UsuarioAutenticado> firmada = ArgumentCaptor.forClass(UsuarioAutenticado.class);

        // Act
        AccessTokenResponse respuesta = servicio.renovarAccessToken(new RenovarTokenRequest("refresh-token"));

        // Assert
        assertEquals("access-token", respuesta.accessToken());
        verify(jwtService).emitirAccessToken(firmada.capture());
        assertEquals("Operador", firmada.getValue().rolNombre());
        verify(jwtService, never()).emitirRefreshToken(anyString());
    }

    @Test
    void unRefreshTokenDeUnUsuarioQueYaNoExisteODadoDeBajaNoRenueva() {
        // Arrange
        when(jwtService.verificarRefreshToken("refresh-token")).thenReturn(ID_USUARIO);
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty()).thenReturn(Optional.of(dadoDeBaja()));

        // Act
        AutorizacionException inexistente = assertThrows(AutorizacionException.class,
                () -> servicio.renovarAccessToken(new RenovarTokenRequest("refresh-token")));
        AutorizacionException deBaja = assertThrows(AutorizacionException.class,
                () -> servicio.renovarAccessToken(new RenovarTokenRequest("refresh-token")));

        // Assert
        assertEquals(401, inexistente.getCodigo());
        assertEquals(401, deBaja.getCodigo());
        verify(jwtService, never()).emitirAccessToken(any());
    }

    @Test
    void unRefreshTokenInvalidoNoLlegaAConsultarUsuarios() {
        // Arrange
        when(jwtService.verificarRefreshToken("manipulado")).thenThrow(AutorizacionException.throwException(401, "El refresh token no es valido"));

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class,
                () -> servicio.renovarAccessToken(new RenovarTokenRequest("manipulado")));

        // Assert
        assertEquals(401, error.getCodigo());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void laRenovacionExigeElRefreshToken() {
        // Act
        DomainException sinPeticion = assertThrows(DomainException.class, () -> servicio.renovarAccessToken(null));
        DomainException sinToken = assertThrows(DomainException.class, () -> servicio.renovarAccessToken(new RenovarTokenRequest(null)));

        // Assert
        assertEquals(400, sinPeticion.getCodigo());
        assertEquals(400, sinToken.getCodigo());
        verifyNoInteractions(jwtService);
    }
}
