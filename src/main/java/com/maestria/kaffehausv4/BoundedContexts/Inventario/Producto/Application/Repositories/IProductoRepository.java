package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ObtenerProductosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IProductoRepository {
    Producto crear(Producto producto);
    Producto actualizar(ProductoId productoId, Producto producto);
    Optional<Producto> obtenerPorId(ProductoId productoId);
    PaginatedResponse<List<Producto>> obtenerTodos(ObtenerProductosRequest obtenerProductosRequest);
    void eliminar(ProductoId productoId);

    /** Presentaciones vivas de un producto base: las que lo tienen por padre (INV-06). */
    List<Producto> obtenerPresentaciones(ProductoId productoBaseId);

    /**
     * Lectura con bloqueo pesimista (SELECT ... FOR UPDATE). Serializa las operaciones que
     * recalculan el stock de un mismo producto (inventario.md INV-52).
     */
    Optional<Producto> obtenerPorIdParaActualizar(ProductoId productoId);

    /** Persiste solo el stock proyectado, sin tocar el resto de campos. */
    void actualizarStock(Producto producto);

    /** Hay un producto vivo de ese tipo (logica.md GENERAL-18). */
    boolean existeConTipo(TipoId tipoId);

    /** Hay un producto vivo de esa categoria (logica.md GENERAL-18). */
    boolean existeConCategoria(CategoriaId categoriaId);

    /** Hay un producto vivo cuya unidad actual es esa (logica.md GENERAL-18). */
    boolean existeConUnidad(UnidadId unidadId);
}
