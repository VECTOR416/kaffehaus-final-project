package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearDescuentoRequest {
    private BigDecimal descuento;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFinal;
    private boolean estado;
    private String idProducto;
    private String idTipoDescuento;
    private boolean borrado;

    private CrearDescuentoRequest(
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        boolean estado,
        String idProducto,
        String idTipoDescuento,
        boolean borrado
    ) {
        this.descuento = descuento;
        this.fechaInicio = fechaInicio;
        this.fechaFinal = fechaFinal;
        this.estado = estado;
        this.idProducto = idProducto;
        this.idTipoDescuento = idTipoDescuento;
        this.borrado = borrado;
    }

    public static CrearDescuentoRequest createFromRequest(Map<String, Object> request){
        BigDecimal descuento = BigDecimal.valueOf(Validador.numeroRequerido(request.get("descuento"), "descuento"));
        LocalDateTime fechaInicio = Validador.fechaRequerida(request.get("fechaInicio"), "fechaInicio");
        LocalDateTime fechaFinal = Validador.fechaRequerida(request.get("fechaFinal"), "fechaFinal");
        Boolean estadoTmp = Validador.booleanoOpcional(request.get("estado"), "estado");
        boolean estado = estadoTmp != null ? estadoTmp : true;
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");
        String idTipoDescuento = Validador.textoRequerido(request.get("idTipoDescuento"), "idTipoDescuento");

        return new CrearDescuentoRequest(descuento, fechaInicio, fechaFinal, estado, idProducto, idTipoDescuento, false);
    }

    public BigDecimal getDescuento() {return descuento;}
    public void setDescuento(BigDecimal descuento) {this.descuento = descuento;}
    public LocalDateTime getFechaInicio() {return fechaInicio;}
    public void setFechaInicio(LocalDateTime fechaInicio) {this.fechaInicio = fechaInicio;}
    public LocalDateTime getFechaFinal() {return fechaFinal;}
    public void setFechaFinal(LocalDateTime fechaFinal) {this.fechaFinal = fechaFinal;}
    public boolean isEstado() {return estado;}
    public void setEstado(boolean estado) {this.estado = estado;}
    public String getIdProducto() {return idProducto;}
    public void setIdProducto(String idProducto) {this.idProducto = idProducto;}
    public String getIdTipoDescuento() {return idTipoDescuento;}
    public void setIdTipoDescuento(String idTipoDescuento) {this.idTipoDescuento = idTipoDescuento;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
