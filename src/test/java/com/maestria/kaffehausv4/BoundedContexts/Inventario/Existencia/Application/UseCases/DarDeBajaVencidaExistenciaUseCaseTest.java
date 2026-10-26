package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.DarDeBajaVencidaExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de la baja por vencimiento de una existencia (logica.md VENCIMIENTO-03): queda
 * en 0 y 'Agotada', con un movimiento 'Vencimiento', y se recalcula el stock de su producto base.
 */
@ExtendWith(MockitoExtension.class)
class DarDeBajaVencidaExistenciaUseCaseTest {

    private static final String ID = "a6000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE = "a6000000-0000-0000-0000-000000000002";
    private static final String ID_HARINA = "a6000000-0000-0000-0000-000000000003";
    private static final String ID_BOLSA = "a6000000-0000-0000-0000-000000000004";

    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IUnidadRepository unidadRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private DarDeBajaVencidaExistenciaUseCase useCase;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void prepararEscenario() {
        useCase = new DarDeBajaVencidaExistenciaUseCase(existenciaRepository, usuarioRepository, loteRepository, productoRepository,
                estadoExistenciaRepository, unidadRepository, movimientoRepository, tipoMovimientoRepository, recalcularStockUseCase);
        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Agotada")).thenReturn(Optional.of(AGOTADA));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre("Vencimiento")).thenReturn(Optional.of(VENCIMIENTO));
        lenient().when(unidadRepository.obtenerPorId(any())).thenReturn(Optional.of(UNIDAD));
        lenient().when(existenciaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(loteRepository.obtenerProductoDeDetalle(any())).thenReturn(Optional.of(new ProductoId(ID_BOLSA)));
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_BOLSA.equals(id.getValue()))))
                .thenReturn(Optional.of(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA)));
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_HARINA.equals(id.getValue()))))
                .thenReturn(Optional.of(productoDirecto(ID_HARINA, "Harina")));
    }

    private Existencia hayExistencia(String actual, LocalDate vence) {
        Existencia existencia = existencia(ID, ID_DETALLE, "1000", actual, vence);
        lenient().when(existenciaRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(existencia));
        return existencia;
    }

    private static DarDeBajaVencidaExistenciaRequest peticion() {
        return DarDeBajaVencidaExistenciaRequest.createFromRequest(Map.of(), ID_USUARIO);
    }

    @Test
    void dejaLaExistenciaAgotadaYRegistraElVencimiento() {
        // Arrange
        hayExistencia("350", hoy.minusDays(2));
        ArgumentCaptor<Movimiento> movimiento = ArgumentCaptor.forClass(Movimiento.class);

        // Act
        ExistenciaResponse respuesta = useCase.execute(ID, peticion());

        // Assert
        assertEquals(0, respuesta.getContenidoActual().signum());
        assertEquals("Agotada", respuesta.getEstadoExistencia().getNombre());
        assertNotNull(respuesta.getFechaAcabado());
        verify(movimientoRepository).crear(movimiento.capture());
        assertEquals(0, new BigDecimal("350").compareTo(movimiento.getValue().getCantidad()));
        assertEquals(ID_TIPO_VENCIMIENTO, movimiento.getValue().getTipoMovimientoId().getValue());
        assertEquals(ID, movimiento.getValue().getExistenciaId().getValue());
        assertNull(movimiento.getValue().getDetalleLoteId());
        assertEquals("Baja por vencimiento", movimiento.getValue().getMotivo());
    }

    @Test
    void recalculaElStockDelProductoBaseDespuesDeBloquearLaExistencia() {
        // Arrange (logica.md GENERAL-15, PRODUCTO-11)
        hayExistencia("350", hoy.minusDays(2));

        // Act
        useCase.execute(ID, peticion());

        // Assert
        InOrder orden = inOrder(existenciaRepository, recalcularStockUseCase);
        orden.verify(existenciaRepository).obtenerPorIdParaActualizar(any());
        orden.verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    @Test
    void unaExistenciaVigenteNoSeDaDeBajaPorVencimiento() {
        // Arrange
        hayExistencia("350", hoy.plusDays(1));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID, peticion()));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("no esta vencida"));
        verify(existenciaRepository, never()).actualizar(any(), any());
        verifyNoInteractions(movimientoRepository, recalcularStockUseCase);
    }

    @Test
    void unaExistenciaSinVencimientoNuncaVence() {
        // Arrange (logica.md GENERAL-09)
        hayExistencia("350", null);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID, peticion()));

        // Assert
        assertEquals(409, error.getCodigo());
    }

    @Test
    void unaExistenciaAgotadaNoTieneNadaQueDarDeBaja() {
        // Arrange
        hayExistencia("0", hoy.minusDays(2));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID, peticion()));

        // Assert
        assertEquals(409, error.getCodigo());
        verifyNoInteractions(movimientoRepository);
    }

    @Test
    void unaExistenciaInexistenteRespondeNoEncontrado() {
        // Arrange
        when(existenciaRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID, peticion()));

        // Assert
        assertEquals(404, error.getCodigo());
    }
}
