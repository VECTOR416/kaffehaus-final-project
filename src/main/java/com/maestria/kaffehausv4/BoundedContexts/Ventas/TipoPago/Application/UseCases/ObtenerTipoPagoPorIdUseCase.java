package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.TipoPagoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerTipoPagoPorIdUseCase {
    private final ITipoPagoRepository tipoPagoRepository;

    public ObtenerTipoPagoPorIdUseCase(
            ITipoPagoRepository tipoPagoRepository) {
        this.tipoPagoRepository = tipoPagoRepository;
    }

    public TipoPagoResponse execute(String tipoPagoId) {
        TipoPagoId id = new TipoPagoId(tipoPagoId);

        TipoPago tipoPago = tipoPagoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de pago con id '" + tipoPagoId + "' no existe"));

        return TipoPagoResponse.createFromAggregate(tipoPago);
    }
}
