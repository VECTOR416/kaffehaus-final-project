package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AGOTADA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AHORA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.HOY;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_ESTADO_ABIERTA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_ESTADO_AGOTADA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_OTRA_UNIDAD;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO_PRODUCCION;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO_VENTA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_USUARIO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.PRODUCCION;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.UNIDAD;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.VENTA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.conversionDePresentacionABase;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.detalle;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.existencia;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ingrediente;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.loteDeCompra;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.presentacion;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.productoBajoDemanda;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.productoDirecto;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.receta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.SalidaDeInventarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.DatosDePrueba;

/**
 * Pruebas unitarias de la salida de inventario (logica.md MOVIMIENTO-19, GENERAL-14, GENERAL-15,
 * LOTE-17, VENTA-24, VENTA-30): plan de consumo agregado por producto, FEFO sobre existencias
 * abiertas para un producto base con presentaciones, descuento de unidades de lote con su
 * movimiento, explosion de recetas MTO y el error estructurado cuando algo no alcanza. Todos los
 * repositorios son dobles de Mockito.
 *
 * Escenario base: harina, que se compra en bolsas de 10 (su presentacion), con existencias
 * abiertas de 10 unidades de contenido.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarSalidaDeInventarioUseCaseTest {

    private static final String ID_HARINA = "a0000000-0000-0000-0000-000000000001";
    private static final String ID_BOLSA_HARINA = "a0000000-0000-0000-0000-000000000002";
    private static final String ID_PAN = "a0000000-0000-0000-0000-000000000003";
    private static final String ID_MASA = "a0000000-0000-0000-0000-000000000004";
    private static final String ID_LOTE = "a1000000-0000-0000-0000-000000000001";
    private static final String ID_LOTE_2 = "a1000000-0000-0000-0000-000000000002";
    private static final String ID_DETALLE_BOLSA = "a2000000-0000-0000-0000-000000000002";
    private static final String ID_DETALLE_PAN_1 = "a2000000-0000-0000-0000-000000000003";
    private static final String ID_DETALLE_PAN_2 = "a2000000-0000-0000-0000-000000000004";
    private static final String ID_EXISTENCIA_1 = "a3000000-0000-0000-0000-000000000001";
    private static final String ID_EXISTENCIA_2 = "a3000000-0000-0000-0000-000000000002";
    private static final String ID_PRODUCCION = "a4000000-0000-0000-0000-000000000001";
    private static final String ID_VENTA = "a7000000-0000-0000-0000-000000000001";

    @Mock private IProductoRepository productoRepository;
    @Mock private IRecetaRepository recetaRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private IUnidadRepository unidadRepository;
    @Mock private IConversionRepository conversionRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private RegistrarSalidaDeInventarioUseCase useCase;
    private final List<Movimiento> movimientosGuardados = new ArrayList<>();

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new RegistrarSalidaDeInventarioUseCase(productoRepository, recetaRepository, loteRepository,
                existenciaRepository, estadoExistenciaRepository, movimientoRepository, tipoMovimientoRepository,
                unidadRepository, conversionRepository, recalcularStockUseCase);

        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Agotada")).thenReturn(Optional.of(AGOTADA));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.PRODUCCION)).thenReturn(Optional.of(PRODUCCION));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.VENTA)).thenReturn(Optional.of(VENTA));
        lenient().when(unidadRepository.obtenerPorId(any())).thenReturn(Optional.of(UNIDAD));
        // Una bolsa de harina da 10 unidades de contenido.
        lenient().when(conversionRepository.obtenerVivas()).thenReturn(List.of(conversionDePresentacionABase("10")));
        lenient().when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of());
        lenient().when(productoRepository.obtenerPresentaciones(argThat(id -> id != null && ID_HARINA.equals(id.getValue()))))
                .thenReturn(List.of(presentacion(ID_BOLSA_HARINA, "Bolsa de harina", ID_HARINA)));
        lenient().when(existenciaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(loteRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> {
            Movimiento guardado = DatosDePrueba.guardado(llamada.getArgument(0), "ad000000-0000-0000-0000-00000000000" + movimientosGuardados.size());
            movimientosGuardados.add(guardado);
            return guardado;
        });
    }

    /** Consumo de insumos de una produccion, vinculado a la produccion. */
    private static RegistrarSalidaDeInventarioRequest produccion() {
        return RegistrarSalidaDeInventarioRequest.deProduccion(ID_USUARIO, ID_PRODUCCION, "Consumo de la produccion 'Pan'", AHORA);
    }

    private static RegistrarSalidaDeInventarioRequest consumo(String idProducto, String cantidad) {
        return produccion().con(idProducto, new BigDecimal(cantidad));
    }

    /** Salida de una venta, vinculada a la venta. */
    private static RegistrarSalidaDeInventarioRequest venta(String idProducto, String cantidad) {
        return RegistrarSalidaDeInventarioRequest.deVenta(ID_USUARIO, ID_VENTA, "Venta del ticket 7", AHORA)
                .con(idProducto, new BigDecimal(cantidad));
    }

    private void hayHarinaCon(Existencia... existencias) {
        hayHarinaCon("10", existencias);
    }

    /** 'cerradas' es la cantidad restante de la linea de la bolsa: unidades que aun no se abrieron. */
    private void hayHarinaCon(String cerradas, Existencia... existencias) {
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_HARINA.equals(id.getValue()))))
                .thenReturn(Optional.of(productoDirecto(ID_HARINA, "Harina")));
        lenient().when(loteRepository.obtenerPorProductosSinCargar(argThat(fuentes -> fuentes != null && fuentes.size() == 2)))
                .thenReturn(List.of(loteDeCompra(ID_LOTE, detalle(ID_DETALLE_BOLSA, ID_BOLSA_HARINA, "10", cerradas, "50.00", null))));
        lenient().when(existenciaRepository.obtenerDisponiblesParaConsumo(anyList())).thenReturn(List.of(existencias));
    }

    /**
     * Pan por unidades: un lote por linea. La lectura sin cargar y el bloqueo de los lotes por su
     * raiz devuelven los mismos lotes (solo los pedidos, en el orden en que llegan los ids).
     */
    private void hayPanCon(Lote... lotes) {
        hayPanCon(List.of(lotes), List.of(lotes));
    }

    /** 'bloqueados' es lo que se lee con el bloqueo tomado: otra operacion pudo cambiarlos. */
    private void hayPanCon(List<Lote> sinCargar, List<Lote> bloqueados) {
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_PAN.equals(id.getValue()))))
                .thenReturn(Optional.of(productoDirecto(ID_PAN, "Pan")));
        lenient().when(loteRepository.obtenerPorProductosSinCargar(argThat(fuentes -> fuentes != null && fuentes.size() == 1)))
                .thenReturn(sinCargar);
        lenient().when(loteRepository.obtenerPorIdsParaActualizar(anyList())).thenAnswer(llamada -> {
            List<String> ids = llamada.<List<LoteId>>getArgument(0).stream().map(LoteId::getValue).toList();
            return bloqueados.stream().filter(lote -> ids.contains(lote.getId().getValue())).toList();
        });
    }

    /** La masa es MTO: rinde 10 con 4 de harina. */
    private void laMasaLleva4DeHarinaCada10() {
        when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_MASA.equals(id.getValue()))))
                .thenReturn(Optional.of(productoBajoDemanda(ID_MASA, "Masa")));
        when(recetaRepository.obtenerPorProductoObtenido(argThat(id -> id != null && ID_MASA.equals(id.getValue()))))
                .thenReturn(Optional.of(receta("a5000000-0000-0000-0000-000000000001", ID_MASA, "10",
                        ingrediente("a6000000-0000-0000-0000-000000000001", ID_HARINA, "4"))));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> faltantes(DomainException error) {
        return (List<Map<String, Object>>) error.getDetalle().get("faltantes");
    }

    // ------------------------------------------------------------------ producto base con presentaciones

    @Test
    void unInsumoBaseSaleDeSuExistenciaYRegistraUnMovimientoDeProduccion() {
        // Arrange
        Existencia abierta = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null);
        hayHarinaCon(abierta);

        // Act
        SalidaDeInventarioResponse respuesta = useCase.execute(consumo(ID_HARINA, "2"));

        // Assert
        assertEquals(0, new BigDecimal("8").compareTo(abierta.getContenidoActual()));
        assertEquals(1, respuesta.getMovimientos().size());

        Movimiento movimiento = movimientosGuardados.get(0);
        assertEquals(0, new BigDecimal("2").compareTo(movimiento.getCantidad()));
        assertEquals(ID_EXISTENCIA_1, movimiento.getExistenciaId().getValue());
        assertNull(movimiento.getDetalleLoteId());
        assertEquals(ID_TIPO_PRODUCCION, movimiento.getTipoMovimientoId().getValue());
        assertEquals(ID_PRODUCCION, movimiento.getProduccionId().getValue());
        assertNull(movimiento.getVentaId());
        assertEquals(ID_USUARIO, movimiento.getUsuarioId().getValue());
        assertEquals("Consumo de la produccion 'Pan'", movimiento.getMotivo());
        assertEquals(AHORA, movimiento.getFechaMovimiento());
        verify(existenciaRepository).actualizar(any(), any());
        verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    @Test
    void elConsumoSeReparteEntreExistenciasEnElOrdenRecibidoYAgotaLaPrimera() {
        // Arrange
        Existencia primera = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "3", null);
        Existencia segunda = existencia(ID_EXISTENCIA_2, ID_DETALLE_BOLSA, "10", "10", null);
        hayHarinaCon(primera, segunda);

        // Act
        useCase.execute(consumo(ID_HARINA, "5"));

        // Assert
        assertTrue(primera.isAgotada());
        assertEquals(ID_ESTADO_AGOTADA, primera.getEstadoExistenciaId().getValue());
        assertEquals(AHORA, primera.getFechaAcabado());
        assertEquals(0, new BigDecimal("8").compareTo(segunda.getContenidoActual()));
        assertEquals(ID_ESTADO_ABIERTA, segunda.getEstadoExistenciaId().getValue());

        assertEquals(2, movimientosGuardados.size());
        assertEquals(0, new BigDecimal("3").compareTo(movimientosGuardados.get(0).getCantidad()));
        assertEquals(ID_EXISTENCIA_1, movimientosGuardados.get(0).getExistenciaId().getValue());
        assertEquals(0, new BigDecimal("2").compareTo(movimientosGuardados.get(1).getCantidad()));
        assertEquals(ID_EXISTENCIA_2, movimientosGuardados.get(1).getExistenciaId().getValue());
        verify(recalcularStockUseCase, times(1)).execute(ID_HARINA);
    }

    @Test
    void noTocaLasExistenciasQueNoHacenFalta() {
        // Arrange
        Existencia primera = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null);
        Existencia segunda = existencia(ID_EXISTENCIA_2, ID_DETALLE_BOLSA, "10", "10", null);
        hayHarinaCon(primera, segunda);

        // Act
        useCase.execute(consumo(ID_HARINA, "10"));

        // Assert
        assertTrue(primera.isAgotada());
        assertEquals(0, new BigDecimal("10").compareTo(segunda.getContenidoActual()));
        assertEquals(1, movimientosGuardados.size());
        verify(existenciaRepository, times(1)).actualizar(any(), any());
    }

    @Test
    void sinStockAbiertoNiBolsasCerradasRespondeStockInsuficienteSinDescontarNada() {
        // Arrange
        Existencia abierta = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "4", null);
        hayHarinaCon("0", abierta);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(consumo(ID_HARINA, "5")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(RegistrarSalidaDeInventarioUseCase.STOCK_INSUFICIENTE, error.getError());
        assertTrue(error.getMensaje().startsWith("Stock insuficiente de 'Harina'"));
        assertTrue(error.getMensaje().contains("se requieren 5 y hay 4"));
        Map<String, Object> faltante = faltantes(error).get(0);
        assertEquals(ID_HARINA, faltante.get("idProducto"));
        assertEquals(0, new BigDecimal("5").compareTo((BigDecimal) faltante.get("requerido")));
        assertEquals(0, new BigDecimal("4").compareTo((BigDecimal) faltante.get("disponible")));
        assertEquals("Gramo", faltante.get("unidad"));
        assertEquals(0, new BigDecimal("4").compareTo(abierta.getContenidoActual()));
        verify(existenciaRepository, never()).actualizar(any(), any());
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void siLasBolsasCerradasAlcanzanRespondeAperturaRequeridaConLaBolsaSugerida() {
        // Arrange: 4 abiertas, se piden 25; hay 3 bolsas cerradas de 10 => hacen falta 3 bolsas.
        hayHarinaCon("3", existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "4", HOY.plusDays(5)));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(consumo(ID_HARINA, "25")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(RegistrarSalidaDeInventarioUseCase.APERTURA_REQUERIDA, error.getError());
        assertTrue(error.getMensaje().contains("'Harina'"));
        assertTrue(error.getMensaje().contains("abre una presentacion"));

        Map<String, Object> faltante = faltantes(error).get(0);
        assertEquals(0, new BigDecimal("25").compareTo((BigDecimal) faltante.get("requerido")));
        assertEquals(0, new BigDecimal("4").compareTo((BigDecimal) faltante.get("disponibleAbierto")));
        assertEquals(3, faltante.get("bolsasNecesarias"));
        @SuppressWarnings("unchecked")
        Map<String, Object> bolsa = (Map<String, Object>) faltante.get("bolsaSugerida");
        assertEquals(ID_DETALLE_BOLSA, bolsa.get("idDetalleLote"));
        assertEquals(ID_BOLSA_HARINA, bolsa.get("idProductoPresentacion"));
        assertEquals("Bolsa de harina", bolsa.get("presentacion"));
        assertEquals(0, new BigDecimal("10").compareTo((BigDecimal) bolsa.get("contenido")));
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void siLasBolsasCerradasNoAlcanzanNoSugiereAbrir() {
        // Arrange: 4 abiertas + 1 bolsa cerrada de 10 = 14; se piden 20.
        hayHarinaCon("1", existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "4", null));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(consumo(ID_HARINA, "20")));

        // Assert
        assertEquals(RegistrarSalidaDeInventarioUseCase.STOCK_INSUFICIENTE, error.getError());
        assertEquals(0, new BigDecimal("14").compareTo((BigDecimal) faltantes(error).get(0).get("disponible")));
    }

    @Test
    void lasBolsasCerradasYaVencidasNoCuentanParaSugerirUnaApertura() {
        // Arrange
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(productoDirecto(ID_HARINA, "Harina")));
        when(loteRepository.obtenerPorProductosSinCargar(anyList())).thenReturn(List.of(loteDeCompra(ID_LOTE,
                detalle(ID_DETALLE_BOLSA, ID_BOLSA_HARINA, "10", "3", "50.00", HOY.minusDays(1)))));
        when(existenciaRepository.obtenerDisponiblesParaConsumo(anyList())).thenReturn(List.of());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(consumo(ID_HARINA, "1")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(RegistrarSalidaDeInventarioUseCase.STOCK_INSUFICIENTE, error.getError());
        assertTrue(error.getMensaje().startsWith("Stock insuficiente"));
    }

    @Test
    void ignoraLasExistenciasVencidas() {
        // Arrange
        Existencia vencida = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", HOY.minusDays(1));
        Existencia vigente = existencia(ID_EXISTENCIA_2, ID_DETALLE_BOLSA, "10", "10", HOY);
        hayHarinaCon(vencida, vigente);

        // Act
        useCase.execute(consumo(ID_HARINA, "2"));

        // Assert
        assertEquals(0, new BigDecimal("10").compareTo(vencida.getContenidoActual()));
        assertEquals(0, new BigDecimal("8").compareTo(vigente.getContenidoActual()));
        assertEquals(ID_EXISTENCIA_2, movimientosGuardados.get(0).getExistenciaId().getValue());
    }

    @Test
    void ignoraLasExistenciasEnUnaUnidadDistintaALaDelProducto() {
        // Arrange
        Existencia enOtraUnidad = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null, ID_OTRA_UNIDAD);
        hayHarinaCon("0", enOtraUnidad);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(consumo(ID_HARINA, "1")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(0, new BigDecimal("10").compareTo(enOtraUnidad.getContenidoActual()));
    }

    @Test
    void soloConsultaLasExistenciasDeLasLineasDeLaHarinaYSusPresentaciones() {
        // Arrange: el lote trae ademas una linea de pan que no debe consultarse.
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(productoDirecto(ID_HARINA, "Harina")));
        when(loteRepository.obtenerPorProductosSinCargar(anyList())).thenReturn(List.of(loteDeCompra(ID_LOTE,
                detalle(ID_DETALLE_BOLSA, ID_BOLSA_HARINA, "10", "10", "50.00", null),
                detalle(ID_DETALLE_PAN_1, ID_PAN, "10", "10", "9.00", null))));
        when(existenciaRepository.obtenerDisponiblesParaConsumo(anyList()))
                .thenReturn(List.of(existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null)));

        // Act
        useCase.execute(consumo(ID_HARINA, "1"));

        // Assert
        verify(existenciaRepository).obtenerDisponiblesParaConsumo(argThat(detalles ->
                detalles.size() == 1 && ID_DETALLE_BOLSA.equals(detalles.get(0).getValue())));
    }

    @Test
    void unaCantidadQueRedondeaACeroNoDescuentaNiRegistraNada() {
        // Arrange
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(productoDirecto(ID_HARINA, "Harina")));

        // Act
        SalidaDeInventarioResponse respuesta = useCase.execute(consumo(ID_HARINA, "0.00001"));

        // Assert
        assertTrue(respuesta.getMovimientos().isEmpty());
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void sinFechaEnLaPeticionSeUsaElInstanteActual() {
        // Arrange
        hayHarinaCon(existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null));
        RegistrarSalidaDeInventarioRequest sinFecha = RegistrarSalidaDeInventarioRequest
                .deProduccion(ID_USUARIO, ID_PRODUCCION, "Consumo", null)
                .con(ID_HARINA, BigDecimal.ONE);

        // Act
        useCase.execute(sinFecha);

        // Assert
        assertNotNull(movimientosGuardados.get(0).getFechaMovimiento());
    }

    @Test
    void laExistenciaSeGuardaConSuContenidoYaDescontado() {
        // Arrange
        hayHarinaCon(existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null));
        ArgumentCaptor<Existencia> guardada = ArgumentCaptor.forClass(Existencia.class);

        // Act
        useCase.execute(consumo(ID_HARINA, "2.5"));

        // Assert
        verify(existenciaRepository).actualizar(argThat(id -> ID_EXISTENCIA_1.equals(id.getValue())), guardada.capture());
        assertEquals(0, new BigDecimal("7.5").compareTo(guardada.getValue().getContenidoActual()));
    }

    // ------------------------------------------------------------------ producto por unidades

    @Test
    void unProductoPorUnidadesDescuentaDeSusLineasEnOrdenFefoConUnMovimientoPorLinea() {
        // Arrange: la linea que vence antes tiene 2 panes; la otra, que no vence, 5.
        Lote venceAntes = loteDeCompra(ID_LOTE, detalle(ID_DETALLE_PAN_1, ID_PAN, "2", "2", "1.00", HOY.plusDays(2)));
        Lote sinVencimiento = loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_PAN_2, ID_PAN, "5", "5", "1.00", null));
        hayPanCon(sinVencimiento, venceAntes);

        // Act
        SalidaDeInventarioResponse respuesta = useCase.execute(venta(ID_PAN, "3"));

        // Assert
        assertEquals(0, venceAntes.obtenerDetalle(ID_DETALLE_PAN_1).getCantidadRestante().signum());
        assertEquals(0, new BigDecimal("4").compareTo(sinVencimiento.obtenerDetalle(ID_DETALLE_PAN_2).getCantidadRestante()));
        assertEquals(2, respuesta.getMovimientos().size());
        verify(loteRepository, times(2)).actualizar(any(), any());
        verify(existenciaRepository, never()).obtenerDisponiblesParaConsumo(anyList());
        verify(recalcularStockUseCase).execute(ID_PAN);

        // LOTE-17: cada linea tocada deja su movimiento 'Venta' con la venta.
        Movimiento primero = movimientosGuardados.get(0);
        assertEquals(ID_DETALLE_PAN_1, primero.getDetalleLoteId().getValue());
        assertNull(primero.getExistenciaId());
        assertEquals(0, new BigDecimal("2").compareTo(primero.getCantidad()));
        assertEquals(ID_TIPO_VENTA, primero.getTipoMovimientoId().getValue());
        assertEquals(ID_VENTA, primero.getVentaId().getValue());
        assertNull(primero.getProduccionId());
        assertEquals("Venta del ticket 7", primero.getMotivo());
        assertEquals(ID_DETALLE_PAN_2, movimientosGuardados.get(1).getDetalleLoteId().getValue());
        assertEquals(0, BigDecimal.ONE.compareTo(movimientosGuardados.get(1).getCantidad()));
    }

    @Test
    void sinUnidadesVigentesSuficientesLaSalidaSeRechazaSinDescontar() {
        // Arrange: 5 panes vencidos y 1 vigente; se piden 2.
        Lote vencido = loteDeCompra(ID_LOTE, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "5", "1.00", HOY.minusDays(1)));
        Lote vigente = loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_PAN_2, ID_PAN, "5", "1", "1.00", null));
        hayPanCon(vencido, vigente);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(venta(ID_PAN, "2")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(RegistrarSalidaDeInventarioUseCase.STOCK_INSUFICIENTE, error.getError());
        assertTrue(error.getMensaje().contains("se requieren 2 y hay 1 vigentes"));
        verify(loteRepository, never()).actualizar(any(), any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void siOtraOperacionDescontoAntesDeBloquearRespondeStockInsuficiente() {
        // Arrange: la lectura sin cargar ve 3 panes; leido con el bloqueo, el lote ya no tiene ninguno.
        hayPanCon(List.of(loteDeCompra(ID_LOTE, detalle(ID_DETALLE_PAN_1, ID_PAN, "3", "3", "1.00", null))),
                List.of(loteDeCompra(ID_LOTE, detalle(ID_DETALLE_PAN_1, ID_PAN, "3", "0", "1.00", null))));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(venta(ID_PAN, "2")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(RegistrarSalidaDeInventarioUseCase.STOCK_INSUFICIENTE, error.getError());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    // ------------------------------------------------------------------ plan de consumo

    @Test
    void unInsumoQueUsanDosLineasSeDescuentaUnaSolaVezConLaSuma() {
        // Arrange: 1 de harina directa + 5 de masa (2 de harina) = 3 de harina.
        Existencia harinaAbierta = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null);
        hayHarinaCon(harinaAbierta);
        laMasaLleva4DeHarinaCada10();

        // Act
        useCase.execute(produccion().con(ID_HARINA, BigDecimal.ONE).con(ID_MASA, new BigDecimal("5")));

        // Assert (GENERAL-14)
        assertEquals(0, new BigDecimal("7").compareTo(harinaAbierta.getContenidoActual()));
        assertEquals(1, movimientosGuardados.size());
        assertEquals(0, new BigDecimal("3").compareTo(movimientosGuardados.get(0).getCantidad()));
        verify(existenciaRepository, times(1)).obtenerDisponiblesParaConsumo(anyList());
        verify(recalcularStockUseCase, times(1)).execute(ID_HARINA);
    }

    @Test
    void lasEntradasDelPlanSeProcesanEnOrdenDeIdDeProducto() {
        // Arrange: el pan llega antes en la peticion, pero la harina tiene menor id.
        hayHarinaCon(existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null));
        hayPanCon(loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "5", "1.00", null)));

        // Act
        useCase.execute(produccion().con(ID_PAN, BigDecimal.ONE).con(ID_HARINA, BigDecimal.ONE));

        // Assert (GENERAL-15): primero los lotes, despues cada producto en orden de id.
        InOrder orden = inOrder(existenciaRepository, loteRepository, recalcularStockUseCase);
        orden.verify(loteRepository).obtenerPorIdsParaActualizar(anyList());
        orden.verify(existenciaRepository).obtenerDisponiblesParaConsumo(anyList());
        orden.verify(recalcularStockUseCase).execute(ID_HARINA);
        orden.verify(recalcularStockUseCase).execute(ID_PAN);
    }

    @Test
    void bloqueaDeUnaVezEnOrdenDeIdSoloLosLotesQueCubrenLoRequerido() {
        // Arrange: tres lotes de pan; los dos que vencen antes cubren los 4 pedidos.
        String idLote3 = "a1000000-0000-0000-0000-000000000003";
        Lote vencePrimero = loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_PAN_2, ID_PAN, "3", "3", "1.00", HOY.plusDays(1)));
        Lote venceDespues = loteDeCompra(ID_LOTE, detalle(ID_DETALLE_PAN_1, ID_PAN, "3", "3", "1.00", HOY.plusDays(2)));
        Lote noHaceFalta = loteDeCompra(idLote3, detalle("a2000000-0000-0000-0000-000000000009", ID_PAN, "3", "3", "1.00", null));
        hayPanCon(noHaceFalta, venceDespues, vencePrimero);

        // Act
        useCase.execute(venta(ID_PAN, "4"));

        // Assert (LOTE-27): una sola lectura con bloqueo, con los ids ordenados.
        verify(loteRepository, times(1)).obtenerPorIdsParaActualizar(argThat(ids ->
                ids.stream().map(LoteId::getValue).toList().equals(List.of(ID_LOTE, ID_LOTE_2))));
        verify(loteRepository, never()).obtenerPorDetalleIdParaActualizar(any());
    }

    @Test
    void informaTodosLosFaltantesYTrasElPrimeroNoDescuentaNada() {
        // Arrange: falta harina (sin bolsas cerradas) y falta pan; nada se descuenta.
        hayHarinaCon("0", existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "1", null));
        hayPanCon(loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "1", "1.00", null)));

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(produccion().con(ID_PAN, new BigDecimal("2")).con(ID_HARINA, new BigDecimal("3"))));

        // Assert
        assertEquals(RegistrarSalidaDeInventarioUseCase.STOCK_INSUFICIENTE, error.getError());
        assertEquals(List.of(ID_HARINA, ID_PAN), faltantes(error).stream().map(faltante -> faltante.get("idProducto")).toList());
        verify(movimientoRepository, never()).crear(any());
        verify(loteRepository, never()).actualizar(any(), any());
    }

    @Test
    void siUnFaltanteNoSeResuelveAbriendoElErrorEsStockInsuficienteAunqueOtroSi() {
        // Arrange: la harina se resolveria abriendo una bolsa, el pan no.
        hayHarinaCon("3", existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "1", null));
        hayPanCon(loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "0", "1.00", null)));

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(produccion().con(ID_HARINA, new BigDecimal("3")).con(ID_PAN, BigDecimal.ONE)));

        // Assert
        assertEquals(RegistrarSalidaDeInventarioUseCase.STOCK_INSUFICIENTE, error.getError());
        assertEquals(2, faltantes(error).size());
        assertNull(faltantes(error).get(0).get("bolsaSugerida"));
    }

    // ------------------------------------------------------------------ bajo demanda

    @Test
    void enUnaProduccionUnInsumoBajoDemandaDescuentaLosIngredientesDeSuRecetaEscalados() {
        // Arrange: consumir 5 de masa consume 2 de harina.
        Existencia harinaAbierta = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null);
        hayHarinaCon(harinaAbierta);
        laMasaLleva4DeHarinaCada10();

        // Act
        useCase.execute(consumo(ID_MASA, "5"));

        // Assert
        assertEquals(0, new BigDecimal("8").compareTo(harinaAbierta.getContenidoActual()));
        assertEquals(1, movimientosGuardados.size());
        assertEquals(ID_TIPO_PRODUCCION, movimientosGuardados.get(0).getTipoMovimientoId().getValue());
        verify(recalcularStockUseCase).execute(ID_HARINA);
        verify(recalcularStockUseCase, never()).execute(ID_MASA);
    }

    @Test
    void enUnaVentaUnProductoBajoDemandaConsumeLosInsumosDeSuRecetaConMovimientosDeVenta() {
        // Arrange (VENTA-24): vender 5 de masa consume 2 de harina.
        Existencia harinaAbierta = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null);
        hayHarinaCon(harinaAbierta);
        laMasaLleva4DeHarinaCada10();

        // Act
        useCase.execute(venta(ID_MASA, "5"));

        // Assert
        assertEquals(0, new BigDecimal("8").compareTo(harinaAbierta.getContenidoActual()));
        Movimiento movimiento = movimientosGuardados.get(0);
        assertEquals(ID_TIPO_VENTA, movimiento.getTipoMovimientoId().getValue());
        assertEquals(ID_VENTA, movimiento.getVentaId().getValue());
        assertEquals(ID_EXISTENCIA_1, movimiento.getExistenciaId().getValue());
    }

    @Test
    void unIngredienteBajoDemandaExplotaASuVezSuReceta() {
        // Arrange: pan (rinde 1) lleva 1 de masa; masa (rinde 2) lleva 6 de harina => 3 de harina por pan.
        Existencia harinaAbierta = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null);
        hayHarinaCon(harinaAbierta);
        when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_PAN.equals(id.getValue()))))
                .thenReturn(Optional.of(productoBajoDemanda(ID_PAN, "Pan")));
        when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_MASA.equals(id.getValue()))))
                .thenReturn(Optional.of(productoBajoDemanda(ID_MASA, "Masa")));
        when(recetaRepository.obtenerPorProductoObtenido(argThat(id -> id != null && ID_PAN.equals(id.getValue()))))
                .thenReturn(Optional.of(receta("a5000000-0000-0000-0000-000000000001", ID_PAN, "1",
                        ingrediente("a6000000-0000-0000-0000-000000000001", ID_MASA, "1"))));
        when(recetaRepository.obtenerPorProductoObtenido(argThat(id -> id != null && ID_MASA.equals(id.getValue()))))
                .thenReturn(Optional.of(receta("a5000000-0000-0000-0000-000000000002", ID_MASA, "2",
                        ingrediente("a6000000-0000-0000-0000-000000000002", ID_HARINA, "6"))));

        // Act
        useCase.execute(consumo(ID_PAN, "1"));

        // Assert
        assertEquals(0, new BigDecimal("7").compareTo(harinaAbierta.getContenidoActual()));
        assertEquals(ID_TIPO_PRODUCCION, movimientosGuardados.get(0).getTipoMovimientoId().getValue());
    }

    @Test
    void unProductoBajoDemandaSinRecetaNoSePuedeDescontarNiEnUnaVenta() {
        // Arrange
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(productoBajoDemanda(ID_MASA, "Masa")));
        when(recetaRepository.obtenerPorProductoObtenido(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(venta(ID_MASA, "1")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("no tiene receta"));
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void unaRecetaQueSeContieneASiMismaSeRechaza() {
        // Arrange: el pan lleva masa y la masa lleva pan.
        when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_PAN.equals(id.getValue()))))
                .thenReturn(Optional.of(productoBajoDemanda(ID_PAN, "Pan")));
        when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_MASA.equals(id.getValue()))))
                .thenReturn(Optional.of(productoBajoDemanda(ID_MASA, "Masa")));
        when(recetaRepository.obtenerPorProductoObtenido(argThat(id -> id != null && ID_PAN.equals(id.getValue()))))
                .thenReturn(Optional.of(receta("a5000000-0000-0000-0000-000000000001", ID_PAN, "1",
                        ingrediente("a6000000-0000-0000-0000-000000000001", ID_MASA, "1"))));
        when(recetaRepository.obtenerPorProductoObtenido(argThat(id -> id != null && ID_MASA.equals(id.getValue()))))
                .thenReturn(Optional.of(receta("a5000000-0000-0000-0000-000000000002", ID_MASA, "1",
                        ingrediente("a6000000-0000-0000-0000-000000000002", ID_PAN, "1"))));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(consumo(ID_PAN, "1")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("se contiene a si misma"));
    }

    // ------------------------------------------------------------------ precondiciones

    @Test
    void unProductoInexistenteRespondeNoEncontrado() {
        // Arrange
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(consumo(ID_HARINA, "1")));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID_HARINA));
    }

    @Test
    void sinElEstadoAgotadaSembradoNoSeDescuentaNingunaExistencia() {
        // Arrange
        hayHarinaCon(existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null));
        when(estadoExistenciaRepository.obtenerPorNombre("Agotada")).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(consumo(ID_HARINA, "1")));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(existenciaRepository, never()).actualizar(any(), any());
    }

    @Test
    void sinElTipoDeMovimientoSembradoNoSeDescuentaNada() {
        // Arrange
        hayPanCon(loteDeCompra(ID_LOTE, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "5", "1.00", null)));
        when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.VENTA)).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(venta(ID_PAN, "1")));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains("'Venta'"));
        verify(loteRepository, never()).actualizar(any(), any());
    }
}
