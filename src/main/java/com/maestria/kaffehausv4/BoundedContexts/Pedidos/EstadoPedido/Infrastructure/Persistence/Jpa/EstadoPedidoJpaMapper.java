package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;

public class EstadoPedidoJpaMapper {
    private EstadoPedidoJpaMapper() {
    }

    public static EstadoPedidoJpaModel toPersistence(EstadoPedido estadoPedido) {
        EstadoPedidoJpaModel model = new EstadoPedidoJpaModel();
        model.setId(estadoPedido.getId() != null ? UUID.fromString(estadoPedido.getId().getValue()) : UUID.randomUUID());
        model.setNombre(estadoPedido.getNombre());
        model.setBorrado(estadoPedido.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(EstadoPedido estadoPedido, EstadoPedidoJpaModel model) {
        model.setNombre(estadoPedido.getNombre());
        model.setBorrado(estadoPedido.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static EstadoPedido toDomain(EstadoPedidoJpaModel model) {
        return EstadoPedido.restaurar(
                model.getId().toString(),
                model.getNombre(),
                model.getBorrado() != null && model.getBorrado());
    }
}
