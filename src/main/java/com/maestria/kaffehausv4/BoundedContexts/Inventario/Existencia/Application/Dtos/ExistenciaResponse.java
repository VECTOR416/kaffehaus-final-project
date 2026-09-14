package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.EstadoExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.UnidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;

public class ExistenciaResponse {
    private String id;
    private BigDecimal contenidoInicial;
    private BigDecimal contenidoActual;
    private LocalDateTime fechaAcabado;
    private LocalDate fechaVencimiento;
    private String idDetalleLote;
    private EstadoExistenciaResponse estadoExistencia;
    private UnidadResponse unidad;
    private boolean borrado;

    private ExistenciaResponse(
        String id,
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        String idDetalleLote,
        EstadoExistenciaResponse estadoExistencia,
        UnidadResponse unidad,
        boolean borrado
    ) {
        this.id = id;
        this.contenidoInicial = contenidoInicial;
        this.contenidoActual = contenidoActual;
        this.fechaAcabado = fechaAcabado;
        this.fechaVencimiento = fechaVencimiento;
        this.idDetalleLote = idDetalleLote;
        this.estadoExistencia = estadoExistencia;
        this.unidad = unidad;
        this.borrado = borrado;
    }

    public static ExistenciaResponse createFromAggregate(Existencia existencia, EstadoExistencia estadoExistencia, Unidad unidad){
        EstadoExistenciaResponse estadoExistenciaResponse = estadoExistencia != null ? EstadoExistenciaResponse.createFromAggregate(estadoExistencia) : null;
        UnidadResponse unidadResponse = unidad != null ? UnidadResponse.createFromAggregate(unidad) : null;
        return new ExistenciaResponse(
            existencia.getId().getValue(),
            existencia.getContenidoInicial(),
            existencia.getContenidoActual(),
            existencia.getFechaAcabado(),
            existencia.getFechaVencimiento(),
            existencia.getDetalleLoteId() != null ? existencia.getDetalleLoteId().getValue() : null,
            estadoExistenciaResponse,
            unidadResponse,
            existencia.isBorrado()
        );
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
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
    public EstadoExistenciaResponse getEstadoExistencia() {return estadoExistencia;}
    public void setEstadoExistencia(EstadoExistenciaResponse estadoExistencia) {this.estadoExistencia = estadoExistencia;}
    public UnidadResponse getUnidad() {return unidad;}
    public void setUnidad(UnidadResponse unidad) {this.unidad = unidad;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
