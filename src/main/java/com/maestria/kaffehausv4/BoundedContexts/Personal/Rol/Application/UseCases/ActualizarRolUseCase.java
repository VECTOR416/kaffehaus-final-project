package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.ActualizarRolRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.RolResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarRolUseCase {
    private final IRolRepository rolRepository;

    public ActualizarRolUseCase(IRolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public RolResponse execute(String rolId, ActualizarRolRequest actualizarRolRequest) {
        RolId id = new RolId(rolId);

        Rol rol = rolRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El rol con id '" + rolId + "' no existe"));

        rol.actualizar(actualizarRolRequest.getNombre());

        Rol rolActualizado = rolRepository.actualizar(id, rol);

        return RolResponse.createFromAggregate(rolActualizado);
    }
}
