package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases;

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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ActualizarTipoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.CrearTipoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ObtenerTiposRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.TipoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los casos de uso CRUD del catalogo Tipo. El repositorio es un doble de
 * Mockito: se comprueba lo que cada caso de uso le pide y lo que devuelve.
 */
@ExtendWith(MockitoExtension.class)
class TipoUseCasesTest {

    private static final String ID = "ca000002-0000-0000-0000-000000000001";

    @Mock private ITipoRepository repository;
    @Mock private IProductoRepository productoRepository;

    private static Tipo existente() {
        return Tipo.restaurar(ID, "Producto", false);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearGuardaElNombreYDevuelveLoPersistido() {
        // Arrange
        CrearTipoUseCase useCase = new CrearTipoUseCase(repository);
        ArgumentCaptor<Tipo> guardado = ArgumentCaptor.forClass(Tipo.class);
        when(repository.crear(any())).thenReturn(existente());

        // Act
        TipoResponse respuesta = useCase.execute(CrearTipoRequest.createFromRequest(Map.of("nombre", " Producto ")));

        // Assert
        verify(repository).crear(guardado.capture());
        assertNull(guardado.getValue().getId());
        assertEquals("Producto", guardado.getValue().getNombre());
        assertFalse(guardado.getValue().isBorrado());
        assertEquals(ID, respuesta.getId());
        assertEquals("Producto", respuesta.getNombre());
        assertFalse(respuesta.isBorrado());
    }

    @Test
    void crearExigeElNombre() {
        // Act
        DomainException sinNombre = assertThrows(DomainException.class, () -> CrearTipoRequest.createFromRequest(Map.of()));
        DomainException nombreVacio = assertThrows(DomainException.class, () -> CrearTipoRequest.createFromRequest(Map.of("nombre", "  ")));

        // Assert
        assertEquals(400, sinNombre.getCodigo());
        assertEquals(400, nombreVacio.getCodigo());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarCambiaElNombreYGuarda() {
        // Arrange
        ActualizarTipoUseCase useCase = new ActualizarTipoUseCase(repository);
        Tipo actual = existente();
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(actual));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        TipoResponse respuesta = useCase.execute(ID, ActualizarTipoRequest.createFromRequest(Map.of("nombre", "Insumo")));

        // Assert
        assertEquals("Insumo", respuesta.getNombre());
        verify(repository).actualizar(argThat(id -> ID.equals(id.getValue())), argThat(guardado -> guardado == actual));
    }

    @Test
    void actualizarSinNombreConservaElActual() {
        // Arrange
        ActualizarTipoUseCase useCase = new ActualizarTipoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));
        when(repository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));

        // Act
        TipoResponse respuesta = useCase.execute(ID, ActualizarTipoRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Producto", respuesta.getNombre());
    }

    @Test
    void actualizarUnoInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarTipoUseCase useCase = new ActualizarTipoUseCase(repository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarTipoRequest.createFromRequest(Map.of("nombre", "Insumo"))));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID));
        verify(repository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarRechazaUnNombreVacio() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> ActualizarTipoRequest.createFromRequest(Map.of("nombre", "   ")));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminarDaDeBajaElRegistroExistente() {
        // Arrange
        EliminarTipoUseCase useCase = new EliminarTipoUseCase(repository, productoRepository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));

        // Act
        useCase.execute(ID);

        // Assert
        verify(repository).eliminar(argThat(id -> ID.equals(id.getValue())));
    }

    @Test
    void eliminarUnoInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarTipoUseCase useCase = new EliminarTipoUseCase(repository, productoRepository);
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
        ObtenerTipoPorIdUseCase useCase = new ObtenerTipoPorIdUseCase(repository);
        when(repository.obtenerPorId(argThat(id -> ID.equals(id.getValue())))).thenReturn(Optional.of(existente()));

        // Act
        TipoResponse respuesta = useCase.execute(ID);

        // Assert
        assertEquals(ID, respuesta.getId());
        assertEquals("Producto", respuesta.getNombre());
    }

    @Test
    void obtenerPorIdInexistenteRespondeNoEncontrado() {
        // Arrange
        ObtenerTipoPorIdUseCase useCase = new ObtenerTipoPorIdUseCase(repository);
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
        ObtenerTiposUseCase useCase = new ObtenerTiposUseCase(repository);
        ObtenerTiposRequest peticion = ObtenerTiposRequest.createFromRequest(Map.of("skip", 20, "limit", 2, "search", "x"));
        when(repository.obtenerTodos(peticion)).thenReturn(new PaginatedResponse<>(
                List.of(existente(), Tipo.restaurar("ca000002-0000-0000-0000-000000000002", "Insumo", false)), 20, 2, 31, true));

        // Act
        PaginatedResponse<List<TipoResponse>> pagina = useCase.execute(peticion);

        // Assert
        assertEquals(List.of("Producto", "Insumo"), pagina.getData().stream().map(TipoResponse::getNombre).toList());
        assertEquals(20, pagina.getSkip());
        assertEquals(2, pagina.getLimit());
        assertEquals(31, pagina.getTotal());
        assertTrue(pagina.isHasMore());
    }

    @Test
    void elListadoVacioSeDevuelveSinFallar() {
        // Arrange
        ObtenerTiposUseCase useCase = new ObtenerTiposUseCase(repository);
        when(repository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(), 0, 10, 0, false));

        // Act
        PaginatedResponse<List<TipoResponse>> pagina = useCase.execute(ObtenerTiposRequest.createFromRequest(Map.of()));

        // Assert
        assertTrue(pagina.getData().isEmpty());
        assertFalse(pagina.isHasMore());
    }

    @Test
    void laPeticionDeListadoUsaLaPaginacionPorDefecto() {
        // Act
        ObtenerTiposRequest porDefecto = ObtenerTiposRequest.createFromRequest(Map.of());
        ObtenerTiposRequest informada = ObtenerTiposRequest.createFromRequest(Map.of("skip", 5, "limit", 50, "search", "caf"));

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
        EliminarTipoUseCase useCase = new EliminarTipoUseCase(repository, productoRepository);
        when(repository.obtenerPorId(any())).thenReturn(Optional.of(existente()));
        when(productoRepository.existeConTipo(any())).thenReturn(true);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("productos vivos"));
        verify(repository, never()).eliminar(any());
    }
}
