package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de el rol. No se da de baja si un usuario vivo lo tiene: ese usuario dejaria de poder
 * iniciar sesion (logica.md GENERAL-18). Los roles semanticos tampoco (GENERAL-19, en el agregado).
 */
@Service
@Transactional
public class EliminarRolUseCase {
    private final IRolRepository rolRepository;
    private final IUsuarioRepository usuarioRepository;

    public EliminarRolUseCase(
            IRolRepository rolRepository,
            IUsuarioRepository usuarioRepository) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void execute(String rolId) {
        RolId id = new RolId(rolId);

        Rol rol = rolRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El rol con id '" + rolId + "' no existe"));
        if (usuarioRepository.contarPorRol(id) > 0) {
            throw DomainException.throwException(409,
                    "El rol '" + rol.getNombre() + "' lo tienen usuarios vivos y no se puede dar de baja");
        }

        rolRepository.eliminar(id);
    }
}
