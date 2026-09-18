package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;

/**
 * Mapea las lineas del agregado Lote.
 *
 * toPersistence recibe el id del lote por parametro: la entidad DetalleLote ya no guarda la
 * referencia al padre, porque dentro del agregado la relacion es la contencion. La columna
 * 'detalle_lote.id_lote' se completa aqui, a partir de la raiz.
 */
public class DetalleLoteJpaMapper {
    private DetalleLoteJpaMapper() {
    }

    public static DetalleLoteJpaModel toPersistence(DetalleLote detalleLote, UUID idLote) {
        DetalleLoteJpaModel model = new DetalleLoteJpaModel();
        model.setId(detalleLote.getId() != null ? UUID.fromString(detalleLote.getId().getValue()) : UUID.randomUUID());
        model.setCantidadInicial(detalleLote.getCantidadInicial());
        model.setCantidadRestante(detalleLote.getCantidadRestante());
        model.setPrecioUnitario(detalleLote.getPrecioUnitario());
        model.setFraccionable(detalleLote.isFraccionable());
        model.setIdLote(idLote);
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
        model.setBorrado(detalleLote.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static DetalleLote toDomain(DetalleLoteJpaModel model) {
        return DetalleLote.restaurar(
                model.getId().toString(),
                new ProductoId(model.getIdProducto().toString()),
                model.getCantidadInicial(),
                model.getCantidadRestante(),
                model.getPrecioUnitario(),
                model.getFraccionable() != null && model.getFraccionable(),
                model.getBorrado() != null && model.getBorrado());
    }
}
