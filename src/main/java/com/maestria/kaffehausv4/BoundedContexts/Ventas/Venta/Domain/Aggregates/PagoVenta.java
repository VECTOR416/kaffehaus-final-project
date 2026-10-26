package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.PagoVentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Un pago de la venta (arquitectura.md 4.11, CAJ-10..CAJ-13). Una venta puede pagarse con varios: parte en
 * efectivo y parte con tarjeta, por ejemplo. Entidad hija de Venta: solo la raiz la crea.
 *
 * 'monto' es lo que el pago aporta al total. Solo el efectivo admite recibir mas y dar cambio;
 * cualquier otro medio cobra exactamente su monto. El cambio es derivado: nunca se recibe.
 */
public class PagoVenta {

    private PagoVentaId id;
    private TipoPagoId tipoPagoId;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private BigDecimal cambio;
    private String referencia;
    private boolean borrado;

    private PagoVenta(
        PagoVentaId id,
        TipoPagoId tipoPagoId,
        BigDecimal monto,
        BigDecimal montoRecibido,
        BigDecimal cambio,
        String referencia,
        boolean borrado
    ){
        this.id = id;
        this.tipoPagoId = tipoPagoId;
        this.monto = monto;
        this.montoRecibido = montoRecibido;
        this.cambio = cambio;
        this.referencia = referencia;
        this.borrado = borrado;
    }

    public static PagoVenta restaurar(
        String id,
        TipoPagoId tipoPagoId,
        BigDecimal monto,
        BigDecimal montoRecibido,
        BigDecimal cambio,
        String referencia,
        boolean borrado
    ){
        return new PagoVenta(new PagoVentaId(id), tipoPagoId, monto, montoRecibido, cambio, referencia, borrado);
    }

    /**
     * Sin 'montoRecibido' se entiende que se pago exacto. En efectivo, lo recibido debe cubrir
     * el monto (CAJ-12); en cualquier otro medio, si llega, debe ser igual al monto (CAJ-13).
     */
    static PagoVenta crear(TipoPagoId tipoPagoId, boolean esEfectivo, BigDecimal monto, BigDecimal montoRecibido, String referencia) {
        if (tipoPagoId == null) {
            throw DomainException.throwException(400, "Cada pago debe indicar su tipo de pago");
        }
        if (monto == null || monto.signum() <= 0) {
            throw DomainException.throwException(400, "El monto de un pago debe ser mayor que cero");
        }
        BigDecimal importe = aEscalaMonetaria(monto);
        BigDecimal recibido = montoRecibido != null ? aEscalaMonetaria(montoRecibido) : importe;

        if (esEfectivo && recibido.compareTo(importe) < 0) {
            throw DomainException.throwException(400,
                    "El monto recibido (" + recibido + ") no cubre el pago en efectivo de " + importe);
        }
        if (!esEfectivo && recibido.compareTo(importe) != 0) {
            throw DomainException.throwException(400,
                    "Solo el pago en efectivo admite un monto recibido distinto del monto; este pago cobra " + importe);
        }

        return new PagoVenta(null, tipoPagoId, importe, recibido, recibido.subtract(importe), referencia, false);
    }

    private static BigDecimal aEscalaMonetaria(BigDecimal valor) {
        return valor.setScale(DetalleVenta.ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    public PagoVentaId getId() {return id;}
    public TipoPagoId getTipoPagoId() {return tipoPagoId;}
    public BigDecimal getMonto() {return monto;}
    public BigDecimal getMontoRecibido() {return montoRecibido;}
    public BigDecimal getCambio() {return cambio;}
    public String getReferencia() {return referencia;}
    public boolean isBorrado() {return borrado;}
}
