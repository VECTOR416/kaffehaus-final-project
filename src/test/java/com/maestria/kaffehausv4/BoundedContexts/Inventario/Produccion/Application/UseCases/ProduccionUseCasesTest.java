package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.DatosDePrueba;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.SalidaDeInventarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarSalidaDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ActualizarProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.CrearProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ObtenerProduccionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ProduccionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories.IProduccionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;

/**
 * Pruebas unitarias de los casos de uso de Produccion. Registrar una produccion consume los
 * insumos de la receta (a traves del caso de uso de salida, aqui simulado) y abre un lote de
 * produccion con su existencia; el movimiento de entrada es de tipo 'Receta'.
 *
 * Escenario: la receta rinde 10 panes con 4 de harina y 2 de levadura.
 */
@ExtendWith(MockitoExtension.class)
class ProduccionUseCasesTest {

    private static final String ID_PRODUCCION = "cf000000-0000-0000-0000-000000000001";
    private static final String ID_RECETA = "cf000000-0000-0000-0000-000000000002";
    private static final String ID_PAN = "cf000000-0000-0000-0000-000000000003";
    private static final String ID_HARINA = "cf000000-0000-0000-0000-000000000004";
    private static final String ID_LEVADURA = "cf000000-0000-0000-0000-000000000005";
    private static final String ID_LOTE = "cf000000-0000-0000-0000-000000000006";
    private static final String ID_DETALLE = "cf000000-0000-0000-0000-000000000007";
    private static final String ID_EXISTENCIA = "cf000000-0000-0000-0000-000000000008";

    @Mock private IProduccionRepository produccionRepository;
    @Mock private IRecetaRepository recetaRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private CrearProduccionUseCase crear;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        crear = new CrearProduccionUseCase(produccionRepository, recetaRepository, productoRepository, usuarioRepository, loteRepository,
                existenciaRepository, estadoExistenciaRepository, movimientoRepository, tipoMovimientoRepository,
                registrarSalidaDeInventarioUseCase, recalcularStockUseCase);

