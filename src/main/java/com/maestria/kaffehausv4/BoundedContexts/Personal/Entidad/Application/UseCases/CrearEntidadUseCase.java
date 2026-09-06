package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.CrearEntidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.EntidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;

@Service
@Transactional
public class CrearEntidadUseCase {
    private final IEntidadRepository entidadRepository;

    public CrearEntidadUseCase(IEntidadRepository entidadRepository) {
        this.entidadRepository = entidadRepository;
    }

    public EntidadResponse execute(CrearEntidadRequest crearEntidadRequest) {
        Entidad entidad = Entidad.crear(
                crearEntidadRequest.getNombre(),
                crearEntidadRequest.getNit(),
                crearEntidadRequest.getTelefono(),
                crearEntidadRequest.getDescripcion());

        Entidad entidadCreada = entidadRepository.crear(entidad);
        return EntidadResponse.createFromAggregate(entidadCreada);
    }
}
