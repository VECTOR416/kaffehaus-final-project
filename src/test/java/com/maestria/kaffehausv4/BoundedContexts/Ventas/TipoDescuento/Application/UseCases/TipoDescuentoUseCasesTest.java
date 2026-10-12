package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.ActualizarTipoDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.CrearTipoDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.ObtenerTiposDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.TipoDescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los casos de uso CRUD del catalogo TipoDescuento. El repositorio es un doble de
 * Mockito: se comprueba lo que cada caso de uso le pide y lo que devuelve.
 */
@ExtendWith(MockitoExtension.class)
class TipoDescuentoUseCasesTest {

    private static final String ID = "ca000007-0000-0000-0000-000000000001";

    @Mock private ITipoDescuentoRepository repository;

    private static TipoDescuento existente() {
        return TipoDescuento.restaurar(ID, "Porcentaje", false);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearGuardaElNombreYDevuelveLoPersistido() {
        // Arrange
        CrearTipoDescuentoUseCase useCase = new CrearTipoDescuentoUseCase(repository);
        ArgumentCaptor<TipoDescuento> guardado = ArgumentCaptor.forClass(TipoDescuento.class);
        when(repository.crear(any())).thenReturn(existente());

        // Act
        TipoDescuentoResponse respuesta = useCase.execute(CrearTipoDescuentoRequest.createFromRequest(Map.of("nombre", " Porcentaje ")));

        // Assert
        verify(repository).crear(guardado.capture());
        assertNull(guardado.getValue().getId());
        assertEquals("Porcentaje", guardado.getValue().getNombre());
        assertFalse(guardado.getValue().isBorrado());
        assertEquals(ID, respuesta.getId());
        assertEquals("Porcentaje", respuesta.getNombre());
        assertFalse(respuesta.isBorrado());
    }

    @Test
    void crearExigeElNombre() {
        // Act
        DomainException sinNombre = assertThrows(DomainException.class, () -> CrearTipoDescuentoRequest.createFromRequest(Map.of()));
        DomainException nombreVacio = assertThrows(DomainException.class, () -> CrearTipoDescuentoRequest.createFromRequest(Map.of("nombre", "  ")));

        // Assert
        assertEquals(400, sinNombre.getCodigo());
        assertEquals(400, nombreVacio.getCodigo());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarCambiaElNombreYGuarda() {
        // Arrange
        ActualizarTipoDescuentoUseCase useCase = new ActualizarTipoDescuentoUseCase(repository);
        TipoDescuento actual = existente();
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(actual));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        TipoDescuentoResponse respuesta = useCase.execute(ID, ActualizarTipoDescuentoRequest.createFromRequest(Map.of("nombre", "Monto fijo")));

        // Assert
        assertEquals("Monto fijo", respuesta.getNombre());
        verify(repository).actualizar(argThat(id -> ID.equals(id.getValue())), argThat(guardado -> guardado == actual));
    }

    @Test
    void actualizarSinNombreConservaElActual() {
        // Arrange
        ActualizarTipoDescuentoUseCase useCase = new ActualizarTipoDescuentoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        TipoDescuentoResponse respuesta = useCase.execute(ID, ActualizarTipoDescuentoRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Porcentaje", respuesta.getNombre());
    }

    @Test
    void actualizarUnoInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarTipoDescuentoUseCase useCase = new ActualizarTipoDescuentoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarTipoDescuentoRequest.createFromRequest(Map.of("nombre", "Monto fijo"))));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID));
        verify(repository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarRechazaUnNombreVacio() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> ActualizarTipoDescuentoRequest.createFromRequest(Map.of("nombre", "   ")));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminarDaDeBajaElRegistroExistente() {
        // Arrange
        EliminarTipoDescuentoUseCase useCase = new EliminarTipoDescuentoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));

        // Act
        useCase.execute(ID);

        // Assert
        verify(repository).eliminar(argThat(id -> ID.equals(id.getValue())));
    }

    @Test
    void eliminarUnoInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarTipoDescuentoUseCase useCase = new EliminarTipoDescuentoUseCase(repository);
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
        ObtenerTipoDescuentoPorIdUseCase useCase = new ObtenerTipoDescuentoPorIdUseCase(repository);
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(existente()));

        // Act
        TipoDescuentoResponse respuesta = useCase.execute(ID);

        // Assert
        assertEquals(ID, respuesta.getId());
        assertEquals("Porcentaje", respuesta.getNombre());
    }

    @Test
    void obtenerPorIdInexistenteRespondeNoEncontrado() {
        // Arrange
        ObtenerTipoDescuentoPorIdUseCase useCase = new ObtenerTipoDescuentoPorIdUseCase(repository);
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
        ObtenerTiposDescuentoUseCase useCase = new ObtenerTiposDescuentoUseCase(repository);
        ObtenerTiposDescuentoRequest peticion = ObtenerTiposDescuentoRequest.createFromRequest(Map.of("skip", 20, "limit", 2, "search", "x"));
        when(repository.obtenerTodos(peticion)).thenReturn(new PaginatedResponse<>(
                List.of(existente(), TipoDescuento.restaurar("ca000007-0000-0000-0000-000000000002", "Monto fijo", false)), 20, 2, 31, true));

        // Act
        PaginatedResponse<List<TipoDescuentoResponse>> pagina = useCase.execute(peticion);

        // Assert
        assertEquals(List.of("Porcentaje", "Monto fijo"), pagina.getData().stream().map(TipoDescuentoResponse::getNombre).toList());
        assertEquals(20, pagina.getSkip());
        assertEquals(2, pagina.getLimit());
        assertEquals(31, pagina.getTotal());
        assertTrue(pagina.isHasMore());
    }

    @Test
    void elListadoVacioSeDevuelveSinFallar() {
        // Arrange
        ObtenerTiposDescuentoUseCase useCase = new ObtenerTiposDescuentoUseCase(repository);
        when(repository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(), 0, 10, 0, false));

        // Act
        PaginatedResponse<List<TipoDescuentoResponse>> pagina = useCase.execute(ObtenerTiposDescuentoRequest.createFromRequest(Map.of()));

        // Assert
        assertTrue(pagina.getData().isEmpty());
        assertFalse(pagina.isHasMore());
    }

    @Test
    void laPeticionDeListadoUsaLaPaginacionPorDefecto() {
        // Act
        ObtenerTiposDescuentoRequest porDefecto = ObtenerTiposDescuentoRequest.createFromRequest(Map.of());
        ObtenerTiposDescuentoRequest informada = ObtenerTiposDescuentoRequest.createFromRequest(Map.of("skip", 5, "limit", 50, "search", "caf"));

        // Assert
        assertEquals(0, porDefecto.getSkip());
        assertEquals(10, porDefecto.getLimit());
        assertEquals("", porDefecto.getSearch());
        assertEquals(5, informada.getSkip());
        assertEquals(50, informada.getLimit());
        assertEquals("caf", informada.getSearch());
    }
}
