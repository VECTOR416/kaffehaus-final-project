package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarTipoPagoUseCase {
    private final ITipoPagoRepository tipoPagoRepository;

    public EliminarTipoPagoUseCase(ITipoPagoRepository tipoPagoRepository) {
        this.tipoPagoRepository = tipoPagoRepository;
    }

    public void execute(String tipoPagoId) {
        TipoPagoId id = new TipoPagoId(tipoPagoId);

        tipoPagoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de pago con id '" + tipoPagoId + "' no existe"));

        tipoPagoRepository.eliminar(id);
    }
}
