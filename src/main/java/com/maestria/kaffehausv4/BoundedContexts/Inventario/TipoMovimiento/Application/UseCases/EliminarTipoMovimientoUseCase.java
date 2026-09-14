package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarTipoMovimientoUseCase {
    private final ITipoMovimientoRepository tipoMovimientoRepository;

    public EliminarTipoMovimientoUseCase(ITipoMovimientoRepository tipoMovimientoRepository) {
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    public void execute(String tipoMovimientoId) {
        TipoMovimientoId id = new TipoMovimientoId(tipoMovimientoId);

        tipoMovimientoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento con id '" + tipoMovimientoId + "' no existe"));

        tipoMovimientoRepository.eliminar(id);
    }
}
