package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.CrearEstadoVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.EstadoVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;

@Service
@Transactional
public class CrearEstadoVentaUseCase {
    private final IEstadoVentaRepository estadoVentaRepository;

    public CrearEstadoVentaUseCase(
            IEstadoVentaRepository estadoVentaRepository) {
        this.estadoVentaRepository = estadoVentaRepository;
    }

    public EstadoVentaResponse execute(CrearEstadoVentaRequest crearEstadoVentaRequest) {
        EstadoVenta estadoVenta = EstadoVenta.crear(
                crearEstadoVentaRequest.getNombre());

        EstadoVenta estadoVentaCreado = estadoVentaRepository.crear(estadoVenta);

        return EstadoVentaResponse.createFromAggregate(estadoVentaCreado);
    }
}
