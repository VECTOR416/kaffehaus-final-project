package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories.IProduccionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarProduccionUseCase {
    private final IProduccionRepository produccionRepository;

    public EliminarProduccionUseCase(IProduccionRepository produccionRepository) {
        this.produccionRepository = produccionRepository;
    }

    public void execute(String produccionId) {
        ProduccionId id = new ProduccionId(produccionId);

        produccionRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La produccion con id '" + produccionId + "' no existe"));

        produccionRepository.eliminar(id);
    }
}
