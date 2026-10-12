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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.AjustarExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
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

/** Pruebas unitarias del ajuste por inventario fisico de una existencia (INV-43). */
@ExtendWith(MockitoExtension.class)
class AjustarExistenciaUseCaseTest {

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

    private AjustarExistenciaUseCase useCase;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new AjustarExistenciaUseCase(existenciaRepository, loteRepository, productoRepository, usuarioRepository,
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

    private static AjustarExistenciaRequest conteo(String contenidoContado) {
        return AjustarExistenciaRequest.createFromRequest(Map.of("contenidoContado", contenidoContado, "motivo", "Conteo semanal"), ID_USUARIO);
    }

    @Test
    void unConteoMenorRegistraUnAjustePorLaDiferencia() {
        // Arrange
        Existencia existencia = hayExistencia("900", null);

        // Act
        ExistenciaResponse respuesta = useCase.execute(ID_EXISTENCIA, conteo("500"));

        // Assert
        assertEquals(0, new BigDecimal("500").compareTo(existencia.getContenidoActual()));
        assertEquals(0, new BigDecimal("500").compareTo(respuesta.getContenidoActual()));

        Movimiento movimiento = movimientoRegistrado();
        assertEquals(0, new BigDecimal("400").compareTo(movimiento.getCantidad()));
        assertEquals("Conteo semanal", movimiento.getMotivo());
        assertEquals(ID_TIPO_AJUSTE, movimiento.getTipoMovimientoId().getValue());
        verify(existenciaRepository).actualizar(any(), any());
        verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    @Test
    void unConteoMayorRegistraElAjusteConLaCantidadEnPositivo() {
        // Arrange
        Existencia existencia = hayExistencia("500", null);

        // Act
        useCase.execute(ID_EXISTENCIA, conteo("800"));

        // Assert
        assertEquals(0, new BigDecimal("800").compareTo(existencia.getContenidoActual()));
        assertEquals(0, new BigDecimal("300").compareTo(movimientoRegistrado().getCantidad()));
    }

    @Test
    void siElConteoCoincideNoSeRegistraNada() {
        // Arrange
        hayExistencia("500", null);

        // Act
        ExistenciaResponse respuesta = useCase.execute(ID_EXISTENCIA, conteo("500"));

        // Assert
        assertEquals(0, new BigDecimal("500").compareTo(respuesta.getContenidoActual()));
        verify(existenciaRepository, never()).actualizar(any(), any());
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void unConteoEnCeroDejaLaExistenciaAgotada() {
        // Arrange
        Existencia existencia = hayExistencia("500", null);

        // Act
        ExistenciaResponse respuesta = useCase.execute(ID_EXISTENCIA, conteo("0"));

        // Assert
        assertTrue(existencia.isAgotada());
        assertEquals("Agotada", respuesta.getEstadoExistencia().getNombre());
        assertEquals(0, new BigDecimal("500").compareTo(movimientoRegistrado().getCantidad()));
    }

    @Test
    void unaExistenciaAgotadaNoSeAjusta() {
        // Arrange
        hayExistencia("0", null);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, conteo("10")));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void elConteoNoPuedeSuperarElContenidoInicial() {
        // Arrange
        Existencia existencia = hayExistencia("500", null);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, conteo("1000.5")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(0, new BigDecimal("500").compareTo(existencia.getContenidoActual()));
        verify(existenciaRepository, never()).actualizar(any(), any());
    }

    @Test
    void unConteoNegativoSeRechaza() {
        // Arrange
        hayExistencia("500", null);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, conteo("-1")));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void unUsuarioInexistenteNoPuedeAjustar() {
        // Arrange
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, conteo("10")));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(existenciaRepository, never()).obtenerPorIdParaActualizar(any());
    }

    @Test
    void unaExistenciaInexistenteRespondeNoEncontrado() {
        // Arrange
        when(existenciaRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_EXISTENCIA, conteo("10")));

        // Assert
        assertEquals(404, error.getCodigo());
    }

    @Test
    void laPeticionExigeElConteoYElMotivo() {
        // Act
        DomainException sinConteo = assertThrows(DomainException.class,
                () -> AjustarExistenciaRequest.createFromRequest(Map.of("motivo", "Conteo"), ID_USUARIO));
        DomainException sinMotivo = assertThrows(DomainException.class,
                () -> AjustarExistenciaRequest.createFromRequest(Map.of("contenidoContado", "5"), ID_USUARIO));

        // Assert
        assertEquals(400, sinConteo.getCodigo());
        assertEquals(400, sinMotivo.getCodigo());
    }
}
