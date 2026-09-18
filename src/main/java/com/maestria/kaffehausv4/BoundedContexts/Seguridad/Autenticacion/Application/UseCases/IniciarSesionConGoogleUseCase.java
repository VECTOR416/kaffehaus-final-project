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
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.IniciarSesionConGoogleRequest;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.UsuarioAutenticadoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Tokens.IVerificadorDeTokenGoogle;
import com.maestria.kaffehausv4.Shared.Application.Security.IGeneradorDeTokens;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Security.RolDelSistema;
import com.maestria.kaffehausv4.Shared.Domain.Security.UsuarioAutenticado;

/**
 * Login con Google.
 *
 * Google solo acredita quien es el que llama; no da acceso por si mismo. El usuario debe estar
 * ya registrado con ese correo: si no existe, la respuesta es 401 y no se crea nada. Es una
 * decision deliberada de la especificacion, no una limitacion tecnica, y evita que cualquier
 * cuenta de Gmail se convierta en usuario del sistema.
 *
 * El caso de uso es de solo lectura por la misma razon: aqui no se escribe en la base.
 */
@Service
@Transactional(readOnly = true)
public class IniciarSesionConGoogleUseCase {

    private static final String SIN_ACCESO = "La cuenta de Google no corresponde a un usuario registrado";

    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;
    private final IRolRepository rolRepository;
    private final IVerificadorDeTokenGoogle verificadorDeTokenGoogle;
    private final IGeneradorDeTokens generadorDeTokens;

    public IniciarSesionConGoogleUseCase(
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository,
            IRolRepository rolRepository,
            IVerificadorDeTokenGoogle verificadorDeTokenGoogle,
            IGeneradorDeTokens generadorDeTokens) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.verificadorDeTokenGoogle = verificadorDeTokenGoogle;
        this.generadorDeTokens = generadorDeTokens;
    }

    public AutenticacionResponse execute(IniciarSesionConGoogleRequest iniciarSesionConGoogleRequest) {
        String correo = verificadorDeTokenGoogle.obtenerCorreoVerificado(iniciarSesionConGoogleRequest.getGoogleToken());

        Usuario usuario = usuarioRepository.obtenerPorCorreo(correo)
                .orElseThrow(() -> AutorizacionException.throwException(401, SIN_ACCESO));

        if (usuario.isBorrado()) {
            throw AutorizacionException.throwException(401, SIN_ACCESO);
        }

        return armarSesion(usuario);
    }

    private AutenticacionResponse armarSesion(Usuario usuario) {
        Persona persona = personaRepository.obtenerPorId(usuario.getPersonaId())
                .orElseThrow(() -> AutorizacionException.throwException(401, SIN_ACCESO));

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
