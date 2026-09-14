package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.TipoMovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerTipoMovimientoPorIdUseCase {
    private final ITipoMovimientoRepository tipoMovimientoRepository;

    public ObtenerTipoMovimientoPorIdUseCase(
            ITipoMovimientoRepository tipoMovimientoRepository) {
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    public TipoMovimientoResponse execute(String tipoMovimientoId) {
        TipoMovimientoId id = new TipoMovimientoId(tipoMovimientoId);

        TipoMovimiento tipoMovimiento = tipoMovimientoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento con id '" + tipoMovimientoId + "' no existe"));

        return TipoMovimientoResponse.createFromAggregate(tipoMovimiento);
    }
}
