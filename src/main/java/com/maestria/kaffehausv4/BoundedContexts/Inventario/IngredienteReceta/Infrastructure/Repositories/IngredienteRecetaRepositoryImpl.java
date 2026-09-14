package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Datasources.IIngredienteRecetaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.ObtenerIngredientesRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Repositories.IIngredienteRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.ValueObjects.IngredienteRecetaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class IngredienteRecetaRepositoryImpl implements IIngredienteRecetaRepository{

    private final IIngredienteRecetaDatasource ingredienteRecetaDatasource;

    public IngredienteRecetaRepositoryImpl(IIngredienteRecetaDatasource ingredienteRecetaDatasource) {
        this.ingredienteRecetaDatasource = ingredienteRecetaDatasource;
    }

    @Override
    public IngredienteReceta crear(IngredienteReceta ingredienteReceta) {
        return this.ingredienteRecetaDatasource.crear(ingredienteReceta);
    }

    @Override
    public IngredienteReceta actualizar(IngredienteRecetaId ingredienteRecetaId, IngredienteReceta ingredienteReceta) {
        return this.ingredienteRecetaDatasource.actualizar(ingredienteRecetaId, ingredienteReceta);
    }

    @Override
    public Optional<IngredienteReceta> obtenerPorId(IngredienteRecetaId ingredienteRecetaId) {
        return this.ingredienteRecetaDatasource.obtenerPorId(ingredienteRecetaId);
    }

    @Override
    public PaginatedResponse<List<IngredienteReceta>> obtenerTodos(ObtenerIngredientesRecetaRequest obtenerIngredientesRecetaRequest) {
        return this.ingredienteRecetaDatasource.obtenerTodos(obtenerIngredientesRecetaRequest);
    }

    @Override
    public void eliminar(IngredienteRecetaId ingredienteRecetaId) {
        this.ingredienteRecetaDatasource.eliminar(ingredienteRecetaId);
    }

}
