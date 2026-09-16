package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.CrearRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearRecetaUseCase {
    private final IRecetaRepository recetaRepository;
    private final IProductoRepository productoRepository;

    public CrearRecetaUseCase(
            IRecetaRepository recetaRepository,
            IProductoRepository productoRepository) {
        this.recetaRepository = recetaRepository;
        this.productoRepository = productoRepository;
    }

    public RecetaResponse execute(CrearRecetaRequest crearRecetaRequest) {
        ProductoId productoObtenidoId = new ProductoId(crearRecetaRequest.getIdProductoObtenido());
        productoRepository.obtenerPorId(productoObtenidoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + crearRecetaRequest.getIdProductoObtenido() + "' no existe"));

        Receta receta = Receta.crear(
                crearRecetaRequest.getCantidad(),
                crearRecetaRequest.getTitulo(),
                productoObtenidoId);

        Receta recetaCreada = recetaRepository.crear(receta);

        return RecetaResponse.createFromAggregate(recetaCreada);
    }
}
