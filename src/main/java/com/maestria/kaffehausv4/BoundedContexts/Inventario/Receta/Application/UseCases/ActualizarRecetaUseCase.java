package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ActualizarRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarRecetaUseCase {
    private final IRecetaRepository recetaRepository;
    private final IProductoRepository productoRepository;

    public ActualizarRecetaUseCase(
            IRecetaRepository recetaRepository,
            IProductoRepository productoRepository) {
        this.recetaRepository = recetaRepository;
        this.productoRepository = productoRepository;
    }

    public RecetaResponse execute(String recetaId, ActualizarRecetaRequest actualizarRecetaRequest) {
        RecetaId id = new RecetaId(recetaId);

        Receta receta = recetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + recetaId + "' no existe"));

        ProductoId productoObtenidoId = null;
        if (actualizarRecetaRequest.getIdProductoObtenido() != null) {
            productoObtenidoId = new ProductoId(actualizarRecetaRequest.getIdProductoObtenido());
            productoRepository.obtenerPorId(productoObtenidoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + actualizarRecetaRequest.getIdProductoObtenido() + "' no existe"));
        }

        receta.actualizar(
                actualizarRecetaRequest.getCantidad(),
                actualizarRecetaRequest.getTitulo(),
                productoObtenidoId);

        Receta recetaActualizada = recetaRepository.actualizar(id, receta);

        return RecetaResponse.createFromAggregate(recetaActualizada);
    }
}
