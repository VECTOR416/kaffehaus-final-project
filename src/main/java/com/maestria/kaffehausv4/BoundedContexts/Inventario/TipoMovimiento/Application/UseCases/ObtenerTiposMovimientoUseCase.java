package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ObtenerTiposMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.TipoMovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerTiposMovimientoUseCase {
    private final ITipoMovimientoRepository tipoMovimientoRepository;

    public ObtenerTiposMovimientoUseCase(
            ITipoMovimientoRepository tipoMovimientoRepository) {
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    public PaginatedResponse<List<TipoMovimientoResponse>> execute(ObtenerTiposMovimientoRequest obtenerTiposMovimientoRequest) {
        PaginatedResponse<List<TipoMovimiento>> resultado = tipoMovimientoRepository.obtenerTodos(obtenerTiposMovimientoRequest);

        List<TipoMovimientoResponse> tiposMovimiento = resultado.getData().stream()
                .map(TipoMovimientoResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                tiposMovimiento,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
