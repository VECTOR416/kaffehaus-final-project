package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases;

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

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.ActualizarTipoPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.CrearTipoPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.ObtenerTiposPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.TipoPagoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los casos de uso CRUD del catalogo TipoPago. El repositorio es un doble de
 * Mockito: se comprueba lo que cada caso de uso le pide y lo que devuelve.
 */
@ExtendWith(MockitoExtension.class)
class TipoPagoUseCasesTest {

    private static final String ID = "ca000008-0000-0000-0000-000000000001";

    @Mock private ITipoPagoRepository repository;

    private static TipoPago existente() {
        return TipoPago.restaurar(ID, "Efectivo", false);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearGuardaElNombreYDevuelveLoPersistido() {
        // Arrange
        CrearTipoPagoUseCase useCase = new CrearTipoPagoUseCase(repository);
        ArgumentCaptor<TipoPago> guardado = ArgumentCaptor.forClass(TipoPago.class);
        when(repository.crear(any())).thenReturn(existente());

        // Act
        TipoPagoResponse respuesta = useCase.execute(CrearTipoPagoRequest.createFromRequest(Map.of("nombre", " Efectivo ")));

        // Assert
        verify(repository).crear(guardado.capture());
        assertNull(guardado.getValue().getId());
        assertEquals("Efectivo", guardado.getValue().getNombre());
        assertFalse(guardado.getValue().isBorrado());
        assertEquals(ID, respuesta.getId());
        assertEquals("Efectivo", respuesta.getNombre());
        assertFalse(respuesta.isBorrado());
    }

    @Test
    void crearExigeElNombre() {
        // Act
        DomainException sinNombre = assertThrows(DomainException.class, () -> CrearTipoPagoRequest.createFromRequest(Map.of()));
        DomainException nombreVacio = assertThrows(DomainException.class, () -> CrearTipoPagoRequest.createFromRequest(Map.of("nombre", "  ")));

        // Assert
        assertEquals(400, sinNombre.getCodigo());
        assertEquals(400, nombreVacio.getCodigo());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarCambiaElNombreYGuarda() {
        // Arrange
        ActualizarTipoPagoUseCase useCase = new ActualizarTipoPagoUseCase(repository);
        TipoPago actual = TipoPago.restaurar(ID, "Vale", false);
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(actual));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        TipoPagoResponse respuesta = useCase.execute(ID, ActualizarTipoPagoRequest.createFromRequest(Map.of("nombre", "Tarjeta")));

        // Assert
        assertEquals("Tarjeta", respuesta.getNombre());
        verify(repository).actualizar(argThat(id -> ID.equals(id.getValue())), argThat(guardado -> guardado == actual));
    }

    @Test
    void actualizarSinNombreConservaElActual() {
        // Arrange
        ActualizarTipoPagoUseCase useCase = new ActualizarTipoPagoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        TipoPagoResponse respuesta = useCase.execute(ID, ActualizarTipoPagoRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Efectivo", respuesta.getNombre());
    }

    @Test
    void actualizarUnoInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarTipoPagoUseCase useCase = new ActualizarTipoPagoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarTipoPagoRequest.createFromRequest(Map.of("nombre", "Tarjeta"))));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID));
        verify(repository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarRechazaUnNombreVacio() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> ActualizarTipoPagoRequest.createFromRequest(Map.of("nombre", "   ")));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminarDaDeBajaElRegistroExistente() {
        // Arrange
        EliminarTipoPagoUseCase useCase = new EliminarTipoPagoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));

        // Act
        useCase.execute(ID);

        // Assert
        verify(repository).eliminar(argThat(id -> ID.equals(id.getValue())));
    }

    @Test
    void eliminarUnoInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarTipoPagoUseCase useCase = new EliminarTipoPagoUseCase(repository);
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
        ObtenerTipoPagoPorIdUseCase useCase = new ObtenerTipoPagoPorIdUseCase(repository);
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(existente()));

        // Act
        TipoPagoResponse respuesta = useCase.execute(ID);

        // Assert
        assertEquals(ID, respuesta.getId());
        assertEquals("Efectivo", respuesta.getNombre());
    }

    @Test
    void obtenerPorIdInexistenteRespondeNoEncontrado() {
        // Arrange
        ObtenerTipoPagoPorIdUseCase useCase = new ObtenerTipoPagoPorIdUseCase(repository);
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
        ObtenerTiposPagoUseCase useCase = new ObtenerTiposPagoUseCase(repository);
        ObtenerTiposPagoRequest peticion = ObtenerTiposPagoRequest.createFromRequest(Map.of("skip", 20, "limit", 2, "search", "x"));
        when(repository.obtenerTodos(peticion)).thenReturn(new PaginatedResponse<>(
                List.of(existente(), TipoPago.restaurar("ca000008-0000-0000-0000-000000000002", "Tarjeta", false)), 20, 2, 31, true));

        // Act
        PaginatedResponse<List<TipoPagoResponse>> pagina = useCase.execute(peticion);

        // Assert
        assertEquals(List.of("Efectivo", "Tarjeta"), pagina.getData().stream().map(TipoPagoResponse::getNombre).toList());
        assertEquals(20, pagina.getSkip());
        assertEquals(2, pagina.getLimit());
        assertEquals(31, pagina.getTotal());
        assertTrue(pagina.isHasMore());
    }

    @Test
    void elListadoVacioSeDevuelveSinFallar() {
        // Arrange
        ObtenerTiposPagoUseCase useCase = new ObtenerTiposPagoUseCase(repository);
        when(repository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(), 0, 10, 0, false));

        // Act
        PaginatedResponse<List<TipoPagoResponse>> pagina = useCase.execute(ObtenerTiposPagoRequest.createFromRequest(Map.of()));

        // Assert
        assertTrue(pagina.getData().isEmpty());
        assertFalse(pagina.isHasMore());
    }

    @Test
    void laPeticionDeListadoUsaLaPaginacionPorDefecto() {
        // Act
        ObtenerTiposPagoRequest porDefecto = ObtenerTiposPagoRequest.createFromRequest(Map.of());
        ObtenerTiposPagoRequest informada = ObtenerTiposPagoRequest.createFromRequest(Map.of("skip", 5, "limit", 50, "search", "caf"));

        // Assert
        assertEquals(0, porDefecto.getSkip());
        assertEquals(10, porDefecto.getLimit());
        assertEquals("", porDefecto.getSearch());
        assertEquals(5, informada.getSkip());
        assertEquals(50, informada.getLimit());
        assertEquals("caf", informada.getSearch());
    }
}
