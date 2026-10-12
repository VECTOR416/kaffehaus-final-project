package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ActualizarEntidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.CrearEntidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.EntidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ObtenerEntidadesRequest;

/** Pruebas unitarias de los casos de uso CRUD de Entidad (la empresa detras de un proveedor). */
@ExtendWith(MockitoExtension.class)
class EntidadUseCasesTest {

    private static final String ID = "ae000000-0000-0000-0000-000000000001";

    @Mock private IEntidadRepository repository;

    private static Entidad tostadora() {
        return Entidad.restaurar(ID, "Tostadora Andina", "1020304050", "70000000", "Cafe en grano", false);
    }

    @Test
    void crearGuardaLosDatosYDevuelveLoPersistido() {
        // Arrange
        CrearEntidadUseCase useCase = new CrearEntidadUseCase(repository);
        ArgumentCaptor<Entidad> guardada = ArgumentCaptor.forClass(Entidad.class);
        when(repository.crear(any())).thenReturn(tostadora());

        // Act
        EntidadResponse respuesta = useCase.execute(CrearEntidadRequest.createFromRequest(Map.of(
                "nombre", "Tostadora Andina", "nit", "1020304050", "telefono", "70000000", "descripcion", "Cafe en grano")));

        // Assert
        verify(repository).crear(guardada.capture());
        assertNull(guardada.getValue().getId());
        assertEquals("Tostadora Andina", guardada.getValue().getNombre());
        assertEquals("1020304050", guardada.getValue().getNit());
        assertEquals(ID, respuesta.getId());
        assertEquals("Cafe en grano", respuesta.getDescripcion());
    }

    @Test
    void actualizarCambiaSoloLoInformado() {
        // Arrange
        ActualizarEntidadUseCase useCase = new ActualizarEntidadUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(tostadora()));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        EntidadResponse respuesta = useCase.execute(ID, ActualizarEntidadRequest.createFromRequest(Map.of("telefono", "79999999")));

        // Assert
        assertEquals("Tostadora Andina", respuesta.getNombre());
        assertEquals("79999999", respuesta.getTelefono());
        verify(repository).actualizar(argThat(id -> ID.equals(id.getValue())), any());
    }

    @Test
    void actualizarUnaInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarEntidadUseCase useCase = new ActualizarEntidadUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarEntidadRequest.createFromRequest(Map.of("nombre", "x"))));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID));
        verify(repository, never()).actualizar(any(), any());
    }

    @Test
    void eliminarDaDeBajaLaEntidadExistente() {
        // Arrange
        EliminarEntidadUseCase useCase = new EliminarEntidadUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(tostadora()));

        // Act
        useCase.execute(ID);

        // Assert
        verify(repository).eliminar(argThat(id -> ID.equals(id.getValue())));
    }

    @Test
    void eliminarUnaInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarEntidadUseCase useCase = new EliminarEntidadUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(repository, never()).eliminar(any());
    }

    @Test
    void obtenerPorIdDevuelveLaEntidadOResponde404() {
        // Arrange
        ObtenerEntidadPorIdUseCase useCase = new ObtenerEntidadPorIdUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(tostadora())).thenReturn(Optional.empty());

        // Act
        EntidadResponse encontrada = useCase.execute(ID);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals("Tostadora Andina", encontrada.getNombre());
        assertEquals(404, error.getCodigo());
    }

    @Test
    void elListadoConvierteCadaEntidadYConservaLaPaginacion() {
        // Arrange
        ObtenerEntidadesUseCase useCase = new ObtenerEntidadesUseCase(repository);
        when(repository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(tostadora()), 3, 1, 8, true));

        // Act
        PaginatedResponse<List<EntidadResponse>> pagina = useCase.execute(ObtenerEntidadesRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("1020304050", pagina.getData().get(0).getNit());
        assertEquals(3, pagina.getSkip());
        assertEquals(8, pagina.getTotal());
        assertTrue(pagina.isHasMore());
    }
}
