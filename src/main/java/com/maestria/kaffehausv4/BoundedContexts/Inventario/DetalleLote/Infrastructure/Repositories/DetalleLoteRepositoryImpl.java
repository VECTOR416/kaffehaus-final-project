package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Datasources.IDetalleLoteDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.ObtenerDetallesLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Repositories.IDetalleLoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class DetalleLoteRepositoryImpl implements IDetalleLoteRepository{

    private final IDetalleLoteDatasource detalleLoteDatasource;

    public DetalleLoteRepositoryImpl(IDetalleLoteDatasource detalleLoteDatasource) {
        this.detalleLoteDatasource = detalleLoteDatasource;
    }

    @Override
    public DetalleLote crear(DetalleLote detalleLote) {
        return this.detalleLoteDatasource.crear(detalleLote);
    }

    @Override
    public DetalleLote actualizar(DetalleLoteId detalleLoteId, DetalleLote detalleLote) {
        return this.detalleLoteDatasource.actualizar(detalleLoteId, detalleLote);
    }

    @Override
    public Optional<DetalleLote> obtenerPorId(DetalleLoteId detalleLoteId) {
        return this.detalleLoteDatasource.obtenerPorId(detalleLoteId);
    }

    @Override
    public PaginatedResponse<List<DetalleLote>> obtenerTodos(ObtenerDetallesLoteRequest obtenerDetallesLoteRequest) {
        return this.detalleLoteDatasource.obtenerTodos(obtenerDetallesLoteRequest);
    }

    @Override
    public void eliminar(DetalleLoteId detalleLoteId) {
        this.detalleLoteDatasource.eliminar(detalleLoteId);
    }

}
