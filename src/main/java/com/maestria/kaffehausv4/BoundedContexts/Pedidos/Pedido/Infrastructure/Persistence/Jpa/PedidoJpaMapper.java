package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;

public class PedidoJpaMapper {
    private PedidoJpaMapper() {
    }

    public static PedidoJpaModel toPersistence(Pedido pedido) {
        PedidoJpaModel model = new PedidoJpaModel();
        model.setId(pedido.getId() != null ? UUID.fromString(pedido.getId().getValue()) : UUID.randomUUID());
        model.setFechaEntrega(pedido.getFechaEntrega());
        model.setLatitud(pedido.getLatitud());
        model.setLongitud(pedido.getLongitud());
        model.setDescripcion(pedido.getDescripcion());
        model.setIdVenta(UUID.fromString(pedido.getVentaId().getValue()));
        model.setIdEstado(UUID.fromString(pedido.getEstadoId().getValue()));
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Pedido pedido, PedidoJpaModel model) {
        model.setFechaEntrega(pedido.getFechaEntrega());
        model.setLatitud(pedido.getLatitud());
        model.setLongitud(pedido.getLongitud());
        model.setDescripcion(pedido.getDescripcion());
        model.setIdVenta(UUID.fromString(pedido.getVentaId().getValue()));
        model.setIdEstado(UUID.fromString(pedido.getEstadoId().getValue()));
        model.setFecU(OffsetDateTime.now());
    }

    public static Pedido toDomain(PedidoJpaModel model) {
        return Pedido.restaurar(
                model.getId().toString(),
                model.getFechaEntrega(),
                model.getLatitud(),
                model.getLongitud(),
                model.getDescripcion(),
                new VentaId(model.getIdVenta().toString()),
                new EstadoPedidoId(model.getIdEstado().toString()));
    }
}
