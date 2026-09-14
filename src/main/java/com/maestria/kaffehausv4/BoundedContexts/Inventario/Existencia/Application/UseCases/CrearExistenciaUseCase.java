package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Repositories.IDetalleLoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.CrearExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearExistenciaUseCase {
    private final IExistenciaRepository existenciaRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IUnidadRepository unidadRepository;

    public CrearExistenciaUseCase(
            IExistenciaRepository existenciaRepository,
            IDetalleLoteRepository detalleLoteRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IUnidadRepository unidadRepository) {
        this.existenciaRepository = existenciaRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.unidadRepository = unidadRepository;
    }

    public ExistenciaResponse execute(CrearExistenciaRequest crearExistenciaRequest) {
        DetalleLoteId detalleLoteId = new DetalleLoteId(crearExistenciaRequest.getIdDetalleLote());
        EstadoExistenciaId estadoExistenciaId = new EstadoExistenciaId(crearExistenciaRequest.getIdEstadoExistencia());
        EstadoExistencia estadoExistencia = estadoExistenciaRepository.obtenerPorId(estadoExistenciaId)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia con id '" + crearExistenciaRequest.getIdEstadoExistencia() + "' no existe"));
        UnidadId unidadId = new UnidadId(crearExistenciaRequest.getIdUnidad());
        Unidad unidad = unidadRepository.obtenerPorId(unidadId)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + crearExistenciaRequest.getIdUnidad() + "' no existe"));

        Existencia existencia = Existencia.crear(
                crearExistenciaRequest.getContenidoInicial(),
                crearExistenciaRequest.getContenidoActual(),
                crearExistenciaRequest.getFechaAcabado(),
                crearExistenciaRequest.getFechaVencimiento(),
                detalleLoteId,
                estadoExistenciaId,
                unidadId);

        Existencia existenciaCreada = existenciaRepository.crear(existencia);

        return ExistenciaResponse.createFromAggregate(existenciaCreada, estadoExistencia, unidad);
    }
}
