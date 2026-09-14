package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ActualizarTipoMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.TipoMovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarTipoMovimientoUseCase {
    private final ITipoMovimientoRepository tipoMovimientoRepository;

    public ActualizarTipoMovimientoUseCase(
            ITipoMovimientoRepository tipoMovimientoRepository) {
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    public TipoMovimientoResponse execute(String tipoMovimientoId, ActualizarTipoMovimientoRequest actualizarTipoMovimientoRequest) {
        TipoMovimientoId id = new TipoMovimientoId(tipoMovimientoId);

        TipoMovimiento tipoMovimiento = tipoMovimientoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento con id '" + tipoMovimientoId + "' no existe"));

        tipoMovimiento.actualizar(
                actualizarTipoMovimientoRequest.getNombre());

        TipoMovimiento tipoMovimientoActualizado = tipoMovimientoRepository.actualizar(id, tipoMovimiento);

        return TipoMovimientoResponse.createFromAggregate(tipoMovimientoActualizado);
    }
}
