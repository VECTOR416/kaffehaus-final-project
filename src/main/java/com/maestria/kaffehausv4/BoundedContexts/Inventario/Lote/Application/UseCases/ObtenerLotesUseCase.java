package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerLotesUseCase {
    private final ILoteRepository loteRepository;

    public ObtenerLotesUseCase(
            ILoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    public PaginatedResponse<List<LoteResponse>> execute(ObtenerLotesRequest obtenerLotesRequest) {
        PaginatedResponse<List<Lote>> resultado = loteRepository.obtenerTodos(obtenerLotesRequest);

        List<LoteResponse> lotes = resultado.getData().stream()
                .map(LoteResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                lotes,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
