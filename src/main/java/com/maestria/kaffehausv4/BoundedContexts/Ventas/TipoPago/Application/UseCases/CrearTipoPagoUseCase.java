package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.CrearTipoPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.TipoPagoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;

@Service
@Transactional
public class CrearTipoPagoUseCase {
    private final ITipoPagoRepository tipoPagoRepository;

    public CrearTipoPagoUseCase(
            ITipoPagoRepository tipoPagoRepository) {
        this.tipoPagoRepository = tipoPagoRepository;
    }

    public TipoPagoResponse execute(CrearTipoPagoRequest crearTipoPagoRequest) {
        TipoPago tipoPago = TipoPago.crear(
                crearTipoPagoRequest.getNombre());

        TipoPago tipoPagoCreado = tipoPagoRepository.crear(tipoPago);

        return TipoPagoResponse.createFromAggregate(tipoPagoCreado);
    }
}
