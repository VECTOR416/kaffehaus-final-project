package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases;

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
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ActualizarDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.CrearDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.DescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ObtenerDescuentosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;

/** Pruebas unitarias de los casos de uso de Descuento y de sus reglas segun el tipo (VEN-14). */
@ExtendWith(MockitoExtension.class)
class DescuentoUseCasesTest {

    private static final String ID_DESCUENTO = "0f000000-0000-0000-0000-000000000001";
    private static final String ID_CAFE = "0f000000-0000-0000-0000-000000000002";
    private static final String ID_PORCENTAJE = "0f100000-0000-0000-0000-000000000001";
    private static final String ID_MONTO_FIJO = "0f100000-0000-0000-0000-000000000002";
    private static final String ID_DOS_POR_UNO = "0f100000-0000-0000-0000-000000000003";
    private static final String ID_INEXISTENTE = "0f000000-0000-0000-0000-0000000000ee";

    @Mock private IDescuentoRepository descuentoRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private ITipoDescuentoRepository tipoDescuentoRepository;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_CAFE.equals(id.getValue()))))
                .thenReturn(Optional.of(productoDirecto(ID_CAFE, "Cafe")));
        hayTipo(TipoDescuento.restaurar(ID_PORCENTAJE, "Porcentaje", false));
        hayTipo(TipoDescuento.restaurar(ID_MONTO_FIJO, "Monto fijo", false));
        hayTipo(TipoDescuento.restaurar(ID_DOS_POR_UNO, "2x1", false));
        lenient().when(descuentoRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(descuentoRepository.crear(any())).thenAnswer(llamada -> {
            Descuento nuevo = llamada.getArgument(0);
            return Descuento.restaurar(ID_DESCUENTO, nuevo.getDescuento(), nuevo.getFechaInicio(), nuevo.getFechaFinal(), nuevo.isEstado(),
                    nuevo.getProductoId(), nuevo.getTipoDescuentoId(), false);
        });
    }

    private void hayTipo(TipoDescuento tipo) {
        lenient().when(tipoDescuentoRepository.obtenerPorId(argThat(id -> id != null && tipo.getId().getValue().equals(id.getValue()))))
                .thenReturn(Optional.of(tipo));
    }

    private Descuento hayDescuentoDel10PorCiento() {
        Descuento descuento = Descuento.restaurar(ID_DESCUENTO, new BigDecimal("10"), AHORA.minusDays(1), AHORA.plusDays(5), true,
                new ProductoId(ID_CAFE), new TipoDescuentoId(ID_PORCENTAJE), false);
        lenient().when(descuentoRepository.obtenerPorId(argThat(id -> id != null && ID_DESCUENTO.equals(id.getValue())))).thenReturn(Optional.of(descuento));
        return descuento;
    }

    private static Map<String, Object> alta(String valor, String idTipo) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("descuento", valor);
        datos.put("fechaInicio", "2026-10-01");
        datos.put("fechaFinal", "2026-10-31");
        datos.put("idProducto", ID_CAFE);
        datos.put("idTipoDescuento", idTipo);
        return datos;
    }

    private CrearDescuentoUseCase crear() {
        return new CrearDescuentoUseCase(descuentoRepository, productoRepository, tipoDescuentoRepository);
    }

    private ActualizarDescuentoUseCase actualizar() {
        return new ActualizarDescuentoUseCase(descuentoRepository, productoRepository, tipoDescuentoRepository);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearRegistraElDescuentoConSuTipo() {
        // Act
        DescuentoResponse respuesta = crear().execute(CrearDescuentoRequest.createFromRequest(alta("15", ID_PORCENTAJE)));

        // Assert
        assertEquals(ID_DESCUENTO, respuesta.getId());
        assertEquals(0, new BigDecimal("15").compareTo(respuesta.getDescuento()));
        assertEquals("Porcentaje", respuesta.getTipoDescuento().getNombre());
        assertEquals(ID_CAFE, respuesta.getIdProducto());
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), respuesta.getFechaInicio());
    }

    @Test
    void unPorcentajePuedeLlegarACienPeroNoSuperarlo() {
        // Act
        DescuentoResponse enElLimite = crear().execute(CrearDescuentoRequest.createFromRequest(alta("100", ID_PORCENTAJE)));
        DomainException error = assertThrows(DomainException.class,
                () -> crear().execute(CrearDescuentoRequest.createFromRequest(alta("100.01", ID_PORCENTAJE))));

        // Assert
        assertEquals(0, new BigDecimal("100").compareTo(enElLimite.getDescuento()));
        assertEquals(400, error.getCodigo());
        verify(descuentoRepository, times(1)).crear(any());
    }

    @Test
    void unMontoFijoNoTieneElTopeDelCien() {
        // Act
        DescuentoResponse respuesta = crear().execute(CrearDescuentoRequest.createFromRequest(alta("250", ID_MONTO_FIJO)));

        // Assert
        assertEquals("Monto fijo", respuesta.getTipoDescuento().getNombre());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5"})
    void elValorDelDescuentoDebeSerPositivo(String valor) {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> crear().execute(CrearDescuentoRequest.createFromRequest(alta(valor, ID_MONTO_FIJO))));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(descuentoRepository, never()).crear(any());
    }

    @Test
    void unTipoQueElSistemaNoSabeAplicarSeRechaza() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> crear().execute(CrearDescuentoRequest.createFromRequest(alta("10", ID_DOS_POR_UNO))));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("'2x1'"));
        verify(descuentoRepository, never()).crear(any());
    }

    @Test
    void laVentanaDeVigenciaNoPuedeEstarInvertida() {
        // Arrange
        Map<String, Object> invertida = alta("10", ID_PORCENTAJE);
        invertida.put("fechaInicio", "2026-10-31");
        invertida.put("fechaFinal", "2026-10-01");

        // Act
        DomainException error = assertThrows(DomainException.class, () -> crear().execute(CrearDescuentoRequest.createFromRequest(invertida)));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(descuentoRepository, never()).crear(any());
    }

    @Test
    void crearValidaQueExistanElProductoYElTipo() {
        // Arrange
        Map<String, Object> sinProducto = alta("10", ID_PORCENTAJE);
        sinProducto.put("idProducto", ID_INEXISTENTE);

        // Act
        DomainException porProducto = assertThrows(DomainException.class, () -> crear().execute(CrearDescuentoRequest.createFromRequest(sinProducto)));
        DomainException porTipo = assertThrows(DomainException.class,
                () -> crear().execute(CrearDescuentoRequest.createFromRequest(alta("10", ID_INEXISTENTE))));

        // Assert
        assertEquals(404, porProducto.getCodigo());
        assertEquals(404, porTipo.getCodigo());
        verify(descuentoRepository, never()).crear(any());
    }

    @Test
    void laPeticionDeAltaExigeValorFechasProductoYTipo() {
        // Arrange
        Map<String, Object> sinFechaFinal = alta("10", ID_PORCENTAJE);
        sinFechaFinal.remove("fechaFinal");
        Map<String, Object> sinValor = alta("10", ID_PORCENTAJE);
        sinValor.remove("descuento");

        // Act
        DomainException porFecha = assertThrows(DomainException.class, () -> CrearDescuentoRequest.createFromRequest(sinFechaFinal));
        DomainException porValor = assertThrows(DomainException.class, () -> CrearDescuentoRequest.createFromRequest(sinValor));

        // Assert
        assertEquals(400, porFecha.getCodigo());
        assertEquals(400, porValor.getCodigo());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarCambiaElValorYVuelveAValidarloConSuTipo() {
        // Arrange
        hayDescuentoDel10PorCiento();

        // Act
        DescuentoResponse respuesta = actualizar().execute(ID_DESCUENTO, ActualizarDescuentoRequest.createFromRequest(Map.of("descuento", "25")));
        DomainException error = assertThrows(DomainException.class,
                () -> actualizar().execute(ID_DESCUENTO, ActualizarDescuentoRequest.createFromRequest(Map.of("descuento", "150"))));

        // Assert
        assertEquals(0, new BigDecimal("25").compareTo(respuesta.getDescuento()));
        assertEquals(400, error.getCodigo());
        verify(descuentoRepository, times(1)).actualizar(any(), any());
    }

    @Test
    void alCambiarDeTipoSeValidaConElTipoNuevo() {
        // Arrange: 150 no vale como porcentaje, pero si como monto fijo.
        Descuento descuento = hayDescuentoDel10PorCiento();

        // Act
        DescuentoResponse respuesta = actualizar().execute(ID_DESCUENTO,
                ActualizarDescuentoRequest.createFromRequest(Map.of("descuento", "150", "idTipoDescuento", ID_MONTO_FIJO)));

        // Assert
        assertEquals("Monto fijo", respuesta.getTipoDescuento().getNombre());
        assertEquals(ID_MONTO_FIJO, descuento.getTipoDescuentoId().getValue());
    }

    @Test
    void actualizarValidaQueExistanLasReferenciasNuevasYElDescuento() {
        // Arrange
        hayDescuentoDel10PorCiento();

        // Act
        DomainException porProducto = assertThrows(DomainException.class,
                () -> actualizar().execute(ID_DESCUENTO, ActualizarDescuentoRequest.createFromRequest(Map.of("idProducto", ID_INEXISTENTE))));
        DomainException porTipo = assertThrows(DomainException.class,
                () -> actualizar().execute(ID_DESCUENTO, ActualizarDescuentoRequest.createFromRequest(Map.of("idTipoDescuento", ID_INEXISTENTE))));
        DomainException porDescuento = assertThrows(DomainException.class,
                () -> actualizar().execute(ID_INEXISTENTE, ActualizarDescuentoRequest.createFromRequest(Map.of("descuento", "5"))));

        // Assert
        assertEquals(404, porProducto.getCodigo());
        assertEquals(404, porTipo.getCodigo());
        assertEquals(404, porDescuento.getCodigo());
        verify(descuentoRepository, never()).actualizar(any(), any());
    }

    // ------------------------------------------------------------------ eliminar y consultas

    @Test
    void eliminarDaDeBajaElExistenteYRechazaElInexistente() {
        // Arrange
        EliminarDescuentoUseCase useCase = new EliminarDescuentoUseCase(descuentoRepository);
        hayDescuentoDel10PorCiento();

        // Act
        useCase.execute(ID_DESCUENTO);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

        // Assert
        verify(descuentoRepository, times(1)).eliminar(argThat(id -> ID_DESCUENTO.equals(id.getValue())));
        assertEquals(404, error.getCodigo());
    }

    @Test
    void obtenerPorIdDevuelveElDescuentoConSuTipo() {
        // Arrange
        ObtenerDescuentoPorIdUseCase useCase = new ObtenerDescuentoPorIdUseCase(descuentoRepository, tipoDescuentoRepository);
        hayDescuentoDel10PorCiento();

        // Act
        DescuentoResponse respuesta = useCase.execute(ID_DESCUENTO);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

        // Assert
        assertEquals("Porcentaje", respuesta.getTipoDescuento().getNombre());
        assertTrue(respuesta.isEstado());
        assertEquals(404, error.getCodigo());
    }

    @Test
    void elListadoResuelveElTipoDeCadaDescuentoYConservaLaPaginacion() {
        // Arrange
        ObtenerDescuentosUseCase useCase = new ObtenerDescuentosUseCase(descuentoRepository, tipoDescuentoRepository);
        Descuento descuento = hayDescuentoDel10PorCiento();
        when(descuentoRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(descuento), 0, 10, 1, false));

        // Act
        PaginatedResponse<List<DescuentoResponse>> pagina = useCase.execute(ObtenerDescuentosRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Porcentaje", pagina.getData().get(0).getTipoDescuento().getNombre());
        assertEquals(1, pagina.getTotal());
    }
}
