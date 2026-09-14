package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.CrearTipoMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.TipoMovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;

@Service
@Transactional
public class CrearTipoMovimientoUseCase {
    private final ITipoMovimientoRepository tipoMovimientoRepository;

    public CrearTipoMovimientoUseCase(
            ITipoMovimientoRepository tipoMovimientoRepository) {
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    public TipoMovimientoResponse execute(CrearTipoMovimientoRequest crearTipoMovimientoRequest) {
        TipoMovimiento tipoMovimiento = TipoMovimiento.crear(
                crearTipoMovimientoRequest.getNombre());

        TipoMovimiento tipoMovimientoCreado = tipoMovimientoRepository.crear(tipoMovimiento);

        return TipoMovimientoResponse.createFromAggregate(tipoMovimientoCreado);
    }
}
