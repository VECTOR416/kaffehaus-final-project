package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Datasources;

import static com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria.linea;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ActualizarLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DetalleLoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ActualizarLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.EliminarLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/**
 * Persistencia de Lote contra la base: encabezado y lineas en dos tablas, conciliacion por id
 * en el PUT y borrado logico (arquitectura.md 5.13; logica.md LOTE-06, PRODUCTO-16).
 */
class LoteDatasourceImplIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private EscenarioDeCafeteria escenario;
    @Autowired private ILoteRepository loteRepository;
    @Autowired private ActualizarLoteUseCase actualizarLoteUseCase;
    @Autowired private EliminarLoteUseCase eliminarLoteUseCase;

    private String gaseosa;
    private String agua;
    private String proveedor;

    @BeforeEach
    void prepararEscenario() {
        escenario.sembrar();
        gaseosa = escenario.producto("Gaseosa", "5", true, true, "Unidad", null);
        agua = escenario.producto("Agua", "3", true, true, "Unidad", null);
        proveedor = escenario.proveedor();
    }

    private static String idDeLaLinea(LoteResponse lote, String idProducto) {
        return lote.getDetalles().stream().filter(detalle -> detalle.getIdProducto().equals(idProducto))
                .findFirst().orElseThrow().getId();
    }

    @Test
    void elLoteSeGuardaConSusLineasYSuTotal() {
        // Act
        LoteResponse compra = escenario.comprar(proveedor, linea(gaseosa, "10", "2", null), linea(agua, "6", "1.5", null));

        // Assert
        Lote leido = loteRepository.obtenerPorId(new LoteId(compra.getId())).orElseThrow();
        assertEquals(2, leido.getDetallesActivos().size());
        assertEquals(new BigDecimal("29.00"), leido.getTotal());
        assertEquals(compra.getId(), loteRepository.obtenerPorDetalleId(new DetalleLoteId(idDeLaLinea(compra, agua))).orElseThrow().getId().getValue());
    }

    @Test
    void elPutConciliaPorIdConservandoLasLineasYDandoDeBajaLaAusente() {
        // Arrange
        LoteResponse compra = escenario.comprar(proveedor, linea(gaseosa, "10", "2", null), linea(agua, "6", "1.5", null));
        String idGaseosa = idDeLaLinea(compra, gaseosa);
        String idAgua = idDeLaLinea(compra, agua);

        Map<String, Object> corregida = new HashMap<>(Map.of("id", idGaseosa, "precioUnitario", "2.5"));
        Map<String, Object> nueva = new HashMap<>(linea(agua, "4", "1", null));
        Map<String, Object> datos = new HashMap<>();
        datos.put("detalles", List.of(corregida, nueva));

        // Act: se corrige la gaseosa, se agrega otra linea de agua y la de agua original queda fuera.
        LoteResponse actualizado = actualizarLoteUseCase.execute(compra.getId(), ActualizarLoteRequest.createFromRequest(datos));

        // Assert
        Set<String> ids = actualizado.getDetalles().stream().map(DetalleLoteResponse::getId).collect(Collectors.toSet());
        assertTrue(ids.contains(idGaseosa));
        assertFalse(ids.contains(idAgua));
        assertEquals(2, ids.size());
        assertEquals(new BigDecimal("29.00"), actualizado.getTotal());
        assertTrue(loteRepository.obtenerPorDetalleId(new DetalleLoteId(idAgua)).isEmpty());
        // Las unidades de la linea dada de baja dejan de contar: 4 de la linea nueva.
        assertEquals(0, new BigDecimal("4").compareTo(escenario.stock(agua)));
    }

    @Test
    void laBajaDeUnLoteEsLogicaYSusUnidadesDejanDeContar() {
        // Arrange
        LoteResponse compra = escenario.comprar(proveedor, linea(gaseosa, "10", "2", null));

        // Act
        eliminarLoteUseCase.execute(compra.getId());

        // Assert
        assertTrue(loteRepository.obtenerPorId(new LoteId(compra.getId())).isEmpty());
        assertTrue(loteRepository.obtenerPorProductos(List.of(new ProductoId(gaseosa))).isEmpty());
        assertEquals(0, escenario.stock(gaseosa).signum());
    }

    @Test
    void obtenerPorProductosTraeSoloLotesConLineasDeEsosProductos() {
        // Arrange
        escenario.comprar(proveedor, linea(gaseosa, "10", "2", null));
        escenario.comprar(proveedor, linea(agua, "6", "1.5", null));

        // Act
        List<Lote> deGaseosa = loteRepository.obtenerPorProductos(List.of(new ProductoId(gaseosa)));

        // Assert
        assertEquals(1, deGaseosa.size());
        assertEquals(gaseosa, deGaseosa.get(0).getDetallesActivos().get(0).getProductoId().getValue());
    }
}
