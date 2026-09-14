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

    private CrearDescuentoRequest(
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        boolean estado,
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

    public static CrearDescuentoRequest createFromRequest(Map<String, Object> request){
        BigDecimal descuento = BigDecimal.valueOf(Validador.numeroRequerido(request.get("descuento"), "descuento"));
        LocalDateTime fechaInicio = Validador.fechaRequerida(request.get("fechaInicio"), "fechaInicio");
        LocalDateTime fechaFinal = Validador.fechaRequerida(request.get("fechaFinal"), "fechaFinal");
        Boolean estadoTmp = Validador.booleanoOpcional(request.get("estado"), "estado");
        boolean estado = estadoTmp != null ? estadoTmp : true;
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");
        String idTipoDescuento = Validador.textoRequerido(request.get("idTipoDescuento"), "idTipoDescuento");

        return new CrearDescuentoRequest(descuento, fechaInicio, fechaFinal, estado, idProducto, idTipoDescuento);
    }

    public BigDecimal getDescuento() {return descuento;}
    public LocalDateTime getFechaInicio() {return fechaInicio;}
    public LocalDateTime getFechaFinal() {return fechaFinal;}
    public boolean isEstado() {return estado;}
    public String getIdProducto() {return idProducto;}
    public String getIdTipoDescuento() {return idTipoDescuento;}
}
