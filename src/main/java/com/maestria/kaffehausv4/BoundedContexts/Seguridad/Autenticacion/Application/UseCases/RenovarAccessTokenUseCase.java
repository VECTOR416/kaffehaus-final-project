package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.AccessTokenResponse;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.RenovarTokenRequest;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos.UsuarioAutenticadoResponse;
import com.maestria.kaffehausv4.Shared.Application.Security.IGeneradorDeTokens;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Security.RolDelSistema;
import com.maestria.kaffehausv4.Shared.Domain.Security.UsuarioAutenticado;

/**
 * Renovacion del access token a partir del refresh token.
 *
 * El refresh no se limita a verificar la firma: vuelve a leer al usuario y su rol de la base.
 * Asi un usuario dado de baja, o al que le cambiaron el rol, deja de renovar (o renueva con su
 * rol nuevo) sin esperar a que caduquen los 7 dias.
 *
 * Limitacion consciente del diseno elegido: al ser un JWT autocontenido y no haber
 * almacenamiento de tokens, un refresh token robado sigue siendo valido hasta su vencimiento;
 * lo unico que lo corta antes es dar de baja al usuario.
 */
@Service
@Transactional(readOnly = true)
public class RenovarAccessTokenUseCase {

    private static final String REFRESH_INVALIDO = "El refresh token no es valido";

    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;
    private final IRolRepository rolRepository;
    private final IGeneradorDeTokens generadorDeTokens;

    public RenovarAccessTokenUseCase(
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository,
            IRolRepository rolRepository,
            IGeneradorDeTokens generadorDeTokens) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.generadorDeTokens = generadorDeTokens;
    }

    public AccessTokenResponse execute(RenovarTokenRequest renovarTokenRequest) {
        String usuarioId = generadorDeTokens.verificarRefreshToken(renovarTokenRequest.getRefreshToken());

        Usuario usuario = usuarioRepository.obtenerPorId(new UsuarioId(usuarioId))
                .orElseThrow(() -> AutorizacionException.throwException(401, REFRESH_INVALIDO));

        if (usuario.isBorrado()) {
            throw AutorizacionException.throwException(401, REFRESH_INVALIDO);
        }

        Persona persona = personaRepository.obtenerPorId(usuario.getPersonaId())
                .orElseThrow(() -> AutorizacionException.throwException(401, REFRESH_INVALIDO));

        Rol rol = rolRepository.obtenerPorId(usuario.getRolId())
                .orElseThrow(() -> AutorizacionException.throwException(403, "El usuario no tiene un rol asignado valido"));

        RolDelSistema rolDelSistema = RolDelSistema.desdeNombre(rol.getNombre());

        UsuarioAutenticado autenticado = new UsuarioAutenticado(
                usuario.getId().getValue(),
                UsuarioAutenticadoResponse.nombreCompletoDe(persona),
                rolDelSistema.getCodigo(),
                rolDelSistema.getNombre());

        return AccessTokenResponse.create(generadorDeTokens.emitirAccessToken(autenticado));
    }
}
