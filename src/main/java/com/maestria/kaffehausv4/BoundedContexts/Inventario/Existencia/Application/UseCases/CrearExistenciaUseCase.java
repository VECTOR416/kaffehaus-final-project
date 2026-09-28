package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.CrearExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Se conserva para mantener el contrato de cinco operaciones del controller, pero una
 * existencia no se da de alta a mano (inventario.md INV-12): sin movimiento que la explique
 * romperia el libro mayor. Nace solo al abrir una unidad cerrada
 * (POST api/v1/lote/detalles/{id}/apertura) o al registrar una produccion.
 */
@Service
@Transactional
public class CrearExistenciaUseCase {

    public ExistenciaResponse execute(CrearExistenciaRequest crearExistenciaRequest) {
        throw DomainException.throwException(409,
                "Las existencias no se crean a mano: nacen al abrir una unidad cerrada de un lote o al registrar una produccion");
    }
}
