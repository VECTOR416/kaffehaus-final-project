package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;

public class DescuentoJpaMapper {
    private DescuentoJpaMapper() {
    }

    public static DescuentoJpaModel toPersistence(Descuento descuento) {
        DescuentoJpaModel model = new DescuentoJpaModel();
        model.setId(descuento.getId() != null ? UUID.fromString(descuento.getId().getValue()) : UUID.randomUUID());
        model.setDescuento(descuento.getDescuento());
        model.setFechaInicio(descuento.getFechaInicio());
        model.setFechaFinal(descuento.getFechaFinal());
        model.setEstado(descuento.isEstado());
        model.setIdProducto(UUID.fromString(descuento.getProductoId().getValue()));
        model.setIdTipoDescuento(UUID.fromString(descuento.getTipoDescuentoId().getValue()));
        model.setBorrado(descuento.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Descuento descuento, DescuentoJpaModel model) {
        model.setDescuento(descuento.getDescuento());
        model.setFechaInicio(descuento.getFechaInicio());
        model.setFechaFinal(descuento.getFechaFinal());
        model.setEstado(descuento.isEstado());
        model.setIdProducto(UUID.fromString(descuento.getProductoId().getValue()));
        model.setIdTipoDescuento(UUID.fromString(descuento.getTipoDescuentoId().getValue()));
        model.setBorrado(descuento.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Descuento toDomain(DescuentoJpaModel model) {
        return Descuento.restaurar(
                model.getId().toString(),
                model.getDescuento(),
                model.getFechaInicio(),
                model.getFechaFinal(),
                model.getEstado(),
                new ProductoId(model.getIdProducto().toString()),
                new TipoDescuentoId(model.getIdTipoDescuento().toString()),
                model.getBorrado() != null && model.getBorrado());
    }
}
