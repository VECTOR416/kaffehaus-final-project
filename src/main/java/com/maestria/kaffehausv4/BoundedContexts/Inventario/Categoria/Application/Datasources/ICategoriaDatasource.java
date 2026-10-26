package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.ObtenerCategoriasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ICategoriaDatasource {
    Categoria crear(Categoria categoria);
    Categoria actualizar(CategoriaId categoriaId, Categoria categoria);
    Optional<Categoria> obtenerPorId(CategoriaId categoriaId);
    PaginatedResponse<List<Categoria>> obtenerTodos(ObtenerCategoriasRequest obtenerCategoriasRequest);
    void eliminar(CategoriaId categoriaId);

    /** Fila viva con ese nombre, sin distinguir mayusculas. La usa la siembra para no duplicar. */
    Optional<Categoria> obtenerPorNombre(String nombre);
}
