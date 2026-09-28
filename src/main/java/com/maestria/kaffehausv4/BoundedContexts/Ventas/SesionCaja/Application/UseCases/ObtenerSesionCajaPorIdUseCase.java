package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.SesionCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerSesionCajaPorIdUseCase {
    private final ISesionCajaRepository sesionCajaRepository;

    public ObtenerSesionCajaPorIdUseCase(ISesionCajaRepository sesionCajaRepository) {
        this.sesionCajaRepository = sesionCajaRepository;
    }

    public SesionCajaResponse execute(String sesionCajaId) {
        SesionCaja sesionCaja = sesionCajaRepository.obtenerPorId(new SesionCajaId(sesionCajaId))
                .orElseThrow(() -> DomainException.throwException(404, "La sesion de caja con id '" + sesionCajaId + "' no existe"));

        return SesionCajaResponse.createFromAggregate(sesionCaja);
    }
}
