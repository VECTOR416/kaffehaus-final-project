package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de un usuario (logica.md USUARIO-04; solo Administrador, USUARIO-01). Responde 409
 * si quien la pide se da de baja a si mismo, si es el ultimo usuario vivo con rol Administrador
 * (nadie podria volver a gestionar usuarios) o si es el titular de la caja abierta (nadie
 * responderia por ese dinero).
 */
@Service
@Transactional
public class EliminarUsuarioUseCase {
    private final IUsuarioRepository usuarioRepository;
    private final IRolRepository rolRepository;
    private final ISesionCajaRepository sesionCajaRepository;

    public EliminarUsuarioUseCase(
            IUsuarioRepository usuarioRepository,
            IRolRepository rolRepository,
            ISesionCajaRepository sesionCajaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.sesionCajaRepository = sesionCajaRepository;
    }

    /** 'idUsuarioEnSesion' es quien pide la baja: lo pone el controller desde el token. */
    public void execute(String usuarioId, String idUsuarioEnSesion) {
        UsuarioId id = new UsuarioId(usuarioId);

        Usuario usuario = usuarioRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + usuarioId + "' no existe"));

        if (usuarioId.equals(idUsuarioEnSesion)) {
            throw DomainException.throwException(409, "Un usuario no se da de baja a si mismo");
        }
        boolean esAdministrador = rolRepository.obtenerPorId(usuario.getRolId())
                .map(rol -> Rol.ADMINISTRADOR.equalsIgnoreCase(rol.getNombre()))
                .orElse(false);
        if (esAdministrador && usuarioRepository.contarPorRol(usuario.getRolId()) <= 1) {
            throw DomainException.throwException(409,
                    "El usuario '" + usuario.getCorreo() + "' es el ultimo Administrador y no se puede dar de baja");
        }
        boolean esTitularDeLaCaja = sesionCajaRepository.obtenerAbierta()
                .map(sesionCaja -> sesionCaja.getUsuarioId().getValue().equals(usuarioId))
                .orElse(false);
        if (esTitularDeLaCaja) {
            throw DomainException.throwException(409,
                    "El usuario '" + usuario.getCorreo() + "' es el titular de la caja abierta: cierrala antes de darlo de baja");
        }

        usuarioRepository.eliminar(id);
    }
}
