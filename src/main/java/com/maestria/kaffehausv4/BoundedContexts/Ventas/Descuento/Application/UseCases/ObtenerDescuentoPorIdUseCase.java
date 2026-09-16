package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.DescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerDescuentoPorIdUseCase {
    private final IDescuentoRepository descuentoRepository;
    private final ITipoDescuentoRepository tipoDescuentoRepository;

    public ObtenerDescuentoPorIdUseCase(
            IDescuentoRepository descuentoRepository,
            ITipoDescuentoRepository tipoDescuentoRepository) {
        this.descuentoRepository = descuentoRepository;
        this.tipoDescuentoRepository = tipoDescuentoRepository;
    }

    public DescuentoResponse execute(String descuentoId) {
        DescuentoId id = new DescuentoId(descuentoId);

        Descuento descuento = descuentoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El descuento con id '" + descuentoId + "' no existe"));

        return toResponse(descuento);
    }

    private DescuentoResponse toResponse(Descuento descuento) {
        TipoDescuento tipoDescuento = tipoDescuentoRepository.obtenerPorId(descuento.getTipoDescuentoId())
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento asociado a el descuento no existe"));
        return DescuentoResponse.createFromAggregate(descuento, tipoDescuento);
    }
}
