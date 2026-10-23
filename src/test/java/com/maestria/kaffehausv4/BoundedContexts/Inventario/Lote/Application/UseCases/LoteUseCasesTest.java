package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AJUSTE;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_OTRA_UNIDAD;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO_AJUSTE;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO_MERMA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_PROVEEDOR;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_UNIDAD;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_USUARIO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.MERMA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.detalle;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.existencia;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.loteDeCompra;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.loteDeProduccion;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.presentacion;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.productoDirecto;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ActualizarLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.CrearLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DetalleAperturableResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerDetallesAperturablesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los casos de uso de Lote: el registro de una compra, su correccion
 * (que concilia las lineas), su baja y la consulta de lo que se puede abrir. Repositorios
 * simulados.
 *
 * Escenario de correccion: un lote con 10 de cafe a 12.00 y 8 de azucar a 5.00.
 */
@ExtendWith(MockitoExtension.class)
class LoteUseCasesTest {

    private static final String ID_LOTE = "e7000000-0000-0000-0000-000000000001";
    private static final String ID_CAFE = "e7000000-0000-0000-0000-00000000000a";
    private static final String ID_AZUCAR = "e7000000-0000-0000-0000-00000000000b";
    private static final String ID_LINEA_CAFE = "e7000000-0000-0000-0000-0000000000c1";
    private static final String ID_LINEA_AZUCAR = "e7000000-0000-0000-0000-0000000000c2";
    private static final String ID_INEXISTENTE = "e7000000-0000-0000-0000-0000000000ee";

