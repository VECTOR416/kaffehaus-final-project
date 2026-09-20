package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.TipoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerTipoPorIdUseCase {
    private final ITipoRepository tipoRepository;

    public ObtenerTipoPorIdUseCase(
            ITipoRepository tipoRepository) {
        this.tipoRepository = tipoRepository;
    }

    public TipoResponse execute(String tipoId) {
        TipoId id = new TipoId(tipoId);

        Tipo tipo = tipoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo con id '" + tipoId + "' no existe"));

        return TipoResponse.createFromAggregate(tipo);
    }
}
