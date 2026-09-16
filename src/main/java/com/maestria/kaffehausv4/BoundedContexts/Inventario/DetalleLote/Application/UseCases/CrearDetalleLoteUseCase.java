package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.CrearDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.DetalleLoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Repositories.IDetalleLoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearDetalleLoteUseCase {
    private final IDetalleLoteRepository detalleLoteRepository;
    private final ILoteRepository loteRepository;
    private final IProductoRepository productoRepository;

    public CrearDetalleLoteUseCase(
            IDetalleLoteRepository detalleLoteRepository,
            ILoteRepository loteRepository,
            IProductoRepository productoRepository) {
        this.detalleLoteRepository = detalleLoteRepository;
        this.loteRepository = loteRepository;
        this.productoRepository = productoRepository;
    }

    public DetalleLoteResponse execute(CrearDetalleLoteRequest crearDetalleLoteRequest) {
        LoteId loteId = new LoteId(crearDetalleLoteRequest.getIdLote());
        loteRepository.obtenerPorId(loteId)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + crearDetalleLoteRequest.getIdLote() + "' no existe"));
        ProductoId productoId = new ProductoId(crearDetalleLoteRequest.getIdProducto());
        productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + crearDetalleLoteRequest.getIdProducto() + "' no existe"));

        DetalleLote detalleLote = DetalleLote.crear(
                crearDetalleLoteRequest.getCantidadInicial(),
                crearDetalleLoteRequest.getCantidadRestante(),
                crearDetalleLoteRequest.getPrecioUnitario(),
                crearDetalleLoteRequest.isFraccionable(),
                loteId,
                productoId);

        DetalleLote detalleLoteCreado = detalleLoteRepository.crear(detalleLote);

        return DetalleLoteResponse.createFromAggregate(detalleLoteCreado);
    }
}
