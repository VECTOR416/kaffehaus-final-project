package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.AutenticacionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.IniciarSesionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.UsuarioAutenticadoResponse;
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Shared.Application.Security.IGeneradorDeTokens;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Security.RolDelSistema;
import com.maestria.kaffehausv4.Shared.Domain.Security.UsuarioAutenticado;

/**
 * Login clasico: correo y contrasena.
 *
 * Responde 401 con el mismo mensaje tanto si el correo no existe como si la contrasena no
 * coincide. Distinguir ambos casos convertiria el login en un oraculo para averiguar que
 * correos estan registrados.
 */
@Service
@Transactional(readOnly = true)
public class IniciarSesionUseCase {

    private static final String CREDENCIALES_INVALIDAS = "Las credenciales no son validas";

    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;
    private final IRolRepository rolRepository;
    private final IHasheadorDeContrasena hasheadorDeContrasena;
    private final IGeneradorDeTokens generadorDeTokens;

    public IniciarSesionUseCase(
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository,
            IRolRepository rolRepository,
            IHasheadorDeContrasena hasheadorDeContrasena,
            IGeneradorDeTokens generadorDeTokens) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.hasheadorDeContrasena = hasheadorDeContrasena;
        this.generadorDeTokens = generadorDeTokens;
    }

    public AutenticacionResponse execute(IniciarSesionRequest iniciarSesionRequest) {
        Usuario usuario = usuarioRepository.obtenerPorCorreo(iniciarSesionRequest.getEmail())
                .orElseThrow(() -> AutorizacionException.throwException(401, CREDENCIALES_INVALIDAS));

        // obtenerPorCorreo ya filtra los borrados, pero la comprobacion queda explicita: que un
        // usuario dado de baja no pueda entrar es una regla, no un efecto colateral del filtro.
        if (usuario.isBorrado()) {
            throw AutorizacionException.throwException(401, CREDENCIALES_INVALIDAS);
        }

        if (!hasheadorDeContrasena.coincide(iniciarSesionRequest.getPassword(), usuario.getContrasena())) {
            throw AutorizacionException.throwException(401, CREDENCIALES_INVALIDAS);
        }

        return armarSesion(usuario);
    }

    private AutenticacionResponse armarSesion(Usuario usuario) {
        Persona persona = personaRepository.obtenerPorId(usuario.getPersonaId())
                .orElseThrow(() -> AutorizacionException.throwException(401, CREDENCIALES_INVALIDAS));

        Rol rol = rolRepository.obtenerPorId(usuario.getRolId())
                .orElseThrow(() -> AutorizacionException.throwException(403, "El usuario no tiene un rol asignado valido"));

        RolDelSistema rolDelSistema = RolDelSistema.desdeNombre(rol.getNombre());
        String nombreCompleto = UsuarioAutenticadoResponse.nombreCompletoDe(persona);

        UsuarioAutenticado autenticado = new UsuarioAutenticado(
                usuario.getId().getValue(),
                nombreCompleto,
                rolDelSistema.getCodigo(),
                rolDelSistema.getNombre());

        return AutenticacionResponse.create(
                UsuarioAutenticadoResponse.createFromAggregate(usuario, persona, rolDelSistema),
                generadorDeTokens.emitirAccessToken(autenticado),
                generadorDeTokens.emitirRefreshToken(usuario.getId().getValue()));
    }
}
