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
    private LocalDateTime fechaApertura;
    private String idDetalleLote;
    private EstadoExistenciaResponse estadoExistencia;
    private UnidadResponse unidad;
    private String idUsuario;
    private boolean borrado;

    private ExistenciaResponse(
        String id,
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        LocalDateTime fechaApertura,
        String idDetalleLote,
        EstadoExistenciaResponse estadoExistencia,
        UnidadResponse unidad,
        String idUsuario,
        boolean borrado
    ) {
        this.id = id;
        this.contenidoInicial = contenidoInicial;
        this.contenidoActual = contenidoActual;
        this.fechaAcabado = fechaAcabado;
        this.fechaVencimiento = fechaVencimiento;
        this.fechaApertura = fechaApertura;
        this.idDetalleLote = idDetalleLote;
        this.estadoExistencia = estadoExistencia;
        this.unidad = unidad;
        this.idUsuario = idUsuario;
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
            existencia.getFechaApertura(),
            existencia.getDetalleLoteId() != null ? existencia.getDetalleLoteId().getValue() : null,
            estadoExistenciaResponse,
            unidadResponse,
            existencia.getUsuarioId() != null ? existencia.getUsuarioId().getValue() : null,
            existencia.isBorrado()
        );
    }

    public String getId() {return id;}
    public BigDecimal getContenidoInicial() {return contenidoInicial;}
    public BigDecimal getContenidoActual() {return contenidoActual;}
    public LocalDateTime getFechaAcabado() {return fechaAcabado;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public LocalDateTime getFechaApertura() {return fechaApertura;}
    public String getIdDetalleLote() {return idDetalleLote;}
    public EstadoExistenciaResponse getEstadoExistencia() {return estadoExistencia;}
    public UnidadResponse getUnidad() {return unidad;}
    public String getIdUsuario() {return idUsuario;}
    public boolean isBorrado() {return borrado;}
}
