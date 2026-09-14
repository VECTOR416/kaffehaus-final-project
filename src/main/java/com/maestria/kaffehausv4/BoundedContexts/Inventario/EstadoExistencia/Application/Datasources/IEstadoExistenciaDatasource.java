package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.ObtenerEstadosExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IEstadoExistenciaDatasource {
    EstadoExistencia crear(EstadoExistencia estadoExistencia);
    EstadoExistencia actualizar(EstadoExistenciaId estadoExistenciaId, EstadoExistencia estadoExistencia);
    Optional<EstadoExistencia> obtenerPorId(EstadoExistenciaId estadoExistenciaId);
    PaginatedResponse<List<EstadoExistencia>> obtenerTodos(ObtenerEstadosExistenciaRequest obtenerEstadosExistenciaRequest);
    void eliminar(EstadoExistenciaId estadoExistenciaId);
}
