package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearExistenciaRequest {
    private BigDecimal contenidoInicial;
    private BigDecimal contenidoActual;
    private LocalDateTime fechaAcabado;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaApertura;
    private String idDetalleLote;
    private String idEstadoExistencia;
    private String idUnidad;

    private CrearExistenciaRequest(
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        LocalDateTime fechaApertura,
        String idDetalleLote,
        String idEstadoExistencia,
        String idUnidad
    ) {
        this.contenidoInicial = contenidoInicial;
        this.contenidoActual = contenidoActual;
        this.fechaAcabado = fechaAcabado;
        this.fechaVencimiento = fechaVencimiento;
        this.fechaApertura = fechaApertura;
        this.idDetalleLote = idDetalleLote;
        this.idEstadoExistencia = idEstadoExistencia;
        this.idUnidad = idUnidad;
    }

    public static CrearExistenciaRequest createFromRequest(Map<String, Object> request){
        BigDecimal contenidoInicial = BigDecimal.valueOf(Validador.numeroRequerido(request.get("contenidoInicial"), "contenidoInicial"));
        BigDecimal contenidoActual = BigDecimal.valueOf(Validador.numeroRequerido(request.get("contenidoActual"), "contenidoActual"));
        LocalDateTime fechaAcabado = Validador.fechaOpcional(request.get("fechaAcabado"), "fechaAcabado");
        LocalDateTime fechaVencimientoTmp = Validador.fechaOpcional(request.get("fechaVencimiento"), "fechaVencimiento");
        LocalDate fechaVencimiento = fechaVencimientoTmp != null ? fechaVencimientoTmp.toLocalDate() : null;
        LocalDateTime fechaApertura = Validador.fechaOpcional(request.get("fechaApertura"), "fechaApertura");
        String idDetalleLote = Validador.textoRequerido(request.get("idDetalleLote"), "idDetalleLote");
        String idEstadoExistencia = Validador.textoRequerido(request.get("idEstadoExistencia"), "idEstadoExistencia");
        String idUnidad = Validador.textoRequerido(request.get("idUnidad"), "idUnidad");

        return new CrearExistenciaRequest(contenidoInicial, contenidoActual, fechaAcabado, fechaVencimiento, fechaApertura, idDetalleLote, idEstadoExistencia, idUnidad);
    }

    public BigDecimal getContenidoInicial() {return contenidoInicial;}
    public BigDecimal getContenidoActual() {return contenidoActual;}
    public LocalDateTime getFechaAcabado() {return fechaAcabado;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public LocalDateTime getFechaApertura() {return fechaApertura;}
    public String getIdDetalleLote() {return idDetalleLote;}
    public String getIdEstadoExistencia() {return idEstadoExistencia;}
    public String getIdUnidad() {return idUnidad;}
}
