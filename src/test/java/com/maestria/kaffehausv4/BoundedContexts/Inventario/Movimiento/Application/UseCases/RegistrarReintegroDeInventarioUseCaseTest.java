package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.DatosDePrueba;

/**
 * Pruebas unitarias del reintegro de inventario al anular una venta (VEN-42, VEN-43), con
 * todos los repositorios simulados.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarReintegroDeInventarioUseCaseTest {

    private static final String MOTIVO_VENTA = "Venta 'v-1'";
    private static final String MOTIVO_REINTEGRO = "Anulacion de la venta 'v-1'";
    private static final String ID_CAFE = "c0000000-0000-0000-0000-000000000001";
    private static final String ID_LOTE = "c1000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE = "c2000000-0000-0000-0000-000000000001";
    private static final String ID_EXISTENCIA_A = "c3000000-0000-0000-0000-00000000000a";
    private static final String ID_EXISTENCIA_B = "c3000000-0000-0000-0000-00000000000b";
    private static final String ID_EXISTENCIA_NUEVA = "c3000000-0000-0000-0000-0000000000ff";

    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private RegistrarReintegroDeInventarioUseCase useCase;
    private final List<Movimiento> compensatorios = new ArrayList<>();

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new RegistrarReintegroDeInventarioUseCase(movimientoRepository, tipoMovimientoRepository, existenciaRepository,
                estadoExistenciaRepository, loteRepository, productoRepository, recalcularStockUseCase);

        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.MERMA)).thenReturn(Optional.of(MERMA));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Abierta")).thenReturn(Optional.of(ABIERTA));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Agotada")).thenReturn(Optional.of(AGOTADA));
        lenient().when(loteRepository.obtenerPorDetalleId(any()))
                .thenReturn(Optional.of(loteDeCompra(ID_LOTE, detalle(ID_DETALLE, ID_CAFE, "10", "5", "50", null))));
        lenient().when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(productoDirecto(ID_CAFE, "Cafe")));
        lenient().when(existenciaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(existenciaRepository.crear(any())).thenAnswer(llamada -> DatosDePrueba.guardada(llamada.getArgument(0), ID_EXISTENCIA_NUEVA));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> {
            compensatorios.add(llamada.getArgument(0));
            return DatosDePrueba.guardado(llamada.getArgument(0), "cd000000-0000-0000-0000-00000000000" + compensatorios.size());
        });
    }

    private static RegistrarReintegroDeInventarioRequest reintegro() {
        return RegistrarReintegroDeInventarioRequest.crear(MOTIVO_VENTA, MOTIVO_REINTEGRO, ID_USUARIO, AHORA);
    }

    private static Movimiento salida(String id, String idExistencia, String cantidad, TipoMovimiento tipo) {
        return Movimiento.restaurar(id, new BigDecimal(cantidad), AHORA.minusHours(1), MOTIVO_VENTA, new ExistenciaId(idExistencia),
                new TipoMovimientoId(tipo.getId().getValue()), new UsuarioId(ID_USUARIO), null, false);
    }

    private void laVentaDesconto(Movimiento... salidas) {
        when(movimientoRepository.obtenerPorMotivo(MOTIVO_VENTA)).thenReturn(List.of(salidas));
    }

    private void existe(Existencia existencia) {
        lenient().when(existenciaRepository.obtenerPorIdParaActualizar(argThat(id -> id != null && existencia.getId().getValue().equals(id.getValue()))))
                .thenReturn(Optional.of(existencia));
    }

    @Test
    void unaExistenciaVivaRecibeDeVueltaLoDescontado() {
        // Arrange
        Existencia viva = existencia(ID_EXISTENCIA_A, ID_DETALLE, "10", "8", null);
        existe(viva);
        laVentaDesconto(salida("cc000000-0000-0000-0000-000000000001", ID_EXISTENCIA_A, "2", AJUSTE));

        // Act
        useCase.execute(reintegro());

        // Assert
        assertEquals(0, new BigDecimal("10").compareTo(viva.getContenidoActual()));
        verify(existenciaRepository).actualizar(any(), any());
        verify(existenciaRepository, never()).crear(any());

        assertEquals(1, compensatorios.size());
        Movimiento compensatorio = compensatorios.get(0);
        assertEquals(0, new BigDecimal("2").compareTo(compensatorio.getCantidad()));
        assertEquals(MOTIVO_REINTEGRO, compensatorio.getMotivo());
        assertEquals(ID_EXISTENCIA_A, compensatorio.getExistenciaId().getValue());
        assertEquals(ID_TIPO_AJUSTE, compensatorio.getTipoMovimientoId().getValue());
        assertEquals(AHORA, compensatorio.getFechaMovimiento());
        verify(recalcularStockUseCase).execute(ID_CAFE);
    }

    @Test
    void elCompensatorioConservaElTipoDeLaSalidaQueRevierte() {
        // Arrange
        existe(existencia(ID_EXISTENCIA_A, ID_DETALLE, "10", "5", null));
        laVentaDesconto(salida("cc000000-0000-0000-0000-000000000001", ID_EXISTENCIA_A, "1", RECETA));

        // Act
        useCase.execute(reintegro());

        // Assert
        assertEquals(ID_TIPO_RECETA, compensatorios.get(0).getTipoMovimientoId().getValue());
    }

    @Test
    void unaExistenciaAgotadaNoSeReviveSeAbreUnaNuevaSobreLaMismaLinea() {
        // Arrange
        LocalDate caducidad = HOY.plusDays(30);
        Existencia agotada = existencia(ID_EXISTENCIA_A, ID_DETALLE, "10", "0", caducidad);
        existe(agotada);
        laVentaDesconto(salida("cc000000-0000-0000-0000-000000000001", ID_EXISTENCIA_A, "3", AJUSTE));
        ArgumentCaptor<Existencia> nueva = ArgumentCaptor.forClass(Existencia.class);

        // Act
        useCase.execute(reintegro());

        // Assert
        assertTrue(agotada.isAgotada());
        verify(existenciaRepository, never()).actualizar(any(), any());
        verify(existenciaRepository).crear(nueva.capture());
        assertEquals(0, new BigDecimal("3").compareTo(nueva.getValue().getContenidoInicial()));
        assertEquals(0, new BigDecimal("3").compareTo(nueva.getValue().getContenidoActual()));
        assertEquals(ID_DETALLE, nueva.getValue().getDetalleLoteId().getValue());
        assertEquals(caducidad, nueva.getValue().getFechaVencimiento());
        assertEquals(ID_ESTADO_ABIERTA, nueva.getValue().getEstadoExistenciaId().getValue());
        assertEquals(ID_UNIDAD, nueva.getValue().getUnidadId().getValue());
        assertEquals(ID_EXISTENCIA_NUEVA, compensatorios.get(0).getExistenciaId().getValue());
    }

    @Test
    void loReintegradoYaVencidoSeDaDeBajaDeInmediatoComoMerma() {
        // Arrange
        Existencia vencida = existencia(ID_EXISTENCIA_A, ID_DETALLE, "10", "8", HOY.minusDays(1));
        existe(vencida);
        laVentaDesconto(salida("cc000000-0000-0000-0000-000000000001", ID_EXISTENCIA_A, "2", AJUSTE));

        // Act
        useCase.execute(reintegro());

        // Assert: entra el compensatorio y sale la merma por la misma cantidad.
        assertEquals(2, compensatorios.size());
        assertEquals(ID_TIPO_AJUSTE, compensatorios.get(0).getTipoMovimientoId().getValue());
        assertEquals(ID_TIPO_MERMA, compensatorios.get(1).getTipoMovimientoId().getValue());
        assertEquals(MOTIVO_REINTEGRO + " (vencido)", compensatorios.get(1).getMotivo());
        assertEquals(0, new BigDecimal("2").compareTo(compensatorios.get(1).getCantidad()));
        assertEquals(0, new BigDecimal("8").compareTo(vencida.getContenidoActual()));
        verify(existenciaRepository, times(2)).actualizar(any(), any());
    }

    @Test
    void variasSalidasDelMismoProductoRecalculanSuStockUnaSolaVez() {
        // Arrange
        existe(existencia(ID_EXISTENCIA_A, ID_DETALLE, "10", "5", null));
        existe(existencia(ID_EXISTENCIA_B, ID_DETALLE, "10", "5", null));
        laVentaDesconto(
                salida("cc000000-0000-0000-0000-000000000001", ID_EXISTENCIA_A, "1", AJUSTE),
                salida("cc000000-0000-0000-0000-000000000002", ID_EXISTENCIA_B, "2", AJUSTE));

        // Act
        useCase.execute(reintegro());

        // Assert
        assertEquals(2, compensatorios.size());
        verify(recalcularStockUseCase, times(1)).execute(ID_CAFE);
    }

    @Test
    void lasExistenciasSeBloqueanEnOrdenDeIdParaEvitarInterbloqueos() {
        // Arrange: el repositorio las entrega en el orden inverso al de sus ids.
        existe(existencia(ID_EXISTENCIA_A, ID_DETALLE, "10", "5", null));
        existe(existencia(ID_EXISTENCIA_B, ID_DETALLE, "10", "5", null));
        laVentaDesconto(
                salida("cc000000-0000-0000-0000-000000000001", ID_EXISTENCIA_B, "1", AJUSTE),
                salida("cc000000-0000-0000-0000-000000000002", ID_EXISTENCIA_A, "1", AJUSTE));

        // Act
        useCase.execute(reintegro());

        // Assert
        InOrder orden = inOrder(existenciaRepository);
        orden.verify(existenciaRepository).obtenerPorIdParaActualizar(argThat(id -> ID_EXISTENCIA_A.equals(id.getValue())));
        orden.verify(existenciaRepository).obtenerPorIdParaActualizar(argThat(id -> ID_EXISTENCIA_B.equals(id.getValue())));
    }

    @Test
    void elStockQueSeRecalculaEsElDelProductoBaseDeLaPresentacion() {
        // Arrange
        String idBolsa = "c0000000-0000-0000-0000-000000000002";
        existe(existencia(ID_EXISTENCIA_A, ID_DETALLE, "10", "5", null));
        laVentaDesconto(salida("cc000000-0000-0000-0000-000000000001", ID_EXISTENCIA_A, "1", AJUSTE));
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(presentacion(idBolsa, "Bolsa de cafe", ID_CAFE)));

        // Act
        useCase.execute(reintegro());

        // Assert
        verify(recalcularStockUseCase).execute(ID_CAFE);
        verify(recalcularStockUseCase, never()).execute(idBolsa);
    }

    @Test
    void unaVentaSinMovimientosNoReintegraNada() {
        // Arrange
        laVentaDesconto();

        // Act
        useCase.execute(reintegro());

        // Assert
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void siLaExistenciaYaNoExisteElReintegroFalla() {
        // Arrange
        laVentaDesconto(salida("cc000000-0000-0000-0000-000000000001", ID_EXISTENCIA_A, "1", AJUSTE));
        when(existenciaRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(reintegro()));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void unReintegroQueSuperaElContenidoInicialFallaYNoRegistraElCompensatorio() {
        // Arrange
        existe(existencia(ID_EXISTENCIA_A, ID_DETALLE, "10", "9.5", null));
        laVentaDesconto(salida("cc000000-0000-0000-0000-000000000001", ID_EXISTENCIA_A, "1", AJUSTE));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(reintegro()));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void sinElCatalogoSembradoNoSeEmpiezaAReintegrar() {
        // Arrange
        when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.MERMA)).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(reintegro()));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(movimientoRepository, never()).obtenerPorMotivo(anyString());
    }

    @Test
    void sinElEstadoAbiertaSembradoNoSeEmpiezaAReintegrar() {
        // Arrange
        when(estadoExistenciaRepository.obtenerPorNombre("Abierta")).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(reintegro()));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains("'Abierta'"));
    }
}
