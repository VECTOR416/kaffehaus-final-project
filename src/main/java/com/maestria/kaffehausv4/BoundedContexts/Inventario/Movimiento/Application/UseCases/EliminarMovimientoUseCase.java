package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarMovimientoUseCase {
    private final IMovimientoRepository movimientoRepository;

    public EliminarMovimientoUseCase(IMovimientoRepository movimientoRepository) {
        this.movimientoRepository = movimientoRepository;
    }

    public void execute(String movimientoId) {
        MovimientoId id = new MovimientoId(movimientoId);

        movimientoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El movimiento con id '" + movimientoId + "' no existe"));

        movimientoRepository.eliminar(id);
    }
}
