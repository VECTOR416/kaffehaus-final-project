package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.ActualizarUsuarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.UsuarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * La persona asociada se actualiza en sitio y en la misma transaccion, sin pasar por un
 * endpoint de personas. El rol si se puede reapuntar, porque Rol es un catalogo con CRUD
 * propio.
 */
@Service
@Transactional
public class ActualizarUsuarioUseCase {
    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;
    private final IRolRepository rolRepository;
    private final IHasheadorDeContrasena hasheadorDeContrasena;

    public ActualizarUsuarioUseCase(
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository,
            IRolRepository rolRepository,
            IHasheadorDeContrasena hasheadorDeContrasena) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.hasheadorDeContrasena = hasheadorDeContrasena;
    }

    public UsuarioResponse execute(String usuarioId, ActualizarUsuarioRequest actualizarUsuarioRequest) {
        UsuarioId id = new UsuarioId(usuarioId);

        Usuario usuario = usuarioRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + usuarioId + "' no existe"));

        Persona persona = personaRepository.obtenerPorId(usuario.getPersonaId())
                .orElseThrow(() -> DomainException.throwException(404, "La persona asociada al usuario no existe"));

        persona.actualizar(
                actualizarUsuarioRequest.getNombres(),
                actualizarUsuarioRequest.getApellidos(),
                actualizarUsuarioRequest.getTelefono(),
                actualizarUsuarioRequest.getCarnet());

        Persona personaActualizada = personaRepository.actualizar(usuario.getPersonaId(), persona);

        RolId nuevoRolId = null;
        if (actualizarUsuarioRequest.getIdRol() != null) {
            nuevoRolId = new RolId(actualizarUsuarioRequest.getIdRol());
            rolRepository.obtenerPorId(nuevoRolId)
                    .orElseThrow(() -> DomainException.throwException(404, "El rol con id '" + actualizarUsuarioRequest.getIdRol() + "' no existe"));
        }

        String nuevaContrasena = actualizarUsuarioRequest.getContrasena() != null
                ? hasheadorDeContrasena.hashear(actualizarUsuarioRequest.getContrasena())
                : null;

        usuario.actualizar(
                null,
                actualizarUsuarioRequest.getCorreo(),
                nuevaContrasena,
                nuevoRolId != null ? nuevoRolId.getValue() : null);

        Usuario usuarioActualizado = usuarioRepository.actualizar(id, usuario);

        Rol rol = rolRepository.obtenerPorId(usuarioActualizado.getRolId())
                .orElseThrow(() -> DomainException.throwException(404, "El rol asociado al usuario no existe"));

        return UsuarioResponse.createFromAggregate(usuarioActualizado, personaActualizada, rol);
    }
}
