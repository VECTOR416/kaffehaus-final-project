package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarRolUseCase {
    private final IRolRepository rolRepository;

    public EliminarRolUseCase(IRolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public void execute(String rolId) {
        RolId id = new RolId(rolId);

        rolRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El rol con id '" + rolId + "' no existe"));

        rolRepository.eliminar(id);
    }
}
