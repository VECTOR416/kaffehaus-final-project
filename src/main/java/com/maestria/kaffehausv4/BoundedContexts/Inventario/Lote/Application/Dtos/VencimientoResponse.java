package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;

/**
 * Una fila de la alerta de vencimiento (logica.md VENCIMIENTO-04): una linea de lote con unidades
 * o una existencia con contenido, vencida o por vencer.
 *
 * - 'tipo': 'Linea' o 'Existencia'. 'id' es el de esa linea o existencia, que es lo que se envia
 *   para darla de baja (VENCIMIENTO-03).
 * - 'idProducto' y 'producto': el producto de la linea, o el producto base de la existencia.
 * - 'cantidad' en 'idUnidad': unidades disponibles de la linea o contenido de la existencia.
 */
public class VencimientoResponse {
    public static final String LINEA = "Linea";
    public static final String EXISTENCIA = "Existencia";

    private String tipo;
    private String id;
    private String idLote;
    private String idDetalleLote;
    private String idProducto;
    private String producto;
    private BigDecimal cantidad;
    private String idUnidad;
    private LocalDate fechaVencimiento;
    private boolean vencido;

    private VencimientoResponse(
        String tipo,
        String id,
        String idLote,
        String idDetalleLote,
        String idProducto,
        String producto,
        BigDecimal cantidad,
        String idUnidad,
        LocalDate fechaVencimiento,
        boolean vencido
    ) {
        this.tipo = tipo;
        this.id = id;
        this.idLote = idLote;
        this.idDetalleLote = idDetalleLote;
        this.idProducto = idProducto;
        this.producto = producto;
        this.cantidad = cantidad;
        this.idUnidad = idUnidad;
        this.fechaVencimiento = fechaVencimiento;
        this.vencido = vencido;
    }

    /** 'producto' puede ser nulo si se dio de baja: la fila se muestra igual, sin nombre. */
    public static VencimientoResponse deLinea(Lote lote, DetalleLote detalle, Producto producto, LocalDate hoy) {
        return new VencimientoResponse(
                LINEA,
                detalle.getId().getValue(),
                lote.getId().getValue(),
                detalle.getId().getValue(),
                detalle.getProductoId().getValue(),
                producto != null ? producto.getNombre() : null,
                detalle.getCantidadRestante(),
                producto != null ? producto.getUnidadActualId().getValue() : null,
                detalle.getFechaVencimiento(),
                detalle.estaVencido(hoy));
    }

    /** 'base' es el producto base de la existencia; puede ser nulo si se dio de baja. */
    public static VencimientoResponse deExistencia(Existencia existencia, LoteId loteId, Producto base, LocalDate hoy) {
        return new VencimientoResponse(
                EXISTENCIA,
                existencia.getId().getValue(),
                loteId != null ? loteId.getValue() : null,
                existencia.getDetalleLoteId().getValue(),
                base != null ? base.getId().getValue() : null,
                base != null ? base.getNombre() : null,
                existencia.getContenidoActual(),
                existencia.getUnidadId().getValue(),
                existencia.getFechaVencimiento(),
                existencia.estaVencida(hoy));
    }

    public String getTipo() {return tipo;}
    public String getId() {return id;}
    public String getIdLote() {return idLote;}
    public String getIdDetalleLote() {return idDetalleLote;}
    public String getIdProducto() {return idProducto;}
    public String getProducto() {return producto;}
    public BigDecimal getCantidad() {return cantidad;}
    public String getIdUnidad() {return idUnidad;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public boolean isVencido() {return vencido;}
}
