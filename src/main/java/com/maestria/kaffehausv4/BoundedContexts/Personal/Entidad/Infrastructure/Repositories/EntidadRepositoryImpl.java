package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Datasources.IEntidadDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ObtenerEntidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;


@Repository
public class EntidadRepositoryImpl implements IEntidadRepository{

    private final IEntidadDatasource entidadDatasource;

    public EntidadRepositoryImpl(IEntidadDatasource entidadDatasource) {
        this.entidadDatasource = entidadDatasource;
    }

    @Override
    public Entidad crear(Entidad entidad) {
        return this.entidadDatasource.crear(entidad);
    }

    @Override
    public Entidad actualizar(EntidadId entidadId, Entidad entidad) {
        return this.entidadDatasource.actualizar(entidadId, entidad);
    }

    @Override
    public Optional<Entidad> obtenerPorId(EntidadId entidadId) {
        return this.entidadDatasource.obtenerPorId(entidadId);
    }

    @Override
    public PaginatedResponse<List<Entidad>> obtenerTodos(ObtenerEntidadesRequest obtenerEntidadesRequest) {
        return this.entidadDatasource.obtenerTodos(obtenerEntidadesRequest);
    }

    @Override
    public void eliminar(EntidadId entidadId) {
        this.entidadDatasource.eliminar(entidadId);
    }

}
