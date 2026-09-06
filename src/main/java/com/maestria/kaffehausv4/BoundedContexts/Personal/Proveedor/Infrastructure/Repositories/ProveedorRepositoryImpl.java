package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Datasources.IProveedorDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ObtenerProveedoresRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class ProveedorRepositoryImpl implements IProveedorRepository{

    private final IProveedorDatasource proveedorDatasource;

    public ProveedorRepositoryImpl(IProveedorDatasource proveedorDatasource) {
        this.proveedorDatasource = proveedorDatasource;
    }

    @Override
    public Proveedor crear(Proveedor proveedor) {
        return this.proveedorDatasource.crear(proveedor);
    }

    @Override
    public Proveedor actualizar(ProveedorId proveedorId, Proveedor proveedor) {
        return this.proveedorDatasource.actualizar(proveedorId, proveedor);
    }

    @Override
    public Optional<Proveedor> obtenerPorId(ProveedorId proveedorId) {
        return this.proveedorDatasource.obtenerPorId(proveedorId);
    }

    @Override
    public PaginatedResponse<List<Proveedor>> obtenerTodos(ObtenerProveedoresRequest obtenerProveedoresRequest) {
        return this.proveedorDatasource.obtenerTodos(obtenerProveedoresRequest);
    }

    @Override
    public void eliminar(ProveedorId proveedorId) {
        this.proveedorDatasource.eliminar(proveedorId);
    }

}
