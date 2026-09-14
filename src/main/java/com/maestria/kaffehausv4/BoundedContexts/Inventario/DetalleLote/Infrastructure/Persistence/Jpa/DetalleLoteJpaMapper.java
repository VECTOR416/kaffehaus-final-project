package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;

public class DetalleLoteJpaMapper {
    private DetalleLoteJpaMapper() {
    }

    public static DetalleLoteJpaModel toPersistence(DetalleLote detalleLote) {
        DetalleLoteJpaModel model = new DetalleLoteJpaModel();
        model.setId(detalleLote.getId() != null ? UUID.fromString(detalleLote.getId().getValue()) : UUID.randomUUID());
        model.setCantidadInicial(detalleLote.getCantidadInicial());
        model.setCantidadRestante(detalleLote.getCantidadRestante());
        model.setPrecioUnitario(detalleLote.getPrecioUnitario());
        model.setFraccionable(detalleLote.isFraccionable());
        model.setIdLote(UUID.fromString(detalleLote.getLoteId().getValue()));
        model.setIdProducto(UUID.fromString(detalleLote.getProductoId().getValue()));
        model.setBorrado(detalleLote.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(DetalleLote detalleLote, DetalleLoteJpaModel model) {
        model.setCantidadInicial(detalleLote.getCantidadInicial());
        model.setCantidadRestante(detalleLote.getCantidadRestante());
        model.setPrecioUnitario(detalleLote.getPrecioUnitario());
        model.setFraccionable(detalleLote.isFraccionable());
        model.setIdLote(UUID.fromString(detalleLote.getLoteId().getValue()));
        model.setIdProducto(UUID.fromString(detalleLote.getProductoId().getValue()));
        model.setBorrado(detalleLote.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static DetalleLote toDomain(DetalleLoteJpaModel model) {
        return DetalleLote.restaurar(
                model.getId().toString(),
                model.getCantidadInicial(),
                model.getCantidadRestante(),
                model.getPrecioUnitario(),
                model.getFraccionable(),
                new LoteId(model.getIdLote().toString()),
                new ProductoId(model.getIdProducto().toString()),
                model.getBorrado() != null && model.getBorrado());
    }
}
