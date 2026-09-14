package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Datasources.IPasoRecetaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.ObtenerPasosRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Repositories.IPasoRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.ValueObjects.PasoRecetaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class PasoRecetaRepositoryImpl implements IPasoRecetaRepository{

    private final IPasoRecetaDatasource pasoRecetaDatasource;

    public PasoRecetaRepositoryImpl(IPasoRecetaDatasource pasoRecetaDatasource) {
        this.pasoRecetaDatasource = pasoRecetaDatasource;
    }

    @Override
    public PasoReceta crear(PasoReceta pasoReceta) {
        return this.pasoRecetaDatasource.crear(pasoReceta);
    }

    @Override
    public PasoReceta actualizar(PasoRecetaId pasoRecetaId, PasoReceta pasoReceta) {
        return this.pasoRecetaDatasource.actualizar(pasoRecetaId, pasoReceta);
    }

    @Override
    public Optional<PasoReceta> obtenerPorId(PasoRecetaId pasoRecetaId) {
        return this.pasoRecetaDatasource.obtenerPorId(pasoRecetaId);
    }

    @Override
    public PaginatedResponse<List<PasoReceta>> obtenerTodos(ObtenerPasosRecetaRequest obtenerPasosRecetaRequest) {
        return this.pasoRecetaDatasource.obtenerTodos(obtenerPasosRecetaRequest);
    }

    @Override
    public void eliminar(PasoRecetaId pasoRecetaId) {
        this.pasoRecetaDatasource.eliminar(pasoRecetaId);
    }

}
