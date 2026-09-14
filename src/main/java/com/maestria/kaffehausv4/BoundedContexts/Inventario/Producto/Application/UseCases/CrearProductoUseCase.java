package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.CrearProductoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearProductoUseCase {
    private final IProductoRepository productoRepository;
    private final IUnidadRepository unidadRepository;
    private final ICategoriaRepository categoriaRepository;

    public CrearProductoUseCase(
            IProductoRepository productoRepository,
            IUnidadRepository unidadRepository,
            ICategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.unidadRepository = unidadRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public ProductoResponse execute(CrearProductoRequest crearProductoRequest) {
        UnidadId unidadActualId = new UnidadId(crearProductoRequest.getIdUnidadActual());
        Unidad unidadActual = unidadRepository.obtenerPorId(unidadActualId)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + crearProductoRequest.getIdUnidadActual() + "' no existe"));
        CategoriaId categoriaId = new CategoriaId(crearProductoRequest.getIdCategoria());
        Categoria categoria = categoriaRepository.obtenerPorId(categoriaId)
                .orElseThrow(() -> DomainException.throwException(404, "La categoria con id '" + crearProductoRequest.getIdCategoria() + "' no existe"));
        ProductoId productoPadreId = null;
        if (crearProductoRequest.getIdProductoPadre() != null) {
            productoPadreId = new ProductoId(crearProductoRequest.getIdProductoPadre());
            productoRepository.obtenerPorId(productoPadreId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + crearProductoRequest.getIdProductoPadre() + "' no existe"));
        }

        Producto producto = Producto.crear(
                crearProductoRequest.getNombre(),
                crearProductoRequest.getStock(),
                crearProductoRequest.getPrecio(),
                crearProductoRequest.getStockMinimo(),
                crearProductoRequest.isVendible(),
                unidadActualId,
                categoriaId,
                productoPadreId);

        Producto productoCreado = productoRepository.crear(producto);

        return ProductoResponse.createFromAggregate(productoCreado, unidadActual, categoria);
    }
}
