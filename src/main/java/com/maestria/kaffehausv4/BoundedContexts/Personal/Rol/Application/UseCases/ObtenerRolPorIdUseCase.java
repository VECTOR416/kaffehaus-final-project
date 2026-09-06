package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.RolResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerRolPorIdUseCase {
    private final IRolRepository rolRepository;

    public ObtenerRolPorIdUseCase(IRolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public RolResponse execute(String rolId) {
        RolId id = new RolId(rolId);

        Rol rol = rolRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El rol con id '" + rolId + "' no existe"));

        return RolResponse.createFromAggregate(rol);
    }
}
