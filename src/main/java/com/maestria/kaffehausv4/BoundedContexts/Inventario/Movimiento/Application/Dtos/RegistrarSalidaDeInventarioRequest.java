package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrada de RegistrarSalidaDeInventarioUseCase.
 *
 * No viaja por HTTP: la arman otros casos de uso (produccion y venta) con datos ya validados,
 * por eso su factoria es crear(...) y no createFromRequest(Map).
 *
 * 'explotarBajoDemanda' distingue a quien descuenta: la produccion explota la receta de un insumo
 * bajo demanda (logica.md MOVIMIENTO-18); la venta de un producto bajo demanda no descuenta nada
 * (VENTA-23). 'idProduccion' solo se informa en los consumos de una produccion.
 */
public class RegistrarSalidaDeInventarioRequest {
    private String idProducto;
    private BigDecimal cantidad;
    private boolean explotarBajoDemanda;
    private String idUsuario;
    private String idProduccion;
    private String motivo;
    private LocalDateTime fecha;

    private RegistrarSalidaDeInventarioRequest(
        String idProducto,
        BigDecimal cantidad,
        boolean explotarBajoDemanda,
        String idUsuario,
        String idProduccion,
        String motivo,
        LocalDateTime fecha
    ) {
        this.idProducto = idProducto;
        this.cantidad = cantidad;
        this.explotarBajoDemanda = explotarBajoDemanda;
        this.idUsuario = idUsuario;
        this.idProduccion = idProduccion;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    /** Consumo de un insumo de una produccion: explota las recetas de los insumos bajo demanda. */
    public static RegistrarSalidaDeInventarioRequest deProduccion(
        String idProducto,
        BigDecimal cantidad,
        String idUsuario,
        String idProduccion,
        String motivo,
        LocalDateTime fecha
    ) {
        return new RegistrarSalidaDeInventarioRequest(
                idProducto, cantidad, true, idUsuario, idProduccion, motivo, fecha != null ? fecha : LocalDateTime.now());
    }

    /**
     * Salida de una linea de venta: un producto bajo demanda no descuenta nada y uno por unidades no
     * registra movimiento, asi que no lleva motivo.
     */
    public static RegistrarSalidaDeInventarioRequest deVenta(
        String idProducto,
        BigDecimal cantidad,
        String idUsuario,
        LocalDateTime fecha
    ) {
        return new RegistrarSalidaDeInventarioRequest(
                idProducto, cantidad, false, idUsuario, null, null, fecha != null ? fecha : LocalDateTime.now());
    }

    public String getIdProducto() {return idProducto;}
    public BigDecimal getCantidad() {return cantidad;}
    public boolean isExplotarBajoDemanda() {return explotarBajoDemanda;}
    public String getIdUsuario() {return idUsuario;}
    public String getIdProduccion() {return idProduccion;}
    public String getMotivo() {return motivo;}
    public LocalDateTime getFecha() {return fecha;}
}
