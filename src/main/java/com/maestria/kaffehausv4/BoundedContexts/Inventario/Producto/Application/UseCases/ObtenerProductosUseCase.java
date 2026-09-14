package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ObtenerProductosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerProductosUseCase {
    private final IProductoRepository productoRepository;
    private final IUnidadRepository unidadRepository;
    private final ICategoriaRepository categoriaRepository;

    public ObtenerProductosUseCase(
            IProductoRepository productoRepository,
            IUnidadRepository unidadRepository,
            ICategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.unidadRepository = unidadRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public PaginatedResponse<List<ProductoResponse>> execute(ObtenerProductosRequest obtenerProductosRequest) {
        PaginatedResponse<List<Producto>> resultado = productoRepository.obtenerTodos(obtenerProductosRequest);

        List<ProductoResponse> productos = resultado.getData().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                productos,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }

    private ProductoResponse toResponse(Producto producto) {
        Unidad unidadActual = unidadRepository.obtenerPorId(producto.getUnidadActualId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a el producto no existe"));
        Categoria categoria = categoriaRepository.obtenerPorId(producto.getCategoriaId())
                .orElseThrow(() -> DomainException.throwException(404, "La categoria asociada a el producto no existe"));
        return ProductoResponse.createFromAggregate(producto, unidadActual, categoria);
    }
}
