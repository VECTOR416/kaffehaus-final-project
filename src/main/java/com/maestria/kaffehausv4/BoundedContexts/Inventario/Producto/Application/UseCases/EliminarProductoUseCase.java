package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarProductoUseCase {
    private final IProductoRepository productoRepository;

    public EliminarProductoUseCase(IProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public void execute(String productoId) {
        ProductoId id = new ProductoId(productoId);

        productoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + productoId + "' no existe"));

        productoRepository.eliminar(id);
    }
}
