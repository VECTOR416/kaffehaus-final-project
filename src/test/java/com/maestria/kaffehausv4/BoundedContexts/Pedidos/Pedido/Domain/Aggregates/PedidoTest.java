package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro de Pedido. */
class PedidoTest {

    private static final String ID = "b1000000-0000-0000-0000-000000000001";
    private static final LocalDateTime ENTREGA = LocalDateTime.of(2026, 10, 12, 18, 30);
    private static final VentaId VENTA = new VentaId("b1000000-0000-0000-0000-000000000002");
    private static final EstadoPedidoId RECIBIDO = new EstadoPedidoId("b1000000-0000-0000-0000-000000000003");

    private static Pedido pedido() {
        return Pedido.restaurar(ID, ENTREGA, new BigDecimal("-17.78"), new BigDecimal("-63.18"), "Sin azucar", VENTA, RECIBIDO, false);
    }

    @Test
    void crearNaceSinIdYSinBorrar() {
        // Act
        Pedido pedido = Pedido.crear(ENTREGA, new BigDecimal("-17.78"), new BigDecimal("-63.18"), "Sin azucar", VENTA, RECIBIDO);

        // Assert
        assertNull(pedido.getId());
        assertEquals(ENTREGA, pedido.getFechaEntrega());
        assertEquals(new BigDecimal("-17.78"), pedido.getLatitud());
        assertEquals(new BigDecimal("-63.18"), pedido.getLongitud());
        assertEquals("Sin azucar", pedido.getDescripcion());
        assertSame(VENTA, pedido.getVentaId());
        assertSame(RECIBIDO, pedido.getEstadoId());
        assertFalse(pedido.isBorrado());
    }

    @Test
    void actualizarConTodoNuloNoCambiaNada() {
        // Arrange
        Pedido pedido = pedido();

        // Act
        pedido.actualizar(null, null, null, null, null, null);

        // Assert
        assertEquals(ENTREGA, pedido.getFechaEntrega());
        assertEquals("Sin azucar", pedido.getDescripcion());
        assertSame(VENTA, pedido.getVentaId());
        assertSame(RECIBIDO, pedido.getEstadoId());
    }

    @Test
    void actualizarCambiaElEstadoSinTocarElResto() {
        // Arrange
        Pedido pedido = pedido();
        EstadoPedidoId enCamino = new EstadoPedidoId("b1000000-0000-0000-0000-000000000004");

        // Act
        pedido.actualizar(null, null, null, null, null, enCamino);

        // Assert
        assertSame(enCamino, pedido.getEstadoId());
        assertEquals(ENTREGA, pedido.getFechaEntrega());
        assertSame(VENTA, pedido.getVentaId());
    }

    @Test
    void actualizarCambiaLosDatosDeEntrega() {
        // Arrange
        Pedido pedido = pedido();
        LocalDateTime nuevaEntrega = ENTREGA.plusHours(2);

        // Act
        pedido.actualizar(nuevaEntrega, new BigDecimal("-17.80"), new BigDecimal("-63.20"), "Tocar el timbre", null, null);

        // Assert
        assertEquals(nuevaEntrega, pedido.getFechaEntrega());
        assertEquals(new BigDecimal("-17.80"), pedido.getLatitud());
        assertEquals(new BigDecimal("-63.20"), pedido.getLongitud());
        assertEquals("Tocar el timbre", pedido.getDescripcion());
    }

    @Test
    void unPedidoNoSeDaDeBaja() {
        // Arrange
        Pedido pedido = pedido();

        // Act
        DomainException error = assertThrows(DomainException.class, pedido::eliminar);

        // Assert
        assertEquals(409, error.getCodigo());
        assertFalse(pedido.isBorrado());
    }
}
