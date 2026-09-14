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
    private String idDetalleLote;
    private String idEstadoExistencia;
    private String idUnidad;
    private boolean borrado;

    private CrearExistenciaRequest(
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        String idDetalleLote,
        String idEstadoExistencia,
        String idUnidad,
        boolean borrado
    ) {
        this.contenidoInicial = contenidoInicial;
        this.contenidoActual = contenidoActual;
        this.fechaAcabado = fechaAcabado;
        this.fechaVencimiento = fechaVencimiento;
        this.idDetalleLote = idDetalleLote;
        this.idEstadoExistencia = idEstadoExistencia;
        this.idUnidad = idUnidad;
        this.borrado = borrado;
    }

    public static CrearExistenciaRequest createFromRequest(Map<String, Object> request){
        BigDecimal contenidoInicial = BigDecimal.valueOf(Validador.numeroRequerido(request.get("contenidoInicial"), "contenidoInicial"));
        BigDecimal contenidoActual = BigDecimal.valueOf(Validador.numeroRequerido(request.get("contenidoActual"), "contenidoActual"));
        LocalDateTime fechaAcabado = Validador.fechaOpcional(request.get("fechaAcabado"), "fechaAcabado");
        LocalDateTime fechaVencimientoTmp = Validador.fechaOpcional(request.get("fechaVencimiento"), "fechaVencimiento");
        LocalDate fechaVencimiento = fechaVencimientoTmp != null ? fechaVencimientoTmp.toLocalDate() : null;
        String idDetalleLote = Validador.textoRequerido(request.get("idDetalleLote"), "idDetalleLote");
        String idEstadoExistencia = Validador.textoRequerido(request.get("idEstadoExistencia"), "idEstadoExistencia");
        String idUnidad = Validador.textoRequerido(request.get("idUnidad"), "idUnidad");

        return new CrearExistenciaRequest(contenidoInicial, contenidoActual, fechaAcabado, fechaVencimiento, idDetalleLote, idEstadoExistencia, idUnidad, false);
    }

    public BigDecimal getContenidoInicial() {return contenidoInicial;}
    public void setContenidoInicial(BigDecimal contenidoInicial) {this.contenidoInicial = contenidoInicial;}
    public BigDecimal getContenidoActual() {return contenidoActual;}
    public void setContenidoActual(BigDecimal contenidoActual) {this.contenidoActual = contenidoActual;}
    public LocalDateTime getFechaAcabado() {return fechaAcabado;}
    public void setFechaAcabado(LocalDateTime fechaAcabado) {this.fechaAcabado = fechaAcabado;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public void setFechaVencimiento(LocalDate fechaVencimiento) {this.fechaVencimiento = fechaVencimiento;}
    public String getIdDetalleLote() {return idDetalleLote;}
    public void setIdDetalleLote(String idDetalleLote) {this.idDetalleLote = idDetalleLote;}
    public String getIdEstadoExistencia() {return idEstadoExistencia;}
    public void setIdEstadoExistencia(String idEstadoExistencia) {this.idEstadoExistencia = idEstadoExistencia;}
    public String getIdUnidad() {return idUnidad;}
    public void setIdUnidad(String idUnidad) {this.idUnidad = idUnidad;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
