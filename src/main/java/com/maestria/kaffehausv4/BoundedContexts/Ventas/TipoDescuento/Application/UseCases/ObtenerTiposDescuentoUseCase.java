package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.ObtenerTiposDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.TipoDescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerTiposDescuentoUseCase {
    private final ITipoDescuentoRepository tipoDescuentoRepository;

    public ObtenerTiposDescuentoUseCase(
            ITipoDescuentoRepository tipoDescuentoRepository) {
        this.tipoDescuentoRepository = tipoDescuentoRepository;
    }

    public PaginatedResponse<List<TipoDescuentoResponse>> execute(ObtenerTiposDescuentoRequest obtenerTiposDescuentoRequest) {
        PaginatedResponse<List<TipoDescuento>> resultado = tipoDescuentoRepository.obtenerTodos(obtenerTiposDescuentoRequest);

        List<TipoDescuentoResponse> tiposDescuento = resultado.getData().stream()
                .map(TipoDescuentoResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                tiposDescuento,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
