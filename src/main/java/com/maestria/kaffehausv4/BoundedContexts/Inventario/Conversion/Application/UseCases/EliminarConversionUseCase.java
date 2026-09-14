package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.ValueObjects.ConversionId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarConversionUseCase {
    private final IConversionRepository conversionRepository;

    public EliminarConversionUseCase(IConversionRepository conversionRepository) {
        this.conversionRepository = conversionRepository;
    }

    public void execute(String conversionId) {
        ConversionId id = new ConversionId(conversionId);

        conversionRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La conversion con id '" + conversionId + "' no existe"));

        conversionRepository.eliminar(id);
    }
}
