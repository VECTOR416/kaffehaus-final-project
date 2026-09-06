package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.EntidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ObtenerEntidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerEntidadesUseCase {
    private final IEntidadRepository entidadRepository;

    public ObtenerEntidadesUseCase(IEntidadRepository entidadRepository) {
        this.entidadRepository = entidadRepository;
    }

    public PaginatedResponse<List<EntidadResponse>> execute(ObtenerEntidadesRequest obtenerEntidadesRequest) {
        PaginatedResponse<List<Entidad>> resultado = entidadRepository.obtenerTodos(obtenerEntidadesRequest);

        List<EntidadResponse> entidades = resultado.getData().stream()
                .map(EntidadResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                entidades,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
