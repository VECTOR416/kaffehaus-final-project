package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Datasources.ITipoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ObtenerTiposRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class TipoRepositoryImpl implements ITipoRepository{

    private final ITipoDatasource tipoDatasource;

    public TipoRepositoryImpl(ITipoDatasource tipoDatasource) {
        this.tipoDatasource = tipoDatasource;
    }

    @Override
    public Tipo crear(Tipo tipo) {
        return this.tipoDatasource.crear(tipo);
    }

    @Override
    public Tipo actualizar(TipoId tipoId, Tipo tipo) {
        return this.tipoDatasource.actualizar(tipoId, tipo);
    }

    @Override
    public Optional<Tipo> obtenerPorId(TipoId tipoId) {
        return this.tipoDatasource.obtenerPorId(tipoId);
    }

    @Override
    public PaginatedResponse<List<Tipo>> obtenerTodos(ObtenerTiposRequest obtenerTiposRequest) {
        return this.tipoDatasource.obtenerTodos(obtenerTiposRequest);
    }

    @Override
    public void eliminar(TipoId tipoId) {
        this.tipoDatasource.eliminar(tipoId);
    }

    @Override
    public Optional<Tipo> obtenerPorNombre(String nombre) {
        return this.tipoDatasource.obtenerPorNombre(nombre);
    }
}
