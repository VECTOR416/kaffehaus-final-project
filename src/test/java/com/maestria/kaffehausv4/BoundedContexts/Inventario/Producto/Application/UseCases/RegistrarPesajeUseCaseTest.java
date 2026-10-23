package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.RegistrarPesajeRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias del pesaje de un producto base (logica.md EXISTENCIA-15, EXISTENCIA-16):
 * el sistema decide si la diferencia es 'Merma' o 'Ajuste' y la reparte desde la existencia del
 * ultimo movimiento hacia atras.
 *
 * Escenario: harina con dos bolsas abiertas de 1000 g. La "reciente" tuvo su ultimo movimiento hoy
 * y la "antigua", ayer.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarPesajeUseCaseTest {

    private static final String ID_HARINA = "e1000000-0000-0000-0000-000000000001";
    private static final String ID_BOLSA = "e1000000-0000-0000-0000-000000000002";
    private static final String ID_DETALLE = "e2000000-0000-0000-0000-000000000001";
    private static final String ID_ANTIGUA = "e3000000-0000-0000-0000-000000000001";
    private static final String ID_RECIENTE = "e3000000-0000-0000-0000-000000000002";

    @Mock private IProductoRepository productoRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IUnidadRepository unidadRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private RegistrarPesajeUseCase useCase;
    private final List<Movimiento> registrados = new ArrayList<>();

    @BeforeEach
    void prepararEscenario() {
        useCase = new RegistrarPesajeUseCase(productoRepository, loteRepository, existenciaRepository, movimientoRepository,
                tipoMovimientoRepository, estadoExistenciaRepository, unidadRepository, usuarioRepository, recalcularStockUseCase);

        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_HARINA.equals(id.getValue()))))
                .thenReturn(Optional.of(productoDirecto(ID_HARINA, "Harina")));
        lenient().when(productoRepository.obtenerPresentaciones(any()))
                .thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA)));
        lenient().when(loteRepository.obtenerPorProductos(anyList()))
                .thenReturn(List.of(loteDeCompra("e4000000-0000-0000-0000-000000000001", detalle(ID_DETALLE, ID_BOLSA, "5", "3", "12", null))));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.MERMA)).thenReturn(Optional.of(MERMA));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.AJUSTE)).thenReturn(Optional.of(AJUSTE));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Abierta")).thenReturn(Optional.of(ABIERTA));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Agotada")).thenReturn(Optional.of(AGOTADA));
        lenient().when(estadoExistenciaRepository.obtenerPorId(argThat(id -> id != null && ID_ESTADO_ABIERTA.equals(id.getValue()))))
                .thenReturn(Optional.of(ABIERTA));
        lenient().when(estadoExistenciaRepository.obtenerPorId(argThat(id -> id != null && ID_ESTADO_AGOTADA.equals(id.getValue()))))
                .thenReturn(Optional.of(AGOTADA));
        lenient().when(unidadRepository.obtenerPorId(any())).thenReturn(Optional.of(UNIDAD));
        lenient().when(existenciaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> {
            Movimiento movimiento = llamada.getArgument(0);
            registrados.add(movimiento);
            return movimiento;
        });
    }

    /** Las existencias vivas de la harina y su ultimo movimiento (la reciente, hoy; la antigua, ayer). */
    private void hayExistencias(Existencia antigua, Existencia reciente) {
        when(existenciaRepository.obtenerVivasParaPesaje(anyList())).thenReturn(List.of(antigua, reciente));
        lenient().when(movimientoRepository.obtenerPorExistencias(anyList())).thenReturn(List.of(
                movimiento(ID_ANTIGUA, AHORA.minusDays(1)),
                movimiento(ID_RECIENTE, AHORA.minusDays(3)),
                movimiento(ID_RECIENTE, AHORA)));
    }

    private static Movimiento movimiento(String idExistencia, LocalDateTime fecha) {
        return Movimiento.restaurar("e5000000-0000-0000-0000-00000000000" + idExistencia.charAt(idExistencia.length() - 1),
                BigDecimal.ONE, fecha, "Consumo", new ExistenciaId(idExistencia), null, new TipoMovimientoId(ID_TIPO_PRODUCCION),
                new UsuarioId(ID_USUARIO), null, null, false);
    }

    private static RegistrarPesajeRequest pesaje(String pesoTotal) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("pesoTotal", pesoTotal);
        datos.put("motivo", "Pesaje semanal");
        return RegistrarPesajeRequest.createFromRequest(datos, ID_USUARIO);
    }

    @Test
    void siEnLaRealidadHayMenosEsUnaMermaQueEmpiezaPorLaUltimaMovida() {
        // Arrange: el sistema dice 700 (300 + 400); se pesan 500 => faltan 200.
        Existencia antigua = existencia(ID_ANTIGUA, ID_DETALLE, "1000", "300", null);
        Existencia reciente = existencia(ID_RECIENTE, ID_DETALLE, "1000", "400", null);
        hayExistencias(antigua, reciente);

        // Act
        List<ExistenciaResponse> tocadas = useCase.execute(ID_HARINA, pesaje("500"));

        // Assert
        assertEquals(0, new BigDecimal("300").compareTo(antigua.getContenidoActual()));
        assertEquals(0, new BigDecimal("200").compareTo(reciente.getContenidoActual()));
        assertEquals(1, tocadas.size());
        assertEquals(1, registrados.size());
        assertEquals(ID_TIPO_MERMA, registrados.get(0).getTipoMovimientoId().getValue());
        assertEquals(0, new BigDecimal("200").compareTo(registrados.get(0).getCantidad()));
        assertEquals("Pesaje semanal", registrados.get(0).getMotivo());
        verify(recalcularStockUseCase).execute(ID_HARINA);
    }

    @Test
    void unaMermaMayorQueLaUltimaLaVaciaYSigueConLaAnterior() {
        // Arrange: el sistema dice 700; se pesan 100 => faltan 600: 400 de la reciente y 200 de la antigua.
        Existencia antigua = existencia(ID_ANTIGUA, ID_DETALLE, "1000", "300", null);
        Existencia reciente = existencia(ID_RECIENTE, ID_DETALLE, "1000", "400", null);
        hayExistencias(antigua, reciente);

        // Act
        useCase.execute(ID_HARINA, pesaje("100"));

        // Assert
        assertTrue(reciente.isAgotada());
        assertEquals(ID_ESTADO_AGOTADA, reciente.getEstadoExistenciaId().getValue());
        assertEquals(0, new BigDecimal("100").compareTo(antigua.getContenidoActual()));
        assertEquals(2, registrados.size());
        assertEquals(ID_RECIENTE, registrados.get(0).getExistenciaId().getValue());
        assertEquals(0, new BigDecimal("400").compareTo(registrados.get(0).getCantidad()));
        assertEquals(ID_ANTIGUA, registrados.get(1).getExistenciaId().getValue());
        assertEquals(0, new BigDecimal("200").compareTo(registrados.get(1).getCantidad()));
    }

    @Test
    void siEnLaRealidadHayMasEsUnAjusteQueRellenaHastaElContenidoInicial() {
        // Arrange: el sistema dice 1500 (500 + 1000 llena); se pesan 1700 => sobran 200, y la reciente ya
        // esta llena, asi que van a la antigua.
        Existencia antigua = existencia(ID_ANTIGUA, ID_DETALLE, "1000", "500", null);
        Existencia reciente = existencia(ID_RECIENTE, ID_DETALLE, "1000", "1000", null);
        hayExistencias(antigua, reciente);

        // Act
        useCase.execute(ID_HARINA, pesaje("1700"));

        // Assert
        assertEquals(0, new BigDecimal("700").compareTo(antigua.getContenidoActual()));
        assertEquals(0, new BigDecimal("1000").compareTo(reciente.getContenidoActual()));
        assertEquals(1, registrados.size());
        assertEquals(ID_TIPO_AJUSTE, registrados.get(0).getTipoMovimientoId().getValue());
        assertEquals(ID_ANTIGUA, registrados.get(0).getExistenciaId().getValue());
    }

    @Test
    void unAjustePuedeRellenarUnaExistenciaAgotada() {
        // Arrange: la reciente se agoto en una receta; el pesaje encuentra 50 de mas.
        Existencia antigua = existencia(ID_ANTIGUA, ID_DETALLE, "1000", "300", null);
        Existencia reciente = existencia(ID_RECIENTE, ID_DETALLE, "1000", "10", null);
        reciente.consumir(new BigDecimal("10"), AHORA, AGOTADA.getId());
        hayExistencias(antigua, reciente);

        // Act
        useCase.execute(ID_HARINA, pesaje("350"));

        // Assert
        assertEquals(0, new BigDecimal("50").compareTo(reciente.getContenidoActual()));
        assertEquals(ID_ESTADO_ABIERTA, reciente.getEstadoExistenciaId().getValue());
        assertNull(reciente.getFechaAcabado());
        assertEquals(0, new BigDecimal("300").compareTo(antigua.getContenidoActual()));
    }

    @Test
    void unAjusteQueNoCabeEnLasExistenciasSeRechazaSinTocarNada() {
        // Arrange: caben 700 (500 + 200); se pesan 1000 de mas.
        Existencia antigua = existencia(ID_ANTIGUA, ID_DETALLE, "1000", "500", null);
        Existencia reciente = existencia(ID_RECIENTE, ID_DETALLE, "1000", "800", null);
        hayExistencias(antigua, reciente);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_HARINA, pesaje("2300")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(0, new BigDecimal("500").compareTo(antigua.getContenidoActual()));
        verify(existenciaRepository, never()).actualizar(any(), any());
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void siElPesoCoincideNoSeRegistraNada() {
        // Arrange
        hayExistencias(existencia(ID_ANTIGUA, ID_DETALLE, "1000", "300", null), existencia(ID_RECIENTE, ID_DETALLE, "1000", "400", null));

        // Act
        List<ExistenciaResponse> tocadas = useCase.execute(ID_HARINA, pesaje("700"));

        // Assert
        assertTrue(tocadas.isEmpty());
        verify(movimientoRepository, never()).crear(any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void soloSePesaUnProductoBaseConPresentaciones() {
        // Arrange
        when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_HARINA, pesaje("500")));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(existenciaRepository, never()).obtenerVivasParaPesaje(anyList());
    }

    @Test
    void sinExistenciasNoHayNadaQuePesar() {
        // Arrange
        when(existenciaRepository.obtenerVivasParaPesaje(anyList())).thenReturn(List.of());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_HARINA, pesaje("500")));

        // Assert
        assertEquals(409, error.getCodigo());
    }

    @Test
    void laPeticionExigeUnPesoNoNegativoYUnMotivo() {
        // Act
        DomainException porNegativo = assertThrows(DomainException.class,
                () -> RegistrarPesajeRequest.createFromRequest(Map.of("pesoTotal", "-1", "motivo", "x"), ID_USUARIO));
        DomainException sinMotivo = assertThrows(DomainException.class,
                () -> RegistrarPesajeRequest.createFromRequest(Map.of("pesoTotal", "10"), ID_USUARIO));

        // Assert
        assertEquals(400, porNegativo.getCodigo());
        assertEquals(400, sinMotivo.getCodigo());
    }
}
