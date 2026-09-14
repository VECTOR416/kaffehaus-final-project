package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarExistenciaUseCase {
    private final IExistenciaRepository existenciaRepository;

    public EliminarExistenciaUseCase(IExistenciaRepository existenciaRepository) {
        this.existenciaRepository = existenciaRepository;
    }

    public void execute(String existenciaId) {
        ExistenciaId id = new ExistenciaId(existenciaId);

        existenciaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + existenciaId + "' no existe"));

        existenciaRepository.eliminar(id);
    }
}
