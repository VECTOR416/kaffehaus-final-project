package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ObtenerEntidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IEntidadDatasource {
    Entidad crear(Entidad entidad);
    Entidad actualizar(EntidadId entidadId, Entidad entidad);
    Optional<Entidad> obtenerPorId(EntidadId entidadId);
    PaginatedResponse<List<Entidad>> obtenerTodos(ObtenerEntidadesRequest obtenerEntidadesRequest);
    void eliminar(EntidadId entidadId);
}
