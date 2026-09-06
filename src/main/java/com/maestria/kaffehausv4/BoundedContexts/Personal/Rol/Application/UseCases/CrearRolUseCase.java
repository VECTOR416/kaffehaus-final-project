package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.CrearRolRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Dtos.RolResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;

@Service
@Transactional
public class CrearRolUseCase {
    private final IRolRepository rolRepository;

    public CrearRolUseCase(IRolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public RolResponse execute(CrearRolRequest crearRolRequest) {
        Rol rol = Rol.crear(crearRolRequest.getNombre());
        Rol rolCreado = rolRepository.crear(rol);
        return RolResponse.createFromAggregate(rolCreado);
    }
}
