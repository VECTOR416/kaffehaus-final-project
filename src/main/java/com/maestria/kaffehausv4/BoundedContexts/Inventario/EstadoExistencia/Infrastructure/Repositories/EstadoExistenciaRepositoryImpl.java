package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Datasources.IEstadoExistenciaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.ObtenerEstadosExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class EstadoExistenciaRepositoryImpl implements IEstadoExistenciaRepository{

    private final IEstadoExistenciaDatasource estadoExistenciaDatasource;

    public EstadoExistenciaRepositoryImpl(IEstadoExistenciaDatasource estadoExistenciaDatasource) {
        this.estadoExistenciaDatasource = estadoExistenciaDatasource;
    }

    @Override
    public EstadoExistencia crear(EstadoExistencia estadoExistencia) {
        return this.estadoExistenciaDatasource.crear(estadoExistencia);
    }

    @Override
    public EstadoExistencia actualizar(EstadoExistenciaId estadoExistenciaId, EstadoExistencia estadoExistencia) {
        return this.estadoExistenciaDatasource.actualizar(estadoExistenciaId, estadoExistencia);
    }

    @Override
    public Optional<EstadoExistencia> obtenerPorId(EstadoExistenciaId estadoExistenciaId) {
        return this.estadoExistenciaDatasource.obtenerPorId(estadoExistenciaId);
    }

    @Override
    public PaginatedResponse<List<EstadoExistencia>> obtenerTodos(ObtenerEstadosExistenciaRequest obtenerEstadosExistenciaRequest) {
        return this.estadoExistenciaDatasource.obtenerTodos(obtenerEstadosExistenciaRequest);
    }

    @Override
    public void eliminar(EstadoExistenciaId estadoExistenciaId) {
        this.estadoExistenciaDatasource.eliminar(estadoExistenciaId);
    }

}
