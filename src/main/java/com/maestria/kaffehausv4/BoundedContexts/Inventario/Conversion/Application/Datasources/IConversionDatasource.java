package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ObtenerConversionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.ValueObjects.ConversionId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IConversionDatasource {
    Conversion crear(Conversion conversion);
    Conversion actualizar(ConversionId conversionId, Conversion conversion);
    Optional<Conversion> obtenerPorId(ConversionId conversionId);
    PaginatedResponse<List<Conversion>> obtenerTodos(ObtenerConversionesRequest obtenerConversionesRequest);
    void eliminar(ConversionId conversionId);
}
