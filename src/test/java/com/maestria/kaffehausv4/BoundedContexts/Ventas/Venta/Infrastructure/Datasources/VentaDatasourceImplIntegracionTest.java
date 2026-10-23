package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Datasources;

import static com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria.linea;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.CrearVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/**
 * Persistencia de Venta contra la base: encabezado, lineas y pagos en tres tablas, el ticket que
 * asigna la base y las lecturas por clave de idempotencia y por sesion de caja.
 */
class VentaDatasourceImplIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private EscenarioDeCafeteria escenario;
    @Autowired private CrearVentaUseCase crearVentaUseCase;
    @Autowired private IVentaRepository ventaRepository;

    private String usuario;
    private String gaseosa;
    private String caja;

    @BeforeEach
    void prepararEscenario() {
        usuario = escenario.sembrar();
        gaseosa = escenario.producto("Gaseosa", "5", true, true, "Unidad", null);
        escenario.comprar(escenario.proveedor(), linea(gaseosa, "10", "2", null));
        caja = escenario.abrirCaja("50");
    }

    private VentaResponse vender(String claveIdempotencia) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("detalles", List.of(Map.of("idProducto", gaseosa, "cantidad", "2")));
        datos.put("pagos", List.of(
                Map.of("idTipoPago", escenario.idTipoPago("Efectivo"), "monto", "6", "montoRecibido", "10"),
                Map.of("idTipoPago", escenario.idTipoPago("Tarjeta"), "monto", "4")));
        if (claveIdempotencia != null) {
            datos.put("claveIdempotencia", claveIdempotencia);
        }
        return crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(datos, usuario));
    }

    @Test
    void laVentaSeReleeConSusLineasSusPagosYSuTicket() {
        // Act
        VentaResponse creada = vender(null);

        // Assert
        Venta leida = ventaRepository.obtenerPorId(new VentaId(creada.getId())).orElseThrow();
        assertEquals(creada.getNumeroTicket(), leida.getNumeroTicket());
        assertEquals(1, leida.getDetallesActivos().size());
        assertEquals(2, leida.getPagosActivos().size());
        assertEquals(new BigDecimal("10.00"), leida.getTotal());
        assertEquals(new BigDecimal("4.00"), leida.getCambio());
        assertEquals(creada.getId(), ventaRepository.obtenerPorNumeroTicket(creada.getNumeroTicket()).orElseThrow().getId().getValue());
    }

    @Test
    void unaClaveDeIdempotenciaRepetidaDevuelveLaMismaVentaSinDescontarDeNuevo() {
        // Act
        VentaResponse primera = vender("pos-1-0001");
        VentaResponse repetida = vender("pos-1-0001");

        // Assert
        assertEquals(primera.getId(), repetida.getId());
        assertEquals(0, new BigDecimal("8").compareTo(escenario.stock(gaseosa)));
        assertEquals(primera.getId(), ventaRepository.obtenerPorClaveIdempotencia("pos-1-0001").orElseThrow().getId().getValue());
    }

    @Test
    void lasVentasSeLeenPorLaSesionDeCajaEnQueSeCobraron() {
        // Arrange
        vender(null);
        vender(null);

        // Act
        List<Venta> deLaCaja = ventaRepository.obtenerPorSesionCaja(new SesionCajaId(caja));

        // Assert
        assertEquals(2, deLaCaja.size());
    }
}
