package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Datasources.IExistenciaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ObtenerExistenciasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class ExistenciaRepositoryImpl implements IExistenciaRepository{

    private final IExistenciaDatasource existenciaDatasource;

    public ExistenciaRepositoryImpl(IExistenciaDatasource existenciaDatasource) {
        this.existenciaDatasource = existenciaDatasource;
    }

    @Override
    public Existencia crear(Existencia existencia) {
        return this.existenciaDatasource.crear(existencia);
    }

    @Override
    public Existencia actualizar(ExistenciaId existenciaId, Existencia existencia) {
        return this.existenciaDatasource.actualizar(existenciaId, existencia);
    }

    @Override
    public Optional<Existencia> obtenerPorId(ExistenciaId existenciaId) {
        return this.existenciaDatasource.obtenerPorId(existenciaId);
    }

    @Override
    public PaginatedResponse<List<Existencia>> obtenerTodos(ObtenerExistenciasRequest obtenerExistenciasRequest) {
        return this.existenciaDatasource.obtenerTodos(obtenerExistenciasRequest);
    }

    @Override
    public void eliminar(ExistenciaId existenciaId) {
        this.existenciaDatasource.eliminar(existenciaId);
    }

}
