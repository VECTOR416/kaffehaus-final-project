package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Correccion de una produccion ya registrada. Solo 'motivoDiferencia' y 'fechaProduccion'.
 *
 * Las cantidades y la receta se rechazan en lugar de ignorarse: ya produjeron movimientos de
 * consumo y un lote, que son auditoria inmutable, y aceptarlas en silencio haria creer al
 * cliente que el inventario se recalculo.
 */
public class ActualizarProduccionRequest {
    private static final List<String> CAMPOS_INMUTABLES = List.of("cantidadEsperada", "cantidadReal", "diferencia", "idReceta");

    private String motivoDiferencia;
    private LocalDateTime fechaProduccion;

    private ActualizarProduccionRequest(
        String motivoDiferencia,
        LocalDateTime fechaProduccion
    ) {
        this.motivoDiferencia = motivoDiferencia;
        this.fechaProduccion = fechaProduccion;
    }

    public static ActualizarProduccionRequest createFromRequest(Map<String, Object> request){
        for (String campo : CAMPOS_INMUTABLES) {
            if (request.get(campo) != null) {
                throw DomainException.throwException(400,
                        "El campo '" + campo + "' de una produccion registrada no se puede modificar");
            }
        }

        String motivoDiferencia = Validador.textoOpcional(request.get("motivoDiferencia"), "motivoDiferencia");
        LocalDateTime fechaProduccion = Validador.fechaOpcional(request.get("fechaProduccion"), "fechaProduccion");

        return new ActualizarProduccionRequest(motivoDiferencia, fechaProduccion);
    }

    public String getMotivoDiferencia() {return motivoDiferencia;}
    public LocalDateTime getFechaProduccion() {return fechaProduccion;}
}
