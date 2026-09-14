package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ActualizarConversionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ConversionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.ValueObjects.ConversionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarConversionUseCase {
    private final IConversionRepository conversionRepository;
    private final IUnidadRepository unidadRepository;

    public ActualizarConversionUseCase(
            IConversionRepository conversionRepository,
            IUnidadRepository unidadRepository) {
        this.conversionRepository = conversionRepository;
        this.unidadRepository = unidadRepository;
    }

    public ConversionResponse execute(String conversionId, ActualizarConversionRequest actualizarConversionRequest) {
        ConversionId id = new ConversionId(conversionId);

        Conversion conversion = conversionRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La conversion con id '" + conversionId + "' no existe"));

        UnidadId unidadOrigenId = null;
        if (actualizarConversionRequest.getIdUnidadOrigen() != null) {
            unidadOrigenId = new UnidadId(actualizarConversionRequest.getIdUnidadOrigen());
            unidadRepository.obtenerPorId(unidadOrigenId)
                    .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + actualizarConversionRequest.getIdUnidadOrigen() + "' no existe"));
        }
        UnidadId unidadDestinoId = null;
        if (actualizarConversionRequest.getIdUnidadDestino() != null) {
            unidadDestinoId = new UnidadId(actualizarConversionRequest.getIdUnidadDestino());
            unidadRepository.obtenerPorId(unidadDestinoId)
                    .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + actualizarConversionRequest.getIdUnidadDestino() + "' no existe"));
        }

        conversion.actualizar(
                actualizarConversionRequest.getFactorConversion(),
                actualizarConversionRequest.getEsBorrable(),
                unidadOrigenId,
                unidadDestinoId);

        Conversion conversionActualizada = conversionRepository.actualizar(id, conversion);

        return toResponse(conversionActualizada);
    }

    private ConversionResponse toResponse(Conversion conversion) {
        Unidad unidadOrigen = unidadRepository.obtenerPorId(conversion.getUnidadOrigenId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a la conversion no existe"));
        Unidad unidadDestino = unidadRepository.obtenerPorId(conversion.getUnidadDestinoId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a la conversion no existe"));
        return ConversionResponse.createFromAggregate(conversion, unidadOrigen, unidadDestino);
    }
}
