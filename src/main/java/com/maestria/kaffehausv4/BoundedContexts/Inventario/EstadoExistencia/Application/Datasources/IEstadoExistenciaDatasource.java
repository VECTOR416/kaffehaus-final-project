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

    /**
     * Busca la fila viva por nombre, sin distinguir mayusculas. Existe porque el flujo de
     * inventario (produccion y venta) resuelve por nombre las filas de catalogo que necesita,
     * igual que la seguridad resuelve los roles: el catalogo sigue siendo datos, no un enum.
     */
    Optional<EstadoExistencia> obtenerPorNombre(String nombre);
}
