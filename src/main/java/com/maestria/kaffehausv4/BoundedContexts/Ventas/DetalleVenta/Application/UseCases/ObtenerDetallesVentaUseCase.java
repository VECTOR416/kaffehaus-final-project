package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.DetalleVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.ObtenerDetallesVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Repositories.IDetalleVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerDetallesVentaUseCase {
    private final IDetalleVentaRepository detalleVentaRepository;

    public ObtenerDetallesVentaUseCase(
            IDetalleVentaRepository detalleVentaRepository) {
        this.detalleVentaRepository = detalleVentaRepository;
    }

    public PaginatedResponse<List<DetalleVentaResponse>> execute(ObtenerDetallesVentaRequest obtenerDetallesVentaRequest) {
        PaginatedResponse<List<DetalleVenta>> resultado = detalleVentaRepository.obtenerTodos(obtenerDetallesVentaRequest);

        List<DetalleVentaResponse> detallesVenta = resultado.getData().stream()
                .map(DetalleVentaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                detallesVenta,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
