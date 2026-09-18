package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ActualizarDetalleVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ActualizarVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Modificacion de una venta existente, encabezado y lineas, en una sola transaccion.
 *
 * Cuando el payload trae 'detalles' se concilia por id en lugar de reemplazar la coleccion:
 * las lineas con id se corrigen, las que no lo traen se agregan y las ausentes se dan de baja
 * logica. Se concilia y no se reemplaza porque el sistema no usa borrado fisico: rehacer la
 * coleccion entera dejaria una fila muerta por cada linea modificada.
 *
 * Toda mutacion pasa por la raiz, que es quien recalcula el total.
 */
@Service
@Transactional
public class ActualizarVentaUseCase {
    private final IVentaRepository ventaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IClienteRepository clienteRepository;
    private final IEstadoVentaRepository estadoVentaRepository;
    private final IProductoRepository productoRepository;

    public ActualizarVentaUseCase(
            IVentaRepository ventaRepository,
            IUsuarioRepository usuarioRepository,
            IClienteRepository clienteRepository,
            IEstadoVentaRepository estadoVentaRepository,
            IProductoRepository productoRepository) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.estadoVentaRepository = estadoVentaRepository;
        this.productoRepository = productoRepository;
    }

    public VentaResponse execute(String ventaId, ActualizarVentaRequest actualizarVentaRequest) {
        VentaId id = new VentaId(ventaId);

        Venta venta = ventaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + ventaId + "' no existe"));

        UsuarioId usuarioId = null;
        if (actualizarVentaRequest.getIdUsuario() != null) {
            usuarioId = new UsuarioId(actualizarVentaRequest.getIdUsuario());
            usuarioRepository.obtenerPorId(usuarioId)
                    .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + actualizarVentaRequest.getIdUsuario() + "' no existe"));
        }
        ClienteId clienteId = null;
        if (actualizarVentaRequest.getIdCliente() != null) {
            clienteId = new ClienteId(actualizarVentaRequest.getIdCliente());
            clienteRepository.obtenerPorId(clienteId)
                    .orElseThrow(() -> DomainException.throwException(404, "El cliente con id '" + actualizarVentaRequest.getIdCliente() + "' no existe"));
        }
        EstadoVentaId estadoVentaId = null;
        if (actualizarVentaRequest.getIdEstadoVenta() != null) {
            estadoVentaId = new EstadoVentaId(actualizarVentaRequest.getIdEstadoVenta());
            estadoVentaRepository.obtenerPorId(estadoVentaId)
                    .orElseThrow(() -> DomainException.throwException(404, "El estado de venta con id '" + actualizarVentaRequest.getIdEstadoVenta() + "' no existe"));
        }

        venta.actualizar(
                actualizarVentaRequest.getFechaVenta(),
                usuarioId,
                clienteId,
                estadoVentaId);

        if (actualizarVentaRequest.getDetalles() != null) {
            conciliarDetalles(venta, actualizarVentaRequest.getDetalles());
        }

        venta.validarRegistrable();

        Venta ventaActualizada = ventaRepository.actualizar(id, venta);

        return toResponse(ventaActualizada);
    }

    private void conciliarDetalles(Venta venta, List<ActualizarDetalleVentaRequest> detalles) {
        Set<String> idsRecibidos = detalles.stream()
                .map(ActualizarDetalleVentaRequest::getId)
                .filter(idDetalle -> idDetalle != null)
                .collect(Collectors.toSet());

        List<String> idsVivos = venta.getDetallesActivos().stream()
                .map(detalle -> detalle.getId().getValue())
                .collect(Collectors.toList());

        for (String idVivo : idsVivos) {
            if (!idsRecibidos.contains(idVivo)) {
                venta.removerDetalle(idVivo);
            }
        }

        for (ActualizarDetalleVentaRequest detalle : detalles) {
            if (detalle.getId() == null) {
                agregarDetalleNuevo(venta, detalle);
            } else {
                corregirDetalleExistente(venta, detalle);
            }
        }
    }

    private void agregarDetalleNuevo(Venta venta, ActualizarDetalleVentaRequest detalle) {
        if (detalle.getIdProducto() == null || detalle.getPrecioUnitario() == null || detalle.getCantidad() == null) {
            throw DomainException.throwException(400,
                    "Un detalle de venta nuevo requiere 'idProducto', 'precioUnitario' y 'cantidad'");
        }

        ProductoId productoId = new ProductoId(detalle.getIdProducto());
        productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + detalle.getIdProducto() + "' no existe"));

        venta.agregarDetalle(
                productoId,
                detalle.getPrecioUnitario(),
                detalle.getCantidad(),
                detalle.getDescuentoAplicado());
    }

    private void corregirDetalleExistente(Venta venta, ActualizarDetalleVentaRequest detalle) {
        // Cambiar el producto de una linea ya vendida equivaldria a rehacer la venta.
        if (detalle.getIdProducto() != null && !detalle.getIdProducto().equals(productoActualDe(venta, detalle.getId()))) {
            throw DomainException.throwException(400,
                    "No se puede cambiar el producto de un detalle de venta existente; da de baja la linea y agrega una nueva");
        }

        venta.actualizarDetalle(
                detalle.getId(),
                detalle.getPrecioUnitario(),
                detalle.getCantidad(),
                detalle.getDescuentoAplicado());
    }

    private String productoActualDe(Venta venta, String detalleVentaId) {
        return venta.getDetallesActivos().stream()
                .filter(detalle -> detalle.getId().getValue().equals(detalleVentaId))
                .map(DetalleVenta::getProductoId)
                .map(ProductoId::getValue)
                .findFirst()
                .orElseThrow(() -> DomainException.throwException(404,
                        "El detalle con id '" + detalleVentaId + "' no pertenece a esta venta"));
    }

    private VentaResponse toResponse(Venta venta) {
        EstadoVenta estadoVenta = estadoVentaRepository.obtenerPorId(venta.getEstadoVentaId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta asociado a la venta no existe"));
        return VentaResponse.createFromAggregate(venta, estadoVenta);
    }
}
