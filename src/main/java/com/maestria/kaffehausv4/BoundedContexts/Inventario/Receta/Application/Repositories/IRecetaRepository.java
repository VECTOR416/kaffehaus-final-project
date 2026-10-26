package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ObtenerRecetasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IRecetaRepository {
    Receta crear(Receta receta);
    Receta actualizar(RecetaId recetaId, Receta receta);
    Optional<Receta> obtenerPorId(RecetaId recetaId);
    PaginatedResponse<List<Receta>> obtenerTodos(ObtenerRecetasRequest obtenerRecetasRequest);
    void eliminar(RecetaId recetaId);

    /**
     * Receta vigente de un producto: la viva mas reciente que lo obtiene. La usa el descuento
     * de insumos de un producto bajo demanda (MTO) al venderse.
     */
    Optional<Receta> obtenerPorProductoObtenido(ProductoId productoId);

    /** Recetas vivas que usan el producto como ingrediente vivo (logica.md PRODUCTO-20). */
    List<Receta> obtenerPorIngrediente(ProductoId productoId);

    /**
     * Todas las recetas vivas que obtienen el producto, la vigente primero (logica.md RECETA-06,
     * RECETA-09, PRODUCTO-20).
     */
    List<Receta> obtenerVivasPorProductoObtenido(ProductoId productoId);
}
