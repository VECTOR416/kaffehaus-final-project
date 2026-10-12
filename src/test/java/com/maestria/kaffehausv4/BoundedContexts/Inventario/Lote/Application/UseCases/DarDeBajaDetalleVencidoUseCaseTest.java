package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.RegistrarAperturaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DarDeBajaDetalleVencidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.DatosDePrueba;

/**
 * Pruebas unitarias de la baja de las unidades cerradas de una linea vencida: cada unidad se
 * abre y se da de baja de inmediato como merma. La apertura es un caso de uso simulado.
 */
@ExtendWith(MockitoExtension.class)
class DarDeBajaDetalleVencidoUseCaseTest {

    private static final String ID_HARINA = "f5000000-0000-0000-0000-000000000001";
    private static final String ID_BOLSA = "f5000000-0000-0000-0000-000000000002";
    private static final String ID_LOTE = "f6000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE = "f7000000-0000-0000-0000-000000000001";
    private static final LocalDate AYER = LocalDate.now().minusDays(1);

    @Mock private ILoteRepository loteRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private RegistrarAperturaUseCase registrarAperturaUseCase;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;
    @Mock private IUnidadRepository unidadRepository;

    private DarDeBajaDetalleVencidoUseCase useCase;
    private final List<Existencia> abiertas = new ArrayList<>();
    private final List<Movimiento> movimientos = new ArrayList<>();

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new DarDeBajaDetalleVencidoUseCase(loteRepository, usuarioRepository, productoRepository, existenciaRepository,
                estadoExistenciaRepository, movimientoRepository, tipoMovimientoRepository, registrarAperturaUseCase,
                recalcularStockUseCase, unidadRepository);

        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.MERMA)).thenReturn(Optional.of(MERMA));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Agotada")).thenReturn(Optional.of(AGOTADA));
        lenient().when(unidadRepository.obtenerPorId(any())).thenReturn(Optional.of(UNIDAD));
        lenient().when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA)));
        lenient().when(existenciaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> {
            movimientos.add(llamada.getArgument(0));
            return DatosDePrueba.guardado(llamada.getArgument(0), "fd000000-0000-0000-0000-00000000000" + movimientos.size());
        });
        // Cada apertura simulada devuelve una existencia nueva de 1000 de contenido.
        lenient().when(registrarAperturaUseCase.execute(any())).thenAnswer(llamada -> {
            Existencia abierta = existencia("f8000000-0000-0000-0000-00000000000" + abiertas.size(), ID_DETALLE, "1000", "1000", AYER);
            abiertas.add(abierta);
            return ExistenciaResponse.createFromAggregate(abierta, ABIERTA, UNIDAD);
        });
        lenient().when(existenciaRepository.obtenerPorIdParaActualizar(any())).thenAnswer(llamada -> abiertas.stream()
                .filter(abierta -> abierta.getId().getValue().equals(((ExistenciaId) llamada.getArgument(0)).getValue()))
                .findFirst());
    }

    private void hayDetalle(String cerradas, LocalDate vence) {
        lenient().when(loteRepository.obtenerPorDetalleIdParaActualizar(any()))
                .thenReturn(Optional.of(loteDeCompra(ID_LOTE, detalle(ID_DETALLE, ID_BOLSA, "5", cerradas, "50", vence))));
    }

    private static DarDeBajaDetalleVencidoRequest peticion(String motivo) {
        return DarDeBajaDetalleVencidoRequest.createFromRequest(motivo != null ? Map.of("motivo", motivo) : Map.of(), ID_USUARIO);
    }

    @Test
    void cadaUnidadCerradaSeAbreYSeDaDeBajaComoMerma() {
        // Arrange
        hayDetalle("3", AYER);

        // Act
        List<ExistenciaResponse> bajas = useCase.execute(ID_DETALLE, peticion(null));

        // Assert
        assertEquals(3, bajas.size());
        assertTrue(bajas.stream().allMatch(baja -> "Agotada".equals(baja.getEstadoExistencia().getNombre())));
        assertTrue(abiertas.stream().allMatch(Existencia::isAgotada));
        verify(registrarAperturaUseCase, times(3)).execute(argThat(apertura ->
                ID_DETALLE.equals(apertura.getIdDetalleLote()) && apertura.isPermitirVencido()));

        assertEquals(3, movimientos.size());
        assertTrue(movimientos.stream().allMatch(movimiento ->
                ID_TIPO_MERMA.equals(movimiento.getTipoMovimientoId().getValue())
                && new BigDecimal("1000").compareTo(movimiento.getCantidad()) == 0
                && "Vencimiento de unidades cerradas".equals(movimiento.getMotivo())));
        verify(recalcularStockUseCase, times(1)).execute(ID_HARINA);
    }

    @Test
    void elMotivoInformadoSeUsaEnLosMovimientos() {
        // Arrange
        hayDetalle("1", AYER);

        // Act
        useCase.execute(ID_DETALLE, peticion("Lote caducado"));

        // Assert
        assertEquals("Lote caducado", movimientos.get(0).getMotivo());
    }

    @Test
    void soloSeDanDeBajaLasUnidadesEnteras() {
        // Arrange
        hayDetalle("2.75", AYER);

        // Act
        List<ExistenciaResponse> bajas = useCase.execute(ID_DETALLE, peticion(null));

        // Assert
        assertEquals(2, bajas.size());
        verify(registrarAperturaUseCase, times(2)).execute(any());
    }

    @Test
    void unDetalleQueNoEstaVencidoNoSeDaDeBaja() {
        // Arrange
        hayDetalle("3", LocalDate.now());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion(null)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("no esta vencido"));
        verifyNoInteractions(registrarAperturaUseCase, movimientoRepository, recalcularStockUseCase);
    }

    @Test
    void unDetalleVencidoSinUnidadesCerradasNoTieneNadaQueDarDeBaja() {
        // Arrange
        hayDetalle("0.5", AYER);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion(null)));

        // Assert
        assertEquals(409, error.getCodigo());
        verifyNoInteractions(registrarAperturaUseCase);
    }

    @Test
    void unUsuarioInexistenteNoPuedeDarDeBaja() {
        // Arrange
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion(null)));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(loteRepository, never()).obtenerPorDetalleIdParaActualizar(any());
    }

    @Test
    void unDetalleInexistenteRespondeNoEncontrado() {
        // Arrange
        when(loteRepository.obtenerPorDetalleIdParaActualizar(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion(null)));

        // Assert
        assertEquals(404, error.getCodigo());
    }

    @Test
    void sinElCatalogoSembradoNoSeAbreNingunaUnidad() {
        // Arrange
        hayDetalle("3", AYER);
        when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.MERMA)).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion(null)));

        // Assert
        assertEquals(404, error.getCodigo());
        verifyNoInteractions(registrarAperturaUseCase);
    }
}
