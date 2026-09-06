package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ObtenerProveedoresRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IProveedorDatasource {
    Proveedor crear(Proveedor proveedor);
    Proveedor actualizar(ProveedorId proveedorId, Proveedor proveedor);
    Optional<Proveedor> obtenerPorId(ProveedorId proveedorId);
    PaginatedResponse<List<Proveedor>> obtenerTodos(ObtenerProveedoresRequest obtenerProveedoresRequest);
    void eliminar(ProveedorId proveedorId);
}
