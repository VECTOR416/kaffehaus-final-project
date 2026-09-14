package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ConversionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ObtenerConversionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerConversionesUseCase {
    private final IConversionRepository conversionRepository;
    private final IUnidadRepository unidadRepository;

    public ObtenerConversionesUseCase(
            IConversionRepository conversionRepository,
            IUnidadRepository unidadRepository) {
        this.conversionRepository = conversionRepository;
        this.unidadRepository = unidadRepository;
    }

    public PaginatedResponse<List<ConversionResponse>> execute(ObtenerConversionesRequest obtenerConversionesRequest) {
        PaginatedResponse<List<Conversion>> resultado = conversionRepository.obtenerTodos(obtenerConversionesRequest);

        List<ConversionResponse> conversiones = resultado.getData().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                conversiones,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }

    private ConversionResponse toResponse(Conversion conversion) {
        Unidad unidadOrigen = unidadRepository.obtenerPorId(conversion.getUnidadOrigenId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a la conversion no existe"));
        Unidad unidadDestino = unidadRepository.obtenerPorId(conversion.getUnidadDestinoId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a la conversion no existe"));
        return ConversionResponse.createFromAggregate(conversion, unidadOrigen, unidadDestino);
    }
}
