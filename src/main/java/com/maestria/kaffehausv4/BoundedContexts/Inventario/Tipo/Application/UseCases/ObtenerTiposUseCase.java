package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.TipoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ObtenerTiposRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerTiposUseCase {
    private final ITipoRepository tipoRepository;

    public ObtenerTiposUseCase(
            ITipoRepository tipoRepository) {
        this.tipoRepository = tipoRepository;
    }

    public PaginatedResponse<List<TipoResponse>> execute(ObtenerTiposRequest obtenerTiposRequest) {
        PaginatedResponse<List<Tipo>> resultado = tipoRepository.obtenerTodos(obtenerTiposRequest);

        List<TipoResponse> tipos = resultado.getData().stream()
                .map(TipoResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                tipos,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
