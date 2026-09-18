package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects;

/**
 * Identidad de un ingrediente dentro del agregado Receta.
 *
 * Vive en el slice de Receta porque IngredienteReceta dejo de ser raiz de agregado: su ciclo de
 * vida pertenece por completo a la receta que lo contiene.
 */
public class IngredienteRecetaId {
    private final String value;

    public IngredienteRecetaId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
