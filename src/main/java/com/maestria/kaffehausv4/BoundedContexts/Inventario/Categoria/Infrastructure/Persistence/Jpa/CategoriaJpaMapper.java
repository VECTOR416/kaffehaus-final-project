package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;

public class CategoriaJpaMapper {
    private CategoriaJpaMapper() {
    }

    public static CategoriaJpaModel toPersistence(Categoria categoria) {
        CategoriaJpaModel model = new CategoriaJpaModel();
        model.setId(categoria.getId() != null ? UUID.fromString(categoria.getId().getValue()) : UUID.randomUUID());
        model.setNombre(categoria.getNombre());
        model.setEsBorrable(categoria.isEsBorrable());
        model.setBorrado(categoria.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Categoria categoria, CategoriaJpaModel model) {
        model.setNombre(categoria.getNombre());
        model.setEsBorrable(categoria.isEsBorrable());
        model.setBorrado(categoria.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Categoria toDomain(CategoriaJpaModel model) {
        return Categoria.restaurar(
                model.getId().toString(),
                model.getNombre(),
                model.getEsBorrable() != null && model.getEsBorrable(),
                model.getBorrado() != null && model.getBorrado());
    }
}
