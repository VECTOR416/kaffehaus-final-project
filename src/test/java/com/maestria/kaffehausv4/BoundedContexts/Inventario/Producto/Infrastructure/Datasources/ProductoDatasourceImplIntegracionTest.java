package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CrearCategoriaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases.CrearCategoriaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases.EliminarCategoriaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases.ObtenerCategoriaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ObtenerProductosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.ObtenerProductosUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/**
 * Mapeo y consultas comunes contra la base: paginacion con skip/limit/search, presentaciones de un
 * producto y borrado logico (arquitectura.md 4.2, 4.5).
 */
class ProductoDatasourceImplIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private EscenarioDeCafeteria escenario;
    @Autowired private ObtenerProductosUseCase obtenerProductosUseCase;
    @Autowired private IProductoRepository productoRepository;
    @Autowired private CrearCategoriaUseCase crearCategoriaUseCase;
    @Autowired private EliminarCategoriaUseCase eliminarCategoriaUseCase;
    @Autowired private ObtenerCategoriaPorIdUseCase obtenerCategoriaPorIdUseCase;
    @Autowired private ICategoriaRepository categoriaRepository;

    @BeforeEach
    void prepararEscenario() {
        escenario.sembrar();
    }

    private PaginatedResponse<List<ProductoResponse>> pagina(Integer skip, Integer limit, String search) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("skip", skip);
        parametros.put("limit", limit);
        parametros.put("search", search);
        return obtenerProductosUseCase.execute(ObtenerProductosRequest.createFromRequest(parametros));
    }

    @Test
    void elListadoPaginaYBuscaPorNombreSinDistinguirMayusculas() {
        // Arrange
        escenario.producto("Cafe molido", "10", true, true, "Unidad", null);
        escenario.producto("Cafe en grano", "12", true, true, "Unidad", null);
        escenario.producto("Te verde", "8", true, true, "Unidad", null);

        // Act
        PaginatedResponse<List<ProductoResponse>> cafes = pagina(0, 10, "CAFE");
        PaginatedResponse<List<ProductoResponse>> primeraDeUno = pagina(0, 1, null);
        PaginatedResponse<List<ProductoResponse>> segundaDeUno = pagina(1, 1, null);

        // Assert
        assertEquals(2, cafes.getData().size());
        assertEquals(3, primeraDeUno.getTotal());
        assertEquals(1, primeraDeUno.getData().size());
        assertTrue(primeraDeUno.isHasMore());
        assertEquals(1, segundaDeUno.getData().size());
        assertTrue(!primeraDeUno.getData().get(0).getId().equals(segundaDeUno.getData().get(0).getId()));
    }

    @Test
    void obtenerPresentacionesDevuelveLasQueTienenAlProductoPorPadre() {
        // Arrange
        String harina = escenario.producto("Harina", "0", false, true, "Gramo", null);
        String bolsa = escenario.producto("Bolsa de harina 1 kg", "12", true, true, "Kilogramo", harina);
        escenario.producto("Azucar", "0", false, true, "Gramo", null);

        // Act
        var presentaciones = productoRepository.obtenerPresentaciones(new ProductoId(harina));

        // Assert
        assertEquals(1, presentaciones.size());
        assertEquals(bolsa, presentaciones.get(0).getId().getValue());
    }

    @Test
    void laBajaEsLogicaYUnaCategoriaNoBorrableSeConserva() {
        // Arrange
        String borrable = crearCategoriaUseCase.execute(CrearCategoriaRequest.createFromRequest(
                Map.of("nombre", "Temporada", "esBorrable", true))).getId();
        String sembrada = categoriaRepository.obtenerPorNombre("Insumos").orElseThrow().getId().getValue();

        // Act
        eliminarCategoriaUseCase.execute(borrable);
        DomainException noBorrable = assertThrows(DomainException.class, () -> eliminarCategoriaUseCase.execute(sembrada));

        // Assert
        assertEquals(404, assertThrows(DomainException.class, () -> obtenerCategoriaPorIdUseCase.execute(borrable)).getCodigo());
        assertEquals(409, noBorrable.getCodigo());
        assertTrue(categoriaRepository.obtenerPorNombre("Insumos").isPresent());
    }
}
