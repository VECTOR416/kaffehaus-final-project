package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.ActualizarCategoriaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CategoriaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CrearCategoriaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.ObtenerCategoriasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;

/** Pruebas unitarias de los casos de uso CRUD de Categoria. */
@ExtendWith(MockitoExtension.class)
class CategoriaUseCasesTest {

    private static final String ID = "c7000000-0000-0000-0000-000000000001";

    @Mock private ICategoriaRepository repository;

    private static Categoria cafes() {
        return Categoria.restaurar(ID, "Cafes", true, false);
    }

    @Test
    void crearGuardaLaCategoriaConLoQueLlega() {
        // Arrange
        CrearCategoriaUseCase useCase = new CrearCategoriaUseCase(repository);
        ArgumentCaptor<Categoria> guardada = ArgumentCaptor.forClass(Categoria.class);
        when(repository.crear(any())).thenReturn(cafes());

        // Act
        CategoriaResponse respuesta = useCase.execute(CrearCategoriaRequest.createFromRequest(Map.of("nombre", "Cafes", "esBorrable", false)));

        // Assert
        verify(repository).crear(guardada.capture());
        assertNull(guardada.getValue().getId());
        assertEquals("Cafes", guardada.getValue().getNombre());
        assertFalse(guardada.getValue().isEsBorrable());
        assertEquals(ID, respuesta.getId());
    }

    @Test
    void crearExigeElNombreYValidaElBooleano() {
        // Act
        DomainException sinNombre = assertThrows(DomainException.class, () -> CrearCategoriaRequest.createFromRequest(Map.of()));
        DomainException booleanoInvalido = assertThrows(DomainException.class,
                () -> CrearCategoriaRequest.createFromRequest(Map.of("nombre", "Cafes", "esBorrable", "quizas")));

        // Assert
        assertEquals(400, sinNombre.getCodigo());
        assertEquals(400, booleanoInvalido.getCodigo());
    }

    @Test
    void actualizarCambiaSoloLoInformado() {
        // Arrange
        ActualizarCategoriaUseCase useCase = new ActualizarCategoriaUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(cafes()));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        CategoriaResponse respuesta = useCase.execute(ID, ActualizarCategoriaRequest.createFromRequest(Map.of("nombre", "Bebidas")));

        // Assert
        assertEquals("Bebidas", respuesta.getNombre());
        assertTrue(respuesta.isEsBorrable());
        verify(repository).actualizar(argThat(id -> ID.equals(id.getValue())), any());
    }

    @Test
    void actualizarUnaInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarCategoriaUseCase useCase = new ActualizarCategoriaUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarCategoriaRequest.createFromRequest(Map.of("nombre", "x"))));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(repository, never()).actualizar(any(), any());
    }

    @Test
    void eliminarDaDeBajaLaExistenteYRechazaLaInexistente() {
        // Arrange
        EliminarCategoriaUseCase useCase = new EliminarCategoriaUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(cafes())).thenReturn(Optional.empty());

        // Act
        useCase.execute(ID);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        verify(repository, times(1)).eliminar(argThat(id -> ID.equals(id.getValue())));
        assertEquals(404, error.getCodigo());
    }

    @Test
    void obtenerPorIdDevuelveLaCategoriaOResponde404() {
        // Arrange
        ObtenerCategoriaPorIdUseCase useCase = new ObtenerCategoriaPorIdUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(cafes())).thenReturn(Optional.empty());

        // Act
        CategoriaResponse encontrada = useCase.execute(ID);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals("Cafes", encontrada.getNombre());
        assertEquals(404, error.getCodigo());
    }

    @Test
    void elListadoConvierteCadaCategoriaYConservaLaPaginacion() {
        // Arrange
        ObtenerCategoriasUseCase useCase = new ObtenerCategoriasUseCase(repository);
        when(repository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(cafes()), 4, 2, 9, true));

        // Act
        PaginatedResponse<List<CategoriaResponse>> pagina = useCase.execute(ObtenerCategoriasRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Cafes", pagina.getData().get(0).getNombre());
        assertEquals(4, pagina.getSkip());
        assertEquals(9, pagina.getTotal());
        assertTrue(pagina.isHasMore());
    }
}
