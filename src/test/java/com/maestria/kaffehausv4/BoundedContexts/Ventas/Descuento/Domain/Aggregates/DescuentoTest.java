package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro del calculo de descuentos (ventas.md VEN-14). */
class DescuentoTest {

    private static final TipoDescuento PORCENTAJE = TipoDescuento.restaurar("d0000000-0000-0000-0000-000000000001", "Porcentaje", false);
    private static final TipoDescuento MONTO_FIJO = TipoDescuento.restaurar("d0000000-0000-0000-0000-000000000002", "Monto fijo", false);
    private static final TipoDescuento OTRO = TipoDescuento.restaurar("d0000000-0000-0000-0000-000000000003", "2x1", false);

    private Descuento descuento(String valor, TipoDescuento tipo) {
        return Descuento.crear(new BigDecimal(valor),
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59), true,
                new ProductoId("d0000000-0000-0000-0000-000000000009"), new TipoDescuentoId(tipo.getId().getValue()));
    }

    @Test
    void unPorcentajeSeAplicaSobreElImporteDeLaLinea() {
        assertEquals(new BigDecimal("2.50"), descuento("10", PORCENTAJE).importeSobre(new BigDecimal("12.50"), new BigDecimal("2"), PORCENTAJE));
    }

    @Test
    void unMontoFijoEsPorUnidadYNoSuperaElPrecio() {
        assertEquals(new BigDecimal("3.00"), descuento("1.50", MONTO_FIJO).importeSobre(new BigDecimal("10.00"), new BigDecimal("2"), MONTO_FIJO));
        assertEquals(new BigDecimal("20.00"), descuento("99", MONTO_FIJO).importeSobre(new BigDecimal("10.00"), new BigDecimal("2"), MONTO_FIJO));
    }

    @Test
    void unPorcentajeNoPuedeSuperarCien() {
        DomainException error = assertThrows(DomainException.class, () -> descuento("150", PORCENTAJE).validarCon(PORCENTAJE));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void unTipoDesconocidoNoSeAplica() {
        DomainException error = assertThrows(DomainException.class, () -> descuento("10", OTRO).validarCon(OTRO));

        assertEquals(409, error.getCodigo());
    }

    @Test
    void laVentanaDebeSerCoherente() {
        Descuento alReves = Descuento.crear(new BigDecimal("10"),
                LocalDateTime.of(2026, 9, 30, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0), true,
                new ProductoId("d0000000-0000-0000-0000-000000000009"), new TipoDescuentoId(PORCENTAJE.getId().getValue()));

        DomainException error = assertThrows(DomainException.class, () -> alReves.validarCon(PORCENTAJE));

        assertEquals(400, error.getCodigo());
    }
}
