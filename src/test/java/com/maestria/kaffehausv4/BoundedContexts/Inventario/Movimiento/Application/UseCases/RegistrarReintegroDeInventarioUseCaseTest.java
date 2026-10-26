package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ABIERTA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AGOTADA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AHORA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.HOY;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_ESTADO_ABIERTA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO_PRODUCCION;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO_REINTEGRO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO_VENTA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_USUARIO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.REINTEGRO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.VENTA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.detalle;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.existencia;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.loteDeCompra;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.presentacion;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias del reintegro de una venta anulada (logica.md VENTA-25, LOTE-24,
 * EXISTENCIA-19): lo que se devuelve sale de los movimientos 'Venta' de la venta y vuelve al mismo
 * sujeto, en el orden de bloqueo de GENERAL-15. Todos los repositorios son dobles de Mockito.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarReintegroDeInventarioUseCaseTest {

    private static final String ID_HARINA = "b0000000-0000-0000-0000-000000000001";
    private static final String ID_BOLSA_HARINA = "b0000000-0000-0000-0000-000000000002";
    private static final String ID_PAN = "b0000000-0000-0000-0000-000000000003";
    private static final String ID_LOTE_1 = "b1000000-0000-0000-0000-000000000001";
    private static final String ID_LOTE_2 = "b1000000-0000-0000-0000-000000000002";
    private static final String ID_DETALLE_PAN_1 = "b2000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE_PAN_2 = "b2000000-0000-0000-0000-000000000002";
    private static final String ID_DETALLE_BOLSA = "b2000000-0000-0000-0000-000000000003";
    private static final String ID_EXISTENCIA = "b3000000-0000-0000-0000-000000000001";
    private static final String ID_VENTA = "b4000000-0000-0000-0000-000000000001";

    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private RegistrarReintegroDeInventarioUseCase useCase;
    private final List<Movimiento> registrados = new ArrayList<>();
    private int secuencia;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new RegistrarReintegroDeInventarioUseCase(movimientoRepository, tipoMovimientoRepository, loteRepository,
                existenciaRepository, estadoExistenciaRepository, productoRepository, recalcularStockUseCase);

        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.VENTA)).thenReturn(Optional.of(VENTA));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.REINTEGRO)).thenReturn(Optional.of(REINTEGRO));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Abierta")).thenReturn(Optional.of(ABIERTA));
        lenient().when(loteRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(existenciaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> {
            registrados.add(llamada.getArgument(0));
            return llamada.getArgument(0);
        });
    }

    private static RegistrarReintegroDeInventarioRequest reintegro() {
        return RegistrarReintegroDeInventarioRequest.crear(ID_VENTA, ID_USUARIO, "Reintegro del ticket 7", AHORA);
    }

    private Movimiento salidaDeLinea(String idDetalle, String cantidad) {
        return salida(null, idDetalle, cantidad, ID_TIPO_VENTA);
    }

    private Movimiento salidaDeExistencia(String idExistencia, String cantidad) {
        return salida(idExistencia, null, cantidad, ID_TIPO_VENTA);
    }

    private Movimiento salida(String idExistencia, String idDetalle, String cantidad, String idTipo) {
        secuencia++;
        return Movimiento.restaurar("b5000000-0000-0000-0000-00000000000" + secuencia, new BigDecimal(cantidad), AHORA.minusHours(2),
                "Venta del ticket 7", idExistencia != null ? new ExistenciaId(idExistencia) : null,
                idDetalle != null ? new DetalleLoteId(idDetalle) : null, new TipoMovimientoId(idTipo), new UsuarioId(ID_USUARIO),
                null, new VentaId(ID_VENTA), false);
    }

    private void laVentaDesconto(Movimiento... movimientos) {
        when(movimientoRepository.obtenerPorVenta(argThat(id -> ID_VENTA.equals(id.getValue())))).thenReturn(List.of(movimientos));
    }

    /**
     * Las lineas de pan, cada una en su lote: su producto y su lote se averiguan sin cargarlas, y
     * los lotes se bloquean por su raiz.
     */
    private void hayLineasDePan(Lote... lotes) {
        lenient().when(loteRepository.obtenerProductoDeDetalle(any())).thenAnswer(llamada -> {
            DetalleLoteId id = llamada.getArgument(0, DetalleLoteId.class);
            return id == null ? Optional.empty()
                    : loteCon(id.getValue(), lotes).map(lote -> lote.obtenerDetalle(id.getValue()).getProductoId());
        });
        lenient().when(loteRepository.obtenerLoteDeDetalle(any())).thenAnswer(llamada -> {
            DetalleLoteId id = llamada.getArgument(0, DetalleLoteId.class);
            return id == null ? Optional.empty() : loteCon(id.getValue(), lotes).map(Lote::getId);
        });
        lenient().when(loteRepository.obtenerPorIdsParaActualizar(anyList())).thenAnswer(llamada -> {
            List<String> ids = llamada.<List<LoteId>>getArgument(0).stream().map(LoteId::getValue).toList();
            return List.of(lotes).stream().filter(lote -> ids.contains(lote.getId().getValue())).toList();
        });
    }

    private static Optional<Lote> loteCon(String idDetalle, Lote... lotes) {
        for (Lote lote : lotes) {
            if (lote.getDetallesActivos().stream().anyMatch(detalle -> detalle.getId().getValue().equals(idDetalle))) {
                return Optional.of(lote);
            }
        }
        return Optional.empty();
    }

    /** Una existencia de harina abierta desde la linea de la bolsa. */
    private void hayExistenciaDeHarina(Existencia existencia) {
        when(existenciaRepository.obtenerDetalleLoteDe(argThat(id -> ID_EXISTENCIA.equals(id.getValue()))))
                .thenReturn(Optional.of(new DetalleLoteId(ID_DETALLE_BOLSA)));
        // doReturn: un when(...) invocaria la respuesta generica de hayLineasDePan con un argumento nulo.
        lenient().doReturn(Optional.of(new ProductoId(ID_BOLSA_HARINA)))
                .when(loteRepository).obtenerProductoDeDetalle(argThat(id -> id != null && ID_DETALLE_BOLSA.equals(id.getValue())));
        when(productoRepository.obtenerPorId(argThat(id -> ID_BOLSA_HARINA.equals(id.getValue()))))
                .thenReturn(Optional.of(presentacion(ID_BOLSA_HARINA, "Bolsa de harina", ID_HARINA)));
        when(existenciaRepository.obtenerPorIdParaActualizar(argThat(id -> ID_EXISTENCIA.equals(id.getValue()))))
                .thenReturn(Optional.of(existencia));
    }

    private static Existencia agotada(String inicial) {
        Existencia existencia = existencia(ID_EXISTENCIA, ID_DETALLE_BOLSA, inicial, inicial, HOY);
        existencia.consumir(new BigDecimal(inicial), AHORA.minusHours(1), new EstadoExistenciaId(AGOTADA.getId().getValue()));
        return existencia;
    }

    // ------------------------------------------------------------------ lineas de lote

    @Test
    void cadaLineaRecuperaExactamenteLoQueLaVentaDescontoDeElla() {
        // Arrange: la venta saco 2 panes de una linea y 1 de otra.
        Lote primero = loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "3", "1.00", null));
        Lote segundo = loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_PAN_2, ID_PAN, "5", "4", "1.00", null));
        hayLineasDePan(primero, segundo);
        laVentaDesconto(salidaDeLinea(ID_DETALLE_PAN_1, "2"), salidaDeLinea(ID_DETALLE_PAN_2, "1"));

        // Act
        useCase.execute(reintegro());

        // Assert
        assertEquals(0, new BigDecimal("5").compareTo(primero.obtenerDetalle(ID_DETALLE_PAN_1).getCantidadRestante()));
        assertEquals(0, new BigDecimal("5").compareTo(segundo.obtenerDetalle(ID_DETALLE_PAN_2).getCantidadRestante()));
        assertEquals(2, registrados.size());
        Movimiento devolucion = registrados.get(0);
        assertEquals(ID_TIPO_REINTEGRO, devolucion.getTipoMovimientoId().getValue());
        assertEquals(ID_DETALLE_PAN_1, devolucion.getDetalleLoteId().getValue());
        assertEquals(ID_VENTA, devolucion.getVentaId().getValue());
        assertEquals("Reintegro del ticket 7", devolucion.getMotivo());
        assertEquals(AHORA, devolucion.getFechaMovimiento());
        verify(recalcularStockUseCase, times(1)).execute(ID_PAN);
    }

    @Test
    void variasSalidasDeLaMismaLineaVuelvenEnUnaSolaDevolucion() {
        // Arrange
        Lote lote = loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "2", "1.00", null));
        hayLineasDePan(lote);
        laVentaDesconto(salidaDeLinea(ID_DETALLE_PAN_1, "2"), salidaDeLinea(ID_DETALLE_PAN_1, "1"));

        // Act
        useCase.execute(reintegro());

        // Assert
        assertEquals(0, new BigDecimal("5").compareTo(lote.obtenerDetalle(ID_DETALLE_PAN_1).getCantidadRestante()));
        assertEquals(1, registrados.size());
        assertEquals(0, new BigDecimal("3").compareTo(registrados.get(0).getCantidad()));
    }

    @Test
    void unaLineaVencidaRecibeIgualSusUnidades() {
        // Arrange
        Lote lote = loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "3", "1.00", HOY.minusDays(3)));
        hayLineasDePan(lote);
        laVentaDesconto(salidaDeLinea(ID_DETALLE_PAN_1, "2"));

        // Act
        useCase.execute(reintegro());

        // Assert
        assertEquals(0, new BigDecimal("5").compareTo(lote.obtenerDetalle(ID_DETALLE_PAN_1).getCantidadRestante()));
    }

    @Test
    void siLaLineaSeCorrigioYNoCabeLaDevolucionLaAnulacionFalla() {
        // Arrange: la linea tiene 5 de 5 (se corrigio despues de la venta) y se devuelven 2.
        hayLineasDePan(loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "5", "1.00", null)));
        laVentaDesconto(salidaDeLinea(ID_DETALLE_PAN_1, "2"));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(reintegro()));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void siLaLineaYaNoExisteLaAnulacionFalla() {
        // Arrange
        hayLineasDePan();
        laVentaDesconto(salidaDeLinea(ID_DETALLE_PAN_1, "2"));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(reintegro()));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID_DETALLE_PAN_1));
    }

    // ------------------------------------------------------------------ existencias

    @Test
    void unaExistenciaAgotadaRecuperaSuContenidoYVuelveAAbierta() {
        // Arrange (EXISTENCIA-19): la venta de un MTO vacio la existencia de harina.
        Existencia existencia = agotada("3");
        hayExistenciaDeHarina(existencia);
        laVentaDesconto(salidaDeExistencia(ID_EXISTENCIA, "3"));

        // Act
        useCase.execute(reintegro());

        // Assert
        assertEquals(0, new BigDecimal("3").compareTo(existencia.getContenidoActual()));
        assertEquals(ID_ESTADO_ABIERTA, existencia.getEstadoExistenciaId().getValue());
        assertNull(existencia.getFechaAcabado());
        Movimiento devolucion = registrados.get(0);
        assertEquals(ID_EXISTENCIA, devolucion.getExistenciaId().getValue());
        assertNull(devolucion.getDetalleLoteId());
        assertEquals(ID_TIPO_REINTEGRO, devolucion.getTipoMovimientoId().getValue());
        // El stock que cambia es el del producto base de la presentacion abierta.
        verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    // ------------------------------------------------------------------ orden y filtros

    @Test
    void procesaLosProductosEnOrdenDeIdYCadaUnoAntesDeRecalcularSuStock() {
        // Arrange: la harina (menor id) llega despues en los movimientos.
        Lote lote = loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "3", "1.00", null));
        hayLineasDePan(lote);
        hayExistenciaDeHarina(agotada("3"));
        laVentaDesconto(salidaDeLinea(ID_DETALLE_PAN_1, "2"), salidaDeExistencia(ID_EXISTENCIA, "3"));

        // Act
        useCase.execute(reintegro());

        // Assert (GENERAL-15): primero los lotes; despues, por producto, existencias y stock.
        InOrder orden = inOrder(existenciaRepository, loteRepository, recalcularStockUseCase);
        orden.verify(loteRepository).obtenerPorIdsParaActualizar(argThat(ids ->
                ids.stream().map(LoteId::getValue).toList().equals(List.of(ID_LOTE_1))));
        orden.verify(existenciaRepository).obtenerPorIdParaActualizar(any());
        orden.verify(recalcularStockUseCase).execute(ID_HARINA);
        orden.verify(recalcularStockUseCase).execute(ID_PAN);
    }

    @Test
    void soloDevuelveLosMovimientosDeTipoVenta() {
        // Arrange: un movimiento de otro tipo con la misma venta no se devuelve.
        Lote lote = loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_PAN_1, ID_PAN, "5", "1", "1.00", null));
        hayLineasDePan(lote);
        laVentaDesconto(salidaDeLinea(ID_DETALLE_PAN_1, "2"), salida(null, ID_DETALLE_PAN_1, "2", ID_TIPO_PRODUCCION));

        // Act
        useCase.execute(reintegro());

        // Assert
        assertEquals(0, new BigDecimal("3").compareTo(lote.obtenerDetalle(ID_DETALLE_PAN_1).getCantidadRestante()));
    }

    @Test
    void unaVentaSinSalidasRegistradasNoSePuedeReintegrar() {
        // Arrange
        when(movimientoRepository.obtenerPorVenta(any())).thenReturn(List.of());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(reintegro()));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("anulala sin reintegro"));
    }

    @Test
    void sinElTipoReintegroSembradoNoDevuelveNada() {
        // Arrange
        when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.REINTEGRO)).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(reintegro()));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains("'Reintegro'"));
        verify(loteRepository, never()).actualizar(any(), any());
    }
}
