package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;

/**
 * Mapea los ingredientes del agregado Receta.
 *
 * toPersistence recibe el id de la receta por parametro: la entidad ya no guarda la referencia
 * al padre, porque dentro del agregado la relacion es la contencion. La columna
 * 'ingredientes_receta.id_receta' se completa aqui, a partir de la raiz.
 */
public class IngredienteRecetaJpaMapper {
    private IngredienteRecetaJpaMapper() {
    }

    public static IngredienteRecetaJpaModel toPersistence(IngredienteReceta ingredienteReceta, UUID idReceta) {
        IngredienteRecetaJpaModel model = new IngredienteRecetaJpaModel();
        model.setId(ingredienteReceta.getId() != null ? UUID.fromString(ingredienteReceta.getId().getValue()) : UUID.randomUUID());
        model.setCantidad(ingredienteReceta.getCantidad());
        model.setIdReceta(idReceta);
        model.setIdProducto(UUID.fromString(ingredienteReceta.getProductoId().getValue()));
        model.setBorrado(ingredienteReceta.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(IngredienteReceta ingredienteReceta, IngredienteRecetaJpaModel model) {
        model.setCantidad(ingredienteReceta.getCantidad());
        model.setBorrado(ingredienteReceta.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static IngredienteReceta toDomain(IngredienteRecetaJpaModel model) {
        return IngredienteReceta.restaurar(
                model.getId().toString(),
                new ProductoId(model.getIdProducto().toString()),
                model.getCantidad(),
                model.getBorrado() != null && model.getBorrado());
    }
}
