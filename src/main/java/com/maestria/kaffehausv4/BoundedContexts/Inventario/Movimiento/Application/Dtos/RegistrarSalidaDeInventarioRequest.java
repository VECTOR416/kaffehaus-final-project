package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrada de RegistrarSalidaDeInventarioUseCase.
 *
 * No viaja por HTTP: la arman otros casos de uso (produccion y venta) con datos ya validados,
 * por eso su factoria es crear(...) y no createFromRequest(Map).
 *
 * Lleva dos tipos de movimiento porque la salida puede tomar dos caminos: el de un producto
 * de stock directo, que sale de sus propias existencias ('Ajuste' en una venta), y el de los
 * insumos que se descuentan al explotar la receta de un producto bajo demanda ('Receta'). En
 * una produccion ambos son 'Receta'. 'idProduccion' solo se informa en los consumos de una
 * produccion.
 */
public class RegistrarSalidaDeInventarioRequest {
    private String idProducto;
    private BigDecimal cantidad;
    private String nombreTipoMovimientoDirecto;
    private String nombreTipoMovimientoPorReceta;
    private String idUsuario;
    private String idProduccion;
    private String motivo;
    private LocalDateTime fecha;

    private RegistrarSalidaDeInventarioRequest(
        String idProducto,
        BigDecimal cantidad,
        String nombreTipoMovimientoDirecto,
        String nombreTipoMovimientoPorReceta,
        String idUsuario,
        String idProduccion,
        String motivo,
        LocalDateTime fecha
    ) {
        this.idProducto = idProducto;
        this.cantidad = cantidad;
        this.nombreTipoMovimientoDirecto = nombreTipoMovimientoDirecto;
        this.nombreTipoMovimientoPorReceta = nombreTipoMovimientoPorReceta;
        this.idUsuario = idUsuario;
        this.idProduccion = idProduccion;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    public static RegistrarSalidaDeInventarioRequest crear(
        String idProducto,
        BigDecimal cantidad,
        String nombreTipoMovimientoDirecto,
        String nombreTipoMovimientoPorReceta,
        String idUsuario,
        String idProduccion,
        String motivo,
        LocalDateTime fecha
    ) {
        return new RegistrarSalidaDeInventarioRequest(
                idProducto,
                cantidad,
                nombreTipoMovimientoDirecto,
                nombreTipoMovimientoPorReceta,
                idUsuario,
                idProduccion,
                motivo,
                fecha != null ? fecha : LocalDateTime.now());
    }

    public String getIdProducto() {return idProducto;}
    public BigDecimal getCantidad() {return cantidad;}
    public String getNombreTipoMovimientoDirecto() {return nombreTipoMovimientoDirecto;}
    public String getNombreTipoMovimientoPorReceta() {return nombreTipoMovimientoPorReceta;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdProduccion() {return idProduccion;}
    public String getMotivo() {return motivo;}
    public LocalDateTime getFecha() {return fecha;}
}
