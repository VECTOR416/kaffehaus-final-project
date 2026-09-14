package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ActualizarDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.DescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarDescuentoUseCase {
    private final IDescuentoRepository descuentoRepository;
    private final IProductoRepository productoRepository;
    private final ITipoDescuentoRepository tipoDescuentoRepository;

    public ActualizarDescuentoUseCase(
            IDescuentoRepository descuentoRepository,
            IProductoRepository productoRepository,
            ITipoDescuentoRepository tipoDescuentoRepository) {
        this.descuentoRepository = descuentoRepository;
        this.productoRepository = productoRepository;
        this.tipoDescuentoRepository = tipoDescuentoRepository;
    }

    public DescuentoResponse execute(String descuentoId, ActualizarDescuentoRequest actualizarDescuentoRequest) {
        DescuentoId id = new DescuentoId(descuentoId);

        Descuento descuento = descuentoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El descuento con id '" + descuentoId + "' no existe"));

        ProductoId productoId = null;
        if (actualizarDescuentoRequest.getIdProducto() != null) {
            productoId = new ProductoId(actualizarDescuentoRequest.getIdProducto());
            productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + actualizarDescuentoRequest.getIdProducto() + "' no existe"));
        }
        TipoDescuentoId tipoDescuentoId = null;
        if (actualizarDescuentoRequest.getIdTipoDescuento() != null) {
            tipoDescuentoId = new TipoDescuentoId(actualizarDescuentoRequest.getIdTipoDescuento());
            tipoDescuentoRepository.obtenerPorId(tipoDescuentoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento con id '" + actualizarDescuentoRequest.getIdTipoDescuento() + "' no existe"));
        }

        descuento.actualizar(
                actualizarDescuentoRequest.getDescuento(),
                actualizarDescuentoRequest.getFechaInicio(),
                actualizarDescuentoRequest.getFechaFinal(),
                actualizarDescuentoRequest.getEstado(),
                productoId,
                tipoDescuentoId);

        Descuento descuentoActualizado = descuentoRepository.actualizar(id, descuento);

        return toResponse(descuentoActualizado);
    }

    private DescuentoResponse toResponse(Descuento descuento) {
        TipoDescuento tipoDescuento = tipoDescuentoRepository.obtenerPorId(descuento.getTipoDescuentoId())
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento asociado a el descuento no existe"));
        return DescuentoResponse.createFromAggregate(descuento, tipoDescuento);
    }
}
