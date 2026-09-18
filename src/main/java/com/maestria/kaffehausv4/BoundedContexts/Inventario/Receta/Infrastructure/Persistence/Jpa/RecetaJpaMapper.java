package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;

/**
 * Mapea el agregado Receta. toDomain reconstruye la raiz junto con sus dos colecciones: no
 * existe una Receta de dominio a medias, sin ingredientes o sin pasos.
 */
public class RecetaJpaMapper {
    private RecetaJpaMapper() {
    }

    public static RecetaJpaModel toPersistence(Receta receta) {
        RecetaJpaModel model = new RecetaJpaModel();
        model.setId(receta.getId() != null ? UUID.fromString(receta.getId().getValue()) : UUID.randomUUID());
        model.setCantidad(receta.getCantidad());
        model.setTitulo(receta.getTitulo());
        model.setIdProductoObtenido(UUID.fromString(receta.getProductoObtenidoId().getValue()));
        model.setBorrado(receta.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Receta receta, RecetaJpaModel model) {
        model.setCantidad(receta.getCantidad());
        model.setTitulo(receta.getTitulo());
        model.setIdProductoObtenido(UUID.fromString(receta.getProductoObtenidoId().getValue()));
        model.setBorrado(receta.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Receta toDomain(
            RecetaJpaModel model,
            List<IngredienteRecetaJpaModel> ingredientesModel,
            List<PasoRecetaJpaModel> pasosModel) {

        List<IngredienteReceta> ingredientes = ingredientesModel != null
                ? ingredientesModel.stream().map(IngredienteRecetaJpaMapper::toDomain).collect(Collectors.toList())
                : Collections.emptyList();

        List<PasoReceta> pasos = pasosModel != null
                ? pasosModel.stream().map(PasoRecetaJpaMapper::toDomain).collect(Collectors.toList())
                : Collections.emptyList();

        return Receta.restaurar(
                model.getId().toString(),
                model.getCantidad(),
                model.getTitulo(),
                new ProductoId(model.getIdProductoObtenido().toString()),
                model.getBorrado() != null && model.getBorrado(),
                ingredientes,
                pasos);
    }
}
