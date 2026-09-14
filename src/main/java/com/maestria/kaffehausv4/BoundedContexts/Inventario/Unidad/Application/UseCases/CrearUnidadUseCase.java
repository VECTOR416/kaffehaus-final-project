package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.CrearUnidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.UnidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;

@Service
@Transactional
public class CrearUnidadUseCase {
    private final IUnidadRepository unidadRepository;

    public CrearUnidadUseCase(
            IUnidadRepository unidadRepository) {
        this.unidadRepository = unidadRepository;
    }

    public UnidadResponse execute(CrearUnidadRequest crearUnidadRequest) {
        Unidad unidad = Unidad.crear(
                crearUnidadRequest.getUnidad(),
                crearUnidadRequest.getAbreviatura(),
                crearUnidadRequest.isEsBorrable());

        Unidad unidadCreada = unidadRepository.crear(unidad);

        return UnidadResponse.createFromAggregate(unidadCreada);
    }
}
