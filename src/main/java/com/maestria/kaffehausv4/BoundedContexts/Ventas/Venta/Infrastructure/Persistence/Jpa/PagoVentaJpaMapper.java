package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;

/** La FK id_venta la pone el datasource a partir de la raiz: el pago no la conoce. */
public class PagoVentaJpaMapper {
    private PagoVentaJpaMapper() {
    }

    public static PagoVentaJpaModel toPersistence(PagoVenta pago, UUID idVenta) {
        PagoVentaJpaModel model = new PagoVentaJpaModel();
        model.setId(pago.getId() != null ? UUID.fromString(pago.getId().getValue()) : UUID.randomUUID());
        model.setIdVenta(idVenta);
        model.setIdTipoPago(UUID.fromString(pago.getTipoPagoId().getValue()));
        model.setMonto(pago.getMonto());
        model.setMontoRecibido(pago.getMontoRecibido());
        model.setCambio(pago.getCambio());
        model.setReferencia(pago.getReferencia());
        model.setBorrado(pago.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    /** Un pago es inmutable: solo puede cambiar su marca de baja. */
    public static void copiarCambios(PagoVenta pago, PagoVentaJpaModel model) {
        model.setBorrado(pago.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static PagoVenta toDomain(PagoVentaJpaModel model) {
        return PagoVenta.restaurar(
                model.getId().toString(),
                new TipoPagoId(model.getIdTipoPago().toString()),
                model.getMonto(),
                model.getMontoRecibido(),
                model.getCambio(),
                model.getReferencia(),
                model.getBorrado() != null && model.getBorrado());
    }
}
