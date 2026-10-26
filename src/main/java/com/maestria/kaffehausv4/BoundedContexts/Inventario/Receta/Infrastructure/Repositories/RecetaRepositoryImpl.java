package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Datasources.IRecetaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ObtenerRecetasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class RecetaRepositoryImpl implements IRecetaRepository{

    private final IRecetaDatasource recetaDatasource;

    public RecetaRepositoryImpl(IRecetaDatasource recetaDatasource) {
        this.recetaDatasource = recetaDatasource;
    }

    @Override
    public Receta crear(Receta receta) {
        return this.recetaDatasource.crear(receta);
    }

    @Override
    public Receta actualizar(RecetaId recetaId, Receta receta) {
        return this.recetaDatasource.actualizar(recetaId, receta);
    }

    @Override
    public Optional<Receta> obtenerPorId(RecetaId recetaId) {
        return this.recetaDatasource.obtenerPorId(recetaId);
    }

    @Override
    public PaginatedResponse<List<Receta>> obtenerTodos(ObtenerRecetasRequest obtenerRecetasRequest) {
        return this.recetaDatasource.obtenerTodos(obtenerRecetasRequest);
    }

    @Override
    public void eliminar(RecetaId recetaId) {
        this.recetaDatasource.eliminar(recetaId);
    }

    @Override
    public Optional<Receta> obtenerPorProductoObtenido(ProductoId productoId) {
        return this.recetaDatasource.obtenerPorProductoObtenido(productoId);
    }

    @Override
    public List<Receta> obtenerPorIngrediente(ProductoId productoId) {
        return this.recetaDatasource.obtenerPorIngrediente(productoId);
    }

    @Override
    public List<Receta> obtenerVivasPorProductoObtenido(ProductoId productoId) {
        return this.recetaDatasource.obtenerVivasPorProductoObtenido(productoId);
    }
}
