package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.CrearDetalleVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.DetalleVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Repositories.IDetalleVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearDetalleVentaUseCase {
    private final IDetalleVentaRepository detalleVentaRepository;
    private final IVentaRepository ventaRepository;
    private final IProductoRepository productoRepository;

    public CrearDetalleVentaUseCase(
            IDetalleVentaRepository detalleVentaRepository,
            IVentaRepository ventaRepository,
            IProductoRepository productoRepository) {
        this.detalleVentaRepository = detalleVentaRepository;
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
    }

    public DetalleVentaResponse execute(CrearDetalleVentaRequest crearDetalleVentaRequest) {
        VentaId ventaId = new VentaId(crearDetalleVentaRequest.getIdVenta());
        ventaRepository.obtenerPorId(ventaId)
                .orElseThrow(() -> DomainException.throwException(404, "La venta con id '" + crearDetalleVentaRequest.getIdVenta() + "' no existe"));
        ProductoId productoId = new ProductoId(crearDetalleVentaRequest.getIdProducto());
        productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + crearDetalleVentaRequest.getIdProducto() + "' no existe"));

        DetalleVenta detalleVenta = DetalleVenta.crear(
                crearDetalleVentaRequest.getPrecioUnitario(),
                crearDetalleVentaRequest.getCantidad(),
                crearDetalleVentaRequest.getSubtotal(),
                crearDetalleVentaRequest.getDescuentoAplicado(),
                crearDetalleVentaRequest.isEstado(),
                ventaId,
                productoId);

        DetalleVenta detalleVentaCreado = detalleVentaRepository.crear(detalleVenta);

        return DetalleVentaResponse.createFromAggregate(detalleVentaCreado);
    }
}
