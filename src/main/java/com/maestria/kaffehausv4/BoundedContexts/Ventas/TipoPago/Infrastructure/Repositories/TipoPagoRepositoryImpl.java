package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Datasources.ITipoPagoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.ObtenerTiposPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class TipoPagoRepositoryImpl implements ITipoPagoRepository{

    private final ITipoPagoDatasource tipoPagoDatasource;

    public TipoPagoRepositoryImpl(ITipoPagoDatasource tipoPagoDatasource) {
        this.tipoPagoDatasource = tipoPagoDatasource;
    }

    @Override
    public TipoPago crear(TipoPago tipoPago) {
        return this.tipoPagoDatasource.crear(tipoPago);
    }

    @Override
    public TipoPago actualizar(TipoPagoId tipoPagoId, TipoPago tipoPago) {
        return this.tipoPagoDatasource.actualizar(tipoPagoId, tipoPago);
    }

    @Override
    public Optional<TipoPago> obtenerPorId(TipoPagoId tipoPagoId) {
        return this.tipoPagoDatasource.obtenerPorId(tipoPagoId);
    }

    @Override
    public PaginatedResponse<List<TipoPago>> obtenerTodos(ObtenerTiposPagoRequest obtenerTiposPagoRequest) {
        return this.tipoPagoDatasource.obtenerTodos(obtenerTiposPagoRequest);
    }

    @Override
    public void eliminar(TipoPagoId tipoPagoId) {
        this.tipoPagoDatasource.eliminar(tipoPagoId);
    }

    @Override
    public Optional<TipoPago> obtenerPorNombre(String nombre) {
        return this.tipoPagoDatasource.obtenerPorNombre(nombre);
    }

    @Override
    public List<TipoPago> obtenerVivos() {
        return this.tipoPagoDatasource.obtenerVivos();
    }

}