        lenient().when(recetaRepository.obtenerPorId(any())).thenReturn(Optional.of(receta(ID_RECETA, ID_PAN, "10",
                ingrediente("cf000000-0000-0000-0000-0000000000a1", ID_HARINA, "4"),
                ingrediente("cf000000-0000-0000-0000-0000000000a2", ID_LEVADURA, "2"))));
        lenient().when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(productoDirecto(ID_PAN, "Pan")));
        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(estadoExistenciaRepository.obtenerPorNombre("Abierta")).thenReturn(Optional.of(ABIERTA));
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.RECETA)).thenReturn(Optional.of(RECETA));
        lenient().when(produccionRepository.crear(any())).thenAnswer(llamada -> {
            Produccion nueva = llamada.getArgument(0);
            return Produccion.restaurar(ID_PRODUCCION, nueva.getCantidadEsperada(), nueva.getCantidadReal(), nueva.getDiferencia(),
                    nueva.getMotivoDiferencia(), nueva.getFechaProduccion(), nueva.getRecetaId(), false);
        });
        lenient().when(produccionRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        // Cada ingrediente consumido costo 15.00.
        lenient().when(registrarSalidaDeInventarioUseCase.execute(any()))
                .thenReturn(SalidaDeInventarioResponse.create(new BigDecimal("15.0000"), List.of()));
        lenient().when(loteRepository.crear(any())).thenAnswer(llamada -> {
            Lote nuevo = llamada.getArgument(0);
            DetalleLote linea = nuevo.getDetalles().get(0);
            return Lote.restaurar(ID_LOTE, nuevo.getFechaCompra(), nuevo.getTotal(), null, nuevo.getUsuarioId(), true, nuevo.getProduccionId(), false,
                    List.of(DetalleLote.restaurar(ID_DETALLE, linea.getProductoId(), linea.getCantidadInicial(), linea.getCantidadRestante(),
                            linea.getPrecioUnitario(), linea.isFraccionable(), linea.getFechaVencimiento(), false)));
        });
        lenient().when(existenciaRepository.crear(any())).thenAnswer(llamada -> DatosDePrueba.guardada(llamada.getArgument(0), ID_EXISTENCIA));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> DatosDePrueba.guardado(llamada.getArgument(0), "cf000000-0000-0000-0000-0000000000d1"));
    }

    private static CrearProduccionRequest produccion(String esperada, String real) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("cantidadEsperada", esperada);
        datos.put("cantidadReal", real);
        datos.put("fechaProduccion", AHORA);
        datos.put("fechaVencimiento", "2026-10-15");
        datos.put("idReceta", ID_RECETA);
        return CrearProduccionRequest.createFromRequest(datos, ID_USUARIO);
    }

    private static Produccion registrada() {
        return Produccion.restaurar(ID_PRODUCCION, new BigDecimal("20.00"), new BigDecimal("18.00"), new BigDecimal("-2.00"),
                "Se quemo una bandeja", AHORA, new RecetaId(ID_RECETA), false);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void registraLaProduccionConSuDiferenciaDerivada() {
        // Act
        ProduccionResponse respuesta = crear.execute(produccion("20", "18"));

        // Assert
        assertEquals(ID_PRODUCCION, respuesta.getId());
        assertEquals(new BigDecimal("20.00"), respuesta.getCantidadEsperada());
        assertEquals(new BigDecimal("18.00"), respuesta.getCantidadReal());
        assertEquals(new BigDecimal("-2.00"), respuesta.getDiferencia());
        assertEquals(ID_RECETA, respuesta.getIdReceta());
    }

    @Test
    void consumeCadaIngredienteEscaladoALaCantidadEsperada() {
        // Arrange: producir 20 con una receta que rinde 10 duplica los ingredientes.
        ArgumentCaptor<RegistrarSalidaDeInventarioRequest> consumos = ArgumentCaptor.forClass(RegistrarSalidaDeInventarioRequest.class);

        // Act
        crear.execute(produccion("20", "18"));

        // Assert
        verify(registrarSalidaDeInventarioUseCase, times(2)).execute(consumos.capture());
        RegistrarSalidaDeInventarioRequest harina = consumos.getAllValues().get(0);
        RegistrarSalidaDeInventarioRequest levadura = consumos.getAllValues().get(1);
        assertEquals(ID_HARINA, harina.getIdProducto());
        assertEquals(0, new BigDecimal("8").compareTo(harina.getCantidad()));
        assertEquals(ID_LEVADURA, levadura.getIdProducto());
        assertEquals(0, new BigDecimal("4").compareTo(levadura.getCantidad()));
        assertEquals(TipoMovimiento.RECETA, harina.getNombreTipoMovimientoDirecto());
        assertEquals(TipoMovimiento.RECETA, harina.getNombreTipoMovimientoPorReceta());
        assertEquals(ID_PRODUCCION, harina.getIdProduccion());
        assertEquals(ID_USUARIO, harina.getIdUsuario());
        assertEquals(AHORA, harina.getFecha());
        assertTrue(harina.getMotivo().startsWith("Consumo de la produccion"));
    }

    @Test
    void elProductoTerminadoEntraEnUnLoteDeProduccionCosteadoConSusInsumos() {
        // Arrange: 2 ingredientes a 15.00 = 30.00 de insumos entre 18 unidades reales = 1.67.
        ArgumentCaptor<Lote> lote = ArgumentCaptor.forClass(Lote.class);

        // Act
        crear.execute(produccion("20", "18"));

        // Assert
        verify(loteRepository).crear(lote.capture());
        assertTrue(lote.getValue().isEsProduccion());
        assertNull(lote.getValue().getProveedorId());
        assertEquals(ID_PRODUCCION, lote.getValue().getProduccionId().getValue());
        DetalleLote linea = lote.getValue().getDetalles().get(0);
        assertEquals(ID_PAN, linea.getProductoId().getValue());
        assertEquals(new BigDecimal("18.00"), linea.getCantidadInicial());
        assertEquals(new BigDecimal("1.67"), linea.getPrecioUnitario());
        assertEquals(LocalDate.of(2026, 10, 15), linea.getFechaVencimiento());
    }

    @Test
    void abreUnaExistenciaConLaCantidadRealYRegistraSuEntradaComoReceta() {
        // Arrange
        ArgumentCaptor<Existencia> existencia = ArgumentCaptor.forClass(Existencia.class);
        ArgumentCaptor<Movimiento> movimiento = ArgumentCaptor.forClass(Movimiento.class);

        // Act
        crear.execute(produccion("20", "18"));

        // Assert
        verify(existenciaRepository).crear(existencia.capture());
        assertEquals(0, new BigDecimal("18").compareTo(existencia.getValue().getContenidoInicial()));
        assertEquals(0, new BigDecimal("18").compareTo(existencia.getValue().getContenidoActual()));
        assertEquals(ID_DETALLE, existencia.getValue().getDetalleLoteId().getValue());
        assertEquals(ID_ESTADO_ABIERTA, existencia.getValue().getEstadoExistenciaId().getValue());
        assertEquals(LocalDate.of(2026, 10, 15), existencia.getValue().getFechaVencimiento());

        verify(movimientoRepository).crear(movimiento.capture());
        assertEquals(0, new BigDecimal("18").compareTo(movimiento.getValue().getCantidad()));
        assertEquals(ID_TIPO_RECETA, movimiento.getValue().getTipoMovimientoId().getValue());
        assertEquals(ID_EXISTENCIA, movimiento.getValue().getExistenciaId().getValue());
        assertEquals(ID_PRODUCCION, movimiento.getValue().getProduccionId().getValue());
        assertTrue(movimiento.getValue().getMotivo().startsWith("Entrada de la produccion"));
        verify(recalcularStockUseCase).execute(ID_PAN);
    }

    @Test
    void unaTandaPerdidaConsumeLosInsumosPeroNoAbreLoteNiExistencia() {
        // Act
        ProduccionResponse respuesta = crear.execute(produccion("20", "0"));

        // Assert
        assertEquals(new BigDecimal("-20.00"), respuesta.getDiferencia());
        verify(registrarSalidaDeInventarioUseCase, times(2)).execute(any());
        verifyNoInteractions(loteRepository, existenciaRepository, movimientoRepository, recalcularStockUseCase);
    }

    @Test
    void unProductoBajoDemandaNoSeProducePorLotes() {
        // Arrange
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.of(productoBajoDemanda(ID_PAN, "Capuchino")));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> crear.execute(produccion("20", "18")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("bajo demanda"));
        verify(produccionRepository, never()).crear(any());
        verifyNoInteractions(registrarSalidaDeInventarioUseCase);
    }

    @Test
    void siFaltaUnInsumoElErrorSePropagaYNoSeAbreElProductoTerminado() {
        // Arrange
        when(registrarSalidaDeInventarioUseCase.execute(any())).thenThrow(DomainException.throwException(409, "Stock insuficiente de 'Harina'"));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> crear.execute(produccion("20", "18")));

        // Assert
        assertEquals(409, error.getCodigo());
        verifyNoInteractions(loteRepository, existenciaRepository, movimientoRepository);
    }

    @Test
    void validaQueExistanLaRecetaElProductoYElUsuario() {
        // Arrange
        CrearProduccionRequest peticion = produccion("20", "18");

        // Act
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        DomainException sinUsuario = assertThrows(DomainException.class, () -> crear.execute(peticion));
        when(productoRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        DomainException sinProducto = assertThrows(DomainException.class, () -> crear.execute(peticion));
        when(recetaRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        DomainException sinReceta = assertThrows(DomainException.class, () -> crear.execute(peticion));

        // Assert
        assertEquals(404, sinUsuario.getCodigo());
        assertEquals(404, sinProducto.getCodigo());
        assertEquals(404, sinReceta.getCodigo());
        verify(produccionRepository, never()).crear(any());
    }

    @Test
    void unaCantidadEsperadaNoPositivaSeRechazaSinConsumirNada() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> crear.execute(produccion("0", "0")));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(produccionRepository, never()).crear(any());
        verifyNoInteractions(registrarSalidaDeInventarioUseCase);
    }

    @Test
    void sinElCatalogoSembradoNoSeAbreElProductoTerminado() {
        // Arrange
        when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.RECETA)).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> crear.execute(produccion("20", "18")));

        // Assert
        assertEquals(404, error.getCodigo());
        verifyNoInteractions(loteRepository, existenciaRepository);
    }

    @Test
    void laPeticionExigeCantidadesRecetaYFechaDeVencimiento() {
        // Arrange
        Map<String, Object> sinVencimiento = new HashMap<>(Map.of("cantidadEsperada", "20", "cantidadReal", "18", "idReceta", ID_RECETA));
        Map<String, Object> sinReceta = new HashMap<>(Map.of("cantidadEsperada", "20", "cantidadReal", "18", "fechaVencimiento", "2026-10-15"));

        // Act
        DomainException porVencimiento = assertThrows(DomainException.class, () -> CrearProduccionRequest.createFromRequest(sinVencimiento, ID_USUARIO));
        DomainException porReceta = assertThrows(DomainException.class, () -> CrearProduccionRequest.createFromRequest(sinReceta, ID_USUARIO));

        // Assert
        assertEquals(400, porVencimiento.getCodigo());
        assertEquals(400, porReceta.getCodigo());
    }

    // ------------------------------------------------------------------ actualizar, eliminar y consultas

    @Test
    void actualizarSoloTocaElMotivoYLaFecha() {
        // Arrange
        ActualizarProduccionUseCase useCase = new ActualizarProduccionUseCase(produccionRepository);
        when(produccionRepository.obtenerPorId(any())).thenReturn(Optional.of(registrada()));

        // Act
        ProduccionResponse respuesta = useCase.execute(ID_PRODUCCION,
                ActualizarProduccionRequest.createFromRequest(Map.of("motivoDiferencia", "Horno averiado")));

        // Assert
        assertEquals("Horno averiado", respuesta.getMotivoDiferencia());
        assertEquals(new BigDecimal("18.00"), respuesta.getCantidadReal());
        assertEquals(AHORA, respuesta.getFechaProduccion());
    }

    @Test
    void actualizarUnaInexistenteRespondeNoEncontradoYNoGuarda() {
        // Arrange
        ActualizarProduccionUseCase useCase = new ActualizarProduccionUseCase(produccionRepository);
        when(produccionRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID_PRODUCCION, ActualizarProduccionRequest.createFromRequest(Map.of())));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(produccionRepository, never()).actualizar(any(), any());
    }

    @Test
    void eliminarDaDeBajaLaExistenteYRechazaLaInexistente() {
        // Arrange
        EliminarProduccionUseCase useCase = new EliminarProduccionUseCase(produccionRepository);
        when(produccionRepository.obtenerPorId(any())).thenReturn(Optional.of(registrada())).thenReturn(Optional.empty());

        // Act
        useCase.execute(ID_PRODUCCION);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_PRODUCCION));

        // Assert
        verify(produccionRepository, times(1)).eliminar(argThat(id -> ID_PRODUCCION.equals(id.getValue())));
        assertEquals(404, error.getCodigo());
    }

    @Test
    void obtenerPorIdDevuelveLaProduccionOResponde404() {
        // Arrange
        ObtenerProduccionPorIdUseCase useCase = new ObtenerProduccionPorIdUseCase(produccionRepository);
        when(produccionRepository.obtenerPorId(any())).thenReturn(Optional.of(registrada())).thenReturn(Optional.empty());

        // Act
        ProduccionResponse encontrada = useCase.execute(ID_PRODUCCION);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_PRODUCCION));

        // Assert
        assertEquals("Se quemo una bandeja", encontrada.getMotivoDiferencia());
        assertEquals(404, error.getCodigo());
    }

    @Test
    void elListadoConvierteCadaProduccionYConservaLaPaginacion() {
        // Arrange
        ObtenerProduccionesUseCase useCase = new ObtenerProduccionesUseCase(produccionRepository);
        when(produccionRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(registrada()), 0, 10, 1, false));

        // Act
        PaginatedResponse<List<ProduccionResponse>> pagina = useCase.execute(ObtenerProduccionesRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals(ID_PRODUCCION, pagina.getData().get(0).getId());
        assertEquals(1, pagina.getTotal());
    }
}
