package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;

/**
 * Mapea las lineas del agregado Venta.
 *
 * toPersistence recibe el id de la venta por parametro: la entidad DetalleVenta ya no guarda
 * la referencia al padre, porque dentro del agregado la relacion es la contencion. La
 * columna 'detalle_venta.id_venta' se completa aqui, a partir de la raiz.
 */
public class DetalleVentaJpaMapper {
    private DetalleVentaJpaMapper() {
    }

    public static DetalleVentaJpaModel toPersistence(DetalleVenta detalleVenta, UUID idVenta) {
        DetalleVentaJpaModel model = new DetalleVentaJpaModel();
        model.setId(detalleVenta.getId() != null ? UUID.fromString(detalleVenta.getId().getValue()) : UUID.randomUUID());
        model.setPrecioUnitario(detalleVenta.getPrecioUnitario());
        model.setCantidad(detalleVenta.getCantidad());
        model.setSubtotal(detalleVenta.getSubtotal());
        model.setDescuentoAplicado(detalleVenta.getDescuentoAplicado());
        model.setEstado(detalleVenta.isEstado());
        model.setIdVenta(idVenta);
        model.setIdProducto(UUID.fromString(detalleVenta.getProductoId().getValue()));
        model.setBorrado(detalleVenta.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(DetalleVenta detalleVenta, DetalleVentaJpaModel model) {
        model.setPrecioUnitario(detalleVenta.getPrecioUnitario());
        model.setCantidad(detalleVenta.getCantidad());
        model.setSubtotal(detalleVenta.getSubtotal());
        model.setDescuentoAplicado(detalleVenta.getDescuentoAplicado());
        model.setEstado(detalleVenta.isEstado());
        model.setBorrado(detalleVenta.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static DetalleVenta toDomain(DetalleVentaJpaModel model) {
        return DetalleVenta.restaurar(
                model.getId().toString(),
                new ProductoId(model.getIdProducto().toString()),
                model.getPrecioUnitario(),
                model.getCantidad(),
                model.getSubtotal(),
                model.getDescuentoAplicado(),
                model.getEstado() != null && model.getEstado(),
                model.getBorrado() != null && model.getBorrado());
    }
}
