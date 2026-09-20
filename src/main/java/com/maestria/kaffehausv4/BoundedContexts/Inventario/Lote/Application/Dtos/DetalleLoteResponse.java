package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;

/**
 * Linea de lote en la respuesta. Siempre viaja anidada dentro de LoteResponse, por eso no
 * repite el idLote: el identificador del lote ya esta en el nivel superior.
 */
public class DetalleLoteResponse {
    private String id;
    private String idProducto;
    private BigDecimal cantidadInicial;
    private BigDecimal cantidadRestante;
    private BigDecimal precioUnitario;
    private boolean fraccionable;
    private LocalDate fechaVencimiento;
    private boolean borrado;

    private DetalleLoteResponse(
        String id,
        String idProducto,
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        boolean fraccionable,
        LocalDate fechaVencimiento,
        boolean borrado
    ) {
        this.id = id;
        this.idProducto = idProducto;
        this.cantidadInicial = cantidadInicial;
        this.cantidadRestante = cantidadRestante;
        this.precioUnitario = precioUnitario;
        this.fraccionable = fraccionable;
        this.fechaVencimiento = fechaVencimiento;
        this.borrado = borrado;
    }

    public static DetalleLoteResponse createFromAggregate(DetalleLote detalleLote){
        return new DetalleLoteResponse(
            detalleLote.getId() != null ? detalleLote.getId().getValue() : null,
            detalleLote.getProductoId() != null ? detalleLote.getProductoId().getValue() : null,
            detalleLote.getCantidadInicial(),
            detalleLote.getCantidadRestante(),
            detalleLote.getPrecioUnitario(),
            detalleLote.isFraccionable(),
            detalleLote.getFechaVencimiento(),
            detalleLote.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getIdProducto() {return idProducto;}
    public BigDecimal getCantidadInicial() {return cantidadInicial;}
    public BigDecimal getCantidadRestante() {return cantidadRestante;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public boolean isFraccionable() {return fraccionable;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public boolean isBorrado() {return borrado;}
}
