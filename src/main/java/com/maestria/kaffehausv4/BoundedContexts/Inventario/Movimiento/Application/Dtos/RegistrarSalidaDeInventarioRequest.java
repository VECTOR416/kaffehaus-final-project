package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;

/**
 * Entrada de RegistrarSalidaDeInventarioUseCase.
 *
 * No viaja por HTTP: la arman otros casos de uso (produccion y venta) con datos ya validados,
 * por eso sus factorias son deProduccion(...) y deVenta(...) y no createFromRequest(Map).
 *
 * Lleva todas las lineas de la operacion de una vez, para que la salida arme un solo plan de
 * consumo (logica.md GENERAL-14, MOVIMIENTO-19). El tipo de movimiento y el documento dependen
 * de quien descuenta: 'Produccion' con 'idProduccion', o 'Venta' con 'idVenta'.
 */
public class RegistrarSalidaDeInventarioRequest {
    private final List<Linea> lineas;
    private final String tipoMovimiento;
    private final String idUsuario;
    private final String idProduccion;
    private final String idVenta;
    private final String motivo;
    private final LocalDateTime fecha;

    private RegistrarSalidaDeInventarioRequest(
        String tipoMovimiento,
        String idUsuario,
        String idProduccion,
        String idVenta,
        String motivo,
        LocalDateTime fecha
    ) {
        this.lineas = new ArrayList<>();
        this.tipoMovimiento = tipoMovimiento;
        this.idUsuario = idUsuario;
        this.idProduccion = idProduccion;
        this.idVenta = idVenta;
        this.motivo = motivo;
        this.fecha = fecha != null ? fecha : LocalDateTime.now();
    }

    /** Consumo de los insumos de una produccion. */
    public static RegistrarSalidaDeInventarioRequest deProduccion(
        String idUsuario,
        String idProduccion,
        String motivo,
        LocalDateTime fecha
    ) {
        return new RegistrarSalidaDeInventarioRequest(TipoMovimiento.PRODUCCION, idUsuario, idProduccion, null, motivo, fecha);
    }

    /** Lo que descuenta una venta: sus productos por unidades y los insumos de sus MTO (VENTA-24). */
    public static RegistrarSalidaDeInventarioRequest deVenta(
        String idUsuario,
        String idVenta,
        String motivo,
        LocalDateTime fecha
    ) {
        return new RegistrarSalidaDeInventarioRequest(TipoMovimiento.VENTA, idUsuario, null, idVenta, motivo, fecha);
    }

    /** Agrega una linea: un producto y la cantidad que se descuenta de el. */
    public RegistrarSalidaDeInventarioRequest con(String idProducto, BigDecimal cantidad) {
        this.lineas.add(new Linea(idProducto, cantidad));
        return this;
    }

    public List<Linea> getLineas() {return Collections.unmodifiableList(lineas);}
    public String getTipoMovimiento() {return tipoMovimiento;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdProduccion() {return idProduccion;}
    public String getIdVenta() {return idVenta;}
    public String getMotivo() {return motivo;}
    public LocalDateTime getFecha() {return fecha;}

    /** Un producto y la cantidad que se descuenta de el, en su unidad actual. */
    public static final class Linea {
        private final String idProducto;
        private final BigDecimal cantidad;

        private Linea(String idProducto, BigDecimal cantidad) {
            this.idProducto = idProducto;
            this.cantidad = cantidad;
        }

        public String getIdProducto() {return idProducto;}
        public BigDecimal getCantidad() {return cantidad;}
    }
}
