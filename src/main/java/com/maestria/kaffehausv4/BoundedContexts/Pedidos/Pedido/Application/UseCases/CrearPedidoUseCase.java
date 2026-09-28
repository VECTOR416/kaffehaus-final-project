package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.CrearPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.PedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Alta de un pedido a domicilio (ventas.md VEN-50): la entrega de una venta ya completada. El
 * inventario salio al vender, no al despachar. Nace 'Recibido' (VEN-51); no se admite sobre
 * una venta anulada ni un segundo pedido de la misma venta (VEN-63).
 */
@Service
@Transactional
public class CrearPedidoUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_RECIBIDO = "Recibido";
    private static final String ESTADO_VENTA_COMPLETADA = "Completada";

    private final IPedidoRepository pedidoRepository;
    private final IVentaRepository ventaRepository;
    private final IEstadoPedidoRepository estadoPedidoRepository;
    private final IEstadoVentaRepository estadoVentaRepository;

    public CrearPedidoUseCase(
            IPedidoRepository pedidoRepository,
            IVentaRepository ventaRepository,
            IEstadoPedidoRepository estadoPedidoRepository,
            IEstadoVentaRepository estadoVentaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.ventaRepository = ventaRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
        this.estadoVentaRepository = estadoVentaRepository;
    }

    public PedidoResponse execute(CrearPedidoRequest crearPedidoRequest) {
        VentaId ventaId = new VentaId(crearPedidoRequest.getIdVenta());
        Venta venta = ventaRepository.obtenerPorId(ventaId)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + crearPedidoRequest.getIdVenta() + "' no existe"));

        EstadoVenta completada = estadoVentaRepository.obtenerPorNombre(ESTADO_VENTA_COMPLETADA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta '" + ESTADO_VENTA_COMPLETADA + "' no esta registrado"));
        if (!venta.estaEn(completada.getId())) {
            throw DomainException.throwException(409, "Solo se registra un pedido de una venta completada");
        }
        if (pedidoRepository.obtenerPorVentaId(ventaId).isPresent()) {
            throw DomainException.throwException(409, "La venta con id '" + crearPedidoRequest.getIdVenta() + "' ya tiene un pedido");
        }

        EstadoPedido recibido = estadoPedidoRepository.obtenerPorNombre(ESTADO_RECIBIDO)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido '" + ESTADO_RECIBIDO + "' no esta registrado"));

        Pedido pedido = Pedido.crear(
                crearPedidoRequest.getFechaEntrega(),
                crearPedidoRequest.getLatitud(),
                crearPedidoRequest.getLongitud(),
                crearPedidoRequest.getDescripcion(),
                ventaId,
                recibido.getId());

        Pedido pedidoCreado = pedidoRepository.crear(pedido);

        return PedidoResponse.createFromAggregate(pedidoCreado, recibido);
    }
}
