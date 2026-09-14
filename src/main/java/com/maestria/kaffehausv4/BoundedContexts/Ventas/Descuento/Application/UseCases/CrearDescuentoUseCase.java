package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.CrearDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.DescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearDescuentoUseCase {
    private final IDescuentoRepository descuentoRepository;
    private final IProductoRepository productoRepository;
    private final ITipoDescuentoRepository tipoDescuentoRepository;

    public CrearDescuentoUseCase(
            IDescuentoRepository descuentoRepository,
            IProductoRepository productoRepository,
            ITipoDescuentoRepository tipoDescuentoRepository) {
        this.descuentoRepository = descuentoRepository;
        this.productoRepository = productoRepository;
        this.tipoDescuentoRepository = tipoDescuentoRepository;
    }

    public DescuentoResponse execute(CrearDescuentoRequest crearDescuentoRequest) {
        ProductoId productoId = new ProductoId(crearDescuentoRequest.getIdProducto());
        productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + crearDescuentoRequest.getIdProducto() + "' no existe"));
        TipoDescuentoId tipoDescuentoId = new TipoDescuentoId(crearDescuentoRequest.getIdTipoDescuento());
        TipoDescuento tipoDescuento = tipoDescuentoRepository.obtenerPorId(tipoDescuentoId)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento con id '" + crearDescuentoRequest.getIdTipoDescuento() + "' no existe"));

        Descuento descuento = Descuento.crear(
                crearDescuentoRequest.getDescuento(),
                crearDescuentoRequest.getFechaInicio(),
                crearDescuentoRequest.getFechaFinal(),
                crearDescuentoRequest.isEstado(),
                productoId,
                tipoDescuentoId);

        Descuento descuentoCreado = descuentoRepository.crear(descuento);

        return DescuentoResponse.createFromAggregate(descuentoCreado, tipoDescuento);
    }
}
