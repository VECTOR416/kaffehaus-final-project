package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Datasources.IConversionDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ObtenerConversionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.ValueObjects.ConversionId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class ConversionRepositoryImpl implements IConversionRepository{

    private final IConversionDatasource conversionDatasource;

    public ConversionRepositoryImpl(IConversionDatasource conversionDatasource) {
        this.conversionDatasource = conversionDatasource;
    }

    @Override
    public Conversion crear(Conversion conversion) {
        return this.conversionDatasource.crear(conversion);
    }

    @Override
    public Conversion actualizar(ConversionId conversionId, Conversion conversion) {
        return this.conversionDatasource.actualizar(conversionId, conversion);
    }

    @Override
    public Optional<Conversion> obtenerPorId(ConversionId conversionId) {
        return this.conversionDatasource.obtenerPorId(conversionId);
    }

    @Override
    public PaginatedResponse<List<Conversion>> obtenerTodos(ObtenerConversionesRequest obtenerConversionesRequest) {
        return this.conversionDatasource.obtenerTodos(obtenerConversionesRequest);
    }

    @Override
    public List<Conversion> obtenerVivas() {
        return this.conversionDatasource.obtenerVivas();
    }

    @Override
    public void eliminar(ConversionId conversionId) {
        this.conversionDatasource.eliminar(conversionId);
    }

}
