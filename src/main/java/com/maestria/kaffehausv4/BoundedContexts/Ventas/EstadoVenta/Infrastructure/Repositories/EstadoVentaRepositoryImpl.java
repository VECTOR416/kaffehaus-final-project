package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Datasources.IEstadoVentaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.ObtenerEstadosVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class EstadoVentaRepositoryImpl implements IEstadoVentaRepository{

    private final IEstadoVentaDatasource estadoVentaDatasource;

    public EstadoVentaRepositoryImpl(IEstadoVentaDatasource estadoVentaDatasource) {
        this.estadoVentaDatasource = estadoVentaDatasource;
    }

    @Override
    public EstadoVenta crear(EstadoVenta estadoVenta) {
        return this.estadoVentaDatasource.crear(estadoVenta);
    }

    @Override
    public EstadoVenta actualizar(EstadoVentaId estadoVentaId, EstadoVenta estadoVenta) {
        return this.estadoVentaDatasource.actualizar(estadoVentaId, estadoVenta);
    }

    @Override
    public Optional<EstadoVenta> obtenerPorId(EstadoVentaId estadoVentaId) {
        return this.estadoVentaDatasource.obtenerPorId(estadoVentaId);
    }

    @Override
    public PaginatedResponse<List<EstadoVenta>> obtenerTodos(ObtenerEstadosVentaRequest obtenerEstadosVentaRequest) {
        return this.estadoVentaDatasource.obtenerTodos(obtenerEstadosVentaRequest);
    }

    @Override
    public void eliminar(EstadoVentaId estadoVentaId) {
        this.estadoVentaDatasource.eliminar(estadoVentaId);
    }

    @Override
    public Optional<EstadoVenta> obtenerPorNombre(String nombre) {
        return this.estadoVentaDatasource.obtenerPorNombre(nombre);
    }

}
