package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarMermaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Merma de una existencia (inventario.md INV-42): descuenta la cantidad perdida con su motivo,
 * la deja 'Agotada' si se vacia, registra el movimiento 'Merma' y recalcula el stock del
 * producto base. La existencia se bloquea antes de leerla (INV-52).
 */
@Service
@Transactional
public class RegistrarMermaUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String TIPO_MERMA = "Merma";
    private static final String ESTADO_AGOTADA = "Agotada";

    private final IExistenciaRepository existenciaRepository;
    private final ILoteRepository loteRepository;
    private final IProductoRepository productoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IUnidadRepository unidadRepository;
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public RegistrarMermaUseCase(
            IExistenciaRepository existenciaRepository,
            ILoteRepository loteRepository,
            IProductoRepository productoRepository,
            IUsuarioRepository usuarioRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IUnidadRepository unidadRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.existenciaRepository = existenciaRepository;
        this.loteRepository = loteRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.unidadRepository = unidadRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public ExistenciaResponse execute(String existenciaId, RegistrarMermaRequest request) {
        UsuarioId usuarioId = usuarioValido(request.getIdUsuario());
        LocalDateTime ahora = LocalDateTime.now();

        Existencia existencia = existenciaBloqueada(existenciaId);
        existencia.consumir(request.getCantidad(), ahora, estado(ESTADO_AGOTADA).getId());
        existenciaRepository.actualizar(existencia.getId(), existencia);

        movimientoRepository.crear(Movimiento.crear(
                request.getCantidad(), ahora, request.getMotivo(), existencia.getId(), tipo(TIPO_MERMA).getId(), usuarioId, null));

        recalcularStockDe(existencia);
        return toResponse(existencia);
    }

    private Existencia existenciaBloqueada(String existenciaId) {
        return existenciaRepository.obtenerPorIdParaActualizar(new ExistenciaId(existenciaId))
                .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + existenciaId + "' no existe"));
    }

    private UsuarioId usuarioValido(String idUsuario) {
        UsuarioId usuarioId = new UsuarioId(idUsuario);
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + idUsuario + "' no existe"));
        return usuarioId;
    }

    private TipoMovimiento tipo(String nombre) {
        return tipoMovimientoRepository.obtenerPorNombre(nombre)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + nombre + "' no esta registrado"));
    }

    private EstadoExistencia estado(String nombre) {
        return estadoExistenciaRepository.obtenerPorNombre(nombre)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia '" + nombre + "' no esta registrado"));
    }

    /** El stock que cambia es el del producto base que contiene la existencia (INV-08, INV-44). */
    private void recalcularStockDe(Existencia existencia) {
        Lote lote = loteRepository.obtenerPorDetalleId(existencia.getDetalleLoteId())
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote de la existencia no existe"));
        Producto producto = productoRepository.obtenerPorId(lote.obtenerDetalle(existencia.getDetalleLoteId().getValue()).getProductoId())
                .orElseThrow(() -> DomainException.throwException(404, "El producto de la existencia no existe"));
        recalcularStockUseCase.execute(producto.productoBaseId().getValue());
    }

    private ExistenciaResponse toResponse(Existencia existencia) {
        EstadoExistencia estadoExistencia = estadoExistenciaRepository.obtenerPorId(existencia.getEstadoExistenciaId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia asociado a la existencia no existe"));
        Unidad unidad = unidadRepository.obtenerPorId(existencia.getUnidadId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a la existencia no existe"));
        return ExistenciaResponse.createFromAggregate(existencia, estadoExistencia, unidad);
    }
}
