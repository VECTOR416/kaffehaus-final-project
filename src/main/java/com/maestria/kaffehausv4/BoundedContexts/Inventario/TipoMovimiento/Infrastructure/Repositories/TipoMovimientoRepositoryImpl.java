package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Datasources.ITipoMovimientoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ObtenerTiposMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class TipoMovimientoRepositoryImpl implements ITipoMovimientoRepository{

    private final ITipoMovimientoDatasource tipoMovimientoDatasource;

    public TipoMovimientoRepositoryImpl(ITipoMovimientoDatasource tipoMovimientoDatasource) {
        this.tipoMovimientoDatasource = tipoMovimientoDatasource;
    }

    @Override
    public TipoMovimiento crear(TipoMovimiento tipoMovimiento) {
        return this.tipoMovimientoDatasource.crear(tipoMovimiento);
    }

    @Override
    public TipoMovimiento actualizar(TipoMovimientoId tipoMovimientoId, TipoMovimiento tipoMovimiento) {
        return this.tipoMovimientoDatasource.actualizar(tipoMovimientoId, tipoMovimiento);
    }

    @Override
    public Optional<TipoMovimiento> obtenerPorId(TipoMovimientoId tipoMovimientoId) {
        return this.tipoMovimientoDatasource.obtenerPorId(tipoMovimientoId);
    }

    @Override
    public PaginatedResponse<List<TipoMovimiento>> obtenerTodos(ObtenerTiposMovimientoRequest obtenerTiposMovimientoRequest) {
        return this.tipoMovimientoDatasource.obtenerTodos(obtenerTiposMovimientoRequest);
    }

    @Override
    public void eliminar(TipoMovimientoId tipoMovimientoId) {
        this.tipoMovimientoDatasource.eliminar(tipoMovimientoId);
    }

    @Override
    public Optional<TipoMovimiento> obtenerPorNombre(String nombre) {
        return this.tipoMovimientoDatasource.obtenerPorNombre(nombre);
    }

}
