package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.util.List;

/**
 * Resultado de una salida de inventario: los movimientos registrados al consumir existencias.
 * Las unidades descontadas de lotes no generan movimiento (logica.md MOVIMIENTO-18), y la salida
 * no calcula dinero (GENERAL-11).
 */
public class SalidaDeInventarioResponse {
    private List<MovimientoResponse> movimientos;

    private SalidaDeInventarioResponse(List<MovimientoResponse> movimientos) {
        this.movimientos = movimientos;
    }

    public static SalidaDeInventarioResponse create(List<MovimientoResponse> movimientos) {
        return new SalidaDeInventarioResponse(movimientos);
    }

    public List<MovimientoResponse> getMovimientos() {return movimientos;}
}
