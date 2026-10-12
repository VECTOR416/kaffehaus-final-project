package com.maestria.kaffehausv4.Shared.Presentation.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Pruebas unitarias del interceptor que aplica @RequiereRol, con peticion y respuesta simuladas. */
class GuardiaDeRolesTest {

    /** Controller de mentira: solo aporta metodos con y sin la anotacion. */
    static class ControllerAbierto {
        public void libre() {}

        @RequiereRol("Administrador")
        public void soloAdministrador() {}

        @RequiereRol({"Administrador", "Operador"})
        public void variosRoles() {}
    }

    @RequiereRol("Administrador")
    static class ControllerRestringido {
        public void heredaDeLaClase() {}

        @RequiereRol("Operador")
        public void elMetodoManda() {}
    }

    private final GuardiaDeRoles guardia = new GuardiaDeRoles();
    private final HttpServletRequest peticion = mock(HttpServletRequest.class);
    private final HttpServletResponse respuesta = mock(HttpServletResponse.class);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static HandlerMethod metodo(Object controller, String nombre) throws NoSuchMethodException {
        return new HandlerMethod(controller, controller.getClass().getMethod(nombre));
    }

    private static void autenticarConRol(String rol) {
        SecurityUser usuario = new SecurityUser(new UsuarioAutenticado("u-1", "Ana Rojas", "r-1", rol));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    @Test
    void dejaPasarLoQueNoEsUnMetodoDeController() {
        // Act
        boolean pasa = guardia.preHandle(peticion, respuesta, new Object());

        // Assert
        assertTrue(pasa);
    }

    @Test
    void dejaPasarUnEndpointSinAnotacionAunqueNoHayaUsuario() throws Exception {
        // Act
        boolean pasa = guardia.preHandle(peticion, respuesta, metodo(new ControllerAbierto(), "libre"));

        // Assert
        assertTrue(pasa);
    }

    @Test
    void sinAutenticarRespondeNoAutorizado() throws Exception {
        // Arrange
        HandlerMethod handler = metodo(new ControllerAbierto(), "soloAdministrador");

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class, () -> guardia.preHandle(peticion, respuesta, handler));

        // Assert
        assertEquals(401, error.getCodigo());
    }

    @Test
    void unPrincipalQueNoEsSecurityUserCuentaComoNoAutenticado() throws Exception {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("anonimo", null));
        HandlerMethod handler = metodo(new ControllerAbierto(), "soloAdministrador");

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class, () -> guardia.preHandle(peticion, respuesta, handler));

        // Assert
        assertEquals(401, error.getCodigo());
    }

    @Test
    void conElRolExigidoDejaPasar() throws Exception {
        // Arrange
        autenticarConRol("administrador");

        // Act
        boolean pasa = guardia.preHandle(peticion, respuesta, metodo(new ControllerAbierto(), "soloAdministrador"));

        // Assert
        assertTrue(pasa);
    }

    @Test
    void conOtroRolRespondeProhibidoYNombraElRol() throws Exception {
        // Arrange
        autenticarConRol("Operador");
        HandlerMethod handler = metodo(new ControllerAbierto(), "soloAdministrador");

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class, () -> guardia.preHandle(peticion, respuesta, handler));

        // Assert
        assertEquals(403, error.getCodigo());
        assertTrue(error.getMensaje().contains("'Operador'"));
    }

    @Test
    void bastaConTenerUnoDeLosRolesPermitidos() throws Exception {
        // Arrange
        autenticarConRol("Operador");

        // Act
        boolean pasa = guardia.preHandle(peticion, respuesta, metodo(new ControllerAbierto(), "variosRoles"));

        // Assert
        assertTrue(pasa);
    }

    @Test
    void sinAnotacionEnElMetodoSeAplicaLaDeLaClase() throws Exception {
        // Arrange
        autenticarConRol("Operador");
        HandlerMethod handler = metodo(new ControllerRestringido(), "heredaDeLaClase");

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class, () -> guardia.preHandle(peticion, respuesta, handler));

        // Assert
        assertEquals(403, error.getCodigo());
    }

    @Test
    void laAnotacionDelMetodoMandaSobreLaDeLaClase() throws Exception {
        // Arrange
        autenticarConRol("Operador");

        // Act
        boolean pasa = guardia.preHandle(peticion, respuesta, metodo(new ControllerRestringido(), "elMetodoManda"));

        // Assert
        assertTrue(pasa);
    }
}
