package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Un pago del payload de alta de una venta. Viaja anidado dentro de CrearVentaRequest.
 *
 * 'montoRecibido' es opcional (se entiende pago exacto) y solo puede superar al monto en
 * efectivo. El 'cambio' lo calcula el sistema: si llega se rechaza (arquitectura.md 4.11, CAJ-12).
 */
public class PagoVentaRequest {
    private String idTipoPago;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private String referencia;

    private PagoVentaRequest(String idTipoPago, BigDecimal monto, BigDecimal montoRecibido, String referencia) {
        this.idTipoPago = idTipoPago;
        this.monto = monto;
        this.montoRecibido = montoRecibido;
        this.referencia = referencia;
    }

    public static PagoVentaRequest createFromRequest(Map<String, Object> request) {
        if (request.get("cambio") != null) {
            throw DomainException.throwException(400, "El campo 'cambio' de un pago lo calcula el sistema; no se envia");
        }
        String idTipoPago = Validador.textoRequerido(request.get("idTipoPago"), "idTipoPago");
        BigDecimal monto = BigDecimal.valueOf(Validador.numeroRequerido(request.get("monto"), "monto"));
        Double recibido = Validador.numeroOpcional(request.get("montoRecibido"), "montoRecibido");
        BigDecimal montoRecibido = recibido != null ? BigDecimal.valueOf(recibido) : null;
        String referencia = Validador.textoOpcional(request.get("referencia"), "referencia");

        return new PagoVentaRequest(idTipoPago, monto, montoRecibido, referencia);
    }

    public String getIdTipoPago() {return idTipoPago;}
    public BigDecimal getMonto() {return monto;}
    public BigDecimal getMontoRecibido() {return montoRecibido;}
    public String getReferencia() {return referencia;}
}
