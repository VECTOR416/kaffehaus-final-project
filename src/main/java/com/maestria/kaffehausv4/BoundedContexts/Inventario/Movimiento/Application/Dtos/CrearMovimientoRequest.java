package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearMovimientoRequest {
    private BigDecimal cantidad;
    private LocalDateTime fechaMovimiento;
    private String motivo;
    private String idExistencia;
    private String idTipoMovimiento;
    private String idUsuario;

    private CrearMovimientoRequest(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        String idExistencia,
        String idTipoMovimiento,
        String idUsuario
    ) {
        this.cantidad = cantidad;
        this.fechaMovimiento = fechaMovimiento;
        this.motivo = motivo;
        this.idExistencia = idExistencia;
        this.idTipoMovimiento = idTipoMovimiento;
        this.idUsuario = idUsuario;
    }

    public static CrearMovimientoRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        LocalDateTime fechaMovimiento = Validador.fechaOpcional(request.get("fechaMovimiento"), "fechaMovimiento");
        fechaMovimiento = fechaMovimiento != null ? fechaMovimiento : LocalDateTime.now();
        String motivo = Validador.textoOpcional(request.get("motivo"), "motivo");
        String idExistencia = Validador.textoRequerido(request.get("idExistencia"), "idExistencia");
        String idTipoMovimiento = Validador.textoRequerido(request.get("idTipoMovimiento"), "idTipoMovimiento");
        String idUsuario = Validador.textoRequerido(request.get("idUsuario"), "idUsuario");

        return new CrearMovimientoRequest(cantidad, fechaMovimiento, motivo, idExistencia, idTipoMovimiento, idUsuario);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public LocalDateTime getFechaMovimiento() {return fechaMovimiento;}
    public String getMotivo() {return motivo;}
    public String getIdExistencia() {return idExistencia;}
    public String getIdTipoMovimiento() {return idTipoMovimiento;}
    public String getIdUsuario() {return idUsuario;}
}
