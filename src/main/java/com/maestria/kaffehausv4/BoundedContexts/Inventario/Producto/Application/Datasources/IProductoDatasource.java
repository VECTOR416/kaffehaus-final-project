package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ObtenerProductosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IProductoDatasource {
    Producto crear(Producto producto);
    Producto actualizar(ProductoId productoId, Producto producto);
    Optional<Producto> obtenerPorId(ProductoId productoId);
    PaginatedResponse<List<Producto>> obtenerTodos(ObtenerProductosRequest obtenerProductosRequest);
    void eliminar(ProductoId productoId);
}
