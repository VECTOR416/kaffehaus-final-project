package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.CrearMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.MovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearMovimientoUseCase {
    private final IMovimientoRepository movimientoRepository;
    private final IExistenciaRepository existenciaRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final IUsuarioRepository usuarioRepository;

    public CrearMovimientoUseCase(
            IMovimientoRepository movimientoRepository,
            IExistenciaRepository existenciaRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            IUsuarioRepository usuarioRepository) {
        this.movimientoRepository = movimientoRepository;
        this.existenciaRepository = existenciaRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public MovimientoResponse execute(CrearMovimientoRequest crearMovimientoRequest) {
        ExistenciaId existenciaId = new ExistenciaId(crearMovimientoRequest.getIdExistencia());
        existenciaRepository.obtenerPorId(existenciaId)
                .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + crearMovimientoRequest.getIdExistencia() + "' no existe"));
        TipoMovimientoId tipoMovimientoId = new TipoMovimientoId(crearMovimientoRequest.getIdTipoMovimiento());
        TipoMovimiento tipoMovimiento = tipoMovimientoRepository.obtenerPorId(tipoMovimientoId)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento con id '" + crearMovimientoRequest.getIdTipoMovimiento() + "' no existe"));

        UsuarioId usuarioId = new UsuarioId(crearMovimientoRequest.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + crearMovimientoRequest.getIdUsuario() + "' no existe"));

        Movimiento movimiento = Movimiento.crear(
                crearMovimientoRequest.getCantidad(),
                crearMovimientoRequest.getFechaMovimiento(),
                crearMovimientoRequest.getMotivo(),
                existenciaId,
                tipoMovimientoId,
                usuarioId);

        Movimiento movimientoCreado = movimientoRepository.crear(movimiento);

        return MovimientoResponse.createFromAggregate(movimientoCreado, tipoMovimiento);
    }
}
