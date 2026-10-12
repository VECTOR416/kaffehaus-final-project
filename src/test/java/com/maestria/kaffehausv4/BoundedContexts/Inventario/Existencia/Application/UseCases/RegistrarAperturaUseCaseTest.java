package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarAperturaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.RegistrarAperturaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.DatosDePrueba;

/**
 * Pruebas unitarias de la apertura de una unidad cerrada (INV-09, INV-10): saca una unidad de
 * la linea de lote y crea una existencia con su contenido en la unidad del producto base.
 *
 * Escenario: una bolsa de harina (presentacion, en kilogramos) cuyo producto base es la harina
 * (en gramos), con la conversion kilogramo -> gramo = 1000.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarAperturaUseCaseTest {

    private static final String ID_HARINA = "e0000000-0000-0000-0000-000000000001";
    private static final String ID_BOLSA = "e0000000-0000-0000-0000-000000000002";
    private static final String ID_LOTE = "e1000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE = "e2000000-0000-0000-0000-000000000001";
    private static final String ID_EXISTENCIA_NUEVA = "e3000000-0000-0000-0000-000000000001";

    @Mock private ILoteRepository loteRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private IConversionRepository conversionRepository;
    @Mock private IUnidadRepository unidadRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private RegistrarAperturaUseCase useCase;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new RegistrarAperturaUseCase(loteRepository, productoRepository, conversionRepository, unidadRepository,
                existenciaRepository, estadoExistenciaRepository, movimientoRepository, tipoMovimientoRepository, recalcularStockUseCase);

        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_BOLSA.equals(id.getValue()))))
                .thenReturn(Optional.of(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA)));
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_HARINA.equals(id.getValue()))))
                .thenReturn(Optional.of(productoDirecto(ID_HARINA, "Harina")));
        lenient().when(conversionRepository.obtenerVivas()).thenReturn(List.of(
                Conversion.crear(new BigDecimal("1000"), false, new UnidadId(ID_OTRA_UNIDAD), new UnidadId(ID_UNIDAD))));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Abierta")).thenReturn(Optional.of(ABIERTA));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.AJUSTE)).thenReturn(Optional.of(AJUSTE));
        lenient().when(unidadRepository.obtenerPorId(any())).thenReturn(Optional.of(UNIDAD));
        lenient().when(existenciaRepository.crear(any())).thenAnswer(llamada -> DatosDePrueba.guardada(llamada.getArgument(0), ID_EXISTENCIA_NUEVA));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> DatosDePrueba.guardado(llamada.getArgument(0), "ed000000-0000-0000-0000-000000000001"));
        lenient().when(loteRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    private Lote hayLoteCon(String idProducto, String cerradas, LocalDate vence) {
        Lote lote = loteDeCompra(ID_LOTE, detalle(ID_DETALLE, idProducto, "5", cerradas, "50", vence));
        lenient().when(loteRepository.obtenerPorDetalleIdParaActualizar(any())).thenReturn(Optional.of(lote));
        return lote;
    }

    private static RegistrarAperturaRequest apertura(boolean permitirVencido) {
        return RegistrarAperturaRequest.crear(ID_DETALLE, ID_USUARIO, permitirVencido, AHORA);
    }

    @Test
    void abrirSacaUnaUnidadDelLoteYCreaLaExistenciaEnLaUnidadDelProductoBase() {
        // Arrange
        LocalDate caducidad = HOY.plusDays(60);
        Lote lote = hayLoteCon(ID_BOLSA, "5", caducidad);
        ArgumentCaptor<Existencia> creada = ArgumentCaptor.forClass(Existencia.class);

        // Act
        ExistenciaResponse respuesta = useCase.execute(apertura(false));

        // Assert
        assertEquals(new BigDecimal("4.00"), lote.obtenerDetalle(ID_DETALLE).getCantidadRestante());
        verify(loteRepository).actualizar(any(), any());

        verify(existenciaRepository).crear(creada.capture());
        assertEquals(0, new BigDecimal("1000").compareTo(creada.getValue().getContenidoInicial()));
        assertEquals(0, new BigDecimal("1000").compareTo(creada.getValue().getContenidoActual()));
        assertEquals(caducidad, creada.getValue().getFechaVencimiento());
        assertEquals(AHORA, creada.getValue().getFechaApertura());
        assertEquals(ID_DETALLE, creada.getValue().getDetalleLoteId().getValue());
        assertEquals(ID_ESTADO_ABIERTA, creada.getValue().getEstadoExistenciaId().getValue());
        assertEquals(ID_UNIDAD, creada.getValue().getUnidadId().getValue());

        assertEquals(ID_EXISTENCIA_NUEVA, respuesta.getId());
        assertEquals("Abierta", respuesta.getEstadoExistencia().getNombre());
    }

    @Test
    void laAperturaQuedaComoMovimientoDeAjusteSobreLaExistenciaNueva() {
        // Arrange
        hayLoteCon(ID_BOLSA, "5", null);
        ArgumentCaptor<Movimiento> movimiento = ArgumentCaptor.forClass(Movimiento.class);

        // Act
        useCase.execute(apertura(false));

        // Assert
        verify(movimientoRepository).crear(movimiento.capture());
        assertEquals(0, new BigDecimal("1000").compareTo(movimiento.getValue().getCantidad()));
        assertEquals("Apertura de 'Bolsa de harina'", movimiento.getValue().getMotivo());
        assertEquals(ID_EXISTENCIA_NUEVA, movimiento.getValue().getExistenciaId().getValue());
        assertEquals(ID_TIPO_AJUSTE, movimiento.getValue().getTipoMovimientoId().getValue());
        assertEquals(ID_USUARIO, movimiento.getValue().getUsuarioId().getValue());
        verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    @Test
    void unProductoSinPresentacionSeAbreConContenidoUno() {
        // Arrange: la linea es del propio producto base, asi que no hace falta conversion.
        hayLoteCon(ID_HARINA, "5", null);
        ArgumentCaptor<Existencia> creada = ArgumentCaptor.forClass(Existencia.class);

        // Act
        useCase.execute(apertura(false));

        // Assert
        verify(existenciaRepository).crear(creada.capture());
        assertEquals(0, BigDecimal.ONE.compareTo(creada.getValue().getContenidoInicial()));
        verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    @Test
    void unDetalleVencidoNoSeAbreSalvoQueSePermita() {
        // Arrange
        Lote lote = hayLoteCon(ID_BOLSA, "5", HOY.minusDays(1));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura(false)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("esta vencido"));
        assertEquals(new BigDecimal("5"), lote.obtenerDetalle(ID_DETALLE).getCantidadRestante());
        verify(existenciaRepository, never()).crear(any());
    }

    @Test
    void laBajaPorVencimientoPuedeAbrirUnDetalleVencido() {
        // Arrange
        hayLoteCon(ID_BOLSA, "5", HOY.minusDays(1));

        // Act
        ExistenciaResponse respuesta = useCase.execute(apertura(true));

        // Assert
        assertEquals(ID_EXISTENCIA_NUEVA, respuesta.getId());
        verify(existenciaRepository).crear(any());
    }

    @Test
    void sinUnidadesCerradasNoHayNadaQueAbrir() {
        // Arrange
        hayLoteCon(ID_BOLSA, "0.5", null);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura(false)));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(loteRepository, never()).actualizar(any(), any());
        verify(existenciaRepository, never()).crear(any());
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void sinConversionEntreLasUnidadesNoSePuedeAbrir() {
        // Arrange
        Lote lote = hayLoteCon(ID_BOLSA, "5", null);
        when(conversionRepository.obtenerVivas()).thenReturn(List.of());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura(false)));

        // Assert: falla antes de tocar el lote.
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().startsWith("No existe conversion"));
        assertEquals(new BigDecimal("5"), lote.obtenerDetalle(ID_DETALLE).getCantidadRestante());
        verify(loteRepository, never()).actualizar(any(), any());
    }

    @Test
    void unaJerarquiaDeMasDeUnNivelSeRechaza() {
        // Arrange: el "producto base" de la bolsa es a su vez una presentacion.
        hayLoteCon(ID_BOLSA, "5", null);
        when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_HARINA.equals(id.getValue()))))
                .thenReturn(Optional.of(presentacion(ID_HARINA, "Saco de harina", "e0000000-0000-0000-0000-000000000009")));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura(false)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("un solo nivel"));
        verify(existenciaRepository, never()).crear(any());
    }

    @Test
    void unDetalleDeLoteInexistenteRespondeNoEncontrado() {
        // Arrange
        when(loteRepository.obtenerPorDetalleIdParaActualizar(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura(false)));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID_DETALLE));
    }

    @Test
    void siElProductoDelDetalleNoExisteRespondeNoEncontrado() {
        // Arrange
        hayLoteCon("e0000000-0000-0000-0000-0000000000aa", "5", null);
        when(productoRepository.obtenerPorId(argThat(id -> id != null && id.getValue().endsWith("aa")))).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura(false)));

        // Assert
        assertEquals(404, error.getCodigo());
    }

    @Test
    void siElProductoBaseNoExisteRespondeNoEncontrado() {
        // Arrange
        hayLoteCon(ID_BOLSA, "5", null);
        when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_HARINA.equals(id.getValue())))).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura(false)));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains("Bolsa de harina"));
    }

    @Test
    void sinElCatalogoSembradoNoSeCreaLaExistencia() {
        // Arrange
        hayLoteCon(ID_BOLSA, "5", null);
        when(estadoExistenciaRepository.obtenerPorNombre("Abierta")).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura(false)));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(existenciaRepository, never()).crear(any());
    }

    @Test
    void sinElTipoAjusteSembradoNoSeCreaLaExistencia() {
        // Arrange
        hayLoteCon(ID_BOLSA, "5", null);
        when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.AJUSTE)).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura(false)));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(existenciaRepository, never()).crear(any());
        verify(movimientoRepository, never()).crear(any());
    }
}
