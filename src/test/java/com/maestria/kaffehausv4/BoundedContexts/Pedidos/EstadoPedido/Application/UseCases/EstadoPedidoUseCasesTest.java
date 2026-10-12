package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.UseCases;

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

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.ActualizarEstadoPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.CrearEstadoPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.ObtenerEstadosPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.EstadoPedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los casos de uso CRUD del catalogo EstadoPedido. El repositorio es un doble de
 * Mockito: se comprueba lo que cada caso de uso le pide y lo que devuelve.
 */
@ExtendWith(MockitoExtension.class)
class EstadoPedidoUseCasesTest {

    private static final String ID = "ca000004-0000-0000-0000-000000000001";

    @Mock private IEstadoPedidoRepository repository;

    private static EstadoPedido existente() {
        return EstadoPedido.restaurar(ID, "Recibido", false);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearGuardaElNombreYDevuelveLoPersistido() {
        // Arrange
        CrearEstadoPedidoUseCase useCase = new CrearEstadoPedidoUseCase(repository);
        ArgumentCaptor<EstadoPedido> guardado = ArgumentCaptor.forClass(EstadoPedido.class);
        when(repository.crear(any())).thenReturn(existente());

        // Act
        EstadoPedidoResponse respuesta = useCase.execute(CrearEstadoPedidoRequest.createFromRequest(Map.of("nombre", " Recibido ")));

        // Assert
        verify(repository).crear(guardado.capture());
        assertNull(guardado.getValue().getId());
        assertEquals("Recibido", guardado.getValue().getNombre());
        assertFalse(guardado.getValue().isBorrado());
        assertEquals(ID, respuesta.getId());
        assertEquals("Recibido", respuesta.getNombre());
        assertFalse(respuesta.isBorrado());
    }

    @Test
    void crearExigeElNombre() {
        // Act
        DomainException sinNombre = assertThrows(DomainException.class, () -> CrearEstadoPedidoRequest.createFromRequest(Map.of()));
        DomainException nombreVacio = assertThrows(DomainException.class, () -> CrearEstadoPedidoRequest.createFromRequest(Map.of("nombre", "  ")));

        // Assert
        assertEquals(400, sinNombre.getCodigo());
        assertEquals(400, nombreVacio.getCodigo());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarCambiaElNombreYGuarda() {
        // Arrange
        ActualizarEstadoPedidoUseCase useCase = new ActualizarEstadoPedidoUseCase(repository);
        EstadoPedido actual = existente();
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(actual));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        EstadoPedidoResponse respuesta = useCase.execute(ID, ActualizarEstadoPedidoRequest.createFromRequest(Map.of("nombre", "Entregado")));

        // Assert
        assertEquals("Entregado", respuesta.getNombre());
        verify(repository).actualizar(argThat(id -> ID.equals(id.getValue())), argThat(guardado -> guardado == actual));
    }

    @Test
    void actualizarSinNombreConservaElActual() {
        // Arrange
        ActualizarEstadoPedidoUseCase useCase = new ActualizarEstadoPedidoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        EstadoPedidoResponse respuesta = useCase.execute(ID, ActualizarEstadoPedidoRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Recibido", respuesta.getNombre());
    }

    @Test
    void actualizarUnoInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarEstadoPedidoUseCase useCase = new ActualizarEstadoPedidoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarEstadoPedidoRequest.createFromRequest(Map.of("nombre", "Entregado"))));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID));
        verify(repository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarRechazaUnNombreVacio() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> ActualizarEstadoPedidoRequest.createFromRequest(Map.of("nombre", "   ")));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminarDaDeBajaElRegistroExistente() {
        // Arrange
        EliminarEstadoPedidoUseCase useCase = new EliminarEstadoPedidoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));

        // Act
        useCase.execute(ID);

        // Assert
        verify(repository).eliminar(argThat(id -> ID.equals(id.getValue())));
    }

    @Test
    void eliminarUnoInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarEstadoPedidoUseCase useCase = new EliminarEstadoPedidoUseCase(repository);
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
        ObtenerEstadoPedidoPorIdUseCase useCase = new ObtenerEstadoPedidoPorIdUseCase(repository);
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(existente()));

        // Act
        EstadoPedidoResponse respuesta = useCase.execute(ID);

        // Assert
        assertEquals(ID, respuesta.getId());
        assertEquals("Recibido", respuesta.getNombre());
    }

    @Test
    void obtenerPorIdInexistenteRespondeNoEncontrado() {
        // Arrange
        ObtenerEstadoPedidoPorIdUseCase useCase = new ObtenerEstadoPedidoPorIdUseCase(repository);
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
        ObtenerEstadosPedidoUseCase useCase = new ObtenerEstadosPedidoUseCase(repository);
        ObtenerEstadosPedidoRequest peticion = ObtenerEstadosPedidoRequest.createFromRequest(Map.of("skip", 20, "limit", 2, "search", "x"));
        when(repository.obtenerTodos(peticion)).thenReturn(new PaginatedResponse<>(
                List.of(existente(), EstadoPedido.restaurar("ca000004-0000-0000-0000-000000000002", "Entregado", false)), 20, 2, 31, true));

        // Act
        PaginatedResponse<List<EstadoPedidoResponse>> pagina = useCase.execute(peticion);

        // Assert
        assertEquals(List.of("Recibido", "Entregado"), pagina.getData().stream().map(EstadoPedidoResponse::getNombre).toList());
        assertEquals(20, pagina.getSkip());
        assertEquals(2, pagina.getLimit());
        assertEquals(31, pagina.getTotal());
        assertTrue(pagina.isHasMore());
    }

    @Test
    void elListadoVacioSeDevuelveSinFallar() {
        // Arrange
        ObtenerEstadosPedidoUseCase useCase = new ObtenerEstadosPedidoUseCase(repository);
        when(repository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(), 0, 10, 0, false));

        // Act
        PaginatedResponse<List<EstadoPedidoResponse>> pagina = useCase.execute(ObtenerEstadosPedidoRequest.createFromRequest(Map.of()));

        // Assert
        assertTrue(pagina.getData().isEmpty());
        assertFalse(pagina.isHasMore());
    }

    @Test
    void laPeticionDeListadoUsaLaPaginacionPorDefecto() {
        // Act
        ObtenerEstadosPedidoRequest porDefecto = ObtenerEstadosPedidoRequest.createFromRequest(Map.of());
        ObtenerEstadosPedidoRequest informada = ObtenerEstadosPedidoRequest.createFromRequest(Map.of("skip", 5, "limit", 50, "search", "caf"));

        // Assert
        assertEquals(0, porDefecto.getSkip());
        assertEquals(10, porDefecto.getLimit());
        assertEquals("", porDefecto.getSearch());
        assertEquals(5, informada.getSkip());
        assertEquals(50, informada.getLimit());
        assertEquals("caf", informada.getSearch());
    }
}
