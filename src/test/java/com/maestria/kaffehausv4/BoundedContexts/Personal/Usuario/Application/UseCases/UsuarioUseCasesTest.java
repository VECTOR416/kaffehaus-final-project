package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ActualizarUsuarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.CrearUsuarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ObtenerUsuariosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.UsuarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;

/**
 * Pruebas unitarias de los casos de uso de Usuario. El hasheador de contrasenas es un doble:
 * aqui importa que la contrasena en claro nunca llegue al repositorio.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioUseCasesTest {

    private static final String ID_USUARIO = "ad000000-0000-0000-0000-000000000001";
    private static final String ID_ROL = "ad000000-0000-0000-0000-000000000002";
    private static final String ID_OTRO_ROL = "ad000000-0000-0000-0000-000000000003";

    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IPersonaRepository personaRepository;
    @Mock private IRolRepository rolRepository;
    @Mock private IHasheadorDeContrasena hasheadorDeContrasena;

    private static final String ID_PERSONA = "ab000000-0000-0000-0000-000000000001";
    private static final String ID_PERSONA_NUEVA = "ab000000-0000-0000-0000-0000000000ff";

    private static Persona ana() {
        return Persona.restaurar(ID_PERSONA, "Ana", "Rojas", "70000000", "1234567", false);
    }

    /** La persistencia simulada devuelve la persona nueva con id y la actualizada tal cual. */
    private void prepararPersonas() {
        lenient().when(personaRepository.crear(any())).thenAnswer(llamada -> {
            Persona nueva = llamada.getArgument(0);
            return Persona.restaurar(ID_PERSONA_NUEVA, nueva.getNombres(), nueva.getApellidos(), nueva.getTelefono(), nueva.getCarnet(), false);
        });
        lenient().when(personaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    private static Map<String, Object> datosDe(String nombres, String apellidos) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombres", nombres);
        datos.put("apellidos", apellidos);
        datos.put("telefono", "71111111");
        datos.put("carnet", "7654321");
        return datos;
    }

    @BeforeEach
    void prepararPersistencia() {
        prepararPersonas();
        lenient().when(hasheadorDeContrasena.hashear(anyString())).thenAnswer(llamada -> "hash(" + llamada.getArgument(0) + ")");
        lenient().when(rolRepository.obtenerPorId(argThat(id -> id != null && ID_ROL.equals(id.getValue()))))
                .thenReturn(Optional.of(Rol.restaurar(ID_ROL, "Operador", false)));
        lenient().when(rolRepository.obtenerPorId(argThat(id -> id != null && ID_OTRO_ROL.equals(id.getValue()))))
                .thenReturn(Optional.of(Rol.restaurar(ID_OTRO_ROL, "Administrador", false)));
        lenient().when(usuarioRepository.crear(any())).thenAnswer(llamada -> {
            Usuario nuevo = llamada.getArgument(0);
            return Usuario.restaurar(ID_USUARIO, nuevo.getPersonaId().getValue(), nuevo.getCorreo(), nuevo.getContrasena(), false, nuevo.getRolId());
        });
        lenient().when(usuarioRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    private static Usuario usuarioExistente() {
        return Usuario.restaurar(ID_USUARIO, ID_PERSONA, "ana@kaffe.com", "hash(vieja)", false, new RolId(ID_ROL));
    }

    private static Map<String, Object> alta(String idRol) {
        Map<String, Object> datos = datosDe("Luis", "Mamani");
        datos.put("correo", "luis@kaffe.com");
        datos.put("contrasena", "secreta");
        datos.put("idRol", idRol);
        return datos;
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearRegistraLaPersonaYGuardaLaContrasenaHasheada() {
        // Arrange
        CrearUsuarioUseCase useCase = new CrearUsuarioUseCase(usuarioRepository, personaRepository, rolRepository, hasheadorDeContrasena);
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);

        // Act
        UsuarioResponse respuesta = useCase.execute(CrearUsuarioRequest.createFromRequest(alta(ID_ROL)));

        // Assert
        verify(usuarioRepository).crear(guardado.capture());
        assertEquals("hash(secreta)", guardado.getValue().getContrasena());
        assertEquals("luis@kaffe.com", guardado.getValue().getCorreo());
        assertEquals(ID_PERSONA_NUEVA, guardado.getValue().getPersonaId().getValue());
        assertEquals(ID_ROL, guardado.getValue().getRolId().getValue());
        assertEquals(ID_USUARIO, respuesta.getId());
        assertEquals("luis@kaffe.com", respuesta.getCorreo());
        assertEquals("Operador", respuesta.getRol().getNombre());
        assertEquals("Luis", respuesta.getPersona().getNombres());
    }

    @Test
    void crearConUnRolInexistenteNoRegistraNiLaPersona() {
        // Arrange
        CrearUsuarioUseCase useCase = new CrearUsuarioUseCase(usuarioRepository, personaRepository, rolRepository, hasheadorDeContrasena);

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(CrearUsuarioRequest.createFromRequest(alta("ad000000-0000-0000-0000-0000000000ee"))));

        // Assert
        assertEquals(404, error.getCodigo());
        verifyNoInteractions(personaRepository, hasheadorDeContrasena);
        verify(usuarioRepository, never()).crear(any());
    }

    @Test
    void crearConUnaPersonaExistenteLaReutiliza() {
        // Arrange
        CrearUsuarioUseCase useCase = new CrearUsuarioUseCase(usuarioRepository, personaRepository, rolRepository, hasheadorDeContrasena);
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));
        Map<String, Object> datos = alta(ID_ROL);
        datos.put("idPersona", ID_PERSONA);

        // Act
        UsuarioResponse respuesta = useCase.execute(CrearUsuarioRequest.createFromRequest(datos));

        // Assert
        assertEquals(ID_PERSONA, respuesta.getPersona().getIdPersona());
        verify(personaRepository, never()).crear(any());
        verify(personaRepository).actualizar(any(), any());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarSinContrasenaConservaElHashActual() {
        // Arrange
        ActualizarUsuarioUseCase useCase = new ActualizarUsuarioUseCase(usuarioRepository, personaRepository, rolRepository, hasheadorDeContrasena);
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuarioExistente()));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));
        Map<String, Object> datos = datosDe("Ana Maria", null);
        datos.put("correo", "ana.maria@kaffe.com");

        // Act
        UsuarioResponse respuesta = useCase.execute(ID_USUARIO, ActualizarUsuarioRequest.createFromRequest(datos));

        // Assert
        assertEquals("ana.maria@kaffe.com", respuesta.getCorreo());
        assertEquals("Ana Maria", respuesta.getPersona().getNombres());
        assertEquals("Operador", respuesta.getRol().getNombre());
        verifyNoInteractions(hasheadorDeContrasena);
        verify(usuarioRepository).actualizar(any(), argThat(guardado -> "hash(vieja)".equals(guardado.getContrasena())));
    }

    @Test
    void actualizarConContrasenaGuardaElNuevoHash() {
        // Arrange
        ActualizarUsuarioUseCase useCase = new ActualizarUsuarioUseCase(usuarioRepository, personaRepository, rolRepository, hasheadorDeContrasena);
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuarioExistente()));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));
        Map<String, Object> datos = datosDe(null, null);
        datos.put("contrasena", "nueva");

        // Act
        useCase.execute(ID_USUARIO, ActualizarUsuarioRequest.createFromRequest(datos));

        // Assert
        verify(usuarioRepository).actualizar(any(), argThat(guardado -> "hash(nueva)".equals(guardado.getContrasena())));
    }

    @Test
    void actualizarPuedeCambiarElRol() {
        // Arrange
        ActualizarUsuarioUseCase useCase = new ActualizarUsuarioUseCase(usuarioRepository, personaRepository, rolRepository, hasheadorDeContrasena);
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuarioExistente()));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));
        Map<String, Object> datos = datosDe(null, null);
        datos.put("idRol", ID_OTRO_ROL);

        // Act
        UsuarioResponse respuesta = useCase.execute(ID_USUARIO, ActualizarUsuarioRequest.createFromRequest(datos));

        // Assert
        assertEquals("Administrador", respuesta.getRol().getNombre());
        verify(usuarioRepository).actualizar(any(), argThat(guardado -> ID_OTRO_ROL.equals(guardado.getRolId().getValue())));
    }

    @Test
    void actualizarConUnRolInexistenteNoGuardaElUsuario() {
        // Arrange
        ActualizarUsuarioUseCase useCase = new ActualizarUsuarioUseCase(usuarioRepository, personaRepository, rolRepository, hasheadorDeContrasena);
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuarioExistente()));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));
        Map<String, Object> datos = datosDe(null, null);
        datos.put("idRol", "ad000000-0000-0000-0000-0000000000ee");

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID_USUARIO, ActualizarUsuarioRequest.createFromRequest(datos)));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(usuarioRepository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarRespondeNoEncontradoSiFaltaElUsuarioOSuPersona() {
        // Arrange
        ActualizarUsuarioUseCase useCase = new ActualizarUsuarioUseCase(usuarioRepository, personaRepository, rolRepository, hasheadorDeContrasena);
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty()).thenReturn(Optional.of(usuarioExistente()));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        ActualizarUsuarioRequest peticion = ActualizarUsuarioRequest.createFromRequest(datosDe("x", "y"));

        // Act
        DomainException sinUsuario = assertThrows(DomainException.class, () -> useCase.execute(ID_USUARIO, peticion));
        DomainException sinPersona = assertThrows(DomainException.class, () -> useCase.execute(ID_USUARIO, peticion));

        // Assert
        assertEquals(404, sinUsuario.getCodigo());
        assertTrue(sinUsuario.getMensaje().contains(ID_USUARIO));
        assertEquals(404, sinPersona.getCodigo());
        verify(usuarioRepository, never()).actualizar(any(), any());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminarDaDeBajaElUsuarioExistente() {
        // Arrange
        EliminarUsuarioUseCase useCase = new EliminarUsuarioUseCase(usuarioRepository);
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuarioExistente()));

        // Act
        useCase.execute(ID_USUARIO);

        // Assert
        verify(usuarioRepository).eliminar(argThat(id -> ID_USUARIO.equals(id.getValue())));
    }

    @Test
    void eliminarUnUsuarioInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarUsuarioUseCase useCase = new EliminarUsuarioUseCase(usuarioRepository);
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_USUARIO));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(usuarioRepository, never()).eliminar(any());
    }

    // ------------------------------------------------------------------ consultas

    @Test
    void obtenerPorIdDevuelveElUsuarioConSuPersonaYSuRol() {
        // Arrange
        ObtenerUsuarioPorIdUseCase useCase = new ObtenerUsuarioPorIdUseCase(usuarioRepository, personaRepository, rolRepository);
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuarioExistente()));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));

        // Act
        UsuarioResponse respuesta = useCase.execute(ID_USUARIO);

        // Assert
        assertEquals("ana@kaffe.com", respuesta.getCorreo());
        assertEquals("Ana", respuesta.getPersona().getNombres());
        assertEquals("Operador", respuesta.getRol().getNombre());
    }

    @Test
    void obtenerPorIdRespondeNoEncontradoSiFaltaElUsuarioSuPersonaOSuRol() {
        // Arrange
        ObtenerUsuarioPorIdUseCase useCase = new ObtenerUsuarioPorIdUseCase(usuarioRepository, personaRepository, rolRepository);
        Usuario conRolBorrado = Usuario.restaurar(ID_USUARIO, ID_PERSONA, "ana@kaffe.com", "h", false, new RolId("ad000000-0000-0000-0000-0000000000ee"));
        when(usuarioRepository.obtenerPorId(any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(usuarioExistente()))
                .thenReturn(Optional.of(conRolBorrado));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty()).thenReturn(Optional.of(ana()));

        // Act
        DomainException sinUsuario = assertThrows(DomainException.class, () -> useCase.execute(ID_USUARIO));
        DomainException sinPersona = assertThrows(DomainException.class, () -> useCase.execute(ID_USUARIO));
        DomainException sinRol = assertThrows(DomainException.class, () -> useCase.execute(ID_USUARIO));

        // Assert
        assertEquals(404, sinUsuario.getCodigo());
        assertEquals(404, sinPersona.getCodigo());
        assertEquals(404, sinRol.getCodigo());
    }

    @Test
    void elListadoResuelvePersonaYRolDeCadaUsuarioYConservaLaPaginacion() {
        // Arrange
        ObtenerUsuariosUseCase useCase = new ObtenerUsuariosUseCase(usuarioRepository, personaRepository, rolRepository);
        when(usuarioRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(usuarioExistente()), 10, 5, 11, false));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));

        // Act
        PaginatedResponse<List<UsuarioResponse>> pagina = useCase.execute(ObtenerUsuariosRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Operador", pagina.getData().get(0).getRol().getNombre());
        assertEquals(10, pagina.getSkip());
        assertEquals(5, pagina.getLimit());
        assertEquals(11, pagina.getTotal());
        assertFalse(pagina.isHasMore());
    }
}
