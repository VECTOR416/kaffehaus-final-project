package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarVentaRequest {
    private LocalDateTime fechaVenta;
    private BigDecimal total;
    private String idUsuario;
    private String idCliente;
    private String idEstadoVenta;

    private ActualizarVentaRequest(
        LocalDateTime fechaVenta,
        BigDecimal total,
        String idUsuario,
        String idCliente,
        String idEstadoVenta
    ) {
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.idEstadoVenta = idEstadoVenta;
    }

    public static ActualizarVentaRequest createFromRequest(Map<String, Object> request){
        LocalDateTime fechaVenta = Validador.fechaOpcional(request.get("fechaVenta"), "fechaVenta");
        Double totalTmp = Validador.numeroOpcional(request.get("total"), "total");
        BigDecimal total = totalTmp != null ? BigDecimal.valueOf(totalTmp) : null;
        String idUsuario = Validador.textoOpcional(request.get("idUsuario"), "idUsuario");
        String idCliente = Validador.textoOpcional(request.get("idCliente"), "idCliente");
        String idEstadoVenta = Validador.textoOpcional(request.get("idEstadoVenta"), "idEstadoVenta");

        return new ActualizarVentaRequest(fechaVenta, total, idUsuario, idCliente, idEstadoVenta);
    }

    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public void setFechaVenta(LocalDateTime fechaVenta) {this.fechaVenta = fechaVenta;}
    public BigDecimal getTotal() {return total;}
    public void setTotal(BigDecimal total) {this.total = total;}
    public String getIdUsuario() {return idUsuario;}
    public void setIdUsuario(String idUsuario) {this.idUsuario = idUsuario;}
    public String getIdCliente() {return idCliente;}
    public void setIdCliente(String idCliente) {this.idCliente = idCliente;}
    public String getIdEstadoVenta() {return idEstadoVenta;}
    public void setIdEstadoVenta(String idEstadoVenta) {this.idEstadoVenta = idEstadoVenta;}
}
