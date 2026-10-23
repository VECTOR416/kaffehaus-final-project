package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DarDeBajaVencidoDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerVencimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.VencimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los vencimientos del slice Lote (logica.md VENCIMIENTO-03 y VENCIMIENTO-04):
 * la baja de las unidades vencidas de una linea, que decide el usuario, y la alerta de lo vencido o
 * por vencer.
 */
@ExtendWith(MockitoExtension.class)
class VencimientosUseCasesTest {

    private static final String ID_LOTE = "a5000000-0000-0000-0000-000000000001";
    private static final String ID_LINEA = "a5000000-0000-0000-0000-000000000002";
    private static final String ID_LINEA_LEJANA = "a5000000-0000-0000-0000-000000000003";
    private static final String ID_PAN = "a5000000-0000-0000-0000-000000000004";
    private static final String ID_HARINA = "a5000000-0000-0000-0000-000000000005";
    private static final String ID_BOLSA = "a5000000-0000-0000-0000-000000000006";
    private static final String ID_LINEA_BOLSA = "a5000000-0000-0000-0000-000000000007";
    private static final String ID_EXISTENCIA = "a5000000-0000-0000-0000-000000000008";

    @Mock private ILoteRepository loteRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private final LocalDate hoy = LocalDate.now();

    private void hayProducto(Producto producto) {
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && producto.getId().getValue().equals(id.getValue()))))
                .thenReturn(Optional.of(producto));
    }

    // ================================================================== baja por vencimiento

    @Nested
    class DarDeBajaUnidadesVencidas {

        private DarDeBajaVencidoDetalleLoteUseCase useCase;

        @BeforeEach
        void crearCasoDeUso() {
            useCase = new DarDeBajaVencidoDetalleLoteUseCase(loteRepository, usuarioRepository, movimientoRepository,
                    tipoMovimientoRepository, recalcularStockUseCase);
            lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
            lenient().when(tipoMovimientoRepository.obtenerPorNombre("Vencimiento")).thenReturn(Optional.of(VENCIMIENTO));
            lenient().when(loteRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        }

        private Lote hayLoteConLineaQueVence(String restante, LocalDate vence) {
            Lote lote = loteDeCompra(ID_LOTE, detalle(ID_LINEA, ID_PAN, "10", restante, "2", vence));
            lenient().when(loteRepository.obtenerPorDetalleIdParaActualizar(any())).thenReturn(Optional.of(lote));
            return lote;
        }

        private DarDeBajaVencidoDetalleLoteRequest peticion(Map<String, Object> cuerpo) {
            return DarDeBajaVencidoDetalleLoteRequest.createFromRequest(cuerpo, ID_USUARIO);
        }

        @Test
        void dejaLaLineaEnCeroYRegistraUnMovimientoVencimiento() {
            // Arrange
            hayLoteConLineaQueVence("4", hoy.minusDays(1));
            ArgumentCaptor<Movimiento> movimiento = ArgumentCaptor.forClass(Movimiento.class);

            // Act
            LoteResponse respuesta = useCase.execute(ID_LINEA, peticion(Map.of("motivo", "Pan duro")));

            // Assert
            assertEquals(0, respuesta.getDetalles().get(0).getCantidadRestante().signum());
            verify(movimientoRepository).crear(movimiento.capture());
            assertEquals(0, new BigDecimal("4").compareTo(movimiento.getValue().getCantidad()));
            assertEquals(ID_TIPO_VENCIMIENTO, movimiento.getValue().getTipoMovimientoId().getValue());
            assertEquals(ID_LINEA, movimiento.getValue().getDetalleLoteId().getValue());
            assertNull(movimiento.getValue().getExistenciaId());
            assertEquals("Pan duro", movimiento.getValue().getMotivo());
            assertEquals(ID_USUARIO, movimiento.getValue().getUsuarioId().getValue());
        }

        @Test
        void sinMotivoUsaElPorDefecto() {
            // Arrange
            hayLoteConLineaQueVence("1.5", hoy.minusDays(3));
            ArgumentCaptor<Movimiento> movimiento = ArgumentCaptor.forClass(Movimiento.class);

            // Act
            useCase.execute(ID_LINEA, peticion(new HashMap<>()));

            // Assert: lo que queda puede ser una fraccion (GENERAL-16).
            verify(movimientoRepository).crear(movimiento.capture());
            assertEquals("Baja por vencimiento", movimiento.getValue().getMotivo());
            assertEquals(0, new BigDecimal("1.5").compareTo(movimiento.getValue().getCantidad()));
        }

        @Test
        void bloqueaElLoteYRecalculaElStockDelProductoDespues() {
            // Arrange (logica.md LOTE-27, GENERAL-15)
            hayLoteConLineaQueVence("4", hoy.minusDays(1));

            // Act
            useCase.execute(ID_LINEA, peticion(Map.of()));

            // Assert
            InOrder orden = inOrder(loteRepository, recalcularStockUseCase);
            orden.verify(loteRepository).obtenerPorDetalleIdParaActualizar(argThat(id -> ID_LINEA.equals(id.getValue())));
            orden.verify(recalcularStockUseCase).execute(ID_PAN);
        }

        @Test
        void loQueVenceHoyTodaviaEsVigenteYNoSeDaDeBaja() {
            // Arrange (logica.md GENERAL-09)
            Lote lote = hayLoteConLineaQueVence("4", hoy);

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LINEA, peticion(Map.of())));

            // Assert
            assertEquals(409, error.getCodigo());
            assertTrue(error.getMensaje().contains("no esta vencido"));
            assertEquals(0, new BigDecimal("4").compareTo(lote.obtenerDetalle(ID_LINEA).getCantidadRestante()));
            verifyNoInteractions(movimientoRepository, recalcularStockUseCase);
        }

        @Test
        void unaLineaSinUnidadesNoTieneNadaQueDarDeBaja() {
            // Arrange
            hayLoteConLineaQueVence("0", hoy.minusDays(1));

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LINEA, peticion(Map.of())));

            // Assert
            assertEquals(409, error.getCodigo());
            verify(loteRepository, never()).actualizar(any(), any());
        }

        @Test
        void unaLineaInexistenteRespondeNoEncontrado() {
            // Arrange
            when(loteRepository.obtenerPorDetalleIdParaActualizar(any())).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LINEA, peticion(Map.of())));

            // Assert
            assertEquals(404, error.getCodigo());
        }
    }

    // ================================================================== alerta

    @Nested
    class Alerta {

        private ObtenerVencimientosUseCase useCase;

        @BeforeEach
        void crearCasoDeUso() {
            useCase = new ObtenerVencimientosUseCase(loteRepository, existenciaRepository, productoRepository);
            hayProducto(productoDirecto(ID_PAN, "Pan"));
            hayProducto(productoDirecto(ID_HARINA, "Harina"));
            hayProducto(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA));
        }

        @Test
        void listaLoVencidoYLoQueVenceDentroDelPlazoOrdenadoPorFecha() {
            // Arrange: una linea vencida ayer, otra que vence en 10 dias (fuera del plazo de 3) y una
            // existencia que vence en 2 dias.
            when(loteRepository.obtenerConUnidadesQueVencenHasta(hoy.plusDays(3))).thenReturn(List.of(loteDeCompra(ID_LOTE,
                    detalle(ID_LINEA, ID_PAN, "10", "4", "2", hoy.minusDays(1)),
                    detalle(ID_LINEA_LEJANA, ID_PAN, "10", "10", "2", hoy.plusDays(10)))));
            when(existenciaRepository.obtenerConContenidoQueVencenHasta(hoy.plusDays(3))).thenReturn(List.of(
                    existencia(ID_EXISTENCIA, ID_LINEA_BOLSA, "1000", "250", hoy.plusDays(2))));
            when(loteRepository.obtenerProductoDeDetalle(any())).thenReturn(Optional.of(new ProductoId(ID_BOLSA)));
            when(loteRepository.obtenerLoteDeDetalle(any())).thenReturn(Optional.of(new LoteId(ID_LOTE)));

            // Act
            List<VencimientoResponse> alerta = useCase.execute(ObtenerVencimientosRequest.createFromRequest(new HashMap<>()));

            // Assert
            assertEquals(2, alerta.size());
            VencimientoResponse linea = alerta.get(0);
            assertEquals(VencimientoResponse.LINEA, linea.getTipo());
            assertEquals(ID_LINEA, linea.getId());
            assertEquals("Pan", linea.getProducto());
            assertEquals(0, new BigDecimal("4").compareTo(linea.getCantidad()));
            assertTrue(linea.isVencido());
            VencimientoResponse existencia = alerta.get(1);
            assertEquals(VencimientoResponse.EXISTENCIA, existencia.getTipo());
            assertEquals(ID_HARINA, existencia.getIdProducto());
            assertEquals("Harina", existencia.getProducto());
            assertEquals(ID_LOTE, existencia.getIdLote());
            assertEquals(ID_UNIDAD, existencia.getIdUnidad());
            assertFalse(existencia.isVencido());
        }

        @Test
        void elPlazoSeIndicaEnDias() {
            // Act
            useCase.execute(ObtenerVencimientosRequest.createFromRequest(Map.of("dias", "7")));

            // Assert
            verify(loteRepository).obtenerConUnidadesQueVencenHasta(hoy.plusDays(7));
            verify(existenciaRepository).obtenerConContenidoQueVencenHasta(hoy.plusDays(7));
        }

        @Test
        void losDiasNoPuedenSerNegativosNiFraccionarios() {
            // Act
            DomainException negativo = assertThrows(DomainException.class,
                    () -> ObtenerVencimientosRequest.createFromRequest(Map.of("dias", "-1")));
            DomainException fraccion = assertThrows(DomainException.class,
                    () -> ObtenerVencimientosRequest.createFromRequest(Map.of("dias", "1.5")));

            // Assert
            assertEquals(400, negativo.getCodigo());
            assertEquals(400, fraccion.getCodigo());
            assertEquals(3, ObtenerVencimientosRequest.createFromRequest(new HashMap<>()).getDias());
        }
    }
}
