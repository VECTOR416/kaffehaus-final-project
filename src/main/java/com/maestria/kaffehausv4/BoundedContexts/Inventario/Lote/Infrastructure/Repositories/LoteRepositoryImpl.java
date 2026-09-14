package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Datasources.ILoteDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class LoteRepositoryImpl implements ILoteRepository{

    private final ILoteDatasource loteDatasource;

    public LoteRepositoryImpl(ILoteDatasource loteDatasource) {
        this.loteDatasource = loteDatasource;
    }

    @Override
    public Lote crear(Lote lote) {
        return this.loteDatasource.crear(lote);
    }

    @Override
    public Lote actualizar(LoteId loteId, Lote lote) {
        return this.loteDatasource.actualizar(loteId, lote);
    }

    @Override
    public Optional<Lote> obtenerPorId(LoteId loteId) {
        return this.loteDatasource.obtenerPorId(loteId);
    }

    @Override
    public PaginatedResponse<List<Lote>> obtenerTodos(ObtenerLotesRequest obtenerLotesRequest) {
        return this.loteDatasource.obtenerTodos(obtenerLotesRequest);
    }

    @Override
    public void eliminar(LoteId loteId) {
        this.loteDatasource.eliminar(loteId);
    }

}
