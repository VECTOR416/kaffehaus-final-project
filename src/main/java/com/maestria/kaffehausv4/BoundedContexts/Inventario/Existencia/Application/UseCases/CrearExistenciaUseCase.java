package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
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
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearExistenciaUseCase {
    private final IExistenciaRepository existenciaRepository;
    private final ILoteRepository loteRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IUnidadRepository unidadRepository;
    private final IUsuarioRepository usuarioRepository;

    public CrearExistenciaUseCase(
            IExistenciaRepository existenciaRepository,
            ILoteRepository loteRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IUnidadRepository unidadRepository,
            IUsuarioRepository usuarioRepository) {
        this.existenciaRepository = existenciaRepository;
        this.loteRepository = loteRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.unidadRepository = unidadRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public ExistenciaResponse execute(CrearExistenciaRequest crearExistenciaRequest) {
        DetalleLoteId detalleLoteId = new DetalleLoteId(crearExistenciaRequest.getIdDetalleLote());
        loteRepository.obtenerPorDetalleId(detalleLoteId)
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + crearExistenciaRequest.getIdDetalleLote() + "' no existe"));
        EstadoExistenciaId estadoExistenciaId = new EstadoExistenciaId(crearExistenciaRequest.getIdEstadoExistencia());
        EstadoExistencia estadoExistencia = estadoExistenciaRepository.obtenerPorId(estadoExistenciaId)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia con id '" + crearExistenciaRequest.getIdEstadoExistencia() + "' no existe"));
        UnidadId unidadId = new UnidadId(crearExistenciaRequest.getIdUnidad());
        Unidad unidad = unidadRepository.obtenerPorId(unidadId)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + crearExistenciaRequest.getIdUnidad() + "' no existe"));

        UsuarioId usuarioId = null;
        if (crearExistenciaRequest.getIdUsuario() != null) {
            usuarioId = new UsuarioId(crearExistenciaRequest.getIdUsuario());
            usuarioRepository.obtenerPorId(usuarioId)
                    .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + crearExistenciaRequest.getIdUsuario() + "' no existe"));
        }

        Existencia existencia = Existencia.crear(
                crearExistenciaRequest.getContenidoInicial(),
                crearExistenciaRequest.getContenidoActual(),
                crearExistenciaRequest.getFechaAcabado(),
                crearExistenciaRequest.getFechaVencimiento(),
                crearExistenciaRequest.getFechaApertura(),
                detalleLoteId,
                estadoExistenciaId,
                unidadId,
                usuarioId);

        Existencia existenciaCreada = existenciaRepository.crear(existencia);

        return ExistenciaResponse.createFromAggregate(existenciaCreada, estadoExistencia, unidad);
    }
}
