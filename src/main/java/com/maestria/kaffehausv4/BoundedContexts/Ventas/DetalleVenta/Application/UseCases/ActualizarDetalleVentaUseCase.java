package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.ActualizarDetalleVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.DetalleVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Repositories.IDetalleVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.ValueObjects.DetalleVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarDetalleVentaUseCase {
    private final IDetalleVentaRepository detalleVentaRepository;
    private final IVentaRepository ventaRepository;
    private final IProductoRepository productoRepository;

    public ActualizarDetalleVentaUseCase(
            IDetalleVentaRepository detalleVentaRepository,
            IVentaRepository ventaRepository,
            IProductoRepository productoRepository) {
        this.detalleVentaRepository = detalleVentaRepository;
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
    }

    public DetalleVentaResponse execute(String detalleVentaId, ActualizarDetalleVentaRequest actualizarDetalleVentaRequest) {
        DetalleVentaId id = new DetalleVentaId(detalleVentaId);

        DetalleVenta detalleVenta = detalleVentaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de venta con id '" + detalleVentaId + "' no existe"));

        VentaId ventaId = null;
        if (actualizarDetalleVentaRequest.getIdVenta() != null) {
            ventaId = new VentaId(actualizarDetalleVentaRequest.getIdVenta());
            ventaRepository.obtenerPorId(ventaId)
                    .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + actualizarDetalleVentaRequest.getIdVenta() + "' no existe"));
        }
        ProductoId productoId = null;
        if (actualizarDetalleVentaRequest.getIdProducto() != null) {
            productoId = new ProductoId(actualizarDetalleVentaRequest.getIdProducto());
            productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + actualizarDetalleVentaRequest.getIdProducto() + "' no existe"));
        }

        detalleVenta.actualizar(
                actualizarDetalleVentaRequest.getPrecioUnitario(),
                actualizarDetalleVentaRequest.getCantidad(),
                actualizarDetalleVentaRequest.getSubtotal(),
                actualizarDetalleVentaRequest.getDescuentoAplicado(),
                actualizarDetalleVentaRequest.getEstado(),
                ventaId,
                productoId);

        DetalleVenta detalleVentaActualizado = detalleVentaRepository.actualizar(id, detalleVenta);

        return DetalleVentaResponse.createFromAggregate(detalleVentaActualizado);
    }
}
