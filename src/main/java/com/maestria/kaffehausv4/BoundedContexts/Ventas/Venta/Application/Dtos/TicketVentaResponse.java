package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;

/**
 * Lo que se imprime o se busca en un ticket (arquitectura.md 4.11, CAJ-14): la venta con sus lineas y
 * pagos, el nombre del cliente y el del cajero ya resueltos, y el nombre de cada tipo de pago.
 */
public class TicketVentaResponse {
    private String idVenta;
    private Long numeroTicket;
    private LocalDateTime fechaVenta;
    private String estadoVenta;
    private String idSesionCaja;
    private String idCliente;
    private String nombreCliente;
    private String carnetCliente;
    private String idCajero;
    private String nombreCajero;
    private BigDecimal total;
    private BigDecimal totalPagado;
    private BigDecimal cambio;
    private List<DetalleVentaResponse> detalles;
    private List<PagoVentaResponse> pagos;

    private TicketVentaResponse(
        String idVenta,
        Long numeroTicket,
        LocalDateTime fechaVenta,
        String estadoVenta,
        String idSesionCaja,
        String idCliente,
        String nombreCliente,
        String carnetCliente,
        String idCajero,
        String nombreCajero,
        BigDecimal total,
        BigDecimal totalPagado,
        BigDecimal cambio,
        List<DetalleVentaResponse> detalles,
        List<PagoVentaResponse> pagos
    ) {
        this.idVenta = idVenta;
        this.numeroTicket = numeroTicket;
        this.fechaVenta = fechaVenta;
        this.estadoVenta = estadoVenta;
        this.idSesionCaja = idSesionCaja;
        this.idCliente = idCliente;
        this.nombreCliente = nombreCliente;
        this.carnetCliente = carnetCliente;
        this.idCajero = idCajero;
        this.nombreCajero = nombreCajero;
        this.total = total;
        this.totalPagado = totalPagado;
        this.cambio = cambio;
        this.detalles = detalles;
        this.pagos = pagos;
    }

    /** 'tiposPago' indexa por id los tipos que aparecen en los pagos de la venta. */
    public static TicketVentaResponse createFromAggregate(
        Venta venta,
        EstadoVenta estadoVenta,
        Persona personaCliente,
        Persona personaCajero,
        Map<String, TipoPago> tiposPago
    ) {
        return new TicketVentaResponse(
            venta.getId().getValue(),
            venta.getNumeroTicket(),
            venta.getFechaVenta(),
            estadoVenta != null ? estadoVenta.getNombre() : null,
            venta.getSesionCajaId() != null ? venta.getSesionCajaId().getValue() : null,
            venta.getClienteId().getValue(),
            nombreCompleto(personaCliente),
            personaCliente != null ? personaCliente.getCarnet() : null,
            venta.getUsuarioId().getValue(),
            nombreCompleto(personaCajero),
            venta.getTotal(),
            venta.getTotalPagado(),
            venta.getCambio(),
            venta.getDetallesActivos().stream()
                    .map(DetalleVentaResponse::createFromAggregate)
                    .collect(Collectors.toList()),
            venta.getPagosActivos().stream()
                    .map(pago -> PagoVentaResponse.createFromAggregate(pago, tiposPago.get(pago.getTipoPagoId().getValue())))
                    .collect(Collectors.toList())
        );
    }

    private static String nombreCompleto(Persona persona) {
        return persona != null ? persona.getNombres() + " " + persona.getApellidos() : null;
    }

    public String getIdVenta() {return idVenta;}
    public Long getNumeroTicket() {return numeroTicket;}
    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public String getEstadoVenta() {return estadoVenta;}
    public String getIdSesionCaja() {return idSesionCaja;}
    public String getIdCliente() {return idCliente;}
    public String getNombreCliente() {return nombreCliente;}
    public String getCarnetCliente() {return carnetCliente;}
    public String getIdCajero() {return idCajero;}
    public String getNombreCajero() {return nombreCajero;}
    public BigDecimal getTotal() {return total;}
    public BigDecimal getTotalPagado() {return totalPagado;}
    public BigDecimal getCambio() {return cambio;}
    public List<DetalleVentaResponse> getDetalles() {return detalles;}
    public List<PagoVentaResponse> getPagos() {return pagos;}
}
