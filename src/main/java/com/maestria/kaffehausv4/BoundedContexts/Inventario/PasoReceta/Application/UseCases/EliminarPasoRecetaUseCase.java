package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Repositories.IPasoRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.ValueObjects.PasoRecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarPasoRecetaUseCase {
    private final IPasoRecetaRepository pasoRecetaRepository;

    public EliminarPasoRecetaUseCase(IPasoRecetaRepository pasoRecetaRepository) {
        this.pasoRecetaRepository = pasoRecetaRepository;
    }

    public void execute(String pasoRecetaId) {
        PasoRecetaId id = new PasoRecetaId(pasoRecetaId);

        pasoRecetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El paso de receta con id '" + pasoRecetaId + "' no existe"));

        pasoRecetaRepository.eliminar(id);
    }
}
