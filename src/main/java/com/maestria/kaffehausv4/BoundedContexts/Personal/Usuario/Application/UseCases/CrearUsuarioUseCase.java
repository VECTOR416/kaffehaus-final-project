package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.CrearUsuarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.UsuarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearUsuarioUseCase {
    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;
    private final IRolRepository rolRepository;
    private final IHasheadorDeContrasena hasheadorDeContrasena;

    public CrearUsuarioUseCase(
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository,
            IRolRepository rolRepository,
            IHasheadorDeContrasena hasheadorDeContrasena) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.hasheadorDeContrasena = hasheadorDeContrasena;
    }

    public UsuarioResponse execute(CrearUsuarioRequest crearUsuarioRequest) {
        PersonaId personaId = new PersonaId(crearUsuarioRequest.getIdPersona());
        Persona persona = personaRepository.obtenerPorId(personaId)
                .orElseThrow(() -> DomainException.throwException(404, "La persona con id '" + crearUsuarioRequest.getIdPersona() + "' no existe"));

        RolId rolId = new RolId(crearUsuarioRequest.getIdRol());
        Rol rol = rolRepository.obtenerPorId(rolId)
                .orElseThrow(() -> DomainException.throwException(404, "El rol con id '" + crearUsuarioRequest.getIdRol() + "' no existe"));

        String hashContrasena = hasheadorDeContrasena.hashear(crearUsuarioRequest.getContrasena());

        Usuario usuario = Usuario.crear(personaId, crearUsuarioRequest.getCorreo(), hashContrasena, rolId);
        Usuario usuarioCreado = usuarioRepository.crear(usuario);

        return UsuarioResponse.createFromAggregate(usuarioCreado, persona, rol);
    }
}
