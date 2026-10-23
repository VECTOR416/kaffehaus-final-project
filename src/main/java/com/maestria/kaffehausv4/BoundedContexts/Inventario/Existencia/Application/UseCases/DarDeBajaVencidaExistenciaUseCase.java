package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.DarDeBajaVencidaExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
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
 * Baja por vencimiento del contenido de una existencia (logica.md VENCIMIENTO-03). La decide el
 * usuario, Operador o Administrador: el sistema nunca da de baja por su cuenta.
 *
 * 1. Bloquea la existencia (EXISTENCIA-14) y la deja en 0 y 'Agotada', solo si esta vencida a hoy
 *    y le queda contenido (409 si no).
 * 2. Registra sobre ella un movimiento 'Vencimiento' por lo dado de baja (MOVIMIENTO-22).
 * 3. Recalcula el stock del producto base al que pertenece (PRODUCTO-11), que se bloquea despues
 *    de la existencia (GENERAL-15).
 */
@Service
@Transactional
public class DarDeBajaVencidaExistenciaUseCase {
    // Fila de catalogo que el flujo resuelve por nombre (logica.md GENERAL-04).
    private static final String ESTADO_AGOTADA = "Agotada";

    private final IExistenciaRepository existenciaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ILoteRepository loteRepository;
    private final IProductoRepository productoRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IUnidadRepository unidadRepository;
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public DarDeBajaVencidaExistenciaUseCase(
            IExistenciaRepository existenciaRepository,
            IUsuarioRepository usuarioRepository,
            ILoteRepository loteRepository,
            IProductoRepository productoRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IUnidadRepository unidadRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.existenciaRepository = existenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.loteRepository = loteRepository;
        this.productoRepository = productoRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.unidadRepository = unidadRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public ExistenciaResponse execute(String existenciaId, DarDeBajaVencidaExistenciaRequest request) {
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));
        EstadoExistencia agotada = estadoExistenciaRepository.obtenerPorNombre(ESTADO_AGOTADA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia '" + ESTADO_AGOTADA + "' no esta registrado"));
        TipoMovimiento vencimiento = tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.VENCIMIENTO)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + TipoMovimiento.VENCIMIENTO + "' no esta registrado"));

        LocalDateTime ahora = LocalDateTime.now();
        ExistenciaId id = new ExistenciaId(existenciaId);

        Existencia existencia = existenciaRepository.obtenerPorIdParaActualizar(id)
                .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + existenciaId + "' no existe"));

        BigDecimal baja = existencia.darDeBajaVencida(ahora.toLocalDate(), ahora, agotada.getId());
        Existencia existenciaActualizada = existenciaRepository.actualizar(id, existencia);

        movimientoRepository.crear(Movimiento.registrarSobreExistencia(
                baja, ahora, request.getMotivo(), existencia.getId(), vencimiento.getId(), usuarioId, null, null));
        recalcularStockUseCase.execute(productoBaseDe(existencia).getId().getValue());

        Unidad unidad = unidadRepository.obtenerPorId(existenciaActualizada.getUnidadId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a la existencia no existe"));
        return ExistenciaResponse.createFromAggregate(existenciaActualizada, agotada, unidad);
    }

    /** Una existencia es del producto base de la presentacion que se abrio (PRODUCTO-14). */
    private Producto productoBaseDe(Existencia existencia) {
        Producto presentacion = loteRepository.obtenerProductoDeDetalle(existencia.getDetalleLoteId())
                .flatMap(productoRepository::obtenerPorId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto de la existencia con id '" + existencia.getId().getValue() + "' no existe"));
        if (!presentacion.esPresentacion()) {
            return presentacion;
        }
        return productoRepository.obtenerPorId(presentacion.productoBaseId())
                .orElseThrow(() -> DomainException.throwException(404, "El producto base de '" + presentacion.getNombre() + "' no existe"));
    }
}
