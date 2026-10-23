package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DarDeBajaVencidoDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja por vencimiento de las unidades de una linea de lote (logica.md VENCIMIENTO-03). La decide
 * el usuario, Operador o Administrador: el sistema nunca da de baja por su cuenta.
 *
 * 1. Bloquea el lote por su raiz (LOTE-27) y la raiz deja la linea en 0, solo si esta vencida a
 *    hoy y le quedan unidades (409 si no).
 * 2. Registra sobre la linea un movimiento 'Vencimiento' por lo dado de baja (MOVIMIENTO-22).
 * 3. Recalcula el stock del producto de la linea, que se bloquea despues del lote (GENERAL-15).
 */
@Service
@Transactional
public class DarDeBajaVencidoDetalleLoteUseCase {
    private final ILoteRepository loteRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public DarDeBajaVencidoDetalleLoteUseCase(
            ILoteRepository loteRepository,
            IUsuarioRepository usuarioRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.loteRepository = loteRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public LoteResponse execute(String idDetalleLote, DarDeBajaVencidoDetalleLoteRequest request) {
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));
        TipoMovimiento vencimiento = tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.VENCIMIENTO)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + TipoMovimiento.VENCIMIENTO + "' no esta registrado"));

        LocalDateTime ahora = LocalDateTime.now();

        Lote lote = loteRepository.obtenerPorDetalleIdParaActualizar(new DetalleLoteId(idDetalleLote))
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + idDetalleLote + "' no existe"));
        DetalleLote detalle = lote.obtenerDetalle(idDetalleLote);

        BigDecimal baja = lote.darDeBajaVencidas(idDetalleLote, ahora.toLocalDate());
        Lote loteActualizado = loteRepository.actualizar(lote.getId(), lote);

        movimientoRepository.crear(Movimiento.registrarSobreLinea(
                baja, ahora, request.getMotivo(), detalle.getId(), vencimiento.getId(), usuarioId, null, null));
        recalcularStockUseCase.execute(detalle.getProductoId().getValue());

        return LoteResponse.createFromAggregate(loteActualizado);
    }
}
