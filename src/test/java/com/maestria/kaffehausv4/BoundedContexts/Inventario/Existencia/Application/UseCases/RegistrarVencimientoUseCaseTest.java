package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarVencimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.DatosDePrueba;

/** Pruebas unitarias de la baja por vencimiento de una existencia abierta (INV-42). */
@ExtendWith(MockitoExtension.class)
class RegistrarVencimientoUseCaseTest {

    private static final String ID_HARINA = "d0000000-0000-0000-0000-000000000001";
    private static final String ID_BOLSA = "d0000000-0000-0000-0000-000000000002";
    private static final String ID_LOTE = "d1000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE = "d2000000-0000-0000-0000-000000000001";
    private static final String ID_EXISTENCIA = "d3000000-0000-0000-0000-000000000001";

    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IUnidadRepository unidadRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private RegistrarVencimientoUseCase useCase;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new RegistrarVencimientoUseCase(existenciaRepository, loteRepository, productoRepository, usuarioRepository,
                estadoExistenciaRepository, unidadRepository, movimientoRepository, tipoMovimientoRepository, recalcularStockUseCase);

        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Agotada")).thenReturn(Optional.of(AGOTADA));
        lenient().when(estadoExistenciaRepository.obtenerPorId(any())).thenAnswer(llamada ->
                Optional.of(ID_ESTADO_AGOTADA.equals(((com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId) llamada.getArgument(0)).getValue()) ? AGOTADA : ABIERTA));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.MERMA)).thenReturn(Optional.of(MERMA));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.AJUSTE)).thenReturn(Optional.of(AJUSTE));
        lenient().when(unidadRepository.obtenerPorId(any())).thenReturn(Optional.of(UNIDAD));
        // La existencia se abrio desde una bolsa (presentacion) de harina: el stock que cambia es el de la harina.
        lenient().when(loteRepository.obtenerPorDetalleId(any()))
                .thenReturn(Optional.of(loteDeCompra(ID_LOTE, detalle(ID_DETALLE, ID_BOLSA, "10", "5", "50", null))));
        lenient().when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA)));
        lenient().when(existenciaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> DatosDePrueba.guardado(llamada.getArgument(0), "dd000000-0000-0000-0000-000000000001"));
    }

    private Existencia hayExistencia(String contenidoActual, LocalDate vence) {
        Existencia existencia = existencia(ID_EXISTENCIA, ID_DETALLE, "1000", contenidoActual, vence);
        lenient().when(existenciaRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(existencia));
        return existencia;
    }

    private Movimiento movimientoRegistrado() {
        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).crear(captor.capture());
        return captor.getValue();
    }

    private static final LocalDate AYER = LocalDate.now().minusDays(1);

    private static RegistrarVencimientoRequest vencimiento(String motivo) {
        return RegistrarVencimientoRequest.createFromRequest(motivo != null ? Map.of("motivo", motivo) : Map.of(), ID_USUARIO);
    }

    @Test
    void unaExistenciaVencidaSeDaDeBajaPorTodoSuContenido() {
        // Arrange
        Existencia existencia = hayExistencia("320", AYER);

        // Act
        ExistenciaResponse respuesta = useCase.execute(ID_EXISTENCIA, vencimiento("Caduco en almacen"));

        // Assert
        assertTrue(existencia.isAgotada());
        assertEquals("Agotada", respuesta.getEstadoExistencia().getNombre());

        Movimiento movimiento = movimientoRegistrado();
        assertEquals(0, new BigDecimal("320").compareTo(movimiento.getCantidad()));
        assertEquals("Caduco en almacen", movimiento.getMotivo());
        assertEquals(ID_TIPO_MERMA, movimiento.getTipoMovimientoId().getValue());
        verify(existenciaRepository).actualizar(any(), any());
        verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    @Test
    void sinMotivoSeRegistraElMotivoPorDefecto() {
        // Arrange
        hayExistencia("320", AYER);

        // Act
        useCase.execute(ID_EXISTENCIA, vencimiento(null));

        // Assert
        assertEquals("Vencimiento", movimientoRegistrado().getMotivo());
    }

    @Test
    void unaExistenciaQueVenceHoyTodaviaNoEstaVencida() {
        // Arrange
        Existencia existencia = hayExistencia("320", LocalDate.now());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, vencimiento(null)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("no esta vencida"));
        assertEquals(0, new BigDecimal("320").compareTo(existencia.getContenidoActual()));
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void unaExistenciaSinCaducidadNoSePuedeDarDeBajaPorVencimiento() {
        // Arrange
        hayExistencia("320", null);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, vencimiento(null)));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(existenciaRepository, never()).actualizar(any(), any());
    }

    @Test
    void unaExistenciaVencidaPeroYaAgotadaNoSeVuelveADarDeBaja() {
        // Arrange
        hayExistencia("0", AYER);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, vencimiento(null)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("ya esta agotada"));
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void unUsuarioInexistenteNoPuedeRegistrarVencimientos() {
        // Arrange
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, vencimiento(null)));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(existenciaRepository, never()).obtenerPorIdParaActualizar(any());
    }

    @Test
    void unaExistenciaInexistenteRespondeNoEncontrado() {
        // Arrange
        when(existenciaRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, vencimiento(null)));

        // Assert
        assertEquals(404, error.getCodigo());
    }

    @Test
    void siElProductoDeLaExistenciaNoExisteLaOperacionFalla() {
        // Arrange
        hayExistencia("320", AYER);
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, vencimiento(null)));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }
}
