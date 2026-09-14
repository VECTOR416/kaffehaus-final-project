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


@Service
@Transactional
public class CrearDetalleLoteUseCase {
    private final IDetalleLoteRepository detalleLoteRepository;

    public CrearDetalleLoteUseCase(
            IDetalleLoteRepository detalleLoteRepository,
            ILoteRepository loteRepository,
            IProductoRepository productoRepository) {
        this.detalleLoteRepository = detalleLoteRepository;
    }

    public DetalleLoteResponse execute(CrearDetalleLoteRequest crearDetalleLoteRequest) {
        LoteId loteId = new LoteId(crearDetalleLoteRequest.getIdLote());
        ProductoId productoId = new ProductoId(crearDetalleLoteRequest.getIdProducto());
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
