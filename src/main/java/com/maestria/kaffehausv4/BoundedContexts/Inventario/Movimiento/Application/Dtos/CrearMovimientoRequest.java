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
    private String idProduccion;

    private CrearMovimientoRequest(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        String idExistencia,
        String idTipoMovimiento,
        String idUsuario,
        String idProduccion
    ) {
        this.cantidad = cantidad;
        this.fechaMovimiento = fechaMovimiento;
        this.motivo = motivo;
        this.idExistencia = idExistencia;
        this.idTipoMovimiento = idTipoMovimiento;
        this.idUsuario = idUsuario;
        this.idProduccion = idProduccion;
    }

    /**
     * 'idUsuarioEnSesion' es quien firma la peticion: lo pone el controller desde el token con
     * @UsuarioEnSesion. El frontend no envia 'idUsuario'; si llegara en el cuerpo, se ignora.
     */
    public static CrearMovimientoRequest createFromRequest(Map<String, Object> request, String idUsuarioEnSesion){
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        LocalDateTime fechaMovimiento = Validador.fechaOpcional(request.get("fechaMovimiento"), "fechaMovimiento");
        fechaMovimiento = fechaMovimiento != null ? fechaMovimiento : LocalDateTime.now();
        String motivo = Validador.textoOpcional(request.get("motivo"), "motivo");
        String idExistencia = Validador.textoRequerido(request.get("idExistencia"), "idExistencia");
        String idTipoMovimiento = Validador.textoRequerido(request.get("idTipoMovimiento"), "idTipoMovimiento");
        String idUsuario = Validador.textoRequerido(idUsuarioEnSesion, "idUsuario");
        String idProduccion = Validador.textoOpcional(request.get("idProduccion"), "idProduccion");

        return new CrearMovimientoRequest(cantidad, fechaMovimiento, motivo, idExistencia, idTipoMovimiento, idUsuario, idProduccion);
    }

    public BigDecimal getCantidad() {return cantidad;}
    public LocalDateTime getFechaMovimiento() {return fechaMovimiento;}
    public String getMotivo() {return motivo;}
    public String getIdExistencia() {return idExistencia;}
    public String getIdTipoMovimiento() {return idTipoMovimiento;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdProduccion() {return idProduccion;}
}
