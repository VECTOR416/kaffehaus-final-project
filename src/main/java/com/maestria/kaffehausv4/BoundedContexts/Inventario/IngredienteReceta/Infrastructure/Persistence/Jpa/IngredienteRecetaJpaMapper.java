package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;

public class IngredienteRecetaJpaMapper {
    private IngredienteRecetaJpaMapper() {
    }

    public static IngredienteRecetaJpaModel toPersistence(IngredienteReceta ingredienteReceta) {
        IngredienteRecetaJpaModel model = new IngredienteRecetaJpaModel();
        model.setId(ingredienteReceta.getId() != null ? UUID.fromString(ingredienteReceta.getId().getValue()) : UUID.randomUUID());
        model.setCantidad(ingredienteReceta.getCantidad());
        model.setIdReceta(UUID.fromString(ingredienteReceta.getRecetaId().getValue()));
        model.setIdProducto(UUID.fromString(ingredienteReceta.getProductoId().getValue()));
        model.setBorrado(ingredienteReceta.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(IngredienteReceta ingredienteReceta, IngredienteRecetaJpaModel model) {
        model.setCantidad(ingredienteReceta.getCantidad());
        model.setIdReceta(UUID.fromString(ingredienteReceta.getRecetaId().getValue()));
        model.setIdProducto(UUID.fromString(ingredienteReceta.getProductoId().getValue()));
        model.setBorrado(ingredienteReceta.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static IngredienteReceta toDomain(IngredienteRecetaJpaModel model) {
        return IngredienteReceta.restaurar(
                model.getId().toString(),
                model.getCantidad(),
                new RecetaId(model.getIdReceta().toString()),
                new ProductoId(model.getIdProducto().toString()),
                model.getBorrado() != null && model.getBorrado());
    }
}
