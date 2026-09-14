package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.ActualizarDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.DetalleLoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Repositories.IDetalleLoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarDetalleLoteUseCase {
    private final IDetalleLoteRepository detalleLoteRepository;
    private final ILoteRepository loteRepository;
    private final IProductoRepository productoRepository;

    public ActualizarDetalleLoteUseCase(
            IDetalleLoteRepository detalleLoteRepository,
            ILoteRepository loteRepository,
            IProductoRepository productoRepository) {
        this.detalleLoteRepository = detalleLoteRepository;
        this.loteRepository = loteRepository;
        this.productoRepository = productoRepository;
    }

    public DetalleLoteResponse execute(String detalleLoteId, ActualizarDetalleLoteRequest actualizarDetalleLoteRequest) {
        DetalleLoteId id = new DetalleLoteId(detalleLoteId);

        DetalleLote detalleLote = detalleLoteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + detalleLoteId + "' no existe"));

        LoteId loteId = null;
        if (actualizarDetalleLoteRequest.getIdLote() != null) {
            loteId = new LoteId(actualizarDetalleLoteRequest.getIdLote());
            loteRepository.obtenerPorId(loteId)
                    .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + actualizarDetalleLoteRequest.getIdLote() + "' no existe"));
        }
        ProductoId productoId = null;
        if (actualizarDetalleLoteRequest.getIdProducto() != null) {
            productoId = new ProductoId(actualizarDetalleLoteRequest.getIdProducto());
            productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + actualizarDetalleLoteRequest.getIdProducto() + "' no existe"));
        }

        detalleLote.actualizar(
                actualizarDetalleLoteRequest.getCantidadInicial(),
                actualizarDetalleLoteRequest.getCantidadRestante(),
                actualizarDetalleLoteRequest.getPrecioUnitario(),
                actualizarDetalleLoteRequest.getFraccionable(),
                loteId,
                productoId);

        DetalleLote detalleLoteActualizado = detalleLoteRepository.actualizar(id, detalleLote);

        return DetalleLoteResponse.createFromAggregate(detalleLoteActualizado);
    }
}
