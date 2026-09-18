package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.EstadoVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;

/**
 * Respuesta del agregado completo: encabezado mas sus lineas. La venta siempre se devuelve
 * entera porque entera es como se lee y se escribe.
 */
public class VentaResponse {
    private String id;
    private LocalDateTime fechaVenta;
    private BigDecimal total;
    private String idUsuario;
    private String idCliente;
    private EstadoVentaResponse estadoVenta;
    private List<DetalleVentaResponse> detalles;

    private VentaResponse(
        String id,
        LocalDateTime fechaVenta,
        BigDecimal total,
        String idUsuario,
        String idCliente,
        EstadoVentaResponse estadoVenta,
        List<DetalleVentaResponse> detalles
    ) {
        this.id = id;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.estadoVenta = estadoVenta;
        this.detalles = detalles;
    }

    public static VentaResponse createFromAggregate(Venta venta, EstadoVenta estadoVenta){
        EstadoVentaResponse estadoVentaResponse = estadoVenta != null ? EstadoVentaResponse.createFromAggregate(estadoVenta) : null;

        List<DetalleVentaResponse> detalles = venta.getDetalles().stream()
                .map(DetalleVentaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new VentaResponse(
            venta.getId().getValue(),
            venta.getFechaVenta(),
            venta.getTotal(),
            venta.getUsuarioId() != null ? venta.getUsuarioId().getValue() : null,
            venta.getClienteId() != null ? venta.getClienteId().getValue() : null,
            estadoVentaResponse,
            detalles
        );
    }

    public String getId() {return id;}
    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public BigDecimal getTotal() {return total;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdCliente() {return idCliente;}
    public EstadoVentaResponse getEstadoVenta() {return estadoVenta;}
    public List<DetalleVentaResponse> getDetalles() {return detalles;}
}
