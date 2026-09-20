package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarTipoUseCase {
    private final ITipoRepository tipoRepository;

    public EliminarTipoUseCase(ITipoRepository tipoRepository) {
        this.tipoRepository = tipoRepository;
    }

    public void execute(String tipoId) {
        TipoId id = new TipoId(tipoId);

        tipoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo con id '" + tipoId + "' no existe"));

        tipoRepository.eliminar(id);
    }
}
