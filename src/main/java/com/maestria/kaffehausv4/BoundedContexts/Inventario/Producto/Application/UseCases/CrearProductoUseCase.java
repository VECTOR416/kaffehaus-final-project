package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
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
    private final ITipoRepository tipoRepository;
    private final ICategoriaRepository categoriaRepository;

    public CrearProductoUseCase(
            IProductoRepository productoRepository,
            IUnidadRepository unidadRepository,
            ITipoRepository tipoRepository,
            ICategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.unidadRepository = unidadRepository;
        this.tipoRepository = tipoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public ProductoResponse execute(CrearProductoRequest crearProductoRequest) {
        UnidadId unidadActualId = new UnidadId(crearProductoRequest.getIdUnidadActual());
        Unidad unidadActual = unidadRepository.obtenerPorId(unidadActualId)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + crearProductoRequest.getIdUnidadActual() + "' no existe"));
        TipoId tipoId = new TipoId(crearProductoRequest.getIdTipo());
        Tipo tipo = tipoRepository.obtenerPorId(tipoId)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo con id '" + crearProductoRequest.getIdTipo() + "' no existe"));
        CategoriaId categoriaId = new CategoriaId(crearProductoRequest.getIdCategoria());
        Categoria categoria = categoriaRepository.obtenerPorId(categoriaId)
                .orElseThrow(() -> DomainException.throwException(404, "La categoria con id '" + crearProductoRequest.getIdCategoria() + "' no existe"));
        ProductoId productoPadreId = null;
        if (crearProductoRequest.getIdProductoPadre() != null) {
            productoPadreId = new ProductoId(crearProductoRequest.getIdProductoPadre());
            Producto padre = productoRepository.obtenerPorId(productoPadreId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + crearProductoRequest.getIdProductoPadre() + "' no existe"));
            // La jerarquia presentacion -> producto base tiene un solo nivel (inventario.md INV-07).
            if (padre.esPresentacion()) {
                throw DomainException.throwException(409,
                        "El producto '" + padre.getNombre() + "' es una presentacion y no puede ser producto base de otra");
            }
        }

        Producto producto = Producto.crear(
                crearProductoRequest.getNombre(),
                crearProductoRequest.getPrecio(),
                crearProductoRequest.getStockMinimo(),
                crearProductoRequest.isVendible(),
                crearProductoRequest.isEsStockDirecto(),
                unidadActualId,
                tipoId,
                categoriaId,
                productoPadreId);

        Producto productoCreado = productoRepository.crear(producto);

        return ProductoResponse.createFromAggregate(productoCreado, unidadActual, tipo, categoria);
    }
}
