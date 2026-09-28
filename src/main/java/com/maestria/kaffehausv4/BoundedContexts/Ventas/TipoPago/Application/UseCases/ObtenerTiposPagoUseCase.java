package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.TipoPagoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.ObtenerTiposPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerTiposPagoUseCase {
    private final ITipoPagoRepository tipoPagoRepository;

    public ObtenerTiposPagoUseCase(
            ITipoPagoRepository tipoPagoRepository) {
        this.tipoPagoRepository = tipoPagoRepository;
    }

    public PaginatedResponse<List<TipoPagoResponse>> execute(ObtenerTiposPagoRequest obtenerTiposPagoRequest) {
        PaginatedResponse<List<TipoPago>> resultado = tipoPagoRepository.obtenerTodos(obtenerTiposPagoRequest);

        List<TipoPagoResponse> tiposPago = resultado.getData().stream()
                .map(TipoPagoResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                tiposPago,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
