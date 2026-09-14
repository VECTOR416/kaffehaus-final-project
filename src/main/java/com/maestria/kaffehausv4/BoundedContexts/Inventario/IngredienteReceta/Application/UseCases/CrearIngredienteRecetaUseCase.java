package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.CrearIngredienteRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.IngredienteRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Repositories.IIngredienteRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;

@Service
@Transactional
public class CrearIngredienteRecetaUseCase {
    private final IIngredienteRecetaRepository ingredienteRecetaRepository;

    public CrearIngredienteRecetaUseCase(
            IIngredienteRecetaRepository ingredienteRecetaRepository,
            IRecetaRepository recetaRepository,
            IProductoRepository productoRepository) {
        this.ingredienteRecetaRepository = ingredienteRecetaRepository;

    }

    public IngredienteRecetaResponse execute(CrearIngredienteRecetaRequest crearIngredienteRecetaRequest) {
        RecetaId recetaId = new RecetaId(crearIngredienteRecetaRequest.getIdReceta());

        ProductoId productoId = new ProductoId(crearIngredienteRecetaRequest.getIdProducto());
        IngredienteReceta ingredienteReceta = IngredienteReceta.crear(
                crearIngredienteRecetaRequest.getCantidad(),
                recetaId,
                productoId);

        IngredienteReceta ingredienteRecetaCreado = ingredienteRecetaRepository.crear(ingredienteReceta);

        return IngredienteRecetaResponse.createFromAggregate(ingredienteRecetaCreado);
    }
}
