package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de un lote y sus lineas. Se rechaza si alguna linea tiene existencias con
 * contenido: su stock quedaria sin origen (logica.md LOTE-07). Primero se consumen o se ajustan
 * con un pesaje. Las unidades del lote dejan de contar en el stock de sus productos (PRODUCTO-16).
 */
@Service
@Transactional
public class EliminarLoteUseCase {
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public EliminarLoteUseCase(
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public void execute(String loteId) {
        LoteId id = new LoteId(loteId);

        // Bloqueado por su raiz: ninguna operacion descuenta de sus lineas mientras se da de baja (LOTE-27).
        Lote lote = loteRepository.obtenerPorIdParaActualizar(id)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId + "' no existe"));

        if (!existenciaRepository.obtenerDisponiblesPorDetallesLote(
                lote.getDetallesActivos().stream().map(DetalleLote::getId).toList()).isEmpty()) {
            throw DomainException.throwException(409,
                    "El lote con id '" + loteId + "' tiene existencias con contenido y no se puede dar de baja");
        }

        loteRepository.eliminar(id);
        CrearLoteUseCase.productosDe(lote).forEach(recalcularStockUseCase::execute);
    }
}
