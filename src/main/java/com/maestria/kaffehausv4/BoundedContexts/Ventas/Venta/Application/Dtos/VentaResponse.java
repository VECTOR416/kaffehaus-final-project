package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.EstadoVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;

/**
 * Respuesta del agregado completo: encabezado mas sus lineas y pagos vivos. Lo dado de baja
 * no se devuelve, igual que el resto del proyecto oculta lo borrado logicamente.
 */
public class VentaResponse {
    private String id;
    private Long numeroTicket;
    private LocalDateTime fechaVenta;
    private BigDecimal total;
    private String idUsuario;
    private String idCliente;
    private String idSesionCaja;
    private EstadoVentaResponse estadoVenta;
    private String claveIdempotencia;
    private String motivoAnulacion;
    private String idUsuarioAnulacion;
    private String referenciaReembolso;
    private boolean borrado;
    private List<DetalleVentaResponse> detalles;
    private BigDecimal cambio;
    private List<PagoVentaResponse> pagos;

    private VentaResponse(
        String id,
        Long numeroTicket,
        LocalDateTime fechaVenta,
        BigDecimal total,
        String idUsuario,
        String idCliente,
        String idSesionCaja,
        EstadoVentaResponse estadoVenta,
        String claveIdempotencia,
        String motivoAnulacion,
        String idUsuarioAnulacion,
        String referenciaReembolso,
        boolean borrado,
        List<DetalleVentaResponse> detalles,
        BigDecimal cambio,
        List<PagoVentaResponse> pagos
    ) {
        this.id = id;
        this.numeroTicket = numeroTicket;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.idSesionCaja = idSesionCaja;
        this.estadoVenta = estadoVenta;
        this.claveIdempotencia = claveIdempotencia;
        this.motivoAnulacion = motivoAnulacion;
        this.idUsuarioAnulacion = idUsuarioAnulacion;
        this.referenciaReembolso = referenciaReembolso;
        this.borrado = borrado;
        this.detalles = detalles;
        this.cambio = cambio;
        this.pagos = pagos;
    }

    public static VentaResponse createFromAggregate(Venta venta, EstadoVenta estadoVenta){
        EstadoVentaResponse estadoVentaResponse = estadoVenta != null ? EstadoVentaResponse.createFromAggregate(estadoVenta) : null;

        List<DetalleVentaResponse> detalles = venta.getDetallesActivos().stream()
                .map(DetalleVentaResponse::createFromAggregate)
                .collect(Collectors.toList());

        List<PagoVentaResponse> pagos = venta.getPagosActivos().stream()
                .map(pago -> PagoVentaResponse.createFromAggregate(pago, null))
                .collect(Collectors.toList());

        return new VentaResponse(
            venta.getId().getValue(),
            venta.getNumeroTicket(),
            venta.getFechaVenta(),
            venta.getTotal(),
            venta.getUsuarioId() != null ? venta.getUsuarioId().getValue() : null,
            venta.getClienteId() != null ? venta.getClienteId().getValue() : null,
            venta.getSesionCajaId() != null ? venta.getSesionCajaId().getValue() : null,
            estadoVentaResponse,
            venta.getClaveIdempotencia(),
            venta.getMotivoAnulacion(),
            venta.getUsuarioAnulacionId() != null ? venta.getUsuarioAnulacionId().getValue() : null,
            venta.getReferenciaReembolso(),
            venta.isBorrado(),
            detalles,
            venta.getCambio(),
            pagos
        );
    }

    public String getId() {return id;}
    public Long getNumeroTicket() {return numeroTicket;}
    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public BigDecimal getTotal() {return total;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdCliente() {return idCliente;}
    public String getIdSesionCaja() {return idSesionCaja;}
    public EstadoVentaResponse getEstadoVenta() {return estadoVenta;}
    public String getClaveIdempotencia() {return claveIdempotencia;}
    public String getMotivoAnulacion() {return motivoAnulacion;}
    public String getIdUsuarioAnulacion() {return idUsuarioAnulacion;}
    public String getReferenciaReembolso() {return referenciaReembolso;}
    public boolean isBorrado() {return borrado;}
    public List<DetalleVentaResponse> getDetalles() {return detalles;}
    public BigDecimal getCambio() {return cambio;}
    public List<PagoVentaResponse> getPagos() {return pagos;}
}
