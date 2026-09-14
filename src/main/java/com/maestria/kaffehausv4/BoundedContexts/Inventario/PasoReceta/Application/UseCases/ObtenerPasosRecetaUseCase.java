package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.ObtenerPasosRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.PasoRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Repositories.IPasoRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerPasosRecetaUseCase {
    private final IPasoRecetaRepository pasoRecetaRepository;

    public ObtenerPasosRecetaUseCase(
            IPasoRecetaRepository pasoRecetaRepository) {
        this.pasoRecetaRepository = pasoRecetaRepository;
    }

    public PaginatedResponse<List<PasoRecetaResponse>> execute(ObtenerPasosRecetaRequest obtenerPasosRecetaRequest) {
        PaginatedResponse<List<PasoReceta>> resultado = pasoRecetaRepository.obtenerTodos(obtenerPasosRecetaRequest);

        List<PasoRecetaResponse> pasosReceta = resultado.getData().stream()
                .map(PasoRecetaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                pasosReceta,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
