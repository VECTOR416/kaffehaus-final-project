package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
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
        model.setFechaVencimiento(detalleLote.getFechaVencimiento());
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
        model.setFechaVencimiento(detalleLote.getFechaVencimiento());
        model.setBorrado(detalleLote.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    /** La misma linea reconstruida desde su proyeccion de lectura (arquitectura.md 5.7). */
    public static DetalleLote toDomain(DetalleLoteLecturaProyeccion lectura) {
        return DetalleLote.restaurar(
                lectura.getId().toString(),
                new ProductoId(lectura.getIdProducto().toString()),
                lectura.getCantidadInicial(),
                lectura.getCantidadRestante(),
                lectura.getPrecioUnitario(),
                lectura.getFraccionable() != null && lectura.getFraccionable(),
                lectura.getFechaVencimiento(),
                lectura.getBorrado() != null && lectura.getBorrado());
    }

    /**
     * Si la linea del dominio difiere de su fila. El datasource solo escribe las lineas que
     * cambiaron: reescribir una linea que la operacion no toco pisaria con un valor ya leido lo
     * que otra transaccion haya descontado de ella (arquitectura.md 5.7).
     */
    public static boolean difiere(DetalleLote detalleLote, DetalleLoteJpaModel model) {
        return !iguales(detalleLote.getCantidadInicial(), model.getCantidadInicial())
                || !iguales(detalleLote.getCantidadRestante(), model.getCantidadRestante())
                || !iguales(detalleLote.getPrecioUnitario(), model.getPrecioUnitario())
                || detalleLote.isFraccionable() != (model.getFraccionable() != null && model.getFraccionable())
                || !Objects.equals(detalleLote.getFechaVencimiento(), model.getFechaVencimiento())
                || detalleLote.isBorrado() != (model.getBorrado() != null && model.getBorrado());
    }

    private static boolean iguales(BigDecimal uno, BigDecimal otro) {
        return uno == null ? otro == null : otro != null && uno.compareTo(otro) == 0;
    }

    public static DetalleLote toDomain(DetalleLoteJpaModel model) {
        return DetalleLote.restaurar(
                model.getId().toString(),
                new ProductoId(model.getIdProducto().toString()),
                model.getCantidadInicial(),
                model.getCantidadRestante(),
                model.getPrecioUnitario(),
                model.getFraccionable() != null && model.getFraccionable(),
                model.getFechaVencimiento(),
                model.getBorrado() != null && model.getBorrado());
    }
}
