package com.maestria.kaffehausv4.BoundedContexts.Inventario;

import static com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria.linea;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.ActualizarEstadoExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases.ActualizarEstadoExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ActualizarExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.DarDeBajaVencidaExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.ActualizarExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.DarDeBajaVencidaExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.AbrirDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DarDeBajaVencidoDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerVencimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.VencimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.AbrirDetalleLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.DarDeBajaVencidoDetalleLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.EliminarLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ObtenerVencimientosUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ObtenerMovimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.InventarioProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.EliminarProductoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.ObtenerInventarioProductoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.CrearRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.CrearRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases.EliminarTipoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.UseCases.EliminarTipoPagoUseCase;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/**
 * Vencimientos y bajas contra la base (logica.md VENCIMIENTO-02 a VENCIMIENTO-05, LOTE-26,
 * PRODUCTO-20, RECETA-07, GENERAL-18 y GENERAL-19), con los mismos casos de uso que la API.
 *
 * Escenario: pan por unidades y harina en gramos que se compra en bolsas de 1 kg.
 */
class VencimientosYBajasIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private EscenarioDeCafeteria escenario;
    @Autowired private ObtenerVencimientosUseCase obtenerVencimientosUseCase;
    @Autowired private DarDeBajaVencidoDetalleLoteUseCase darDeBajaVencidoDetalleLoteUseCase;
    @Autowired private DarDeBajaVencidaExistenciaUseCase darDeBajaVencidaExistenciaUseCase;
    @Autowired private ActualizarExistenciaUseCase actualizarExistenciaUseCase;
    @Autowired private AbrirDetalleLoteUseCase abrirDetalleLoteUseCase;
    @Autowired private ObtenerInventarioProductoUseCase obtenerInventarioProductoUseCase;
    @Autowired private EliminarLoteUseCase eliminarLoteUseCase;
    @Autowired private EliminarProductoUseCase eliminarProductoUseCase;
    @Autowired private CrearRecetaUseCase crearRecetaUseCase;
    @Autowired private EliminarTipoUseCase eliminarTipoUseCase;
    @Autowired private EliminarTipoPagoUseCase eliminarTipoPagoUseCase;
    @Autowired private ActualizarEstadoExistenciaUseCase actualizarEstadoExistenciaUseCase;
    @Autowired private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Autowired private ITipoRepository tipoRepository;
    @Autowired private IRecetaRepository recetaRepository;
    @Autowired private IMovimientoRepository movimientoRepository;
    @Autowired private ITipoMovimientoRepository tipoMovimientoRepository;

    private String usuario;
    private String proveedor;
    private String pan;
    private String harina;
    private String bolsa;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void prepararEscenario() {
        usuario = escenario.sembrar();
        proveedor = escenario.proveedor();
        pan = escenario.producto("Pan", "1.5", true, true, "Unidad", null);
        harina = escenario.producto("Harina", "0", false, true, "Gramo", null);
        bolsa = escenario.producto("Bolsa de harina 1 kg", "12", true, true, "Kilogramo", harina);
    }

    private InventarioProductoResponse inventario(String idProducto) {
        return obtenerInventarioProductoUseCase.execute(idProducto);
    }

    private List<Movimiento> vencimientosDeLaLinea(String idDetalle) {
        String idTipo = tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.VENCIMIENTO).orElseThrow().getId().getValue();
        return movimientoRepository.obtenerTodos(ObtenerMovimientosRequest.createFromRequest(Map.of("limit", 100))).getData().stream()
                .filter(movimiento -> movimiento.getDetalleLoteId() != null && idDetalle.equals(movimiento.getDetalleLoteId().getValue()))
                .filter(movimiento -> movimiento.getTipoMovimientoId().getValue().equals(idTipo))
                .toList();
    }

    // ------------------------------------------------------------------ lineas de lote

    @Test
    void elPanVencidoSeVeEnLaAlertaSeSeparaDelVigenteYSeDaDeBaja() {
        // Arrange: 5 panes que vencieron ayer y 3 que vencen en un mes.
        LoteResponse lote = escenario.comprar(proveedor,
                linea(pan, "5", "1", hoy.minusDays(1).toString()),
                linea(pan, "3", "1", hoy.plusDays(30).toString()));
        String idVencida = lote.getDetalles().stream()
                .filter(detalle -> hoy.minusDays(1).equals(detalle.getFechaVencimiento()))
                .findFirst().orElseThrow().getId();

        // Act
        List<VencimientoResponse> alerta = obtenerVencimientosUseCase.execute(ObtenerVencimientosRequest.createFromRequest(new HashMap<>()));
        InventarioProductoResponse antes = inventario(pan);
        darDeBajaVencidoDetalleLoteUseCase.execute(idVencida, DarDeBajaVencidoDetalleLoteRequest.createFromRequest(Map.of(), usuario));

        // Assert: la alerta muestra solo lo vencido (lo otro vence fuera del plazo de 3 dias).
        assertEquals(1, alerta.size());
        assertEquals(idVencida, alerta.get(0).getId());
        assertTrue(alerta.get(0).isVencido());
        // VENCIMIENTO-02: 8 fisicos, 3 vigentes, 5 vencidos.
        assertEquals(0, new BigDecimal("8").compareTo(antes.getStock()));
        assertEquals(0, new BigDecimal("3").compareTo(antes.getStockVigente()));
        assertEquals(0, new BigDecimal("5").compareTo(antes.getStockVencido()));
        // VENCIMIENTO-03: tras la baja el fisico coincide con el vigente y queda la traza.
        assertEquals(0, new BigDecimal("3").compareTo(escenario.stock(pan)));
        assertEquals(1, vencimientosDeLaLinea(idVencida).size());
        assertEquals(0, new BigDecimal("5").compareTo(vencimientosDeLaLinea(idVencida).get(0).getCantidad()));
        assertTrue(obtenerVencimientosUseCase.execute(ObtenerVencimientosRequest.createFromRequest(new HashMap<>())).isEmpty());
    }

    @Test
    void loVigenteNoSeDaDeBajaPorVencimiento() {
        // Arrange
        String idLinea = escenario.comprar(proveedor, linea(pan, "5", "1", hoy.toString())).getDetalles().get(0).getId();

        // Act
        DomainException error = assertThrows(DomainException.class, () -> darDeBajaVencidoDetalleLoteUseCase.execute(
                idLinea, DarDeBajaVencidoDetalleLoteRequest.createFromRequest(Map.of(), usuario)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(0, new BigDecimal("5").compareTo(escenario.stock(pan)));
    }

    @Test
    void unLoteSinUsarSeDaDeBajaPeroUnoUsadoNo() {
        // Arrange (logica.md LOTE-26)
        LoteResponse equivocado = escenario.comprar(proveedor, linea(pan, "5", "1", null));
        LoteResponse usado = escenario.comprar(proveedor, linea(pan, "4", "1", hoy.minusDays(1).toString()));
        darDeBajaVencidoDetalleLoteUseCase.execute(usado.getDetalles().get(0).getId(),
                DarDeBajaVencidoDetalleLoteRequest.createFromRequest(Map.of(), usuario));

        // Act
        eliminarLoteUseCase.execute(equivocado.getId());
        DomainException error = assertThrows(DomainException.class, () -> eliminarLoteUseCase.execute(usado.getId()));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(0, escenario.stock(pan).signum());
    }

    // ------------------------------------------------------------------ existencias

    @Test
    void unaExistenciaNoVenceDespuesQueSuBolsaYSiVencioSeDaDeBaja() {
        // Arrange: se abre una bolsa vigente y despues un Administrador corrige su vencimiento a ayer.
        String idDetalle = escenario.comprar(proveedor, linea(bolsa, "1", "12", hoy.plusDays(20).toString())).getDetalles().get(0).getId();
        ExistenciaResponse abierta = abrirDetalleLoteUseCase.execute(idDetalle, AbrirDetalleLoteRequest.createFromRequest(Map.of(), usuario));

        // Act
        DomainException posterior = assertThrows(DomainException.class, () -> actualizarExistenciaUseCase.execute(abierta.getId(),
                ActualizarExistenciaRequest.createFromRequest(Map.of("fechaVencimiento", hoy.plusDays(40).toString()))));
        actualizarExistenciaUseCase.execute(abierta.getId(),
                ActualizarExistenciaRequest.createFromRequest(Map.of("fechaVencimiento", hoy.minusDays(1).toString())));
        ExistenciaResponse dadaDeBaja = darDeBajaVencidaExistenciaUseCase.execute(abierta.getId(),
                DarDeBajaVencidaExistenciaRequest.createFromRequest(Map.of("motivo", "Humedad"), usuario));

        // Assert
        assertEquals(409, posterior.getCodigo());
        assertEquals("Agotada", dadaDeBaja.getEstadoExistencia().getNombre());
        assertEquals(0, escenario.stock(harina).signum());
    }

    // ------------------------------------------------------------------ productos, recetas y catalogos

    @Test
    void unProductoConStockNoSeDaDeBajaYUnoSinInventarioArrastraSusRecetas() {
        // Arrange (logica.md PRODUCTO-20)
        escenario.comprar(proveedor, linea(pan, "2", "1", null));
        String sandwich = escenario.producto("Sandwich", "10", true, false, "Unidad", null);
        Map<String, Object> receta = new HashMap<>();
        receta.put("cantidad", "1");
        receta.put("titulo", "Sandwich de miga");
        receta.put("idProductoObtenido", sandwich);
        receta.put("ingredientes", List.of(Map.of("idProducto", pan, "cantidad", "0.5")));
        receta.put("pasos", List.of(Map.of("descripcion", "Armar")));
        crearRecetaUseCase.execute(CrearRecetaRequest.createFromRequest(receta));

        // Act
        DomainException conStock = assertThrows(DomainException.class, () -> eliminarProductoUseCase.execute(pan));
        eliminarProductoUseCase.execute(sandwich);

        // Assert
        assertEquals(409, conStock.getCodigo());
        assertTrue(recetaRepository.obtenerPorProductoObtenido(new ProductoId(sandwich)).isEmpty());
    }

    @Test
    void unaRecetaNoFormaUnCicloEntreMto() {
        // Arrange (logica.md RECETA-07)
        String mocha = escenario.producto("Mocha", "12", true, false, "Unidad", null);
        String base = escenario.producto("Base de chocolate", "0", false, false, "Unidad", null);
        crearRecetaUseCase.execute(CrearRecetaRequest.createFromRequest(receta(base, mocha)));

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> crearRecetaUseCase.execute(CrearRecetaRequest.createFromRequest(receta(mocha, base))));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("Mocha -> Base de chocolate -> Mocha"));
    }

    private static Map<String, Object> receta(String obtenido, String ingrediente) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("cantidad", "1");
        datos.put("titulo", "Receta");
        datos.put("idProductoObtenido", obtenido);
        datos.put("ingredientes", List.of(Map.of("idProducto", ingrediente, "cantidad", "1")));
        datos.put("pasos", List.of(Map.of("descripcion", "Preparar")));
        return datos;
    }

    @Test
    void losCatalogosEnUsoOSemanticosNoSeDanDeBajaNiSeRenombran() {
        // Arrange (logica.md GENERAL-18, GENERAL-19)
        String insumo = tipoRepository.obtenerPorNombre("Insumo").orElseThrow().getId().getValue();
        String efectivo = escenario.idTipoPago("Efectivo");
        String abierta = estadoExistenciaRepository.obtenerPorNombre("Abierta").orElseThrow().getId().getValue();

        // Act
        DomainException tipoEnUso = assertThrows(DomainException.class, () -> eliminarTipoUseCase.execute(insumo));
        DomainException pagoSemantico = assertThrows(DomainException.class, () -> eliminarTipoPagoUseCase.execute(efectivo));
        DomainException estadoRenombrado = assertThrows(DomainException.class, () -> actualizarEstadoExistenciaUseCase.execute(abierta,
                ActualizarEstadoExistenciaRequest.createFromRequest(Map.of("nombre", "Activa"))));

        // Assert
        assertEquals(409, tipoEnUso.getCodigo());
        assertEquals(409, pagoSemantico.getCodigo());
        assertEquals(409, estadoRenombrado.getCodigo());
        assertFalse(tipoRepository.obtenerPorNombre("Insumo").isEmpty());
        assertFalse(estadoExistenciaRepository.obtenerPorNombre("Abierta").isEmpty());
    }
}