    @Mock private ILoteRepository loteRepository;
    @Mock private IProveedorRepository proveedorRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private IConversionRepository conversionRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        lenient().when(proveedorRepository.obtenerPorId(argThat(id -> id != null && ID_PROVEEDOR.equals(id.getValue()))))
                .thenReturn(Optional.of(Proveedor.restaurar(ID_PROVEEDOR, new PersonaId("e7000000-0000-0000-0000-0000000000f1"), null, null, null, false)));
        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        hayProducto(productoDirecto(ID_CAFE, "Cafe"));
        hayProducto(productoDirecto(ID_AZUCAR, "Azucar"));
        lenient().when(existenciaRepository.obtenerDisponiblesPorDetallesLote(anyList())).thenReturn(List.of());
        lenient().when(loteRepository.actualizar(any(), any())).thenAnswer(llamada -> conIds(llamada.getArgument(1)));
        lenient().when(loteRepository.crear(any())).thenAnswer(llamada -> conIds(llamada.getArgument(0)));
    }

    private void hayProducto(Producto producto) {
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && producto.getId().getValue().equals(id.getValue()))))
                .thenReturn(Optional.of(producto));
    }

    /** Lo que hace la persistencia al guardar: asignar id al lote y a las lineas que no lo tienen. */
    private static Lote conIds(Lote lote) {
        List<DetalleLote> lineas = new ArrayList<>();
        for (DetalleLote linea : lote.getDetalles()) {
            String id = linea.getId() != null ? linea.getId().getValue() : "e7000000-0000-0000-0000-0000000000d" + lineas.size();
            lineas.add(DetalleLote.restaurar(id, linea.getProductoId(), linea.getCantidadInicial(), linea.getCantidadRestante(),
                    linea.getPrecioUnitario(), linea.isFraccionable(), linea.getFechaVencimiento(), linea.isBorrado()));
        }
        return Lote.restaurar(ID_LOTE, lote.getFechaCompra(), lote.getTotal(), lote.getProveedorId(), lote.getUsuarioId(),
                lote.isEsProduccion(), lote.getProduccionId(), lote.isBorrado(), lineas);
    }

    private Lote hayLoteRegistrado() {
        Lote lote = loteDeCompra(ID_LOTE,
                detalle(ID_LINEA_CAFE, ID_CAFE, "10", "10", "12", null),
                detalle(ID_LINEA_AZUCAR, ID_AZUCAR, "8", "8", "5", null));
        lenient().when(loteRepository.obtenerPorId(argThat(id -> id != null && ID_LOTE.equals(id.getValue())))).thenReturn(Optional.of(lote));
        lenient().when(loteRepository.obtenerPorIdParaActualizar(argThat(id -> id != null && ID_LOTE.equals(id.getValue()))))
                .thenReturn(Optional.of(lote));
        return lote;
    }

    private Lote loteGuardado() {
        ArgumentCaptor<Lote> captor = ArgumentCaptor.forClass(Lote.class);
        verify(loteRepository).actualizar(any(), captor.capture());
        return captor.getValue();
    }

    private static Map<String, Object> linea(String idProducto, String cantidad, String precio) {
        return Map.of("idProducto", idProducto, "cantidadInicial", cantidad, "precioUnitario", precio);
    }

    // ================================================================== crear

    @Nested
    class Crear {

        private CrearLoteUseCase useCase;

        @BeforeEach
        void crearCasoDeUso() {
            useCase = new CrearLoteUseCase(loteRepository, proveedorRepository, usuarioRepository, productoRepository, recalcularStockUseCase);
        }

        private CrearLoteRequest compra(List<Map<String, Object>> lineas) {
            return CrearLoteRequest.createFromRequest(Map.of("idProveedor", ID_PROVEEDOR, "detalles", lineas), ID_USUARIO);
        }

        @Test
        void registraLaCompraConSusLineasYSuTotal() {
            // Act
            LoteResponse respuesta = useCase.execute(compra(List.of(linea(ID_CAFE, "10", "12"), linea(ID_AZUCAR, "8", "5"))));

            // Assert
            assertEquals(ID_LOTE, respuesta.getId());
            assertEquals(new BigDecimal("160.00"), respuesta.getTotal());
            assertEquals(ID_PROVEEDOR, respuesta.getIdProveedor());
            assertEquals(ID_USUARIO, respuesta.getIdUsuario());
            assertFalse(respuesta.isEsProduccion());
            assertEquals(2, respuesta.getDetalles().size());
            assertEquals(new BigDecimal("10.00"), respuesta.getDetalles().get(0).getCantidadRestante());
        }

        @Test
        void laCompraRecalculaElStockDeCadaProductoEnOrdenDeId() {
            // Act
            useCase.execute(compra(List.of(linea(ID_CAFE, "10", "12"), linea(ID_AZUCAR, "8", "5"))));

            // Assert: sube el stock de unidades de lo comprado (logica.md PRODUCTO-16).
            InOrder orden = inOrder(recalcularStockUseCase);
            String primero = ID_CAFE.compareTo(ID_AZUCAR) < 0 ? ID_CAFE : ID_AZUCAR;
            String segundo = primero.equals(ID_CAFE) ? ID_AZUCAR : ID_CAFE;
            orden.verify(recalcularStockUseCase).execute(primero);
            orden.verify(recalcularStockUseCase).execute(segundo);
        }

        @Test
        void unProductoBaseConPresentacionesNoSeCompraSuelto() {
            // Arrange: el cafe tiene presentaciones (logica.md LOTE-13).
            when(productoRepository.obtenerPresentaciones(argThat(id -> id != null && ID_CAFE.equals(id.getValue()))))
                    .thenReturn(List.of(presentacion("e7000000-0000-0000-0000-0000000000f1", "Bolsa de cafe", ID_CAFE)));

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(compra(List.of(linea(ID_CAFE, "1", "1")))));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("tiene presentaciones"));
            verify(loteRepository, never()).crear(any());
            verifyNoInteractions(recalcularStockUseCase);
        }

        @Test
        void validaQueExistanProveedorUsuarioYProductos() {
            // Arrange
            CrearLoteRequest conProductoInexistente = compra(List.of(linea(ID_INEXISTENTE, "1", "1")));
            CrearLoteRequest valida = compra(List.of(linea(ID_CAFE, "1", "1")));
            CrearLoteRequest conProveedorInexistente = CrearLoteRequest.createFromRequest(
                    Map.of("idProveedor", ID_INEXISTENTE, "detalles", List.of(linea(ID_CAFE, "1", "1"))), ID_USUARIO);

            // Act
            DomainException sinProducto = assertThrows(DomainException.class, () -> useCase.execute(conProductoInexistente));
            DomainException sinProveedor = assertThrows(DomainException.class, () -> useCase.execute(conProveedorInexistente));
            when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());
            DomainException sinUsuario = assertThrows(DomainException.class, () -> useCase.execute(valida));

            // Assert
            assertEquals(404, sinProducto.getCodigo());
            assertEquals(404, sinProveedor.getCodigo());
            assertEquals(404, sinUsuario.getCodigo());
            verify(loteRepository, never()).crear(any());
        }

        @Test
        void unaLineaConCantidadOPrecioInvalidosImpideLaCompra() {
            // Act
            DomainException porCantidad = assertThrows(DomainException.class, () -> useCase.execute(compra(List.of(linea(ID_CAFE, "0", "12")))));
            DomainException porPrecio = assertThrows(DomainException.class, () -> useCase.execute(compra(List.of(linea(ID_CAFE, "1", "-1")))));

            // Assert
            assertEquals(400, porCantidad.getCodigo());
            assertEquals(400, porPrecio.getCodigo());
            verify(loteRepository, never()).crear(any());
        }

        @Test
        void laPeticionExigeProveedorYAlMenosUnaLinea() {
            // Act
            DomainException sinLineas = assertThrows(DomainException.class,
                    () -> CrearLoteRequest.createFromRequest(Map.of("idProveedor", ID_PROVEEDOR, "detalles", List.of()), ID_USUARIO));
            DomainException sinProveedor = assertThrows(DomainException.class,
                    () -> CrearLoteRequest.createFromRequest(Map.of("detalles", List.of(linea(ID_CAFE, "1", "1"))), ID_USUARIO));

            // Assert
            assertEquals(400, sinLineas.getCodigo());
            assertEquals(400, sinProveedor.getCodigo());
        }
    }

    // ================================================================== actualizar

    @Nested
    class Actualizar {

        private ActualizarLoteUseCase useCase;

        @BeforeEach
        void crearCasoDeUso() {
            useCase = new ActualizarLoteUseCase(loteRepository, proveedorRepository, productoRepository, existenciaRepository,
                    movimientoRepository, tipoMovimientoRepository, recalcularStockUseCase);
            lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.MERMA)).thenReturn(Optional.of(MERMA));
            lenient().when(tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.AJUSTE)).thenReturn(Optional.of(AJUSTE));
        }

        private ActualizarLoteRequest conLineas(List<Map<String, Object>> lineas) {
            return ActualizarLoteRequest.createFromRequest(Map.of("detalles", lineas), ID_USUARIO);
        }

        /** Una correccion por conteo, con su motivo (logica.md LOTE-23). */
        private ActualizarLoteRequest conteo(List<Map<String, Object>> lineas) {
            return ActualizarLoteRequest.createFromRequest(Map.of("detalles", lineas, "motivo", "Conteo semanal"), ID_USUARIO);
        }

        private Movimiento movimientoRegistrado() {
            ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
            verify(movimientoRepository).crear(captor.capture());
            return captor.getValue();
        }

        @Test
        void sinLineasEnLaPeticionSoloCorrigeLaCabecera() {
            // Arrange
            hayLoteRegistrado();

            // Act
            useCase.execute(ID_LOTE, ActualizarLoteRequest.createFromRequest(Map.of("fechaCompra", "2026-09-01"), ID_USUARIO));

            // Assert
            Lote guardado = loteGuardado();
            assertEquals(LocalDate.of(2026, 9, 1), guardado.getFechaCompra().toLocalDate());
            assertEquals(2, guardado.getDetallesActivos().size());
        }

        @Test
        void corregirElPrecioDeUnaLineaRecalculaElTotal() {
            // Arrange
            hayLoteRegistrado();

            // Act
            LoteResponse respuesta = useCase.execute(ID_LOTE, conLineas(List.of(
                    Map.of("id", ID_LINEA_CAFE, "precioUnitario", "15"), Map.of("id", ID_LINEA_AZUCAR))));

            // Assert: 10 x 15.00 + 8 x 5.00
            assertEquals(new BigDecimal("190.00"), respuesta.getTotal());
        }

        @Test
        void unaLineaQueYaNoVieneSeDaDeBajaSiNoTieneExistencias() {
            // Arrange
            hayLoteRegistrado();

            // Act
            useCase.execute(ID_LOTE, conLineas(List.of(Map.of("id", ID_LINEA_CAFE))));

            // Assert
            Lote guardado = loteGuardado();
            assertEquals(1, guardado.getDetallesActivos().size());
            assertEquals(ID_LINEA_CAFE, guardado.getDetallesActivos().get(0).getId().getValue());
            assertEquals(new BigDecimal("120.00"), guardado.getTotal());
        }

        @Test
        void unaLineaConExistenciasEnUsoNoSePuedeDarDeBaja() {
            // Arrange
            hayLoteRegistrado();
            when(existenciaRepository.obtenerDisponiblesPorDetallesLote(argThat(lineas ->
                    lineas != null && lineas.size() == 1 && ID_LINEA_AZUCAR.equals(lineas.get(0).getValue()))))
                    .thenReturn(List.of(existencia("e7000000-0000-0000-0000-0000000000a9", ID_LINEA_AZUCAR, "1000", "500", null)));

            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_LOTE, conLineas(List.of(Map.of("id", ID_LINEA_CAFE)))));

            // Assert
            assertEquals(409, error.getCodigo());
            assertTrue(error.getMensaje().contains(ID_LINEA_AZUCAR));
            verify(loteRepository, never()).actualizar(any(), any());
        }

        @Test
        void unaLineaSinIdSeAgregaComoNueva() {
            // Arrange
            hayLoteRegistrado();
            String idLeche = "e7000000-0000-0000-0000-00000000000c";
            hayProducto(productoDirecto(idLeche, "Leche"));

            // Act
            useCase.execute(ID_LOTE, conLineas(List.of(
                    Map.of("id", ID_LINEA_CAFE), Map.of("id", ID_LINEA_AZUCAR), linea(idLeche, "4", "7.5"))));

            // Assert: 120 + 40 + 30
            Lote guardado = loteGuardado();
            assertEquals(3, guardado.getDetallesActivos().size());
            assertEquals(new BigDecimal("190.00"), guardado.getTotal());
        }

        @Test
        void unaLineaNuevaExigeProductoCantidadYPrecio() {
            // Arrange
            hayLoteRegistrado();

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LOTE, conLineas(List.of(
                    Map.of("id", ID_LINEA_CAFE), Map.of("id", ID_LINEA_AZUCAR), Map.of("idProducto", ID_CAFE, "cantidadInicial", "1")))));

            // Assert
            assertEquals(400, error.getCodigo());
            verify(loteRepository, never()).actualizar(any(), any());
        }

        @Test
        void unaLineaNuevaConUnProductoInexistenteSeRechaza() {
            // Arrange
            hayLoteRegistrado();

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LOTE, conLineas(List.of(
                    Map.of("id", ID_LINEA_CAFE), Map.of("id", ID_LINEA_AZUCAR), linea(ID_INEXISTENTE, "1", "1")))));

            // Assert
            assertEquals(404, error.getCodigo());
        }

        @Test
        void noSePuedeCambiarElProductoDeUnaLineaExistente() {
            // Arrange
            hayLoteRegistrado();

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LOTE, conLineas(List.of(
                    Map.of("id", ID_LINEA_CAFE, "idProducto", ID_AZUCAR), Map.of("id", ID_LINEA_AZUCAR)))));

            // Assert
            assertEquals(400, error.getCodigo());
            verify(loteRepository, never()).actualizar(any(), any());
        }

        @Test
        void seAceptaRepetirElMismoProductoDeLaLinea() {
            // Arrange
            hayLoteRegistrado();

            // Act
            useCase.execute(ID_LOTE, conLineas(List.of(Map.of("id", ID_LINEA_CAFE, "idProducto", ID_CAFE), Map.of("id", ID_LINEA_AZUCAR))));

            // Assert
            assertEquals(2, loteGuardado().getDetallesActivos().size());
        }

        @Test
        void contarMenosUnidadesAjustaLaLineaYRegistraUnaMerma() {
            // Arrange: la linea de cafe tenia 10.
            hayLoteRegistrado();

            // Act
            useCase.execute(ID_LOTE, conteo(List.of(Map.of("id", ID_LINEA_CAFE, "cantidadRestante", "6"), Map.of("id", ID_LINEA_AZUCAR))));

            // Assert (LOTE-23)
            assertEquals(new BigDecimal("6.00"), loteGuardado().obtenerDetalle(ID_LINEA_CAFE).getCantidadRestante());
            Movimiento merma = movimientoRegistrado();
            assertEquals(ID_TIPO_MERMA, merma.getTipoMovimientoId().getValue());
            assertEquals(0, new BigDecimal("4").compareTo(merma.getCantidad()));
            assertEquals(ID_LINEA_CAFE, merma.getDetalleLoteId().getValue());
            assertEquals("Conteo semanal", merma.getMotivo());
            assertEquals(ID_USUARIO, merma.getUsuarioId().getValue());
        }

        @Test
        void contarMasUnidadesRegistraUnAjuste() {
            // Arrange: la linea de cafe tiene 10 de 10; se baja antes a 7 para poder contar 9.
            hayLoteRegistrado().ajustarCantidadRestante(ID_LINEA_CAFE, new BigDecimal("7"));

            // Act
            useCase.execute(ID_LOTE, conteo(List.of(Map.of("id", ID_LINEA_CAFE, "cantidadRestante", "9"), Map.of("id", ID_LINEA_AZUCAR))));

            // Assert
            Movimiento ajuste = movimientoRegistrado();
            assertEquals(ID_TIPO_AJUSTE, ajuste.getTipoMovimientoId().getValue());
            assertEquals(0, new BigDecimal("2").compareTo(ajuste.getCantidad()));
        }

        @Test
        void corregirLaCantidadRestanteSinMotivoSeRechazaSinGuardar() {
            // Arrange
            hayLoteRegistrado();

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LOTE,
                    conLineas(List.of(Map.of("id", ID_LINEA_CAFE, "cantidadRestante", "6"), Map.of("id", ID_LINEA_AZUCAR)))));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("'motivo'"));
            verify(loteRepository, never()).actualizar(any(), any());
            verify(movimientoRepository, never()).crear(any());
        }

        @Test
        void unConteoQueCoincideNoExigeMotivoNiRegistraMovimiento() {
            // Arrange
            hayLoteRegistrado();

            // Act
            useCase.execute(ID_LOTE, conLineas(List.of(Map.of("id", ID_LINEA_CAFE, "cantidadRestante", "10"), Map.of("id", ID_LINEA_AZUCAR))));

            // Assert
            verify(movimientoRepository, never()).crear(any());
        }

        @Test
        void elLoteSeBloqueaPorSuRaizAntesDeLeerlo() {
            // Arrange
            hayLoteRegistrado();

            // Act
            useCase.execute(ID_LOTE, conteo(List.of(Map.of("id", ID_LINEA_CAFE, "cantidadRestante", "6"), Map.of("id", ID_LINEA_AZUCAR))));

            // Assert
            // LOTE-27: se lee una sola vez, con el bloqueo del agregado tomado.
            verify(loteRepository).obtenerPorIdParaActualizar(argThat(id -> ID_LOTE.equals(id.getValue())));
            verify(loteRepository, never()).obtenerPorId(any());
            verify(loteRepository, never()).obtenerPorDetalleIdParaActualizar(any());
        }

        @Test
        void laCantidadRestanteNoPuedeSuperarLaComprada() {
            // Arrange
            hayLoteRegistrado();

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LOTE, conLineas(List.of(
                    Map.of("id", ID_LINEA_CAFE, "cantidadRestante", "11"), Map.of("id", ID_LINEA_AZUCAR)))));

            // Assert
            assertEquals(400, error.getCodigo());
            verify(loteRepository, never()).actualizar(any(), any());
        }

        @Test
        void unaLineaQueNoPerteneceAlLoteSeRechaza() {
            // Arrange
            hayLoteRegistrado();

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LOTE, conLineas(List.of(
                    Map.of("id", ID_LINEA_CAFE), Map.of("id", ID_LINEA_AZUCAR), Map.of("id", ID_INEXISTENTE, "precioUnitario", "1")))));

            // Assert
            assertEquals(404, error.getCodigo());
        }

        @Test
        void elNuevoProveedorDebeExistir() {
            // Arrange
            hayLoteRegistrado();

            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_LOTE, ActualizarLoteRequest.createFromRequest(Map.of("idProveedor", ID_INEXISTENTE), ID_USUARIO)));

            // Assert
            assertEquals(404, error.getCodigo());
            verify(loteRepository, never()).actualizar(any(), any());
        }

        @Test
        void unLoteDeProduccionNoAdmiteProveedor() {
            // Arrange
            when(loteRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(loteDeProduccion(ID_LOTE, "e7000000-0000-0000-0000-0000000000b9",
                    detalle(ID_LINEA_CAFE, ID_CAFE, "10", "0", "3", null))));

            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_LOTE, ActualizarLoteRequest.createFromRequest(Map.of("idProveedor", ID_PROVEEDOR), ID_USUARIO)));

            // Assert
            assertEquals(400, error.getCodigo());
        }

        @Test
        void unLoteInexistenteRespondeNoEncontrado() {
            // Arrange
            when(loteRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_LOTE, ActualizarLoteRequest.createFromRequest(Map.of(), ID_USUARIO)));

            // Assert
            assertEquals(404, error.getCodigo());
            assertTrue(error.getMensaje().contains(ID_LOTE));
        }
    }

    // ================================================================== eliminar y consultas

    @Nested
    class EliminarYConsultar {

        @Test
        void eliminarDaDeBajaUnLoteSinExistenciasEnUso() {
            // Arrange
            EliminarLoteUseCase useCase = new EliminarLoteUseCase(loteRepository, existenciaRepository, recalcularStockUseCase);
            hayLoteRegistrado();

            // Act
            useCase.execute(ID_LOTE);

            // Assert
            verify(existenciaRepository).obtenerDisponiblesPorDetallesLote(argThat(lineas -> lineas.size() == 2));
            verify(loteRepository).eliminar(argThat(id -> ID_LOTE.equals(id.getValue())));
        }

        @Test
        void unLoteConExistenciasConContenidoNoSeDaDeBaja() {
            // Arrange
            EliminarLoteUseCase useCase = new EliminarLoteUseCase(loteRepository, existenciaRepository, recalcularStockUseCase);
            hayLoteRegistrado();
            when(existenciaRepository.obtenerDisponiblesPorDetallesLote(anyList()))
                    .thenReturn(List.of(existencia("e7000000-0000-0000-0000-0000000000a9", ID_LINEA_CAFE, "1000", "1", null)));

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LOTE));

            // Assert
            assertEquals(409, error.getCodigo());
            verify(loteRepository, never()).eliminar(any());
        }

        @Test
        void eliminarUnLoteInexistenteRespondeNoEncontrado() {
            // Arrange
            EliminarLoteUseCase useCase = new EliminarLoteUseCase(loteRepository, existenciaRepository, recalcularStockUseCase);
            when(loteRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_LOTE));

            // Assert
            assertEquals(404, error.getCodigo());
            verify(loteRepository, never()).eliminar(any());
        }

        @Test
        void obtenerPorIdDevuelveElLoteOResponde404() {
            // Arrange
            ObtenerLotePorIdUseCase useCase = new ObtenerLotePorIdUseCase(loteRepository);
            hayLoteRegistrado();

            // Act
            LoteResponse encontrado = useCase.execute(ID_LOTE);
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

            // Assert
            assertEquals(ID_LOTE, encontrado.getId());
            assertEquals(2, encontrado.getDetalles().size());
            assertEquals(404, error.getCodigo());
        }

        @Test
        void elListadoConvierteCadaLoteYConservaLaPaginacion() {
            // Arrange
            ObtenerLotesUseCase useCase = new ObtenerLotesUseCase(loteRepository);
            Lote registrado = hayLoteRegistrado();
            when(loteRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(registrado), 0, 10, 1, false));

            // Act
            PaginatedResponse<List<LoteResponse>> pagina = useCase.execute(ObtenerLotesRequest.createFromRequest(Map.of()));

            // Assert
            assertEquals(ID_LOTE, pagina.getData().get(0).getId());
            assertEquals(1, pagina.getTotal());
            assertFalse(pagina.isHasMore());
        }
    }

    // ================================================================== lineas que se pueden abrir

    /** Harina (en gramos) comprada en bolsas de 1 kg: la conversion kg -> g es 1000. */
    @Nested
    class Aperturables {

        private static final String ID_HARINA = "e7000000-0000-0000-0000-0000000000a1";
        private static final String ID_BOLSA = "e7000000-0000-0000-0000-0000000000a2";
        private static final String ID_LINEA_TARDIA = "e7000000-0000-0000-0000-0000000000a3";
        private static final String ID_LINEA_PROXIMA = "e7000000-0000-0000-0000-0000000000a4";
        private final LocalDate hoy = LocalDate.now();

        private ObtenerDetallesAperturablesUseCase useCase;

        @BeforeEach
        void prepararEscenario() {
            useCase = new ObtenerDetallesAperturablesUseCase(productoRepository, loteRepository, conversionRepository);
            hayProducto(productoDirecto(ID_HARINA, "Harina"));
            hayProducto(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA));
            lenient().when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA)));
            lenient().when(conversionRepository.obtenerVivas()).thenReturn(List.of(
                    Conversion.crear(new BigDecimal("1000"), false, new UnidadId(ID_OTRA_UNIDAD), new UnidadId(ID_UNIDAD))));
        }

        private List<DetalleAperturableResponse> consultar(String idProducto) {
            return useCase.execute(ObtenerDetallesAperturablesRequest.createFromRequest(Map.of("idProducto", idProducto)));
        }

        @Test
        void proponeLaLineaQueVenceAntesYSumaLasUnidadesCerradas() {
            // Arrange
            when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(loteDeCompra(ID_LOTE,
                    detalle(ID_LINEA_TARDIA, ID_BOLSA, "5", "2", "50", hoy.plusDays(30)),
                    detalle(ID_LINEA_PROXIMA, ID_BOLSA, "5", "3", "50", hoy.plusDays(10)))));

            // Act
            List<DetalleAperturableResponse> filas = consultar(ID_HARINA);

            // Assert
            assertEquals(1, filas.size());
            DetalleAperturableResponse fila = filas.get(0);
            assertEquals(ID_LINEA_PROXIMA, fila.getIdDetalleLote());
            assertEquals(ID_LOTE, fila.getIdLote());
            assertEquals(ID_BOLSA, fila.getIdProducto());
            assertEquals("Bolsa de harina", fila.getNombreProducto());
            assertEquals(0, new BigDecimal("3").compareTo(fila.getCantidadRestante()));
            assertEquals(0, new BigDecimal("5").compareTo(fila.getUnidadesCerradasTotales()));
            assertEquals(0, new BigDecimal("1000").compareTo(fila.getContenidoPorUnidad()));
            assertEquals(0, new BigDecimal("0.05").compareTo(fila.getCostoPorUnidadDeContenido()));
            assertEquals(ID_UNIDAD, fila.getIdUnidadBase());
            assertEquals(hoy.plusDays(10), fila.getFechaVencimiento());
        }

        @Test
        void noProponeLineasVencidasNiSinUnidadesCerradas() {
            // Arrange
            when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(loteDeCompra(ID_LOTE,
                    detalle(ID_LINEA_TARDIA, ID_BOLSA, "5", "2", "50", hoy.minusDays(1)),
                    detalle(ID_LINEA_PROXIMA, ID_BOLSA, "5", "0", "50", hoy.plusDays(10)))));

            // Act
            List<DetalleAperturableResponse> filas = consultar(ID_HARINA);

            // Assert
            assertTrue(filas.isEmpty());
        }

        @Test
        void sinConversionLaFilaSeDevuelveSinContenidoNiCosto() {
            // Arrange
            when(conversionRepository.obtenerVivas()).thenReturn(List.of());
            when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(loteDeCompra(ID_LOTE,
                    detalle(ID_LINEA_PROXIMA, ID_BOLSA, "5", "3", "50", hoy.plusDays(10)))));

            // Act
            List<DetalleAperturableResponse> filas = consultar(ID_HARINA);

            // Assert
            assertEquals(1, filas.size());
            assertNull(filas.get(0).getContenidoPorUnidad());
            assertNull(filas.get(0).getCostoPorUnidadDeContenido());
        }

        @Test
        void soloSeOfrecenPresentacionesNoElProductoBase() {
            // Arrange: una linea vieja del producto base y otra de su presentacion (logica.md LOTE-14).
            when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(loteDeCompra(ID_LOTE,
                    detalle(ID_LINEA_TARDIA, ID_BOLSA, "5", "2", "50", hoy.plusDays(30)),
                    detalle(ID_LINEA_PROXIMA, ID_HARINA, "5", "3", "9", hoy.plusDays(10)))));

            // Act
            List<DetalleAperturableResponse> filas = consultar(ID_HARINA);

            // Assert
            assertEquals(List.of(ID_LINEA_TARDIA), filas.stream().map(DetalleAperturableResponse::getIdDetalleLote).toList());
        }

        @Test
        void hayQueConsultarPorElProductoBaseNoPorLaPresentacion() {
            // Act
            DomainException error = assertThrows(DomainException.class, () -> consultar(ID_BOLSA));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(error.getMensaje().contains("es una presentacion"));
        }

        @Test
        void unProductoInexistenteRespondeNoEncontrado() {
            // Act
            DomainException error = assertThrows(DomainException.class, () -> consultar(ID_INEXISTENTE));

            // Assert
            assertEquals(404, error.getCodigo());
        }

        @Test
        void laPeticionExigeElProducto() {
            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> ObtenerDetallesAperturablesRequest.createFromRequest(new HashMap<>()));

            // Assert
            assertEquals(400, error.getCodigo());
        }
    }
}
