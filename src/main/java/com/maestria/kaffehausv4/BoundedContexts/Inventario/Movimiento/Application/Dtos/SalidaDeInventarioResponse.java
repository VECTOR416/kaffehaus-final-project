package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de una salida de inventario: los movimientos registrados y el costo de lo
 * consumido, que la produccion usa para costear su producto terminado.
 */
public class SalidaDeInventarioResponse {
    private BigDecimal costoTotal;
    private List<MovimientoResponse> movimientos;

    private SalidaDeInventarioResponse(BigDecimal costoTotal, List<MovimientoResponse> movimientos) {
        this.costoTotal = costoTotal;
        this.movimientos = movimientos;
    }

    public static SalidaDeInventarioResponse create(BigDecimal costoTotal, List<MovimientoResponse> movimientos) {
        return new SalidaDeInventarioResponse(costoTotal, movimientos);
    }

    public BigDecimal getCostoTotal() {return costoTotal;}
    public List<MovimientoResponse> getMovimientos() {return movimientos;}
}
