package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Datasources;

import static com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria.linea;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.AbrirDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DetalleLoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerDetallesAperturablesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.AbrirDetalleLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ObtenerDetallesAperturablesUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.RegistrarPesajeRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RegistrarPesajeUseCase;
import com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/**
 * Consultas derivadas de Existencia contra la base: orden FEFO con lo que no vence al final
 * (como en PostgreSQL) y la lectura de todas las vivas que usa el pesaje.
 */
class ExistenciaDatasourceImplIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private EscenarioDeCafeteria escenario;
    @Autowired private ObtenerDetallesAperturablesUseCase obtenerDetallesAperturablesUseCase;
    @Autowired private AbrirDetalleLoteUseCase abrirDetalleLoteUseCase;
    @Autowired private RegistrarPesajeUseCase registrarPesajeUseCase;
    @Autowired private IExistenciaRepository existenciaRepository;

    private String usuario;
    private String harina;
    private List<DetalleLoteId> lineas;

    /** Tres bolsas de 1 kg: una vence en 10 dias, otra en 5 y otra no vence. Se abren las tres. */
    @BeforeEach
    void prepararEscenario() {
        usuario = escenario.sembrar();
        harina = escenario.producto("Harina", "0", false, true, "Gramo", null);
        String bolsa = escenario.producto("Bolsa de harina 1 kg", "12", true, true, "Kilogramo", harina);
        LoteResponse compra = escenario.comprar(escenario.proveedor(),
                linea(bolsa, "1", "12", LocalDate.now().plusDays(10).toString()),
                linea(bolsa, "1", "12", LocalDate.now().plusDays(5).toString()),
                linea(bolsa, "1", "12", null));
        lineas = compra.getDetalles().stream().map(DetalleLoteResponse::getId).map(DetalleLoteId::new).toList();
        for (int i = 0; i < 3; i++) {
            String siguiente = obtenerDetallesAperturablesUseCase.execute(
                    ObtenerDetallesAperturablesRequest.createFromRequest(Map.of("idProducto", harina))).get(0).getIdDetalleLote();
            abrirDetalleLoteUseCase.execute(siguiente, AbrirDetalleLoteRequest.createFromRequest(Map.of(), usuario));
        }
    }

    @Test
    void lasDisponiblesVienenEnOrdenFefoYLaQueNoVenceAlFinal() {
        // Act
        List<Existencia> disponibles = existenciaRepository.obtenerDisponiblesPorDetallesLote(lineas);

        // Assert
        assertEquals(3, disponibles.size());
        assertEquals(LocalDate.now().plusDays(5), disponibles.get(0).getFechaVencimiento());
        assertEquals(LocalDate.now().plusDays(10), disponibles.get(1).getFechaVencimiento());
        assertNull(disponibles.get(2).getFechaVencimiento());
    }

    @Test
    void lasVivasParaElPesajeIncluyenLasAgotadas() {
        // Arrange: un pesaje de 2000 g sobre 3000 vacia la bolsa del ultimo movimiento.
        registrarPesajeUseCase.execute(harina, RegistrarPesajeRequest.createFromRequest(Map.of("pesoTotal", "2000", "motivo", "Pesaje"), usuario));

        // Act
        List<Existencia> disponibles = existenciaRepository.obtenerDisponiblesPorDetallesLote(lineas);
        List<Existencia> vivas = existenciaRepository.obtenerVivasParaPesaje(lineas);

        // Assert
        assertEquals(2, disponibles.size());
        assertEquals(3, vivas.size());
        assertEquals(1, vivas.stream().filter(Existencia::isAgotada).count());
    }
}
