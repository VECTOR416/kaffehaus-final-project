package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Datasources.ITipoDescuentoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.ObtenerTiposDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class TipoDescuentoRepositoryImpl implements ITipoDescuentoRepository{

    private final ITipoDescuentoDatasource tipoDescuentoDatasource;

    public TipoDescuentoRepositoryImpl(ITipoDescuentoDatasource tipoDescuentoDatasource) {
        this.tipoDescuentoDatasource = tipoDescuentoDatasource;
    }

    @Override
    public TipoDescuento crear(TipoDescuento tipoDescuento) {
        return this.tipoDescuentoDatasource.crear(tipoDescuento);
    }

    @Override
    public TipoDescuento actualizar(TipoDescuentoId tipoDescuentoId, TipoDescuento tipoDescuento) {
        return this.tipoDescuentoDatasource.actualizar(tipoDescuentoId, tipoDescuento);
    }

    @Override
    public Optional<TipoDescuento> obtenerPorId(TipoDescuentoId tipoDescuentoId) {
        return this.tipoDescuentoDatasource.obtenerPorId(tipoDescuentoId);
    }

    @Override
    public PaginatedResponse<List<TipoDescuento>> obtenerTodos(ObtenerTiposDescuentoRequest obtenerTiposDescuentoRequest) {
        return this.tipoDescuentoDatasource.obtenerTodos(obtenerTiposDescuentoRequest);
    }

    @Override
    public void eliminar(TipoDescuentoId tipoDescuentoId) {
        this.tipoDescuentoDatasource.eliminar(tipoDescuentoId);
    }

}
