package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
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
    PaginatedResponse<List<Lote>> obtenerTodos(ObtenerLotesRequest obtenerLotesRequest);
    void eliminar(LoteId loteId);
}
