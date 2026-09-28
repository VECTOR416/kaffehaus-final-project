package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;

/**
 * Una fila de la consulta de aperturables (inventario.md INV-13): por cada presentacion, el
 * detalle que toca abrir, cuantas unidades cerradas quedan y que contenido dara al abrirse.
 * 'contenidoPorUnidad' y 'costoPorUnidadDeContenido' son nulos si no hay conversion posible.
 */
public class DetalleAperturableResponse {
    private String idLote;
    private String idDetalleLote;
    private String idProducto;
    private String nombreProducto;
    private BigDecimal cantidadRestante;
    private BigDecimal unidadesCerradasTotales;
    private LocalDate fechaVencimiento;
    private BigDecimal contenidoPorUnidad;
    private String idUnidadBase;
    private BigDecimal costoPorUnidadDeContenido;

    private DetalleAperturableResponse(
        String idLote,
        String idDetalleLote,
        String idProducto,
        String nombreProducto,
        BigDecimal cantidadRestante,
        BigDecimal unidadesCerradasTotales,
        LocalDate fechaVencimiento,
        BigDecimal contenidoPorUnidad,
        String idUnidadBase,
        BigDecimal costoPorUnidadDeContenido
    ) {
        this.idLote = idLote;
        this.idDetalleLote = idDetalleLote;
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidadRestante = cantidadRestante;
        this.unidadesCerradasTotales = unidadesCerradasTotales;
        this.fechaVencimiento = fechaVencimiento;
        this.contenidoPorUnidad = contenidoPorUnidad;
        this.idUnidadBase = idUnidadBase;
        this.costoPorUnidadDeContenido = costoPorUnidadDeContenido;
    }

    public static DetalleAperturableResponse createFromAggregate(
            Lote lote,
            DetalleLote detalle,
            Producto presentacion,
            Producto base,
            BigDecimal unidadesCerradasTotales,
            BigDecimal contenidoPorUnidad,
            BigDecimal costoPorUnidadDeContenido) {
        return new DetalleAperturableResponse(
                lote.getId().getValue(),
                detalle.getId().getValue(),
                presentacion.getId().getValue(),
                presentacion.getNombre(),
                detalle.getCantidadRestante(),
                unidadesCerradasTotales,
                detalle.getFechaVencimiento(),
                contenidoPorUnidad,
                base.getUnidadActualId().getValue(),
                costoPorUnidadDeContenido);
    }

    public String getIdLote() {return idLote;}
    public String getIdDetalleLote() {return idDetalleLote;}
    public String getIdProducto() {return idProducto;}
    public String getNombreProducto() {return nombreProducto;}
    public BigDecimal getCantidadRestante() {return cantidadRestante;}
    public BigDecimal getUnidadesCerradasTotales() {return unidadesCerradasTotales;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public BigDecimal getContenidoPorUnidad() {return contenidoPorUnidad;}
    public String getIdUnidadBase() {return idUnidadBase;}
    public BigDecimal getCostoPorUnidadDeContenido() {return costoPorUnidadDeContenido;}
}
