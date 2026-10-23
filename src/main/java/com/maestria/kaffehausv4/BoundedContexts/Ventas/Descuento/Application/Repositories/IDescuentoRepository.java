package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ObtenerDescuentosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IDescuentoRepository {
    Descuento crear(Descuento descuento);
    Descuento actualizar(DescuentoId descuentoId, Descuento descuento);
    Optional<Descuento> obtenerPorId(DescuentoId descuentoId);
    PaginatedResponse<List<Descuento>> obtenerTodos(ObtenerDescuentosRequest obtenerDescuentosRequest);
    void eliminar(DescuentoId descuentoId);

    /**
     * Descuentos vivos y activos del producto cuya ventana contiene la fecha (ventas.md VEN-14).
     * La venta elige entre ellos el mas favorable para el cliente.
     */
    List<Descuento> obtenerVigentes(ProductoId productoId, LocalDateTime fecha);

    /** Hay un descuento vivo de ese tipo (logica.md GENERAL-18). */
    boolean existeConTipoDescuento(TipoDescuentoId tipoDescuentoId);

    /** Descuentos vivos de un producto, activos o no: los que caen con el (logica.md DESCUENTO-05). */
    List<Descuento> obtenerPorProducto(ProductoId productoId);
}
