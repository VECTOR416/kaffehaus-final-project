package com.maestria.kaffehausv4.BoundedContexts.Inventario;

import static com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria.linea;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.AbrirDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DetalleAperturableResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerDetallesAperturablesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.AbrirDetalleLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.ObtenerDetallesAperturablesUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.CrearProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ProduccionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases.CrearProduccionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.InventarioProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.RegistrarPesajeRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.ObtenerInventarioProductoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RegistrarPesajeUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.CrearRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.CrearRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/**
 * Flujo de inventario contra la base (logica.md seccion 1): comprar bolsas de harina, abrir una,
 * producir pan con ella, pesar lo que queda abierto y consultar el inventario.
 *
 * Escenario: harina (gramos) se compra en bolsas de 1 kg; el pan (unidades) se produce con una
 * receta que rinde 10 panes con 400 g de harina.
 */
class FlujoInventarioIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private EscenarioDeCafeteria escenario;
    @Autowired private ObtenerDetallesAperturablesUseCase obtenerDetallesAperturablesUseCase;
    @Autowired private AbrirDetalleLoteUseCase abrirDetalleLoteUseCase;
    @Autowired private CrearRecetaUseCase crearRecetaUseCase;
    @Autowired private CrearProduccionUseCase crearProduccionUseCase;
    @Autowired private RegistrarPesajeUseCase registrarPesajeUseCase;
    @Autowired private ObtenerInventarioProductoUseCase obtenerInventarioProductoUseCase;
    @Autowired private ILoteRepository loteRepository;
    @Autowired private IMovimientoRepository movimientoRepository;
    @Autowired private ITipoMovimientoRepository tipoMovimientoRepository;

    private String usuario;
    private String harina;
    private String bolsa;
    private String pan;
    private String proveedor;
    private final String vence = LocalDate.now().plusDays(60).toString();

    @BeforeEach
    void prepararEscenario() {
        usuario = escenario.sembrar();
        harina = escenario.producto("Harina", "0", false, true, "Gramo", null);
        bolsa = escenario.producto("Bolsa de harina 1 kg", "12", true, true, "Kilogramo", harina);
        pan = escenario.producto("Pan", "1.5", true, true, "Unidad", null);
        proveedor = escenario.proveedor();
    }

    private ExistenciaResponse abrirUnaBolsa() {
        List<DetalleAperturableResponse> filas = obtenerDetallesAperturablesUseCase.execute(
                ObtenerDetallesAperturablesRequest.createFromRequest(Map.of("idProducto", harina)));
        return abrirDetalleLoteUseCase.execute(filas.get(0).getIdDetalleLote(), AbrirDetalleLoteRequest.createFromRequest(Map.of(), usuario));
    }

    private String recetaDePan() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("cantidad", "10");
        datos.put("titulo", "Pan de molde");
        datos.put("idProductoObtenido", pan);
        datos.put("ingredientes", List.of(Map.of("idProducto", harina, "cantidad", "400")));
        datos.put("pasos", List.of(Map.of("descripcion", "Amasar"), Map.of("descripcion", "Hornear")));
        return crearRecetaUseCase.execute(CrearRecetaRequest.createFromRequest(datos)).getId();
    }

    private ProduccionResponse producir(String idReceta, String esperada, String real) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("cantidadEsperada", esperada);
        datos.put("cantidadReal", real);
        datos.put("fechaVencimiento", LocalDate.now().plusDays(3).toString());
        datos.put("idReceta", idReceta);
        return crearProduccionUseCase.execute(CrearProduccionRequest.createFromRequest(datos, usuario));
    }

    private List<Movimiento> movimientosDe(String idExistencia) {
        return movimientoRepository.obtenerPorExistencias(List.of(new ExistenciaId(idExistencia)));
    }

    private String idTipo(String nombre) {
        return tipoMovimientoRepository.obtenerPorNombre(nombre).orElseThrow().getId().getValue();
    }

    @Test
    void comprarBolsasSubeElStockDeLaPresentacionYNoElDeLaHarina() {
        // Act
        escenario.comprar(proveedor, linea(bolsa, "3", "12", vence));

        // Assert
        assertEquals(0, new BigDecimal("3").compareTo(escenario.stock(bolsa)));
        assertEquals(0, escenario.stock(harina).signum());
    }

    @Test
    void abrirUnaBolsaLaVuelveExistenciaDeHarinaConUnMovimientoDeAperturaEnCero() {
        // Arrange
        escenario.comprar(proveedor, linea(bolsa, "3", "12", vence));

        // Act
        ExistenciaResponse abierta = abrirUnaBolsa();

        // Assert
        assertEquals(0, new BigDecimal("1000").compareTo(abierta.getContenidoInicial()));
        assertEquals(0, new BigDecimal("2").compareTo(escenario.stock(bolsa)));
        assertEquals(0, new BigDecimal("1000").compareTo(escenario.stock(harina)));
        List<Movimiento> movimientos = movimientosDe(abierta.getId());
        assertEquals(1, movimientos.size());
        assertEquals(idTipo(TipoMovimiento.APERTURA), movimientos.get(0).getTipoMovimientoId().getValue());
        assertEquals(0, movimientos.get(0).getCantidad().signum());
    }

    @Test
    void producirConsumeLaHarinaAbiertaYDejaLosPanesComoUnidadesDeSuLote() {
        // Arrange
        escenario.comprar(proveedor, linea(bolsa, "1", "12", vence));
        ExistenciaResponse abierta = abrirUnaBolsa();
        String receta = recetaDePan();

        // Act
        ProduccionResponse produccion = producir(receta, "10", "10");

        // Assert: 400 g de harina menos y 10 panes en stock.
        assertEquals(0, new BigDecimal("600").compareTo(escenario.stock(harina)));
        assertEquals(0, new BigDecimal("10").compareTo(escenario.stock(pan)));

        List<Movimiento> consumo = movimientosDe(abierta.getId()).stream()
                .filter(movimiento -> movimiento.getTipoMovimientoId().getValue().equals(idTipo(TipoMovimiento.PRODUCCION)))
                .toList();
        assertEquals(1, consumo.size());
        assertEquals(0, new BigDecimal("400").compareTo(consumo.get(0).getCantidad()));
        assertEquals(produccion.getId(), consumo.get(0).getProduccionId().getValue());

        Lote loteDelPan = loteRepository.obtenerPorProductos(List.of(new ProductoId(pan))).get(0);
        assertTrue(loteDelPan.isEsProduccion());
        assertEquals(0, new BigDecimal("10").compareTo(loteDelPan.getDetallesActivos().get(0).getCantidadRestante()));
        assertEquals(0, loteDelPan.getTotal().signum());
    }

    @Test
    void sinHarinaAbiertaSuficienteLaProduccionPideAbrirUnaBolsa() {
        // Arrange: hay una bolsa cerrada, pero nada abierto.
        escenario.comprar(proveedor, linea(bolsa, "1", "12", vence));
        String receta = recetaDePan();

        // Act
        DomainException error = assertThrows(DomainException.class, () -> producir(receta, "10", "10"));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("abre una presentacion"));
    }

    @Test
    void unPesajeConMenosRegistraMermaYUnoConMasRegistraAjuste() {
        // Arrange
        escenario.comprar(proveedor, linea(bolsa, "1", "12", vence));
        ExistenciaResponse abierta = abrirUnaBolsa();
        String merma = idTipo(TipoMovimiento.MERMA);
        String ajuste = idTipo(TipoMovimiento.AJUSTE);

        // Act: el sistema dice 1000 g; se pesan 950 y luego 980.
        registrarPesajeUseCase.execute(harina, RegistrarPesajeRequest.createFromRequest(Map.of("pesoTotal", "950", "motivo", "Pesaje"), usuario));
        assertEquals(0, new BigDecimal("950").compareTo(escenario.stock(harina)));
        registrarPesajeUseCase.execute(harina, RegistrarPesajeRequest.createFromRequest(Map.of("pesoTotal", "980", "motivo", "Pesaje"), usuario));

        // Assert
        assertEquals(0, new BigDecimal("980").compareTo(escenario.stock(harina)));
        List<Movimiento> movimientos = movimientosDe(abierta.getId());
        assertEquals(1, movimientos.stream().filter(m -> m.getTipoMovimientoId().getValue().equals(merma)
                && new BigDecimal("50").compareTo(m.getCantidad()) == 0).count());
        assertEquals(1, movimientos.stream().filter(m -> m.getTipoMovimientoId().getValue().equals(ajuste)
                && new BigDecimal("30").compareTo(m.getCantidad()) == 0).count());
    }

    @Test
    void laHarinaNoSeCompraSueltaPorqueTienePresentaciones() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> escenario.comprar(proveedor, linea(harina, "1000", "0.01", vence)));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void laConsultaDeInventarioDistingueLaHarinaYElPan() {
        // Arrange
        LoteResponse compra = escenario.comprar(proveedor, linea(bolsa, "2", "12", vence), linea(pan, "6", "1", vence));
        abrirUnaBolsa();

        // Act
        InventarioProductoResponse deHarina = obtenerInventarioProductoUseCase.execute(harina);
        InventarioProductoResponse dePan = obtenerInventarioProductoUseCase.execute(pan);

        // Assert: harina 1000 g abiertos + 1 bolsa cerrada (1000 g); pan 6 unidades disponibles.
        assertEquals(2, compra.getDetalles().size());
        assertEquals(0, new BigDecimal("1000").compareTo(deHarina.getStockAbierto()));
        assertEquals(0, new BigDecimal("1000").compareTo(deHarina.getStockCerrado()));
        assertEquals(0, new BigDecimal("6").compareTo(dePan.getStockAbierto()));
        assertEquals(0, dePan.getStockCerrado().signum());
    }
}
