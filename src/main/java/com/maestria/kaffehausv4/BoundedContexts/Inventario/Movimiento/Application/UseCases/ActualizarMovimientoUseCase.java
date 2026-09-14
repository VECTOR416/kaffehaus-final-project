package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ActualizarMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.MovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarMovimientoUseCase {
    private final IMovimientoRepository movimientoRepository;
    private final IExistenciaRepository existenciaRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;

    public ActualizarMovimientoUseCase(
            IMovimientoRepository movimientoRepository,
            IExistenciaRepository existenciaRepository,
            ITipoMovimientoRepository tipoMovimientoRepository) {
        this.movimientoRepository = movimientoRepository;
        this.existenciaRepository = existenciaRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    public MovimientoResponse execute(String movimientoId, ActualizarMovimientoRequest actualizarMovimientoRequest) {
        MovimientoId id = new MovimientoId(movimientoId);

        Movimiento movimiento = movimientoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El movimiento con id '" + movimientoId + "' no existe"));

        ExistenciaId existenciaId = null;
        if (actualizarMovimientoRequest.getIdExistencia() != null) {
            existenciaId = new ExistenciaId(actualizarMovimientoRequest.getIdExistencia());
            existenciaRepository.obtenerPorId(existenciaId)
                    .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + actualizarMovimientoRequest.getIdExistencia() + "' no existe"));
        }
        TipoMovimientoId tipoMovimientoId = null;
        if (actualizarMovimientoRequest.getIdTipoMovimiento() != null) {
            tipoMovimientoId = new TipoMovimientoId(actualizarMovimientoRequest.getIdTipoMovimiento());
            tipoMovimientoRepository.obtenerPorId(tipoMovimientoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento con id '" + actualizarMovimientoRequest.getIdTipoMovimiento() + "' no existe"));
        }

        movimiento.actualizar(
                actualizarMovimientoRequest.getCantidad(),
                actualizarMovimientoRequest.getFechaMovimiento(),
                actualizarMovimientoRequest.getMotivo(),
                existenciaId,
                tipoMovimientoId);

        Movimiento movimientoActualizado = movimientoRepository.actualizar(id, movimiento);

        return toResponse(movimientoActualizado);
    }

    private MovimientoResponse toResponse(Movimiento movimiento) {
        TipoMovimiento tipoMovimiento = tipoMovimientoRepository.obtenerPorId(movimiento.getTipoMovimientoId())
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento asociado a el movimiento no existe"));
        return MovimientoResponse.createFromAggregate(movimiento, tipoMovimiento);
    }
}
