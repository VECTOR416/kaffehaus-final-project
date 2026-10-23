package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ActualizarProductoRequest;
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
public class ActualizarProductoUseCase {
    private final IProductoRepository productoRepository;
    private final IUnidadRepository unidadRepository;
    private final ITipoRepository tipoRepository;
    private final ICategoriaRepository categoriaRepository;
    private final ILoteRepository loteRepository;

    public ActualizarProductoUseCase(
            IProductoRepository productoRepository,
            IUnidadRepository unidadRepository,
            ITipoRepository tipoRepository,
            ICategoriaRepository categoriaRepository,
            ILoteRepository loteRepository) {
        this.productoRepository = productoRepository;
        this.unidadRepository = unidadRepository;
        this.tipoRepository = tipoRepository;
        this.categoriaRepository = categoriaRepository;
        this.loteRepository = loteRepository;
    }

    public ProductoResponse execute(String productoId, ActualizarProductoRequest actualizarProductoRequest) {
        ProductoId id = new ProductoId(productoId);

        Producto producto = productoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + productoId + "' no existe"));

        UnidadId unidadActualId = null;
        if (actualizarProductoRequest.getIdUnidadActual() != null) {
            unidadActualId = new UnidadId(actualizarProductoRequest.getIdUnidadActual());
            unidadRepository.obtenerPorId(unidadActualId)
                    .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + actualizarProductoRequest.getIdUnidadActual() + "' no existe"));
        }
        TipoId tipoId = null;
        if (actualizarProductoRequest.getIdTipo() != null) {
            tipoId = new TipoId(actualizarProductoRequest.getIdTipo());
            tipoRepository.obtenerPorId(tipoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El tipo con id '" + actualizarProductoRequest.getIdTipo() + "' no existe"));
        }
        CategoriaId categoriaId = null;
        if (actualizarProductoRequest.getIdCategoria() != null) {
            categoriaId = new CategoriaId(actualizarProductoRequest.getIdCategoria());
            categoriaRepository.obtenerPorId(categoriaId)
                    .orElseThrow(() -> DomainException.throwException(404, "La categoria con id '" + actualizarProductoRequest.getIdCategoria() + "' no existe"));
        }
        ProductoId productoPadreId = null;
        if (actualizarProductoRequest.getIdProductoPadre() != null) {
            productoPadreId = new ProductoId(actualizarProductoRequest.getIdProductoPadre());
            Producto padre = productoRepository.obtenerPorId(productoPadreId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + actualizarProductoRequest.getIdProductoPadre() + "' no existe"));
            exigirJerarquiaDeUnNivel(producto, padre);
        }

        exigirSinInventarioSiCambiaSuManejo(producto, actualizarProductoRequest);

        producto.actualizar(
                actualizarProductoRequest.getNombre(),
                actualizarProductoRequest.getPrecio(),
                actualizarProductoRequest.getStockMinimo(),
                actualizarProductoRequest.getVendible(),
                actualizarProductoRequest.getEsStockDirecto(),
                unidadActualId,
                tipoId,
                categoriaId,
                productoPadreId);

        Producto productoActualizado = productoRepository.actualizar(id, producto);

        return toResponse(productoActualizado);
    }

    /**
     * 'esStockDirecto', 'unidadActual' y 'productoPadre' deciden como se maneja el inventario de un
     * producto; no cambian si ya tiene stock, lineas de lote vivas o presentaciones (logica.md
     * PRODUCTO-15). Sus existencias cuelgan de esas lineas o de sus presentaciones, asi que tambien
     * quedan cubiertas.
     */
    private void exigirSinInventarioSiCambiaSuManejo(Producto producto, ActualizarProductoRequest request) {
        boolean cambiaNaturaleza = request.getEsStockDirecto() != null
                && request.getEsStockDirecto() != producto.isEsStockDirecto();
        boolean cambiaUnidad = request.getIdUnidadActual() != null
                && !request.getIdUnidadActual().equals(producto.getUnidadActualId().getValue());
        boolean cambiaPadre = request.getIdProductoPadre() != null
                && (producto.getProductoPadreId() == null
                        || !request.getIdProductoPadre().equals(producto.getProductoPadreId().getValue()));
        if (!cambiaNaturaleza && !cambiaUnidad && !cambiaPadre) {
            return;
        }
        if (tieneInventario(producto)) {
            throw DomainException.throwException(409,
                    "El producto '" + producto.getNombre() + "' ya tiene inventario: no se puede cambiar si es de stock directo, su unidad ni su producto base");
        }
    }

    private boolean tieneInventario(Producto producto) {
        if (producto.getStock() != null && producto.getStock().signum() > 0) {
            return true;
        }
        if (!productoRepository.obtenerPresentaciones(producto.getId()).isEmpty()) {
            return true;
        }
        String idProducto = producto.getId().getValue();
        return loteRepository.obtenerPorProductos(List.of(producto.getId())).stream()
                .filter(lote -> !lote.isBorrado())
                .flatMap(lote -> lote.getDetallesActivos().stream())
                .anyMatch(detalle -> Objects.equals(idProducto, detalle.getProductoId().getValue()));
    }

    /** La jerarquia presentacion -> producto base tiene un solo nivel (inventario.md INV-07). */
    private void exigirJerarquiaDeUnNivel(Producto producto, Producto padre) {
        if (padre.getId().getValue().equals(producto.getId().getValue())) {
            throw DomainException.throwException(409, "Un producto no puede ser su propio producto base");
        }
        if (padre.esPresentacion()) {
            throw DomainException.throwException(409,
                    "El producto '" + padre.getNombre() + "' es una presentacion y no puede ser producto base de otra");
        }
        if (!productoRepository.obtenerPresentaciones(producto.getId()).isEmpty()) {
            throw DomainException.throwException(409,
                    "El producto '" + producto.getNombre() + "' es producto base de otras presentaciones y no puede tener padre");
        }
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
