package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AGOTADA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AHORA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AJUSTE;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.HOY;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_ESTADO_ABIERTA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_ESTADO_AGOTADA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_OTRA_UNIDAD;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO_AJUSTE;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO_RECETA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_USUARIO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.RECETA;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.SalidaDeInventarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.DatosDePrueba;

/**
 * Pruebas unitarias del consumo de inventario (INV-32): FEFO sobre existencias abiertas para un
 * producto de stock directo y explosion de receta para uno bajo demanda. Todos los
 * repositorios son dobles de Mockito.
 *
 * Escenario base: harina comprada en un lote a 50.00 la unidad, con existencias abiertas de
 * 10 unidades de contenido, es decir, 5.00 por unidad de contenido.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarSalidaDeInventarioUseCaseTest {

    private static final String ID_HARINA = "a0000000-0000-0000-0000-000000000001";
    private static final String ID_BOLSA_HARINA = "a0000000-0000-0000-0000-000000000002";
    private static final String ID_PAN = "a0000000-0000-0000-0000-000000000003";
    private static final String ID_MASA = "a0000000-0000-0000-0000-000000000004";
    private static final String ID_LOTE = "a1000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE = "a2000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE_BOLSA = "a2000000-0000-0000-0000-000000000002";
    private static final String ID_EXISTENCIA_1 = "a3000000-0000-0000-0000-000000000001";
    private static final String ID_EXISTENCIA_2 = "a3000000-0000-0000-0000-000000000002";
    private static final String ID_PRODUCCION = "a4000000-0000-0000-0000-000000000001";

    @Mock private IProductoRepository productoRepository;
    @Mock private IRecetaRepository recetaRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private RegistrarSalidaDeInventarioUseCase useCase;
    private final List<Movimiento> movimientosGuardados = new ArrayList<>();

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new RegistrarSalidaDeInventarioUseCase(productoRepository, recetaRepository, loteRepository,
                existenciaRepository, estadoExistenciaRepository, movimientoRepository, tipoMovimientoRepository,
                recalcularStockUseCase);

        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Agotada")).thenReturn(Optional.of(AGOTADA));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.AJUSTE)).thenReturn(Optional.of(AJUSTE));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.RECETA)).thenReturn(Optional.of(RECETA));
        lenient().when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of());
        lenient().when(existenciaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> {
            Movimiento guardado = DatosDePrueba.guardado(llamada.getArgument(0), "ad000000-0000-0000-0000-00000000000" + movimientosGuardados.size());
            movimientosGuardados.add(guardado);
            return guardado;
        });
    }

    /** Salida por venta: lo directo es 'Ajuste' y lo explotado de una receta es 'Receta'. */
    private static RegistrarSalidaDeInventarioRequest salida(String idProducto, String cantidad) {
        return RegistrarSalidaDeInventarioRequest.crear(idProducto, new BigDecimal(cantidad),
                TipoMovimiento.AJUSTE, TipoMovimiento.RECETA, ID_USUARIO, null, "Venta 'v-1'", AHORA);
    }

    private void hayHarinaCon(Existencia... existencias) {
        hayHarinaCon("10", existencias);
    }

    /** 'cerradas' es la cantidad restante de la linea de lote: unidades que aun no se abrieron. */
    private void hayHarinaCon(String cerradas, Existencia... existencias) {
        Producto harina = productoDirecto(ID_HARINA, "Harina");
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_HARINA.equals(id.getValue()))))
                .thenReturn(Optional.of(harina));
        lenient().when(loteRepository.obtenerPorProductos(anyList()))
                .thenReturn(List.of(loteDeCompra(ID_LOTE, detalle(ID_DETALLE, ID_HARINA, "10", cerradas, "50.00", null))));
        lenient().when(existenciaRepository.obtenerDisponiblesParaConsumo(anyList())).thenReturn(List.of(existencias));
    }

    // ------------------------------------------------------------------ stock directo

    @Test
    void unProductoDeStockDirectoSaleDeSuExistenciaYRegistraUnMovimiento() {
        // Arrange
        Existencia abierta = existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", null);
        hayHarinaCon(abierta);

        // Act
        SalidaDeInventarioResponse respuesta = useCase.execute(salida(ID_HARINA, "2"));

        // Assert
        assertEquals(0, new BigDecimal("8").compareTo(abierta.getContenidoActual()));
        assertEquals(0, new BigDecimal("10").compareTo(respuesta.getCostoTotal()));
        assertEquals(1, respuesta.getMovimientos().size());

        Movimiento movimiento = movimientosGuardados.get(0);
        assertEquals(0, new BigDecimal("2").compareTo(movimiento.getCantidad()));
        assertEquals(ID_EXISTENCIA_1, movimiento.getExistenciaId().getValue());
        assertEquals(ID_TIPO_AJUSTE, movimiento.getTipoMovimientoId().getValue());
        assertEquals(ID_USUARIO, movimiento.getUsuarioId().getValue());
        assertEquals("Venta 'v-1'", movimiento.getMotivo());
        assertEquals(AHORA, movimiento.getFechaMovimiento());
        assertNull(movimiento.getProduccionId());
        verify(existenciaRepository).actualizar(any(), any());
        verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    @Test
    void elConsumoSeReparteEntreExistenciasEnElOrdenRecibidoYAgotaLaPrimera() {
        // Arrange
        Existencia primera = existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "3", null);
        Existencia segunda = existencia(ID_EXISTENCIA_2, ID_DETALLE, "10", "10", null);
        hayHarinaCon(primera, segunda);

        // Act
        SalidaDeInventarioResponse respuesta = useCase.execute(salida(ID_HARINA, "5"));

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
        assertEquals(0, new BigDecimal("25").compareTo(respuesta.getCostoTotal()));
        verify(recalcularStockUseCase, times(1)).execute(ID_HARINA);
    }

    @Test
    void noTocaLasExistenciasQueNoHacenFalta() {
        // Arrange
        Existencia primera = existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", null);
        Existencia segunda = existencia(ID_EXISTENCIA_2, ID_DETALLE, "10", "10", null);
        hayHarinaCon(primera, segunda);

        // Act
        useCase.execute(salida(ID_HARINA, "10"));

        // Assert
        assertTrue(primera.isAgotada());
        assertEquals(0, new BigDecimal("10").compareTo(segunda.getContenidoActual()));
        assertEquals(1, movimientosGuardados.size());
        verify(existenciaRepository, times(1)).actualizar(any(), any());
    }

    @Test
    void sinStockAbiertoNiUnidadesCerradasRechazaLaSalidaSinDescontarNada() {
        // Arrange
        Existencia abierta = existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "4", null);
        hayHarinaCon("0", abierta);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(salida(ID_HARINA, "5")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().startsWith("Stock insuficiente de 'Harina'"));
        assertTrue(error.getMensaje().contains("se requieren 5 y hay 4"));
        assertEquals(0, new BigDecimal("4").compareTo(abierta.getContenidoActual()));
        verify(existenciaRepository, never()).actualizar(any(), any());
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void siHayUnidadesCerradasElErrorPideAbrirUnaPresentacion() {
        // Arrange
        hayHarinaCon("3", existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "4", null));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(salida(ID_HARINA, "5")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("abre una presentacion"));
        assertTrue(error.getMensaje().contains(ID_HARINA));
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void lasUnidadesCerradasYaVencidasNoCuentanParaSugerirUnaApertura() {
        // Arrange
        Producto harina = productoDirecto(ID_HARINA, "Harina");
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(harina));
        when(loteRepository.obtenerPorProductos(anyList()))
                .thenReturn(List.of(loteDeCompra(ID_LOTE, detalle(ID_DETALLE, ID_HARINA, "10", "3", "50.00", HOY.minusDays(1)))));
        when(existenciaRepository.obtenerDisponiblesParaConsumo(anyList())).thenReturn(List.of());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(salida(ID_HARINA, "1")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().startsWith("Stock insuficiente"));
    }

    @Test
    void ignoraLasExistenciasVencidas() {
        // Arrange
        Existencia vencida = existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", HOY.minusDays(1));
        Existencia vigente = existencia(ID_EXISTENCIA_2, ID_DETALLE, "10", "10", HOY);
        hayHarinaCon(vencida, vigente);

        // Act
        useCase.execute(salida(ID_HARINA, "2"));

        // Assert
        assertEquals(0, new BigDecimal("10").compareTo(vencida.getContenidoActual()));
        assertEquals(0, new BigDecimal("8").compareTo(vigente.getContenidoActual()));
        assertEquals(ID_EXISTENCIA_2, movimientosGuardados.get(0).getExistenciaId().getValue());
    }

    @Test
    void ignoraLasExistenciasEnUnaUnidadDistintaALaDelProducto() {
        // Arrange
        Existencia enOtraUnidad = existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", null, ID_OTRA_UNIDAD);
        hayHarinaCon("0", enOtraUnidad);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(salida(ID_HARINA, "1")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(0, new BigDecimal("10").compareTo(enOtraUnidad.getContenidoActual()));
    }

    @Test
    void consumeTambienLasExistenciasAbiertasDesdeUnaPresentacion() {
        // Arrange
        Producto harina = productoDirecto(ID_HARINA, "Harina");
        Existencia desdeLaBolsa = existencia(ID_EXISTENCIA_1, ID_DETALLE_BOLSA, "10", "10", null);
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(harina));
        when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of(presentacion(ID_BOLSA_HARINA, "Bolsa de harina", ID_HARINA)));
        when(loteRepository.obtenerPorProductos(argThat(fuentes -> fuentes != null && fuentes.size() == 2)))
                .thenReturn(List.of(loteDeCompra(ID_LOTE, detalle(ID_DETALLE_BOLSA, ID_BOLSA_HARINA, "10", "10", "50.00", null))));
        when(existenciaRepository.obtenerDisponiblesParaConsumo(argThat(detalles ->
                detalles != null && detalles.size() == 1 && ID_DETALLE_BOLSA.equals(detalles.get(0).getValue()))))
                .thenReturn(List.of(desdeLaBolsa));

        // Act
        useCase.execute(salida(ID_HARINA, "4"));

        // Assert
        assertEquals(0, new BigDecimal("6").compareTo(desdeLaBolsa.getContenidoActual()));
        verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    @Test
    void soloConsultaLasExistenciasDeLasLineasDelProductoPedido() {
        // Arrange: el lote trae ademas una linea de otro producto que no debe consultarse.
        Producto harina = productoDirecto(ID_HARINA, "Harina");
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(harina));
        when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(loteDeCompra(ID_LOTE,
                detalle(ID_DETALLE, ID_HARINA, "10", "10", "50.00", null),
                detalle(ID_DETALLE_BOLSA, ID_PAN, "10", "10", "9.00", null))));
        when(existenciaRepository.obtenerDisponiblesParaConsumo(anyList()))
                .thenReturn(List.of(existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", null)));

        // Act
        useCase.execute(salida(ID_HARINA, "1"));

        // Assert
        verify(existenciaRepository).obtenerDisponiblesParaConsumo(argThat(detalles ->
                detalles.size() == 1 && ID_DETALLE.equals(detalles.get(0).getValue())));
    }

    @Test
    void unaCantidadQueRedondeaACeroNoDescuentaNiRegistraNada() {
        // Arrange
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(productoDirecto(ID_HARINA, "Harina")));

        // Act
        SalidaDeInventarioResponse respuesta = useCase.execute(salida(ID_HARINA, "0.00001"));

        // Assert
        assertEquals(0, respuesta.getCostoTotal().signum());
        assertTrue(respuesta.getMovimientos().isEmpty());
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void elConsumoDeUnaProduccionQuedaVinculadoAElla() {
        // Arrange
        hayHarinaCon(existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", null));
        RegistrarSalidaDeInventarioRequest consumo = RegistrarSalidaDeInventarioRequest.crear(ID_HARINA, new BigDecimal("1"),
                TipoMovimiento.RECETA, TipoMovimiento.RECETA, ID_USUARIO, ID_PRODUCCION, "Consumo de la produccion 'Pan'", AHORA);

        // Act
        useCase.execute(consumo);

        // Assert
        Movimiento movimiento = movimientosGuardados.get(0);
        assertEquals(ID_PRODUCCION, movimiento.getProduccionId().getValue());
        assertEquals(ID_TIPO_RECETA, movimiento.getTipoMovimientoId().getValue());
    }

    @Test
    void sinFechaEnLaPeticionSeUsaElInstanteActual() {
        // Arrange
        hayHarinaCon(existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", null));
        RegistrarSalidaDeInventarioRequest sinFecha = RegistrarSalidaDeInventarioRequest.crear(ID_HARINA, new BigDecimal("1"),
                TipoMovimiento.AJUSTE, TipoMovimiento.RECETA, ID_USUARIO, null, "Venta", null);

        // Act
        useCase.execute(sinFecha);

        // Assert
        assertNotNull(movimientosGuardados.get(0).getFechaMovimiento());
    }

    // ------------------------------------------------------------------ bajo demanda

    @Test
    void unProductoBajoDemandaDescuentaLosIngredientesDeSuRecetaEscalados() {
        // Arrange: la receta rinde 10 panes con 4 de harina; pedir 5 panes consume 2 de harina.
        Existencia harinaAbierta = existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", null);
        hayHarinaCon(harinaAbierta);
        when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_PAN.equals(id.getValue()))))
                .thenReturn(Optional.of(productoBajoDemanda(ID_PAN, "Pan")));
        when(recetaRepository.obtenerPorProductoObtenido(any()))
                .thenReturn(Optional.of(receta("a5000000-0000-0000-0000-000000000001", ID_PAN, "10",
                        ingrediente("a6000000-0000-0000-0000-000000000001", ID_HARINA, "4"))));

        // Act
        SalidaDeInventarioResponse respuesta = useCase.execute(salida(ID_PAN, "5"));

        // Assert
        assertEquals(0, new BigDecimal("8").compareTo(harinaAbierta.getContenidoActual()));
        assertEquals(0, new BigDecimal("10").compareTo(respuesta.getCostoTotal()));
        assertEquals(1, movimientosGuardados.size());
        assertEquals(ID_TIPO_RECETA, movimientosGuardados.get(0).getTipoMovimientoId().getValue());
        verify(recalcularStockUseCase).execute(ID_HARINA);
        verify(recalcularStockUseCase, never()).execute(ID_PAN);
    }

    @Test
    void unIngredienteBajoDemandaExplotaASuVezSuReceta() {
        // Arrange: pan (rinde 1) lleva 1 de masa; masa (rinde 2) lleva 6 de harina => 3 de harina por pan.
        Existencia harinaAbierta = existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", null);
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
        useCase.execute(salida(ID_PAN, "1"));

        // Assert
        assertEquals(0, new BigDecimal("7").compareTo(harinaAbierta.getContenidoActual()));
        assertEquals(ID_TIPO_RECETA, movimientosGuardados.get(0).getTipoMovimientoId().getValue());
    }

    @Test
    void unProductoBajoDemandaSinRecetaNoSePuedeDescontar() {
        // Arrange
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(productoBajoDemanda(ID_PAN, "Pan")));
        when(recetaRepository.obtenerPorProductoObtenido(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(salida(ID_PAN, "1")));

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
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(salida(ID_PAN, "1")));

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
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(salida(ID_HARINA, "1")));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID_HARINA));
    }

    @Test
    void sinElEstadoAgotadaSembradoNoSeEmpiezaADescontar() {
        // Arrange
        when(estadoExistenciaRepository.obtenerPorNombre("Agotada")).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(salida(ID_HARINA, "1")));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(productoRepository, never()).obtenerPorId(any());
    }

    @Test
    void sinElTipoDeMovimientoSembradoNoSeEmpiezaADescontar() {
        // Arrange
        when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.RECETA)).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(salida(ID_HARINA, "1")));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains("'Receta'"));
        verify(productoRepository, never()).obtenerPorId(any());
    }

    @Test
    void laExistenciaSeGuardaConSuContenidoYaDescontado() {
        // Arrange
        hayHarinaCon(existencia(ID_EXISTENCIA_1, ID_DETALLE, "10", "10", null));
        ArgumentCaptor<Existencia> guardada = ArgumentCaptor.forClass(Existencia.class);

        // Act
        useCase.execute(salida(ID_HARINA, "2.5"));

        // Assert
        verify(existenciaRepository).actualizar(argThat(id -> ID_EXISTENCIA_1.equals(id.getValue())), guardada.capture());
        assertEquals(0, new BigDecimal("7.5").compareTo(guardada.getValue().getContenidoActual()));
    }
}
