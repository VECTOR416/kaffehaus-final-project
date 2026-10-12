package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.ActualizarUnidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.CrearUnidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.ObtenerUnidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.UnidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;

/** Pruebas unitarias de los casos de uso CRUD de Unidad. */
@ExtendWith(MockitoExtension.class)
class UnidadUseCasesTest {

    private static final String ID = "c8000000-0000-0000-0000-000000000001";

    @Mock private IUnidadRepository repository;

    private static Unidad gramo() {
        return Unidad.restaurar(ID, "Gramo", "g", false, false);
    }

    @Test
    void crearGuardaLaUnidadConSuAbreviatura() {
        // Arrange
        CrearUnidadUseCase useCase = new CrearUnidadUseCase(repository);
        ArgumentCaptor<Unidad> guardada = ArgumentCaptor.forClass(Unidad.class);
        when(repository.crear(any())).thenReturn(gramo());

        // Act
        UnidadResponse respuesta = useCase.execute(CrearUnidadRequest.createFromRequest(Map.of("unidad", "Gramo", "abreviatura", "g")));

        // Assert
        verify(repository).crear(guardada.capture());
        assertNull(guardada.getValue().getId());
        assertEquals("Gramo", guardada.getValue().getUnidad());
        assertEquals("g", guardada.getValue().getAbreviatura());
        assertEquals(ID, respuesta.getId());
    }

    @Test
    void crearExigeElNombreDeLaUnidad() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> CrearUnidadRequest.createFromRequest(Map.of("abreviatura", "g")));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void actualizarCambiaSoloLoInformado() {
        // Arrange
        ActualizarUnidadUseCase useCase = new ActualizarUnidadUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(gramo()));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        UnidadResponse respuesta = useCase.execute(ID, ActualizarUnidadRequest.createFromRequest(Map.of("abreviatura", "gr")));

        // Assert
        assertEquals("Gramo", respuesta.getUnidad());
        assertEquals("gr", respuesta.getAbreviatura());
    }

    @Test
    void actualizarUnaInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarUnidadUseCase useCase = new ActualizarUnidadUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarUnidadRequest.createFromRequest(Map.of("unidad", "x"))));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(repository, never()).actualizar(any(), any());
    }

    @Test
    void eliminarDaDeBajaLaExistenteYRechazaLaInexistente() {
        // Arrange
        EliminarUnidadUseCase useCase = new EliminarUnidadUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(gramo())).thenReturn(Optional.empty());

        // Act
        useCase.execute(ID);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        verify(repository, times(1)).eliminar(argThat(id -> ID.equals(id.getValue())));
        assertEquals(404, error.getCodigo());
    }

    @Test
    void obtenerPorIdDevuelveLaUnidadOResponde404() {
        // Arrange
        ObtenerUnidadPorIdUseCase useCase = new ObtenerUnidadPorIdUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(gramo())).thenReturn(Optional.empty());

        // Act
        UnidadResponse encontrada = useCase.execute(ID);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals("g", encontrada.getAbreviatura());
        assertEquals(404, error.getCodigo());
    }

    @Test
    void elListadoConvierteCadaUnidadYConservaLaPaginacion() {
        // Arrange
        ObtenerUnidadesUseCase useCase = new ObtenerUnidadesUseCase(repository);
        when(repository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(gramo()), 0, 10, 1, false));

        // Act
        PaginatedResponse<List<UnidadResponse>> pagina = useCase.execute(ObtenerUnidadesRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Gramo", pagina.getData().get(0).getUnidad());
        assertEquals(1, pagina.getTotal());
        assertFalse(pagina.isHasMore());
    }
}
