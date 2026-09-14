package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;

public class ProductoJpaMapper {
    private ProductoJpaMapper() {
    }

    public static ProductoJpaModel toPersistence(Producto producto) {
        ProductoJpaModel model = new ProductoJpaModel();
        model.setId(producto.getId() != null ? UUID.fromString(producto.getId().getValue()) : UUID.randomUUID());
        model.setNombre(producto.getNombre());
        model.setStock(producto.getStock());
        model.setPrecio(producto.getPrecio());
        model.setStockMinimo(producto.getStockMinimo());
        model.setVendible(producto.isVendible());
        model.setUnidadActual(UUID.fromString(producto.getUnidadActualId().getValue()));
        model.setIdCategoria(UUID.fromString(producto.getCategoriaId().getValue()));
        model.setIdProductoPadre(producto.getProductoPadreId() != null ? UUID.fromString(producto.getProductoPadreId().getValue()) : null);
        model.setBorrado(producto.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Producto producto, ProductoJpaModel model) {
        model.setNombre(producto.getNombre());
        model.setStock(producto.getStock());
        model.setPrecio(producto.getPrecio());
        model.setStockMinimo(producto.getStockMinimo());
        model.setVendible(producto.isVendible());
        model.setUnidadActual(UUID.fromString(producto.getUnidadActualId().getValue()));
        model.setIdCategoria(UUID.fromString(producto.getCategoriaId().getValue()));
        model.setIdProductoPadre(producto.getProductoPadreId() != null ? UUID.fromString(producto.getProductoPadreId().getValue()) : null);
        model.setBorrado(producto.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Producto toDomain(ProductoJpaModel model) {
        return Producto.restaurar(
                model.getId().toString(),
                model.getNombre(),
                model.getStock(),
                model.getPrecio(),
                model.getStockMinimo(),
                model.getVendible(),
                new UnidadId(model.getUnidadActual().toString()),
                new CategoriaId(model.getIdCategoria().toString()),
                model.getIdProductoPadre() != null ? new ProductoId(model.getIdProductoPadre().toString()) : null,
                model.getBorrado() != null && model.getBorrado());
    }
}
