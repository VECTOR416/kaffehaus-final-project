package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Correccion de una venta registrada: solo 'idCliente' (ventas.md VEN-31). Productos,
 * cantidades, importes, fecha, usuario y estado ya justificaron movimientos de inventario
 * inmutables; se corrigen anulando y registrando otra venta (VEN-30). Cualquier otro campo se
 * rechaza en lugar de ignorarse.
 */
public class ActualizarVentaRequest {
    private String idCliente;

    private ActualizarVentaRequest(String idCliente) {
        this.idCliente = idCliente;
    }

    public static ActualizarVentaRequest createFromRequest(Map<String, Object> request){
        for (Map.Entry<String, Object> campo : request.entrySet()) {
            if (!"idCliente".equals(campo.getKey()) && campo.getValue() != null) {
                throw DomainException.throwException(400, "El campo '" + campo.getKey()
                        + "' de una venta registrada no se modifica; anula la venta y registra otra");
            }
        }
        String idCliente = Validador.textoRequerido(request.get("idCliente"), "idCliente");

        return new ActualizarVentaRequest(idCliente);
    }

    public String getIdCliente() {return idCliente;}
}
