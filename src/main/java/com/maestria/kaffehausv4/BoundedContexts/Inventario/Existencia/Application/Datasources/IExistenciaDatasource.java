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

    /**
     * Las mismas existencias, en el mismo orden FEFO, bloqueadas (SELECT ... FOR UPDATE) para
     * descontarlas (inventario.md INV-52). Solo la usa el algoritmo de consumo.
     */
    List<Existencia> obtenerDisponiblesParaConsumo(List<DetalleLoteId> detalleLoteIds);

    /** Lectura con bloqueo de una existencia, para mermas, vencimientos y ajustes. */
    Optional<Existencia> obtenerPorIdParaActualizar(ExistenciaId existenciaId);
}
