package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ILoteRepository {
    Lote crear(Lote lote);
    Lote actualizar(LoteId loteId, Lote lote);
    Optional<Lote> obtenerPorId(LoteId loteId);

    /**
     * Devuelve el lote al que pertenece una linea. Es la unica forma que tiene el exterior de
     * alcanzar un DetalleLote ahora que dejo de ser raiz: siempre a traves de su agregado.
     * Lo usa Existencia para validar la linea de la que sale su stock.
     */
    Optional<Lote> obtenerPorDetalleId(DetalleLoteId detalleLoteId);

    /**
     * Lotes vivos con al menos una linea viva de alguno de los productos. Es el punto de
     * partida para localizar las existencias y las unidades cerradas de un insumo base y de
     * sus presentaciones, y el que da el costo de cada una.
     */
    List<Lote> obtenerPorProductos(List<ProductoId> productoIds);

    /**
     * Los mismos lotes que obtenerPorProductos, leidos sin cargarlos en el contexto de
     * persistencia: una lectura con bloqueo posterior de sus lineas
     * (obtenerPorDetalleIdParaActualizar) trae su estado vigente y no el leido aqui. La usa la
     * salida de inventario para planificar antes de bloquear (logica.md GENERAL-14).
     */
    List<Lote> obtenerPorProductosSinCargar(List<ProductoId> productoIds);

    /**
     * Como obtenerPorDetalleId, pero bloqueando el lote por su raiz (SELECT ... FOR UPDATE) antes
     * de leer sus lineas (logica.md LOTE-27). Lo usa la apertura para que dos usuarios no abran
     * la misma unidad.
     */
    Optional<Lote> obtenerPorDetalleIdParaActualizar(DetalleLoteId detalleLoteId);

    /**
     * El lote entero, bloqueado por su raiz antes de leer sus lineas (logica.md LOTE-27). Lo usa
     * toda operacion que modifica un lote existente.
     */
    Optional<Lote> obtenerPorIdParaActualizar(LoteId loteId);

    /**
     * Varios lotes bloqueados por su raiz en orden de id, en una sola lectura (logica.md
     * GENERAL-15, LOTE-27). Los que no existen o estan dados de baja no se devuelven.
     */
    List<Lote> obtenerPorIdsParaActualizar(List<LoteId> loteIds);

    /** El lote de una linea viva, sin cargar la linea: para saber que lote bloquear (5.7). */
    Optional<LoteId> obtenerLoteDeDetalle(DetalleLoteId detalleLoteId);

    /**
     * El producto de una linea viva, sin cargarla: el reintegro lo necesita para ordenar los
     * bloqueos por producto (logica.md GENERAL-15) antes de bloquear su lote.
     */
    Optional<ProductoId> obtenerProductoDeDetalle(DetalleLoteId detalleLoteId);
    PaginatedResponse<List<Lote>> obtenerTodos(ObtenerLotesRequest obtenerLotesRequest);
    void eliminar(LoteId loteId);
}
