package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.TipoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.CrearTipoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;

@Service
@Transactional
public class CrearTipoUseCase {
    private final ITipoRepository tipoRepository;

    public CrearTipoUseCase(
            ITipoRepository tipoRepository) {
        this.tipoRepository = tipoRepository;
    }

    public TipoResponse execute(CrearTipoRequest crearTipoRequest) {
        Tipo tipo = Tipo.crear(
                crearTipoRequest.getNombre());

        Tipo tipoCreada = tipoRepository.crear(tipo);

        return TipoResponse.createFromAggregate(tipoCreada);
    }
}
