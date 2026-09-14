package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarEstadoExistenciaUseCase {
    private final IEstadoExistenciaRepository estadoExistenciaRepository;

    public EliminarEstadoExistenciaUseCase(IEstadoExistenciaRepository estadoExistenciaRepository) {
        this.estadoExistenciaRepository = estadoExistenciaRepository;
    }

    public void execute(String estadoExistenciaId) {
        EstadoExistenciaId id = new EstadoExistenciaId(estadoExistenciaId);

        estadoExistenciaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia con id '" + estadoExistenciaId + "' no existe"));

        estadoExistenciaRepository.eliminar(id);
    }
}
