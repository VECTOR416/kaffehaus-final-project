package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.util.List;

/**
 * Resultado de una salida de inventario: un movimiento por cada existencia o linea de lote que
 * toco (logica.md MOVIMIENTO-19, MOVIMIENTO-20). La salida no calcula dinero (GENERAL-11).
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
