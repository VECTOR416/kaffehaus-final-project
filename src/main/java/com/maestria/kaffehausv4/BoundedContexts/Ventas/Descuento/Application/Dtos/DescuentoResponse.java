package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.TipoDescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;

public class DescuentoResponse {
    private String id;
    private BigDecimal descuento;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFinal;
    private boolean estado;
    private String idProducto;
    private TipoDescuentoResponse tipoDescuento;
    private boolean borrado;

    private DescuentoResponse(
        String id,
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        boolean estado,
        String idProducto,
        TipoDescuentoResponse tipoDescuento,
        boolean borrado
    ) {
        this.id = id;
        this.descuento = descuento;
        this.fechaInicio = fechaInicio;
        this.fechaFinal = fechaFinal;
        this.estado = estado;
        this.idProducto = idProducto;
        this.tipoDescuento = tipoDescuento;
        this.borrado = borrado;
    }

    public static DescuentoResponse createFromAggregate(Descuento descuento, TipoDescuento tipoDescuento){
        TipoDescuentoResponse tipoDescuentoResponse = tipoDescuento != null ? TipoDescuentoResponse.createFromAggregate(tipoDescuento) : null;
        return new DescuentoResponse(
            descuento.getId().getValue(),
            descuento.getDescuento(),
            descuento.getFechaInicio(),
            descuento.getFechaFinal(),
            descuento.isEstado(),
            descuento.getProductoId() != null ? descuento.getProductoId().getValue() : null,
            tipoDescuentoResponse,
            descuento.isBorrado()
        );
    }

    public String getId() {return id;}
    public BigDecimal getDescuento() {return descuento;}
    public LocalDateTime getFechaInicio() {return fechaInicio;}
    public LocalDateTime getFechaFinal() {return fechaFinal;}
    public boolean isEstado() {return estado;}
    public String getIdProducto() {return idProducto;}
    public TipoDescuentoResponse getTipoDescuento() {return tipoDescuento;}
    public boolean isBorrado() {return borrado;}
}
