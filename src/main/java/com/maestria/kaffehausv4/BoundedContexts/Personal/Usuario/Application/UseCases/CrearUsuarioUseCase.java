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

/**
 * La persona se crea aqui dentro, en la misma transaccion que el usuario: no existe un
 * alta de personas huerfanas, asi que quien quiera dar de alta un usuario manda tambien
 * sus datos personales. Si envia idPersona se reutiliza esa persona y se refrescan sus
 * datos, que es el caso de alguien ya registrado como cliente o proveedor.
 */
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
        RolId rolId = new RolId(crearUsuarioRequest.getIdRol());
        Rol rol = rolRepository.obtenerPorId(rolId)
                .orElseThrow(() -> DomainException.throwException(404, "El rol con id '" + crearUsuarioRequest.getIdRol() + "' no existe"));

        Persona persona = resolverPersona(crearUsuarioRequest);

        String hashContrasena = hasheadorDeContrasena.hashear(crearUsuarioRequest.getContrasena());

        Usuario usuario = Usuario.crear(persona.getId(), crearUsuarioRequest.getCorreo(), hashContrasena, rolId);
        Usuario usuarioCreado = usuarioRepository.crear(usuario);

        return UsuarioResponse.createFromAggregate(usuarioCreado, persona, rol);
    }

    private Persona resolverPersona(CrearUsuarioRequest crearUsuarioRequest) {
        String idPersona = crearUsuarioRequest.getIdPersona();

        if (idPersona == null) {
            return personaRepository.crear(nuevaPersona(crearUsuarioRequest));
        }

        PersonaId personaId = new PersonaId(idPersona);

        return personaRepository.obtenerPorId(personaId)
                .map(personaDB -> {
                    personaDB.actualizar(
                            crearUsuarioRequest.getNombres(),
                            crearUsuarioRequest.getApellidos(),
                            crearUsuarioRequest.getTelefono(),
                            crearUsuarioRequest.getCarnet());
                    return personaRepository.actualizar(personaId, personaDB);
                })
                .orElseGet(() -> personaRepository.crear(nuevaPersona(crearUsuarioRequest)));
    }

    private static Persona nuevaPersona(CrearUsuarioRequest crearUsuarioRequest) {
        return Persona.crear(
                crearUsuarioRequest.getNombres(),
                crearUsuarioRequest.getApellidos(),
                crearUsuarioRequest.getTelefono(),
                crearUsuarioRequest.getCarnet());
    }
}
