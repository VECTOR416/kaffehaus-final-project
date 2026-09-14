package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Datasources.IDetalleVentaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.ObtenerDetallesVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Repositories.IDetalleVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.ValueObjects.DetalleVentaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class DetalleVentaRepositoryImpl implements IDetalleVentaRepository{

    private final IDetalleVentaDatasource detalleVentaDatasource;

    public DetalleVentaRepositoryImpl(IDetalleVentaDatasource detalleVentaDatasource) {
        this.detalleVentaDatasource = detalleVentaDatasource;
    }

    @Override
    public DetalleVenta crear(DetalleVenta detalleVenta) {
        return this.detalleVentaDatasource.crear(detalleVenta);
    }

    @Override
    public DetalleVenta actualizar(DetalleVentaId detalleVentaId, DetalleVenta detalleVenta) {
        return this.detalleVentaDatasource.actualizar(detalleVentaId, detalleVenta);
    }

    @Override
    public Optional<DetalleVenta> obtenerPorId(DetalleVentaId detalleVentaId) {
        return this.detalleVentaDatasource.obtenerPorId(detalleVentaId);
    }

    @Override
    public PaginatedResponse<List<DetalleVenta>> obtenerTodos(ObtenerDetallesVentaRequest obtenerDetallesVentaRequest) {
        return this.detalleVentaDatasource.obtenerTodos(obtenerDetallesVentaRequest);
    }

    @Override
    public void eliminar(DetalleVentaId detalleVentaId) {
        this.detalleVentaDatasource.eliminar(detalleVentaId);
    }

}
