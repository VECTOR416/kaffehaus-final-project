package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ActualizarTipoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.TipoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarTipoUseCase {
    private final ITipoRepository tipoRepository;

    public ActualizarTipoUseCase(
            ITipoRepository tipoRepository) {
        this.tipoRepository = tipoRepository;
    }

    public TipoResponse execute(String tipoId, ActualizarTipoRequest actualizarTipoRequest) {
        TipoId id = new TipoId(tipoId);

        Tipo tipo = tipoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo con id '" + tipoId + "' no existe"));

        tipo.actualizar(
                actualizarTipoRequest.getNombre());

        Tipo tipoActualizada = tipoRepository.actualizar(id, tipo);

        return TipoResponse.createFromAggregate(tipoActualizada);
    }
}
