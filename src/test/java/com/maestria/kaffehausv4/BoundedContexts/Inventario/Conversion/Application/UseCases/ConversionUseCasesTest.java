package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ActualizarConversionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ConversionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.CrearConversionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ObtenerConversionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;

/** Pruebas unitarias de los casos de uso CRUD de Conversion entre unidades. */
@ExtendWith(MockitoExtension.class)
class ConversionUseCasesTest {

    private static final String ID = "c9000000-0000-0000-0000-000000000001";
    private static final String ID_KG = "c9000000-0000-0000-0000-00000000000a";
    private static final String ID_G = "c9000000-0000-0000-0000-00000000000b";
    private static final String ID_INEXISTENTE = "c9000000-0000-0000-0000-0000000000ee";

    @Mock private IConversionRepository conversionRepository;
    @Mock private IUnidadRepository unidadRepository;

    @BeforeEach
    void prepararUnidades() {
        lenient().when(unidadRepository.obtenerPorId(argThat(id -> id != null && ID_KG.equals(id.getValue()))))
                .thenReturn(Optional.of(Unidad.restaurar(ID_KG, "Kilogramo", "kg", false, false)));
        lenient().when(unidadRepository.obtenerPorId(argThat(id -> id != null && ID_G.equals(id.getValue()))))
                .thenReturn(Optional.of(Unidad.restaurar(ID_G, "Gramo", "g", false, false)));
        lenient().when(conversionRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    private static Conversion kiloAGramo() {
        return Conversion.restaurar(ID, new BigDecimal("1000"), false, new UnidadId(ID_KG), new UnidadId(ID_G), false);
    }

    private static Map<String, Object> alta(String origen, String destino) {
        return Map.of("factorConversion", "1000", "idUnidadOrigen", origen, "idUnidadDestino", destino);
    }

    @Test
    void crearGuardaLaConversionEntreDosUnidadesExistentes() {
        // Arrange
        CrearConversionUseCase useCase = new CrearConversionUseCase(conversionRepository, unidadRepository);
        ArgumentCaptor<Conversion> guardada = ArgumentCaptor.forClass(Conversion.class);
        when(conversionRepository.crear(any())).thenReturn(kiloAGramo());

        // Act
        ConversionResponse respuesta = useCase.execute(CrearConversionRequest.createFromRequest(alta(ID_KG, ID_G)));

        // Assert
        verify(conversionRepository).crear(guardada.capture());
        assertEquals(0, new BigDecimal("1000").compareTo(guardada.getValue().getFactorConversion()));
        assertEquals(ID_KG, guardada.getValue().getUnidadOrigenId().getValue());
        assertEquals(ID_G, guardada.getValue().getUnidadDestinoId().getValue());
        assertEquals("Kilogramo", respuesta.getUnidadOrigen().getUnidad());
        assertEquals("Gramo", respuesta.getUnidadDestino().getUnidad());
    }

    @Test
    void crearConUnaUnidadInexistenteNoGuardaNada() {
        // Arrange
        CrearConversionUseCase useCase = new CrearConversionUseCase(conversionRepository, unidadRepository);

        // Act
        DomainException sinOrigen = assertThrows(DomainException.class,
                () -> useCase.execute(CrearConversionRequest.createFromRequest(alta(ID_INEXISTENTE, ID_G))));
        DomainException sinDestino = assertThrows(DomainException.class,
                () -> useCase.execute(CrearConversionRequest.createFromRequest(alta(ID_KG, ID_INEXISTENTE))));

        // Assert
        assertEquals(404, sinOrigen.getCodigo());
        assertEquals(404, sinDestino.getCodigo());
        verify(conversionRepository, never()).crear(any());
    }

    @Test
    void crearExigeFactorYAmbasUnidades() {
        // Act
        DomainException sinFactor = assertThrows(DomainException.class,
                () -> CrearConversionRequest.createFromRequest(Map.of("idUnidadOrigen", ID_KG, "idUnidadDestino", ID_G)));
        DomainException sinDestino = assertThrows(DomainException.class,
                () -> CrearConversionRequest.createFromRequest(Map.of("factorConversion", "1000", "idUnidadOrigen", ID_KG)));

        // Assert
        assertEquals(400, sinFactor.getCodigo());
        assertEquals(400, sinDestino.getCodigo());
    }

    @Test
    void actualizarCambiaElFactorSinTocarLasUnidades() {
        // Arrange
        ActualizarConversionUseCase useCase = new ActualizarConversionUseCase(conversionRepository, unidadRepository);
        when(conversionRepository.obtenerPorId(any())).thenReturn(Optional.of(kiloAGramo()));

        // Act
        ConversionResponse respuesta = useCase.execute(ID, ActualizarConversionRequest.createFromRequest(Map.of("factorConversion", "500")));

        // Assert
        assertEquals(0, new BigDecimal("500").compareTo(respuesta.getFactorConversion()));
        assertEquals("Kilogramo", respuesta.getUnidadOrigen().getUnidad());
        assertEquals("Gramo", respuesta.getUnidadDestino().getUnidad());
    }

    @Test
    void actualizarConUnaUnidadInexistenteNoGuarda() {
        // Arrange
        ActualizarConversionUseCase useCase = new ActualizarConversionUseCase(conversionRepository, unidadRepository);
        when(conversionRepository.obtenerPorId(any())).thenReturn(Optional.of(kiloAGramo()));

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarConversionRequest.createFromRequest(Map.of("idUnidadDestino", ID_INEXISTENTE))));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(conversionRepository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarUnaInexistenteRespondeNoEncontrado() {
        // Arrange
        ActualizarConversionUseCase useCase = new ActualizarConversionUseCase(conversionRepository, unidadRepository);
        when(conversionRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarConversionRequest.createFromRequest(Map.of("factorConversion", "2"))));

        // Assert
        assertEquals(404, error.getCodigo());
        verifyNoInteractions(unidadRepository);
    }

    @Test
    void eliminarDaDeBajaLaExistenteYRechazaLaInexistente() {
        // Arrange
        EliminarConversionUseCase useCase = new EliminarConversionUseCase(conversionRepository);
        when(conversionRepository.obtenerPorId(any())).thenReturn(Optional.of(kiloAGramo())).thenReturn(Optional.empty());

        // Act
        useCase.execute(ID);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        verify(conversionRepository, times(1)).eliminar(argThat(id -> ID.equals(id.getValue())));
        assertEquals(404, error.getCodigo());
    }

    @Test
    void obtenerPorIdDevuelveLaConversionConSusUnidades() {
        // Arrange
        ObtenerConversionPorIdUseCase useCase = new ObtenerConversionPorIdUseCase(conversionRepository, unidadRepository);
        when(conversionRepository.obtenerPorId(any())).thenReturn(Optional.of(kiloAGramo()));

        // Act
        ConversionResponse respuesta = useCase.execute(ID);

        // Assert
        assertEquals(ID, respuesta.getId());
        assertEquals("kg", respuesta.getUnidadOrigen().getAbreviatura());
        assertEquals("g", respuesta.getUnidadDestino().getAbreviatura());
    }

    @Test
    void obtenerPorIdResponde404SiFaltaLaConversionOUnaDeSusUnidades() {
        // Arrange
        ObtenerConversionPorIdUseCase useCase = new ObtenerConversionPorIdUseCase(conversionRepository, unidadRepository);
        Conversion conUnidadBorrada = Conversion.restaurar(ID, BigDecimal.ONE, false, new UnidadId(ID_KG), new UnidadId(ID_INEXISTENTE), false);
        when(conversionRepository.obtenerPorId(any())).thenReturn(Optional.empty()).thenReturn(Optional.of(conUnidadBorrada));

        // Act
        DomainException sinConversion = assertThrows(DomainException.class, () -> useCase.execute(ID));
        DomainException sinUnidad = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals(404, sinConversion.getCodigo());
        assertEquals(404, sinUnidad.getCodigo());
    }

    @Test
    void elListadoResuelveLasUnidadesDeCadaConversion() {
        // Arrange
        ObtenerConversionesUseCase useCase = new ObtenerConversionesUseCase(conversionRepository, unidadRepository);
        when(conversionRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(kiloAGramo()), 0, 10, 1, false));

        // Act
        PaginatedResponse<List<ConversionResponse>> pagina = useCase.execute(ObtenerConversionesRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Kilogramo", pagina.getData().get(0).getUnidadOrigen().getUnidad());
        assertEquals(1, pagina.getTotal());
    }
}
