package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.ConversionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Dtos.CrearConversionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearConversionUseCase {
    private final IConversionRepository conversionRepository;
    private final IUnidadRepository unidadRepository;

    public CrearConversionUseCase(
            IConversionRepository conversionRepository,
            IUnidadRepository unidadRepository) {
        this.conversionRepository = conversionRepository;
        this.unidadRepository = unidadRepository;
    }

    public ConversionResponse execute(CrearConversionRequest crearConversionRequest) {
        UnidadId unidadOrigenId = new UnidadId(crearConversionRequest.getIdUnidadOrigen());
        Unidad unidadOrigen = unidadRepository.obtenerPorId(unidadOrigenId)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + crearConversionRequest.getIdUnidadOrigen() + "' no existe"));
        UnidadId unidadDestinoId = new UnidadId(crearConversionRequest.getIdUnidadDestino());
        Unidad unidadDestino = unidadRepository.obtenerPorId(unidadDestinoId)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + crearConversionRequest.getIdUnidadDestino() + "' no existe"));

        Conversion conversion = Conversion.crear(
                crearConversionRequest.getFactorConversion(),
                crearConversionRequest.isEsBorrable(),
                unidadOrigenId,
                unidadDestinoId);

        Conversion conversionCreada = conversionRepository.crear(conversion);

        return ConversionResponse.createFromAggregate(conversionCreada, unidadOrigen, unidadDestino);
    }
}
