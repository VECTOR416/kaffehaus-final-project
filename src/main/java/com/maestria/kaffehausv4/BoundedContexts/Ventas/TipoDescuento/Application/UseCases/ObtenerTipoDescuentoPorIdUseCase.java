package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.TipoDescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerTipoDescuentoPorIdUseCase {
    private final ITipoDescuentoRepository tipoDescuentoRepository;

    public ObtenerTipoDescuentoPorIdUseCase(
            ITipoDescuentoRepository tipoDescuentoRepository) {
        this.tipoDescuentoRepository = tipoDescuentoRepository;
    }

    public TipoDescuentoResponse execute(String tipoDescuentoId) {
        TipoDescuentoId id = new TipoDescuentoId(tipoDescuentoId);

        TipoDescuento tipoDescuento = tipoDescuentoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento con id '" + tipoDescuentoId + "' no existe"));

        return TipoDescuentoResponse.createFromAggregate(tipoDescuento);
    }
}
