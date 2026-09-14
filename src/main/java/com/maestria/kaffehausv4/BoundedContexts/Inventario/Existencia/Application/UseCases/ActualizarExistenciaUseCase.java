package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Repositories.IDetalleLoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ActualizarExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarExistenciaUseCase {
    private final IExistenciaRepository existenciaRepository;
    private final IDetalleLoteRepository detalleLoteRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IUnidadRepository unidadRepository;

    public ActualizarExistenciaUseCase(
            IExistenciaRepository existenciaRepository,
            IDetalleLoteRepository detalleLoteRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IUnidadRepository unidadRepository) {
        this.existenciaRepository = existenciaRepository;
        this.detalleLoteRepository = detalleLoteRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.unidadRepository = unidadRepository;
    }

    public ExistenciaResponse execute(String existenciaId, ActualizarExistenciaRequest actualizarExistenciaRequest) {
        ExistenciaId id = new ExistenciaId(existenciaId);

        Existencia existencia = existenciaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + existenciaId + "' no existe"));

        DetalleLoteId detalleLoteId = null;
        if (actualizarExistenciaRequest.getIdDetalleLote() != null) {
            detalleLoteId = new DetalleLoteId(actualizarExistenciaRequest.getIdDetalleLote());
            detalleLoteRepository.obtenerPorId(detalleLoteId)
                    .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + actualizarExistenciaRequest.getIdDetalleLote() + "' no existe"));
        }
        EstadoExistenciaId estadoExistenciaId = null;
        if (actualizarExistenciaRequest.getIdEstadoExistencia() != null) {
            estadoExistenciaId = new EstadoExistenciaId(actualizarExistenciaRequest.getIdEstadoExistencia());
            estadoExistenciaRepository.obtenerPorId(estadoExistenciaId)
                    .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia con id '" + actualizarExistenciaRequest.getIdEstadoExistencia() + "' no existe"));
        }
        UnidadId unidadId = null;
        if (actualizarExistenciaRequest.getIdUnidad() != null) {
            unidadId = new UnidadId(actualizarExistenciaRequest.getIdUnidad());
            unidadRepository.obtenerPorId(unidadId)
                    .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + actualizarExistenciaRequest.getIdUnidad() + "' no existe"));
        }

        existencia.actualizar(
                actualizarExistenciaRequest.getContenidoInicial(),
                actualizarExistenciaRequest.getContenidoActual(),
                actualizarExistenciaRequest.getFechaAcabado(),
                actualizarExistenciaRequest.getFechaVencimiento(),
                detalleLoteId,
                estadoExistenciaId,
                unidadId);

        Existencia existenciaActualizada = existenciaRepository.actualizar(id, existencia);

        return toResponse(existenciaActualizada);
    }

    private ExistenciaResponse toResponse(Existencia existencia) {
        EstadoExistencia estadoExistencia = estadoExistenciaRepository.obtenerPorId(existencia.getEstadoExistenciaId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia asociado a la existencia no existe"));
        Unidad unidad = unidadRepository.obtenerPorId(existencia.getUnidadId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a la existencia no existe"));
        return ExistenciaResponse.createFromAggregate(existencia, estadoExistencia, unidad);
    }
}
