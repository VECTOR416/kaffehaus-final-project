package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;

public class DetalleVentaJpaMapper {
    private DetalleVentaJpaMapper() {
    }

    public static DetalleVentaJpaModel toPersistence(DetalleVenta detalleVenta) {
        DetalleVentaJpaModel model = new DetalleVentaJpaModel();
        model.setId(detalleVenta.getId() != null ? UUID.fromString(detalleVenta.getId().getValue()) : UUID.randomUUID());
        model.setPrecioUnitario(detalleVenta.getPrecioUnitario());
        model.setCantidad(detalleVenta.getCantidad());
        model.setSubtotal(detalleVenta.getSubtotal());
        model.setDescuentoAplicado(detalleVenta.getDescuentoAplicado());
        model.setEstado(detalleVenta.isEstado());
        model.setIdVenta(UUID.fromString(detalleVenta.getVentaId().getValue()));
        model.setIdProducto(UUID.fromString(detalleVenta.getProductoId().getValue()));
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(DetalleVenta detalleVenta, DetalleVentaJpaModel model) {
        model.setPrecioUnitario(detalleVenta.getPrecioUnitario());
        model.setCantidad(detalleVenta.getCantidad());
        model.setSubtotal(detalleVenta.getSubtotal());
        model.setDescuentoAplicado(detalleVenta.getDescuentoAplicado());
        model.setEstado(detalleVenta.isEstado());
        model.setIdVenta(UUID.fromString(detalleVenta.getVentaId().getValue()));
        model.setIdProducto(UUID.fromString(detalleVenta.getProductoId().getValue()));
        model.setFecU(OffsetDateTime.now());
    }

    public static DetalleVenta toDomain(DetalleVentaJpaModel model) {
        return DetalleVenta.restaurar(
                model.getId().toString(),
                model.getPrecioUnitario(),
                model.getCantidad(),
                model.getSubtotal(),
                model.getDescuentoAplicado(),
                model.getEstado(),
                new VentaId(model.getIdVenta().toString()),
                new ProductoId(model.getIdProducto().toString()));
    }
}
