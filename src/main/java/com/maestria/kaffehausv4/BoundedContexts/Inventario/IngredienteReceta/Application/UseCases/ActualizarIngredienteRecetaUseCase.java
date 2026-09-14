package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.ActualizarIngredienteRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.IngredienteRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Repositories.IIngredienteRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.ValueObjects.IngredienteRecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarIngredienteRecetaUseCase {
    private final IIngredienteRecetaRepository ingredienteRecetaRepository;
    private final IRecetaRepository recetaRepository;
    private final IProductoRepository productoRepository;

    public ActualizarIngredienteRecetaUseCase(
            IIngredienteRecetaRepository ingredienteRecetaRepository,
            IRecetaRepository recetaRepository,
            IProductoRepository productoRepository) {
        this.ingredienteRecetaRepository = ingredienteRecetaRepository;
        this.recetaRepository = recetaRepository;
        this.productoRepository = productoRepository;
    }

    public IngredienteRecetaResponse execute(String ingredienteRecetaId, ActualizarIngredienteRecetaRequest actualizarIngredienteRecetaRequest) {
        IngredienteRecetaId id = new IngredienteRecetaId(ingredienteRecetaId);

        IngredienteReceta ingredienteReceta = ingredienteRecetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El ingrediente de receta con id '" + ingredienteRecetaId + "' no existe"));

        RecetaId recetaId = null;
        if (actualizarIngredienteRecetaRequest.getIdReceta() != null) {
            recetaId = new RecetaId(actualizarIngredienteRecetaRequest.getIdReceta());
            recetaRepository.obtenerPorId(recetaId)
                    .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + actualizarIngredienteRecetaRequest.getIdReceta() + "' no existe"));
        }
        ProductoId productoId = null;
        if (actualizarIngredienteRecetaRequest.getIdProducto() != null) {
            productoId = new ProductoId(actualizarIngredienteRecetaRequest.getIdProducto());
            productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + actualizarIngredienteRecetaRequest.getIdProducto() + "' no existe"));
        }

        ingredienteReceta.actualizar(
                actualizarIngredienteRecetaRequest.getCantidad(),
                recetaId,
                productoId);

        IngredienteReceta ingredienteRecetaActualizado = ingredienteRecetaRepository.actualizar(id, ingredienteReceta);

        return IngredienteRecetaResponse.createFromAggregate(ingredienteRecetaActualizado);
    }
}
