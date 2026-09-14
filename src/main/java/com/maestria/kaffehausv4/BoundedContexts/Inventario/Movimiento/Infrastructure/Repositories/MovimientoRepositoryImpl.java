package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Datasources.IMovimientoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ObtenerMovimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class MovimientoRepositoryImpl implements IMovimientoRepository{

    private final IMovimientoDatasource movimientoDatasource;

    public MovimientoRepositoryImpl(IMovimientoDatasource movimientoDatasource) {
        this.movimientoDatasource = movimientoDatasource;
    }

    @Override
    public Movimiento crear(Movimiento movimiento) {
        return this.movimientoDatasource.crear(movimiento);
    }

    @Override
    public Movimiento actualizar(MovimientoId movimientoId, Movimiento movimiento) {
        return this.movimientoDatasource.actualizar(movimientoId, movimiento);
    }

    @Override
    public Optional<Movimiento> obtenerPorId(MovimientoId movimientoId) {
        return this.movimientoDatasource.obtenerPorId(movimientoId);
    }

    @Override
    public PaginatedResponse<List<Movimiento>> obtenerTodos(ObtenerMovimientosRequest obtenerMovimientosRequest) {
        return this.movimientoDatasource.obtenerTodos(obtenerMovimientosRequest);
    }

    @Override
    public void eliminar(MovimientoId movimientoId) {
        this.movimientoDatasource.eliminar(movimientoId);
    }

}
