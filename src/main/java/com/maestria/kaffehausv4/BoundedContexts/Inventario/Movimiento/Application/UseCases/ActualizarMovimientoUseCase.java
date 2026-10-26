package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ActualizarMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.MovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Se conserva para mantener el contrato de cinco operaciones del controller, pero un
 * movimiento es auditoria inmutable: el agregado rechaza toda modificacion con 409. Si el
 * movimiento no existe, responde 404 como el resto del proyecto.
 */
@Service
@Transactional
public class ActualizarMovimientoUseCase {
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;

    public ActualizarMovimientoUseCase(
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository) {
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    public MovimientoResponse execute(String movimientoId, ActualizarMovimientoRequest actualizarMovimientoRequest) {
        MovimientoId id = new MovimientoId(movimientoId);

        Movimiento movimiento = movimientoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El movimiento con id '" + movimientoId + "' no existe"));

        movimiento.actualizar();

        Movimiento movimientoActualizado = movimientoRepository.actualizar(id, movimiento);

        return toResponse(movimientoActualizado);
    }

    private MovimientoResponse toResponse(Movimiento movimiento) {
        TipoMovimiento tipoMovimiento = tipoMovimientoRepository.obtenerPorId(movimiento.getTipoMovimientoId())
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento asociado a el movimiento no existe"));
        return MovimientoResponse.createFromAggregate(movimiento, tipoMovimiento);
    }
}
