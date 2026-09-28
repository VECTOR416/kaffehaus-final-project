package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.ObtenerRolesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IRolDatasource {
    Rol crear(Rol rol);
    Rol actualizar(RolId rolId, Rol rol);
    Optional<Rol> obtenerPorId(RolId rolId);
    PaginatedResponse<List<Rol>> obtenerTodos(ObtenerRolesRequest obtenerRolesRequest);
    void eliminar(RolId rolId);

    /** Los roles se resuelven por nombre (auth.md 5): la siembra busca asi los que ya existen. */
    Optional<Rol> obtenerPorNombre(String nombre);
}
