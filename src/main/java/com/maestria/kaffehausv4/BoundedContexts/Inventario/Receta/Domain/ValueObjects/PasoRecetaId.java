package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects;

/**
 * Identidad de un paso dentro del agregado Receta.
 *
 * Vive en el slice de Receta porque PasoReceta dejo de ser raiz de agregado: su ciclo de
 * vida pertenece por completo a la receta que lo contiene.
 */
public class PasoRecetaId {
    private final String value;

    public PasoRecetaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
