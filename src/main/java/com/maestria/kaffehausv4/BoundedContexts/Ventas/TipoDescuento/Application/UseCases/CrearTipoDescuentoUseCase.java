package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.CrearTipoDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.TipoDescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;

@Service
@Transactional
public class CrearTipoDescuentoUseCase {
    private final ITipoDescuentoRepository tipoDescuentoRepository;

    public CrearTipoDescuentoUseCase(
            ITipoDescuentoRepository tipoDescuentoRepository) {
        this.tipoDescuentoRepository = tipoDescuentoRepository;
    }

    public TipoDescuentoResponse execute(CrearTipoDescuentoRequest crearTipoDescuentoRequest) {
        TipoDescuento tipoDescuento = TipoDescuento.crear(
                crearTipoDescuentoRequest.getNombre());

        TipoDescuento tipoDescuentoCreado = tipoDescuentoRepository.crear(tipoDescuento);

        return TipoDescuentoResponse.createFromAggregate(tipoDescuentoCreado);
    }
}
