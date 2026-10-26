package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Datasources.IUnidadDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.ObtenerUnidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class UnidadRepositoryImpl implements IUnidadRepository{

    private final IUnidadDatasource unidadDatasource;

    public UnidadRepositoryImpl(IUnidadDatasource unidadDatasource) {
        this.unidadDatasource = unidadDatasource;
    }

    @Override
    public Unidad crear(Unidad unidad) {
        return this.unidadDatasource.crear(unidad);
    }

    @Override
    public Unidad actualizar(UnidadId unidadId, Unidad unidad) {
        return this.unidadDatasource.actualizar(unidadId, unidad);
    }

    @Override
    public Optional<Unidad> obtenerPorId(UnidadId unidadId) {
        return this.unidadDatasource.obtenerPorId(unidadId);
    }

    @Override
    public PaginatedResponse<List<Unidad>> obtenerTodos(ObtenerUnidadesRequest obtenerUnidadesRequest) {
        return this.unidadDatasource.obtenerTodos(obtenerUnidadesRequest);
    }

    @Override
    public void eliminar(UnidadId unidadId) {
        this.unidadDatasource.eliminar(unidadId);
    }

    @Override
    public Optional<Unidad> obtenerPorNombre(String nombre) {
        return this.unidadDatasource.obtenerPorNombre(nombre);
    }
}
