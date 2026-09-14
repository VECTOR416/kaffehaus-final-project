package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.CrearPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.PedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearPedidoUseCase {
    private final IPedidoRepository pedidoRepository;
    private final IVentaRepository ventaRepository;
    private final IEstadoPedidoRepository estadoPedidoRepository;

    public CrearPedidoUseCase(
            IPedidoRepository pedidoRepository,
            IVentaRepository ventaRepository,
            IEstadoPedidoRepository estadoPedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.ventaRepository = ventaRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
    }

    public PedidoResponse execute(CrearPedidoRequest crearPedidoRequest) {
        VentaId ventaId = new VentaId(crearPedidoRequest.getIdVenta());
        ventaRepository.obtenerPorId(ventaId)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + crearPedidoRequest.getIdVenta() + "' no existe"));
        EstadoPedidoId estadoId = new EstadoPedidoId(crearPedidoRequest.getIdEstado());
        EstadoPedido estado = estadoPedidoRepository.obtenerPorId(estadoId)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido con id '" + crearPedidoRequest.getIdEstado() + "' no existe"));

        Pedido pedido = Pedido.crear(
                crearPedidoRequest.getFechaEntrega(),
                crearPedidoRequest.getLatitud(),
                crearPedidoRequest.getLongitud(),
                crearPedidoRequest.getDescripcion(),
                ventaId,
                estadoId);

        Pedido pedidoCreado = pedidoRepository.crear(pedido);

        return PedidoResponse.createFromAggregate(pedidoCreado, estado);
    }
}
