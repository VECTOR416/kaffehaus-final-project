package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de un lote y sus lineas. Se rechaza si alguna linea tiene existencias con
 * contenido: su stock quedaria sin origen (inventario.md INV-63). Primero se consumen o se
 * dan de baja con merma o vencimiento.
 */
@Service
@Transactional
public class EliminarLoteUseCase {
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;

    public EliminarLoteUseCase(ILoteRepository loteRepository, IExistenciaRepository existenciaRepository) {
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
    }

    public void execute(String loteId) {
        LoteId id = new LoteId(loteId);

        Lote lote = loteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId + "' no existe"));

        if (!existenciaRepository.obtenerDisponiblesPorDetallesLote(
                lote.getDetallesActivos().stream().map(DetalleLote::getId).toList()).isEmpty()) {
            throw DomainException.throwException(409,
                    "El lote con id '" + loteId + "' tiene existencias con contenido y no se puede dar de baja");
        }

        loteRepository.eliminar(id);
    }
}
