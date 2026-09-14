package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.DetalleLoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.ObtenerDetallesLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Repositories.IDetalleLoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerDetallesLoteUseCase {
    private final IDetalleLoteRepository detalleLoteRepository;

    public ObtenerDetallesLoteUseCase(
            IDetalleLoteRepository detalleLoteRepository) {
        this.detalleLoteRepository = detalleLoteRepository;
    }

    public PaginatedResponse<List<DetalleLoteResponse>> execute(ObtenerDetallesLoteRequest obtenerDetallesLoteRequest) {
        PaginatedResponse<List<DetalleLote>> resultado = detalleLoteRepository.obtenerTodos(obtenerDetallesLoteRequest);

        List<DetalleLoteResponse> detallesLote = resultado.getData().stream()
                .map(DetalleLoteResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                detallesLote,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
