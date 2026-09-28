package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ILoteDatasource {
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
     * Como obtenerPorDetalleId, pero bloqueando la linea (SELECT ... FOR UPDATE) antes de
     * leerla. Lo usa la apertura para que dos usuarios no abran la misma unidad (INV-52).
     */
    Optional<Lote> obtenerPorDetalleIdParaActualizar(DetalleLoteId detalleLoteId);
    PaginatedResponse<List<Lote>> obtenerTodos(ObtenerLotesRequest obtenerLotesRequest);
    void eliminar(LoteId loteId);
}
