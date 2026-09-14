package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarDescuentoRequest {
    private BigDecimal descuento;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFinal;
    private Boolean estado;
    private String idProducto;
    private String idTipoDescuento;

    private ActualizarDescuentoRequest(
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        Boolean estado,
        String idProducto,
        String idTipoDescuento
    ) {
        this.descuento = descuento;
        this.fechaInicio = fechaInicio;
        this.fechaFinal = fechaFinal;
        this.estado = estado;
        this.idProducto = idProducto;
        this.idTipoDescuento = idTipoDescuento;
    }

    public static ActualizarDescuentoRequest createFromRequest(Map<String, Object> request){
        Double descuentoTmp = Validador.numeroOpcional(request.get("descuento"), "descuento");
        BigDecimal descuento = descuentoTmp != null ? BigDecimal.valueOf(descuentoTmp) : null;
        LocalDateTime fechaInicio = Validador.fechaOpcional(request.get("fechaInicio"), "fechaInicio");
        LocalDateTime fechaFinal = Validador.fechaOpcional(request.get("fechaFinal"), "fechaFinal");
        Boolean estadoTmp = Validador.booleanoOpcional(request.get("estado"), "estado");
        Boolean estado = estadoTmp;
        String idProducto = Validador.textoOpcional(request.get("idProducto"), "idProducto");
        String idTipoDescuento = Validador.textoOpcional(request.get("idTipoDescuento"), "idTipoDescuento");

        return new ActualizarDescuentoRequest(descuento, fechaInicio, fechaFinal, estado, idProducto, idTipoDescuento);
    }

    public BigDecimal getDescuento() {return descuento;}
    public void setDescuento(BigDecimal descuento) {this.descuento = descuento;}
    public LocalDateTime getFechaInicio() {return fechaInicio;}
    public void setFechaInicio(LocalDateTime fechaInicio) {this.fechaInicio = fechaInicio;}
    public LocalDateTime getFechaFinal() {return fechaFinal;}
    public void setFechaFinal(LocalDateTime fechaFinal) {this.fechaFinal = fechaFinal;}
    public Boolean getEstado() {return estado;}
    public void setEstado(Boolean estado) {this.estado = estado;}
    public String getIdProducto() {return idProducto;}
    public void setIdProducto(String idProducto) {this.idProducto = idProducto;}
    public String getIdTipoDescuento() {return idTipoDescuento;}
    public void setIdTipoDescuento(String idTipoDescuento) {this.idTipoDescuento = idTipoDescuento;}
}
