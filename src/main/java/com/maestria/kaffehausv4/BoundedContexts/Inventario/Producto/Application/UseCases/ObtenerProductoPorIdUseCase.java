package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerProductoPorIdUseCase {
    private final IProductoRepository productoRepository;
    private final IUnidadRepository unidadRepository;
    private final ITipoRepository tipoRepository;
    private final ICategoriaRepository categoriaRepository;

    public ObtenerProductoPorIdUseCase(
            IProductoRepository productoRepository,
            IUnidadRepository unidadRepository,
            ITipoRepository tipoRepository,
            ICategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.unidadRepository = unidadRepository;
        this.tipoRepository = tipoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public ProductoResponse execute(String productoId) {
        ProductoId id = new ProductoId(productoId);

        Producto producto = productoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + productoId + "' no existe"));

        return toResponse(producto);
    }

    private ProductoResponse toResponse(Producto producto) {
        Unidad unidadActual = unidadRepository.obtenerPorId(producto.getUnidadActualId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a el producto no existe"));
        Tipo tipo = tipoRepository.obtenerPorId(producto.getTipoId())
                .orElseThrow(() -> DomainException.throwException(404, "El tipo asociado a el producto no existe"));
        Categoria categoria = categoriaRepository.obtenerPorId(producto.getCategoriaId())
                .orElseThrow(() -> DomainException.throwException(404, "La categoria asociada a el producto no existe"));
        return ProductoResponse.createFromAggregate(producto, unidadActual, tipo, categoria);
    }
}
