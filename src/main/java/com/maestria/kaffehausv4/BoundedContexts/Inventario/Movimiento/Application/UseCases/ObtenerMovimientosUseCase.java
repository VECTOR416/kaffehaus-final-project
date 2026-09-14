package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.MovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ObtenerMovimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerMovimientosUseCase {
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;

    public ObtenerMovimientosUseCase(
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository) {
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    public PaginatedResponse<List<MovimientoResponse>> execute(ObtenerMovimientosRequest obtenerMovimientosRequest) {
        PaginatedResponse<List<Movimiento>> resultado = movimientoRepository.obtenerTodos(obtenerMovimientosRequest);

        List<MovimientoResponse> movimientos = resultado.getData().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                movimientos,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }

    private MovimientoResponse toResponse(Movimiento movimiento) {
        TipoMovimiento tipoMovimiento = tipoMovimientoRepository.obtenerPorId(movimiento.getTipoMovimientoId())
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento asociado a el movimiento no existe"));
        return MovimientoResponse.createFromAggregate(movimiento, tipoMovimiento);
    }
}
