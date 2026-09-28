package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Datasources.IRolDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.ObtenerRolesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class RolRepositoryImpl implements IRolRepository{
    private final IRolDatasource rolDatasource;

    public RolRepositoryImpl(IRolDatasource rolDatasource) {
        this.rolDatasource = rolDatasource;
    }

    @Override
    public Rol crear(Rol rol) {
        return this.rolDatasource.crear(rol);
    }

    @Override
    public Rol actualizar(RolId rolId, Rol rol) {
        return this.rolDatasource.actualizar(rolId, rol);
    }

    @Override
    public Optional<Rol> obtenerPorId(RolId rolId) {
        return this.rolDatasource.obtenerPorId(rolId);
    }

    @Override
    public PaginatedResponse<List<Rol>> obtenerTodos(ObtenerRolesRequest obtenerRolesRequest) {
        return this.rolDatasource.obtenerTodos(obtenerRolesRequest);
    }

    @Override
    public void eliminar(RolId rolId) {
        this.rolDatasource.eliminar(rolId);
    }

    @Override
    public Optional<Rol> obtenerPorNombre(String nombre) {
        return this.rolDatasource.obtenerPorNombre(nombre);
    }
}
