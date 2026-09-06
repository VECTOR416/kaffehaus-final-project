package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Infrastructure.Datasource;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Datasources.IEntidadDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ObtenerEntidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public class EntidadDatasourceImpl implements IEntidadDatasource{

    @Override
    public Entidad crear(Entidad entidad) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'crear'");
    }

    @Override
    public Entidad actualizar(EntidadId entidadId, Entidad entidad) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'actualizar'");
    }

    @Override
    public Optional<Entidad> obtenerPorId(EntidadId entidadId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'obtenerPorId'");
    }

    @Override
    public PaginatedResponse<List<Entidad>> obtenerTodos(ObtenerEntidadesRequest obtenerEntidadesRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'obtenerTodos'");
    }

    @Override
    public void eliminar(EntidadId entidadId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'eliminar'");
    }

}
