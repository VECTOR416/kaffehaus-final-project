package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.ObtenerRolesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.RolResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerRolesUseCase {
    private final IRolRepository rolRepository;

    public ObtenerRolesUseCase(IRolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public PaginatedResponse<List<RolResponse>> execute(ObtenerRolesRequest obtenerRolesRequest) {
        PaginatedResponse<List<Rol>> resultado = rolRepository.obtenerTodos(obtenerRolesRequest);

        List<RolResponse> roles = resultado.getData().stream()
                .map(RolResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                roles,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
