package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.ActualizarRolRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.CrearRolRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.ObtenerRolesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.RolResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los casos de uso CRUD del catalogo Rol. El repositorio es un doble de
 * Mockito: se comprueba lo que cada caso de uso le pide y lo que devuelve.
 *
 * Las peticiones de este catalogo no pasan por Validador (toman el nombre tal cual llega),
 * asi que aqui no hay pruebas de validacion de entrada.
 */
@ExtendWith(MockitoExtension.class)
class RolUseCasesTest {

    private static final String ID = "ca000005-0000-0000-0000-000000000001";

    @Mock private IRolRepository repository;
    @Mock private IUsuarioRepository usuarioRepository;

    private static Rol existente() {
        return Rol.restaurar(ID, "Administrador", false);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearGuardaElNombreYDevuelveLoPersistido() {
        // Arrange
        CrearRolUseCase useCase = new CrearRolUseCase(repository);
        ArgumentCaptor<Rol> guardado = ArgumentCaptor.forClass(Rol.class);
        when(repository.crear(any())).thenReturn(existente());

        // Act
        RolResponse respuesta = useCase.execute(CrearRolRequest.createFromRequest(Map.of("nombre", "Administrador")));

        // Assert
        verify(repository).crear(guardado.capture());
        assertNull(guardado.getValue().getId());
        assertEquals("Administrador", guardado.getValue().getNombre());
        assertFalse(guardado.getValue().isBorrado());
        assertEquals(ID, respuesta.getId());
        assertEquals("Administrador", respuesta.getNombre());
        assertFalse(respuesta.isBorrado());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarCambiaElNombreYGuarda() {
        // Arrange
        ActualizarRolUseCase useCase = new ActualizarRolUseCase(repository);
        Rol actual = Rol.restaurar(ID, "Supervisor", false);
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(actual));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        RolResponse respuesta = useCase.execute(ID, ActualizarRolRequest.createFromRequest(Map.of("nombre", "Operador")));

        // Assert
        assertEquals("Operador", respuesta.getNombre());
        verify(repository).actualizar(argThat(id -> ID.equals(id.getValue())), argThat(guardado -> guardado == actual));
    }

    @Test
    void actualizarSinNombreConservaElActual() {
        // Arrange
        ActualizarRolUseCase useCase = new ActualizarRolUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        RolResponse respuesta = useCase.execute(ID, ActualizarRolRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Administrador", respuesta.getNombre());
    }

    @Test
    void actualizarUnoInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarRolUseCase useCase = new ActualizarRolUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarRolRequest.createFromRequest(Map.of("nombre", "Operador"))));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID));
        verify(repository, never()).actualizar(any(), any());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminarDaDeBajaElRegistroExistente() {
        // Arrange
        EliminarRolUseCase useCase = new EliminarRolUseCase(repository, usuarioRepository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));

        // Act
        useCase.execute(ID);

        // Assert
        verify(repository).eliminar(argThat(id -> ID.equals(id.getValue())));
    }

    @Test
    void eliminarUnoInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarRolUseCase useCase = new EliminarRolUseCase(repository, usuarioRepository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(repository, never()).eliminar(any());
    }

    // ------------------------------------------------------------------ consultas

    @Test
    void obtenerPorIdDevuelveElRegistro() {
        // Arrange
        ObtenerRolPorIdUseCase useCase = new ObtenerRolPorIdUseCase(repository);
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(existente()));

        // Act
        RolResponse respuesta = useCase.execute(ID);

        // Assert
        assertEquals(ID, respuesta.getId());
        assertEquals("Administrador", respuesta.getNombre());
    }

    @Test
    void obtenerPorIdInexistenteRespondeNoEncontrado() {
        // Arrange
        ObtenerRolPorIdUseCase useCase = new ObtenerRolPorIdUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID));
    }

    @Test
    void elListadoConvierteCadaRegistroYConservaLaPaginacion() {
        // Arrange
        ObtenerRolesUseCase useCase = new ObtenerRolesUseCase(repository);
        ObtenerRolesRequest peticion = ObtenerRolesRequest.createFromRequest(Map.of("skip", 20, "limit", 2, "search", "x"));
        when(repository.obtenerTodos(peticion)).thenReturn(new PaginatedResponse<>(
                List.of(existente(), Rol.restaurar("ca000005-0000-0000-0000-000000000002", "Operador", false)), 20, 2, 31, true));

        // Act
        PaginatedResponse<List<RolResponse>> pagina = useCase.execute(peticion);

        // Assert
        assertEquals(List.of("Administrador", "Operador"), pagina.getData().stream().map(RolResponse::getNombre).toList());
        assertEquals(20, pagina.getSkip());
        assertEquals(2, pagina.getLimit());
        assertEquals(31, pagina.getTotal());
        assertTrue(pagina.isHasMore());
    }

    @Test
    void elListadoVacioSeDevuelveSinFallar() {
        // Arrange
        ObtenerRolesUseCase useCase = new ObtenerRolesUseCase(repository);
        when(repository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(), 0, 10, 0, false));

        // Act
        PaginatedResponse<List<RolResponse>> pagina = useCase.execute(ObtenerRolesRequest.createFromRequest(Map.of()));

        // Assert
        assertTrue(pagina.getData().isEmpty());
        assertFalse(pagina.isHasMore());
    }

    @Test
    void laPeticionDeListadoUsaLaPaginacionPorDefecto() {
        // Act
        ObtenerRolesRequest porDefecto = ObtenerRolesRequest.createFromRequest(Map.of());
        ObtenerRolesRequest informada = ObtenerRolesRequest.createFromRequest(Map.of("skip", 5, "limit", 50, "search", "caf"));

        // Assert
        assertEquals(0, porDefecto.getSkip());
        assertEquals(10, porDefecto.getLimit());
        assertEquals("", porDefecto.getSearch());
        assertEquals(5, informada.getSkip());
        assertEquals(50, informada.getLimit());
        assertEquals("caf", informada.getSearch());
    }

    @Test
    void noSeDaDeBajaSiOtroRegistroVivoLoNecesita() {
        // Arrange (logica.md GENERAL-18)
        EliminarRolUseCase useCase = new EliminarRolUseCase(repository, usuarioRepository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(Rol.restaurar(ID, "Catalogo propio", false)));
        when(usuarioRepository.contarPorRol(any())).thenReturn(1L);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("usuarios vivos"));
        verify(repository, never()).eliminar(any());
    }
}
