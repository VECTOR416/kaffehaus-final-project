package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RegistrarMovimientoCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.SesionCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Registra un ingreso o egreso menor de efectivo en la caja abierta (arquitectura.md 4.11, CAJ-21). La
 * sesion se bloquea en exclusiva, igual que al cerrarla, para que un movimiento no llegue
 * despues de calculado el arqueo.
 */
@Service
@Transactional
public class RegistrarMovimientoCajaUseCase {
    private final ISesionCajaRepository sesionCajaRepository;
    private final IUsuarioRepository usuarioRepository;

    public RegistrarMovimientoCajaUseCase(
            ISesionCajaRepository sesionCajaRepository,
            IUsuarioRepository usuarioRepository) {
        this.sesionCajaRepository = sesionCajaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public SesionCajaResponse execute(RegistrarMovimientoCajaRequest request) {
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));

        SesionCaja sesionCaja = sesionCajaRepository.obtenerAbiertaParaActualizar()
                .orElseThrow(() -> DomainException.throwException(409, "No hay una caja abierta en la que registrar el movimiento"));

        sesionCaja.registrarMovimiento(usuarioId, request.getTipo(), request.getMonto(), request.getMotivo());

        return SesionCajaResponse.createFromAggregate(sesionCajaRepository.actualizar(sesionCaja.getId(), sesionCaja));
    }
}
