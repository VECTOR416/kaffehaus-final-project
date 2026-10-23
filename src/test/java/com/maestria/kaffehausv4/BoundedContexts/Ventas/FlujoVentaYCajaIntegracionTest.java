package com.maestria.kaffehausv4.BoundedContexts.Ventas;

import static com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria.linea;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ActualizarPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.CrearPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases.ActualizarPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases.CrearPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ArqueoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.CerrarSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RegistrarMovimientoCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.CerrarSesionCajaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.RegistrarMovimientoCajaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.AnularVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.TicketVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.AnularVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.CrearVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.ObtenerVentaPorTicketUseCase;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/**
 * Flujo de venta y caja contra la base (logica.md seccion 2): abrir caja, vender, anular con
 * reintegro, registrar egresos y cerrar con arqueo.
 *
 * Escenario: 10 gaseosas compradas a 2.00 y vendidas a 5.00, y un cafe con leche bajo demanda a
 * 8.00. La caja abre con 100.00.
 */
class FlujoVentaYCajaIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private EscenarioDeCafeteria escenario;
    @Autowired private CrearVentaUseCase crearVentaUseCase;
    @Autowired private AnularVentaUseCase anularVentaUseCase;
    @Autowired private ObtenerVentaPorTicketUseCase obtenerVentaPorTicketUseCase;
    @Autowired private RegistrarMovimientoCajaUseCase registrarMovimientoCajaUseCase;
    @Autowired private CerrarSesionCajaUseCase cerrarSesionCajaUseCase;
    @Autowired private CrearPedidoUseCase crearPedidoUseCase;
    @Autowired private ActualizarPedidoUseCase actualizarPedidoUseCase;
    @Autowired private IEstadoPedidoRepository estadoPedidoRepository;

    private String usuario;
    private String gaseosa;
    private String cafeConLeche;
    private String efectivo;

    @BeforeEach
    void prepararEscenario() {
        usuario = escenario.sembrar();
        gaseosa = escenario.producto("Gaseosa", "5", true, true, "Unidad", null);
        cafeConLeche = escenario.producto("Cafe con leche", "8", true, false, "Unidad", null);
        escenario.comprar(escenario.proveedor(), linea(gaseosa, "10", "2", LocalDate.now().plusDays(90).toString()));
        efectivo = escenario.idTipoPago("Efectivo");
        escenario.abrirCaja("100");
    }

    @SafeVarargs
    private VentaResponse vender(String recibido, Map<String, Object>... lineas) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> linea : lineas) {
            BigDecimal precio = linea.get("idProducto").equals(gaseosa) ? new BigDecimal("5") : new BigDecimal("8");
            total = total.add(precio.multiply(new BigDecimal(linea.get("cantidad").toString())));
        }
        Map<String, Object> datos = new HashMap<>();
        datos.put("detalles", List.of(lineas));
        datos.put("pagos", List.of(Map.of("idTipoPago", efectivo, "monto", total.toPlainString(), "montoRecibido", recibido)));
        return crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(datos, usuario));
    }

    private static Map<String, Object> de(String idProducto, String cantidad) {
        return Map.of("idProducto", idProducto, "cantidad", cantidad);
    }

    @Test
    void unaVentaDescuentaLasUnidadesVendidasYRecibeSuTicket() {
        // Act
        VentaResponse venta = vender("30", de(gaseosa, "2"), de(cafeConLeche, "1"));

        // Assert: 2 gaseosas a 5 + 1 cafe a 8 = 18; el cafe bajo demanda no descuenta nada.
        assertEquals(0, new BigDecimal("18").compareTo(venta.getTotal()));
        assertEquals(0, new BigDecimal("12").compareTo(venta.getCambio()));
        assertEquals("Completada", venta.getEstadoVenta().getNombre());
        assertNotNull(venta.getNumeroTicket());
        assertEquals(0, new BigDecimal("8").compareTo(escenario.stock(gaseosa)));
    }

    @Test
    void losTicketsSonCorrelativosYSeBuscanPorSuNumero() {
        // Act
        VentaResponse primera = vender("5", de(gaseosa, "1"));
        VentaResponse segunda = vender("5", de(gaseosa, "1"));
        TicketVentaResponse ticket = obtenerVentaPorTicketUseCase.execute(String.valueOf(segunda.getNumeroTicket()));

        // Assert
        assertEquals(primera.getNumeroTicket() + 1, segunda.getNumeroTicket());
        assertEquals(segunda.getId(), ticket.getIdVenta());
        assertNotNull(ticket.getNombreCajero());
    }

    @Test
    void sinUnidadesSuficientesLaVentaSeRechaza() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> vender("55", de(gaseosa, "11")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().startsWith("Stock insuficiente de 'Gaseosa'"));
    }

    @Test
    void anularConReintegroDevuelveLasUnidades() {
        // Arrange
        VentaResponse venta = vender("10", de(gaseosa, "2"));

        // Act
        VentaResponse anulada = anularVentaUseCase.execute(venta.getId(),
                AnularVentaRequest.createFromRequest(Map.of("motivo", "Cobro duplicado", "reintegrar", true), usuario));

        // Assert
        assertEquals("Anulada", anulada.getEstadoVenta().getNombre());
        assertEquals(0, new BigDecimal("10").compareTo(escenario.stock(gaseosa)));
    }

    @Test
    void elArqueoCuentaElEfectivoDeLasVentasCompletadasYLosMovimientosDeCaja() {
        // Arrange: una venta de 10 completada, otra de 5 anulada y un egreso de 30.
        vender("10", de(gaseosa, "2"));
        VentaResponse anulada = vender("5", de(gaseosa, "1"));
        anularVentaUseCase.execute(anulada.getId(), AnularVentaRequest.createFromRequest(Map.of("motivo", "Error"), usuario));
        registrarMovimientoCajaUseCase.execute(RegistrarMovimientoCajaRequest.createFromRequest(
                Map.of("tipo", "EGRESO", "monto", "30", "motivo", "Hielo"), usuario));

        // Act
        ArqueoCajaResponse arqueo = cerrarSesionCajaUseCase.execute(CerrarSesionCajaRequest.createFromRequest(
                Map.of("montoCierreDeclarado", "80"), usuario));

        // Assert: 100 de fondo + 10 en efectivo - 30 de egreso = 80; la venta anulada no cuenta.
        assertEquals(0, new BigDecimal("80").compareTo(arqueo.getMontoCierreEsperado()));
        assertEquals(0, arqueo.getDiferencia().signum());
        assertEquals(1, arqueo.getCantidadVentasAnuladas());
        assertEquals("CERRADA", arqueo.getSesion().getEstado());
    }

    @Test
    void unEgresoQueDejariaLaCajaEnNegativoSeRechaza() {
        // Arrange: 100 de fondo + 10 vendidos en efectivo = 110.
        vender("10", de(gaseosa, "2"));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> registrarMovimientoCajaUseCase.execute(
                RegistrarMovimientoCajaRequest.createFromRequest(Map.of("tipo", "EGRESO", "monto", "110.01", "motivo", "Retiro"), usuario)));

        // Assert
        assertEquals(409, error.getCodigo());
    }

    @Test
    void cancelarUnPedidoRecibidoAnulaSuVentaYDevuelveLasUnidades() {
        // Arrange
        VentaResponse venta = vender("15", de(gaseosa, "3"));
        Map<String, Object> pedido = new HashMap<>();
        pedido.put("fechaEntrega", LocalDateTime.now().plusHours(2).toString());
        pedido.put("latitud", "-17.78");
        pedido.put("longitud", "-63.18");
        pedido.put("idVenta", venta.getId());
        String idPedido = crearPedidoUseCase.execute(CrearPedidoRequest.createFromRequest(pedido)).getId();
        String cancelado = estadoPedidoRepository.obtenerPorNombre("Cancelado").orElseThrow().getId().getValue();

        // Act
        actualizarPedidoUseCase.execute(idPedido, ActualizarPedidoRequest.createFromRequest(Map.of("idEstado", cancelado), usuario));

        // Assert
        assertEquals(0, new BigDecimal("10").compareTo(escenario.stock(gaseosa)));
    }
}
