package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.AbrirSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.SesionCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Abre el turno de caja a nombre de quien firma la peticion (arquitectura.md 4.11, CAJ-20). Si ya hay una
 * caja abierta responde 409. Dos aperturas simultaneas no pueden colarse las dos: la segunda
 * choca con uq_sesion_caja_abierta y GlobalExceptionHandler la traduce tambien a 409.
 */
@Service
@Transactional
public class AbrirSesionCajaUseCase {
    private final ISesionCajaRepository sesionCajaRepository;
    private final IUsuarioRepository usuarioRepository;

    public AbrirSesionCajaUseCase(
            ISesionCajaRepository sesionCajaRepository,
            IUsuarioRepository usuarioRepository) {
        this.sesionCajaRepository = sesionCajaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public SesionCajaResponse execute(AbrirSesionCajaRequest request) {
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));

        sesionCajaRepository.obtenerAbierta().ifPresent(abierta -> {
            throw DomainException.throwException(409,
                    "Ya hay una caja abierta desde " + abierta.getFechaApertura() + "; cierrala antes de abrir otra");
        });

        SesionCaja sesionCaja = SesionCaja.abrir(usuarioId, request.getMontoApertura(), LocalDateTime.now());

        return SesionCajaResponse.createFromAggregate(sesionCajaRepository.crear(sesionCaja));
    }
}
