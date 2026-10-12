package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ActualizarExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ObtenerExistenciasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas unitarias de los casos de uso CRUD de Existencia. El alta manual esta cerrada a
 * proposito: una existencia solo nace de una apertura o de una produccion.
 */
@ExtendWith(MockitoExtension.class)
class ExistenciaCrudUseCasesTest {

    private static final String ID = "ce000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE = "ce000000-0000-0000-0000-000000000002";
    private static final String ID_INEXISTENTE = "ce000000-0000-0000-0000-0000000000ee";

    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IUnidadRepository unidadRepository;

    @BeforeEach
    void prepararCatalogos() {
        lenient().when(estadoExistenciaRepository.obtenerPorId(argThat(id -> id != null && ID_ESTADO_ABIERTA.equals(id.getValue()))))
                .thenReturn(Optional.of(ABIERTA));
        lenient().when(estadoExistenciaRepository.obtenerPorId(argThat(id -> id != null && ID_ESTADO_AGOTADA.equals(id.getValue()))))
                .thenReturn(Optional.of(AGOTADA));
        lenient().when(unidadRepository.obtenerPorId(argThat(id -> id != null && ID_UNIDAD.equals(id.getValue()))))
                .thenReturn(Optional.of(UNIDAD));
        lenient().when(existenciaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    private static Existencia abierta() {
        return existencia(ID, ID_DETALLE, "1000", "600", null);
    }

    @Test
    void lasExistenciasNoSeCreanAMano() {
        // Arrange
        CrearExistenciaUseCase useCase = new CrearExistenciaUseCase();

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(null));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("no se crean a mano"));
    }

    @Test
    void actualizarSoloCorrigeLaFechaDeVencimiento() {
        // Arrange
        ActualizarExistenciaUseCase useCase = new ActualizarExistenciaUseCase(existenciaRepository, loteRepository, estadoExistenciaRepository, unidadRepository);
        when(existenciaRepository.obtenerPorId(any())).thenReturn(Optional.of(abierta()));

        // Act
        ExistenciaResponse respuesta = useCase.execute(ID, ActualizarExistenciaRequest.createFromRequest(Map.of("fechaVencimiento", "2027-01-31")));

        // Assert
        assertEquals(LocalDate.of(2027, 1, 31), respuesta.getFechaVencimiento());
        assertEquals(0, new BigDecimal("600").compareTo(respuesta.getContenidoActual()));
        assertEquals("Abierta", respuesta.getEstadoExistencia().getNombre());
        assertEquals("Gramo", respuesta.getUnidad().getUnidad());
        verify(existenciaRepository).actualizar(argThat(id -> ID.equals(id.getValue())), any());
        verifyNoInteractions(loteRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"contenidoInicial", "contenidoActual", "fechaAcabado", "fechaApertura", "idDetalleLote", "idEstadoExistencia", "idUnidad"})
    void laPeticionRechazaLosCamposQueSoloCambianPorAperturaMermaOAjuste(String campo) {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> ActualizarExistenciaRequest.createFromRequest(Map.of(campo, "1")));

        // Assert
        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("'" + campo + "'"));
    }

    @Test
    void actualizarUnaInexistenteRespondeNoEncontrado() {
        // Arrange
        ActualizarExistenciaUseCase useCase = new ActualizarExistenciaUseCase(existenciaRepository, loteRepository, estadoExistenciaRepository, unidadRepository);
        when(existenciaRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarExistenciaRequest.createFromRequest(Map.of())));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID));
    }

    @Test
    void eliminarDaDeBajaLaExistenteYRechazaLaInexistente() {
        // Arrange
        EliminarExistenciaUseCase useCase = new EliminarExistenciaUseCase(existenciaRepository);
        when(existenciaRepository.obtenerPorId(any())).thenReturn(Optional.of(abierta())).thenReturn(Optional.empty());

        // Act
        useCase.execute(ID);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        verify(existenciaRepository, times(1)).eliminar(argThat(id -> ID.equals(id.getValue())));
        assertEquals(404, error.getCodigo());
    }

    @Test
    void obtenerPorIdDevuelveLaExistenciaConSuEstadoYSuUnidad() {
        // Arrange
        ObtenerExistenciaPorIdUseCase useCase = new ObtenerExistenciaPorIdUseCase(existenciaRepository, estadoExistenciaRepository, unidadRepository);
        when(existenciaRepository.obtenerPorId(any())).thenReturn(Optional.of(abierta()));

        // Act
        ExistenciaResponse respuesta = useCase.execute(ID);

        // Assert
        assertEquals(ID, respuesta.getId());
        assertEquals(0, new BigDecimal("600").compareTo(respuesta.getContenidoActual()));
        assertEquals("Abierta", respuesta.getEstadoExistencia().getNombre());
        assertEquals(ID_DETALLE, respuesta.getIdDetalleLote());
    }

    @Test
    void obtenerPorIdResponde404SiFaltaLaExistenciaSuEstadoOSuUnidad() {
        // Arrange
        ObtenerExistenciaPorIdUseCase useCase = new ObtenerExistenciaPorIdUseCase(existenciaRepository, estadoExistenciaRepository, unidadRepository);
        when(existenciaRepository.obtenerPorId(any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existencia(ID, ID_DETALLE, "10", "5", null, ID_INEXISTENTE)));

        // Act
        DomainException sinExistencia = assertThrows(DomainException.class, () -> useCase.execute(ID));
        DomainException sinUnidad = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals(404, sinExistencia.getCodigo());
        assertEquals(404, sinUnidad.getCodigo());
    }

    @Test
    void elListadoResuelveEstadoYUnidadDeCadaExistencia() {
        // Arrange
        ObtenerExistenciasUseCase useCase = new ObtenerExistenciasUseCase(existenciaRepository, estadoExistenciaRepository, unidadRepository);
        when(existenciaRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(
                List.of(abierta(), existencia("ce000000-0000-0000-0000-000000000009", ID_DETALLE, "1000", "0", null)), 0, 10, 2, false));

        // Act
        PaginatedResponse<List<ExistenciaResponse>> pagina = useCase.execute(ObtenerExistenciasRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals(List.of("Abierta", "Agotada"), pagina.getData().stream().map(e -> e.getEstadoExistencia().getNombre()).toList());
        assertEquals(2, pagina.getTotal());
    }
}
