package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects;

/**
 * Identidad de una linea de lote.
 *
 * Vive en el slice de Lote porque DetalleLote dejo de ser raiz de agregado. El VO sigue
 * siendo publico y estable porque Existencia referencia por el la linea de la que sale su
 * stock fisico, tal como exige la FK 'existencias.id_detalle_lote'.
 */
public class DetalleLoteId {
    private final String value;

    public DetalleLoteId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
