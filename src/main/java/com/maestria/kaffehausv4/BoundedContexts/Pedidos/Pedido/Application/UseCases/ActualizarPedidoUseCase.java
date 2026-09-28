package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ActualizarPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.PedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.ValueObjects.PedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.RegistrarAnulacionDeVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.RegistrarAnulacionDeVentaUseCase;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Modificacion de un pedido (ventas.md VEN-51..VEN-53).
 *
 * El estado solo avanza por sus transiciones permitidas; un pedido 'Entregado' o 'Cancelado'
 * ya no cambia. Cancelarlo anula su venta en la misma transaccion: con reintegro si estaba
 * 'Recibido' (nada se preparo) y sin reintegro si ya estaba en preparacion o en camino (VEN-52).
 */
@Service
@Transactional
public class ActualizarPedidoUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String RECIBIDO = "recibido";
    private static final String CANCELADO = "cancelado";
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            RECIBIDO, Set.of("en preparacion", CANCELADO),
            "en preparacion", Set.of("en camino", CANCELADO),
            "en camino", Set.of("entregado", CANCELADO),
            "entregado", Set.of(),
            CANCELADO, Set.of());

    private final IPedidoRepository pedidoRepository;
    private final IEstadoPedidoRepository estadoPedidoRepository;
    private final RegistrarAnulacionDeVentaUseCase registrarAnulacionDeVentaUseCase;

    public ActualizarPedidoUseCase(
            IPedidoRepository pedidoRepository,
            IEstadoPedidoRepository estadoPedidoRepository,
            RegistrarAnulacionDeVentaUseCase registrarAnulacionDeVentaUseCase) {
        this.pedidoRepository = pedidoRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
        this.registrarAnulacionDeVentaUseCase = registrarAnulacionDeVentaUseCase;
    }

    public PedidoResponse execute(String pedidoId, ActualizarPedidoRequest actualizarPedidoRequest) {
        PedidoId id = new PedidoId(pedidoId);

        Pedido pedido = pedidoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El pedido con id '" + pedidoId + "' no existe"));

        String actual = nombreDe(pedido.getEstadoId());
        if (TRANSICIONES.getOrDefault(actual, Set.of()).isEmpty()) {
            throw DomainException.throwException(409, "El pedido con id '" + pedidoId + "' esta en un estado final y no admite cambios");
        }

        EstadoPedidoId estadoId = null;
        if (actualizarPedidoRequest.getIdEstado() != null) {
            estadoId = new EstadoPedidoId(actualizarPedidoRequest.getIdEstado());
            String destino = nombreDe(estadoId);
            if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(destino)) {
                throw DomainException.throwException(409,
                        "Un pedido no puede pasar de '" + actual + "' a '" + destino + "'");
            }
            if (CANCELADO.equals(destino)) {
                anularVentaDe(pedido, actual, actualizarPedidoRequest.getIdUsuario());
            }
        }

        pedido.actualizar(
                actualizarPedidoRequest.getFechaEntrega(),
                actualizarPedidoRequest.getLatitud(),
                actualizarPedidoRequest.getLongitud(),
                actualizarPedidoRequest.getDescripcion(),
                null,
                estadoId);

        Pedido pedidoActualizado = pedidoRepository.actualizar(id, pedido);

        return toResponse(pedidoActualizado);
    }

    /** Cancelar un pedido anula su venta; se reintegra solo si nada se preparo (VEN-52). */
    private void anularVentaDe(Pedido pedido, String estadoActual, String idUsuario) {
        if (idUsuario == null) {
            throw DomainException.throwException(400, "Cancelar un pedido requiere 'idUsuario': anula su venta");
        }
        registrarAnulacionDeVentaUseCase.execute(RegistrarAnulacionDeVentaRequest.crear(
                pedido.getVentaId().getValue(),
                idUsuario,
                "Cancelacion del pedido '" + pedido.getId().getValue() + "'",
                RECIBIDO.equals(estadoActual),
                LocalDateTime.now()));
    }

    private String nombreDe(EstadoPedidoId estadoId) {
        return estadoPedidoRepository.obtenerPorId(estadoId)
                .map(estado -> estado.getNombre().toLowerCase())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido con id '" + estadoId.getValue() + "' no existe"));
    }

    private PedidoResponse toResponse(Pedido pedido) {
        EstadoPedido estado = estadoPedidoRepository.obtenerPorId(pedido.getEstadoId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de pedido asociado a el pedido no existe"));
        return PedidoResponse.createFromAggregate(pedido, estado);
    }
}
