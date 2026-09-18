package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects;

/**
 * Identidad de una linea de venta.
 *
 * Vive en el slice de Venta y no en uno propio porque DetalleVenta dejo de ser raiz de
 * agregado: su ciclo de vida pertenece por completo a Venta.
 */
public class DetalleVentaId {
    private final String value;

    public DetalleVentaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
