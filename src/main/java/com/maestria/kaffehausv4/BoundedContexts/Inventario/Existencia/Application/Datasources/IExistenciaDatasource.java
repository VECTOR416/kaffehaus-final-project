package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ObtenerExistenciasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IExistenciaDatasource {
    Existencia crear(Existencia existencia);
    Existencia actualizar(ExistenciaId existenciaId, Existencia existencia);
    Optional<Existencia> obtenerPorId(ExistenciaId existenciaId);
    PaginatedResponse<List<Existencia>> obtenerTodos(ObtenerExistenciasRequest obtenerExistenciasRequest);
    void eliminar(ExistenciaId existenciaId);

    /**
     * Existencias vivas con contenido de esas lineas de lote, en orden FEFO: primero la que
     * vence antes (las que no vencen, al final) y, a igual vencimiento, la mas antigua.
     */
    List<Existencia> obtenerDisponiblesPorDetallesLote(List<DetalleLoteId> detalleLoteIds);
}
