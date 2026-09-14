package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;

public class VentaJpaMapper {
    private VentaJpaMapper() {
    }

    public static VentaJpaModel toPersistence(Venta venta) {
        VentaJpaModel model = new VentaJpaModel();
        model.setId(venta.getId() != null ? UUID.fromString(venta.getId().getValue()) : UUID.randomUUID());
        model.setFechaVenta(venta.getFechaVenta());
        model.setTotal(venta.getTotal());
        model.setIdUsuario(UUID.fromString(venta.getUsuarioId().getValue()));
        model.setIdCliente(venta.getClienteId() != null ? UUID.fromString(venta.getClienteId().getValue()) : null);
        model.setIdEstadoVenta(UUID.fromString(venta.getEstadoVentaId().getValue()));
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Venta venta, VentaJpaModel model) {
        model.setFechaVenta(venta.getFechaVenta());
        model.setTotal(venta.getTotal());
        model.setIdUsuario(UUID.fromString(venta.getUsuarioId().getValue()));
        model.setIdCliente(venta.getClienteId() != null ? UUID.fromString(venta.getClienteId().getValue()) : null);
        model.setIdEstadoVenta(UUID.fromString(venta.getEstadoVentaId().getValue()));
        model.setFecU(OffsetDateTime.now());
    }

    public static Venta toDomain(VentaJpaModel model) {
        return Venta.restaurar(
                model.getId().toString(),
                model.getFechaVenta(),
                model.getTotal(),
                new UsuarioId(model.getIdUsuario().toString()),
                model.getIdCliente() != null ? new ClienteId(model.getIdCliente().toString()) : null,
                new EstadoVentaId(model.getIdEstadoVenta().toString()));
    }
}
