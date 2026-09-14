package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Datasources.IProduccionDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ObtenerProduccionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories.IProduccionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class ProduccionRepositoryImpl implements IProduccionRepository{

    private final IProduccionDatasource produccionDatasource;

    public ProduccionRepositoryImpl(IProduccionDatasource produccionDatasource) {
        this.produccionDatasource = produccionDatasource;
    }

    @Override
    public Produccion crear(Produccion produccion) {
        return this.produccionDatasource.crear(produccion);
    }

    @Override
    public Produccion actualizar(ProduccionId produccionId, Produccion produccion) {
        return this.produccionDatasource.actualizar(produccionId, produccion);
    }

    @Override
    public Optional<Produccion> obtenerPorId(ProduccionId produccionId) {
        return this.produccionDatasource.obtenerPorId(produccionId);
    }

    @Override
    public PaginatedResponse<List<Produccion>> obtenerTodos(ObtenerProduccionesRequest obtenerProduccionesRequest) {
        return this.produccionDatasource.obtenerTodos(obtenerProduccionesRequest);
    }

    @Override
    public void eliminar(ProduccionId produccionId) {
        this.produccionDatasource.eliminar(produccionId);
    }

}
