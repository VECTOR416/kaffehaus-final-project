package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.CrearMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.MovimientoResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Se conserva para mantener el contrato de cinco operaciones del controller, pero un movimiento
 * no se da de alta a mano (logica.md MOVIMIENTO-17): solo lo genera la operacion que explica
 * (consumo de una receta, pesaje o apertura), y uno suelto romperia la trazabilidad de las
 * existencias.
 */
@Service
@Transactional
public class CrearMovimientoUseCase {

    public MovimientoResponse execute(CrearMovimientoRequest crearMovimientoRequest) {
        throw DomainException.throwException(409,
                "Los movimientos no se crean a mano: los generan las recetas, el pesaje y la apertura de una bolsa");
    }
}
