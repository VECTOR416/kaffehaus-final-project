package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.CrearProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ProduccionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories.IProduccionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearProduccionUseCase {
    private final IProduccionRepository produccionRepository;
    private final IRecetaRepository recetaRepository;

    public CrearProduccionUseCase(
            IProduccionRepository produccionRepository,
            IRecetaRepository recetaRepository) {
        this.produccionRepository = produccionRepository;
        this.recetaRepository = recetaRepository;
    }

    public ProduccionResponse execute(CrearProduccionRequest crearProduccionRequest) {
        RecetaId recetaId = new RecetaId(crearProduccionRequest.getIdReceta());
        recetaRepository.obtenerPorId(recetaId)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + crearProduccionRequest.getIdReceta() + "' no existe"));

        Produccion produccion = Produccion.crear(
                crearProduccionRequest.getCantidadEsperada(),
                crearProduccionRequest.getCantidadReal(),
                crearProduccionRequest.getDiferencia(),
                crearProduccionRequest.getMotivoDiferencia(),
                crearProduccionRequest.getFechaProduccion(),
                recetaId);

        Produccion produccionCreada = produccionRepository.crear(produccion);

        return ProduccionResponse.createFromAggregate(produccionCreada);
    }
}
