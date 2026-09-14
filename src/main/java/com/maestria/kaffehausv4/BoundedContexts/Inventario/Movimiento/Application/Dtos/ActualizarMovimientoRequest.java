package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarMovimientoRequest {
    private BigDecimal cantidad;
    private LocalDateTime fechaMovimiento;
    private String motivo;
    private String idExistencia;
    private String idTipoMovimiento;

    private ActualizarMovimientoRequest(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        String idExistencia,
        String idTipoMovimiento
    ) {
        this.cantidad = cantidad;
        this.fechaMovimiento = fechaMovimiento;
        this.motivo = motivo;
        this.idExistencia = idExistencia;
        this.idTipoMovimiento = idTipoMovimiento;
    }

    public static ActualizarMovimientoRequest createFromRequest(Map<String, Object> request){
        Double cantidadTmp = Validador.numeroOpcional(request.get("cantidad"), "cantidad");
        BigDecimal cantidad = cantidadTmp != null ? BigDecimal.valueOf(cantidadTmp) : null;
        LocalDateTime fechaMovimiento = Validador.fechaOpcional(request.get("fechaMovimiento"), "fechaMovimiento");
        String motivo = Validador.textoOpcional(request.get("motivo"), "motivo");
        String idExistencia = Validador.textoOpcional(request.get("idExistencia"), "idExistencia");
        String idTipoMovimiento = Validador.textoOpcional(request.get("idTipoMovimiento"), "idTipoMovimiento");

        return new ActualizarMovimientoRequest(cantidad, fechaMovimiento, motivo, idExistencia, idTipoMovimiento);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public LocalDateTime getFechaMovimiento() {return fechaMovimiento;}
    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {this.fechaMovimiento = fechaMovimiento;}
    public String getMotivo() {return motivo;}
    public void setMotivo(String motivo) {this.motivo = motivo;}
    public String getIdExistencia() {return idExistencia;}
    public void setIdExistencia(String idExistencia) {this.idExistencia = idExistencia;}
    public String getIdTipoMovimiento() {return idTipoMovimiento;}
    public void setIdTipoMovimiento(String idTipoMovimiento) {this.idTipoMovimiento = idTipoMovimiento;}
}
