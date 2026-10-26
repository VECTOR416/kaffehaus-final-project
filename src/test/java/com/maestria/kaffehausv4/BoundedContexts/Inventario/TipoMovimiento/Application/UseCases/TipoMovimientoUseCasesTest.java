package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ActualizarTipoMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.CrearTipoMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ObtenerTiposMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.TipoMovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los casos de uso CRUD del catalogo TipoMovimiento. El repositorio es un doble de
 * Mockito: se comprueba lo que cada caso de uso le pide y lo que devuelve.
 */
@ExtendWith(MockitoExtension.class)
class TipoMovimientoUseCasesTest {

    private static final String ID = "ca000003-0000-0000-0000-000000000001";

    @Mock private ITipoMovimientoRepository repository;

    private static TipoMovimiento existente() {
        return TipoMovimiento.restaurar(ID, "Receta", false);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearGuardaElNombreYDevuelveLoPersistido() {
        // Arrange
        CrearTipoMovimientoUseCase useCase = new CrearTipoMovimientoUseCase(repository);
        ArgumentCaptor<TipoMovimiento> guardado = ArgumentCaptor.forClass(TipoMovimiento.class);
        when(repository.crear(any())).thenReturn(existente());

        // Act
        TipoMovimientoResponse respuesta = useCase.execute(CrearTipoMovimientoRequest.createFromRequest(Map.of("nombre", " Receta ")));

        // Assert
        verify(repository).crear(guardado.capture());
        assertNull(guardado.getValue().getId());
        assertEquals("Receta", guardado.getValue().getNombre());
        assertFalse(guardado.getValue().isBorrado());
        assertEquals(ID, respuesta.getId());
        assertEquals("Receta", respuesta.getNombre());
        assertFalse(respuesta.isBorrado());
    }

    @Test
    void crearExigeElNombre() {
        // Act
        DomainException sinNombre = assertThrows(DomainException.class, () -> CrearTipoMovimientoRequest.createFromRequest(Map.of()));
        DomainException nombreVacio = assertThrows(DomainException.class, () -> CrearTipoMovimientoRequest.createFromRequest(Map.of("nombre", "  ")));

        // Assert
        assertEquals(400, sinNombre.getCodigo());
        assertEquals(400, nombreVacio.getCodigo());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarCambiaElNombreYGuarda() {
        // Arrange
        ActualizarTipoMovimientoUseCase useCase = new ActualizarTipoMovimientoUseCase(repository);
        TipoMovimiento actual = existente();
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(actual));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        TipoMovimientoResponse respuesta = useCase.execute(ID, ActualizarTipoMovimientoRequest.createFromRequest(Map.of("nombre", "Merma")));

        // Assert
        assertEquals("Merma", respuesta.getNombre());
        verify(repository).actualizar(argThat(id -> ID.equals(id.getValue())), argThat(guardado -> guardado == actual));
    }

    @Test
    void actualizarSinNombreConservaElActual() {
        // Arrange
        ActualizarTipoMovimientoUseCase useCase = new ActualizarTipoMovimientoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        TipoMovimientoResponse respuesta = useCase.execute(ID, ActualizarTipoMovimientoRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Receta", respuesta.getNombre());
    }

    @Test
    void actualizarUnoInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarTipoMovimientoUseCase useCase = new ActualizarTipoMovimientoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarTipoMovimientoRequest.createFromRequest(Map.of("nombre", "Merma"))));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID));
        verify(repository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarRechazaUnNombreVacio() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> ActualizarTipoMovimientoRequest.createFromRequest(Map.of("nombre", "   ")));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminarDaDeBajaElRegistroExistente() {
        // Arrange
        EliminarTipoMovimientoUseCase useCase = new EliminarTipoMovimientoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));

        // Act
        useCase.execute(ID);

        // Assert
        verify(repository).eliminar(argThat(id -> ID.equals(id.getValue())));
    }

    @Test
    void eliminarUnoInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarTipoMovimientoUseCase useCase = new EliminarTipoMovimientoUseCase(repository);
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
        ObtenerTipoMovimientoPorIdUseCase useCase = new ObtenerTipoMovimientoPorIdUseCase(repository);
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(existente()));

        // Act
        TipoMovimientoResponse respuesta = useCase.execute(ID);

        // Assert
        assertEquals(ID, respuesta.getId());
        assertEquals("Receta", respuesta.getNombre());
    }

    @Test
    void obtenerPorIdInexistenteRespondeNoEncontrado() {
        // Arrange
        ObtenerTipoMovimientoPorIdUseCase useCase = new ObtenerTipoMovimientoPorIdUseCase(repository);
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
        ObtenerTiposMovimientoUseCase useCase = new ObtenerTiposMovimientoUseCase(repository);
        ObtenerTiposMovimientoRequest peticion = ObtenerTiposMovimientoRequest.createFromRequest(Map.of("skip", 20, "limit", 2, "search", "x"));
        when(repository.obtenerTodos(peticion)).thenReturn(new PaginatedResponse<>(
                List.of(existente(), TipoMovimiento.restaurar("ca000003-0000-0000-0000-000000000002", "Merma", false)), 20, 2, 31, true));

        // Act
        PaginatedResponse<List<TipoMovimientoResponse>> pagina = useCase.execute(peticion);

        // Assert
        assertEquals(List.of("Receta", "Merma"), pagina.getData().stream().map(TipoMovimientoResponse::getNombre).toList());
        assertEquals(20, pagina.getSkip());
        assertEquals(2, pagina.getLimit());
        assertEquals(31, pagina.getTotal());
        assertTrue(pagina.isHasMore());
    }

    @Test
    void elListadoVacioSeDevuelveSinFallar() {
        // Arrange
        ObtenerTiposMovimientoUseCase useCase = new ObtenerTiposMovimientoUseCase(repository);
        when(repository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(), 0, 10, 0, false));

        // Act
        PaginatedResponse<List<TipoMovimientoResponse>> pagina = useCase.execute(ObtenerTiposMovimientoRequest.createFromRequest(Map.of()));

        // Assert
        assertTrue(pagina.getData().isEmpty());
        assertFalse(pagina.isHasMore());
    }

    @Test
    void laPeticionDeListadoUsaLaPaginacionPorDefecto() {
        // Act
        ObtenerTiposMovimientoRequest porDefecto = ObtenerTiposMovimientoRequest.createFromRequest(Map.of());
        ObtenerTiposMovimientoRequest informada = ObtenerTiposMovimientoRequest.createFromRequest(Map.of("skip", 5, "limit", 50, "search", "caf"));

        // Assert
        assertEquals(0, porDefecto.getSkip());
        assertEquals(10, porDefecto.getLimit());
        assertEquals("", porDefecto.getSearch());
        assertEquals(5, informada.getSkip());
        assertEquals(50, informada.getLimit());
        assertEquals("caf", informada.getSearch());
    }
}
