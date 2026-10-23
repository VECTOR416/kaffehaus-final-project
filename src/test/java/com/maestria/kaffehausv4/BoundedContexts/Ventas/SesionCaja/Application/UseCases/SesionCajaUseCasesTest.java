package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AHORA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_PERSONA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_ROL;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_USUARIO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.AbrirSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ArqueoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.CerrarSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.EstadoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ObtenerSesionesCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RecaudacionPorTipoPagoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RegistrarMovimientoCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.SesionCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.MovimientoCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los casos de uso de la caja (arquitectura.md 4.11, CAJ-20..CAJ-24):
 * apertura, movimientos manuales, cierre con arqueo y consultas. Repositorios simulados.
 */
@ExtendWith(MockitoExtension.class)
class SesionCajaUseCasesTest {

    private static final String ID_CAJA = "90000000-0000-0000-0000-000000000001";
    private static final String ID_OTRO_USUARIO = "90000000-0000-0000-0000-0000000000aa";
    private static final String ID_EFECTIVO = "91000000-0000-0000-0000-000000000001";
    private static final String ID_TARJETA = "91000000-0000-0000-0000-000000000002";
    private static final String ID_QR = "91000000-0000-0000-0000-000000000003";
    private static final EstadoVenta COMPLETADA = EstadoVenta.restaurar("92000000-0000-0000-0000-000000000001", "Completada", false);
    private static final EstadoVenta ANULADA = EstadoVenta.restaurar("92000000-0000-0000-0000-000000000002", "Anulada", false);

    @Mock private ISesionCajaRepository sesionCajaRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IRolRepository rolRepository;
    @Mock private IVentaRepository ventaRepository;
    @Mock private IEstadoVentaRepository estadoVentaRepository;
    @Mock private ITipoPagoRepository tipoPagoRepository;
    @Mock private IPersonaRepository personaRepository;

    @BeforeEach
    void prepararPersistencia() {
        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(sesionCajaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(sesionCajaRepository.crear(any())).thenAnswer(llamada -> {
            SesionCaja nueva = llamada.getArgument(0);
            return SesionCaja.restaurar(ID_CAJA, nueva.getUsuarioId(), nueva.getFechaApertura(), null, nueva.getMontoApertura(),
                    null, null, null, null, nueva.getEstado(), false, new ArrayList<>());
        });
    }

    /** Caja abierta por el usuario de prueba con 100.00 de fondo. */
    private static SesionCaja cajaAbierta(MovimientoCaja... movimientos) {
        return SesionCaja.restaurar(ID_CAJA, new UsuarioId(ID_USUARIO), AHORA.minusHours(4), null, new BigDecimal("100.00"),
                null, null, null, null, SesionCaja.ABIERTA, false, new ArrayList<>(List.of(movimientos)));
    }

    private Venta venta(String id, EstadoVenta estado, String total, PagoVenta... pagos) {
        return Venta.restaurar(id, 1L, AHORA.minusHours(1), new BigDecimal(total), new UsuarioId(ID_USUARIO),
                new ClienteId(Cliente.ID_MOSTRADOR), new SesionCajaId(ID_CAJA), estado.getId(), null, null, null, false,
                new ArrayList<>(), new ArrayList<>(List.of(pagos)));
    }

    private PagoVenta pago(String idTipo, String monto, String recibido) {
        BigDecimal importe = new BigDecimal(monto);
        BigDecimal cobrado = new BigDecimal(recibido);
        return PagoVenta.restaurar("94000000-0000-0000-0000-000000000001", new TipoPagoId(idTipo), importe, cobrado,
                cobrado.subtract(importe), null, false);
    }

    private static MovimientoCaja movimiento(String tipo, String monto) {
        return MovimientoCaja.restaurar("93000000-0000-0000-0000-000000000001", new UsuarioId(ID_USUARIO), tipo,
                new BigDecimal(monto), "Motivo", AHORA.minusHours(1), null, false);
    }

    // ================================================================== apertura

    @Nested
    class Apertura {

        private AbrirSesionCajaUseCase useCase;

        @BeforeEach
        void crearCasoDeUso() {
            useCase = new AbrirSesionCajaUseCase(sesionCajaRepository, usuarioRepository);
        }

        private AbrirSesionCajaRequest apertura(Object monto) {
            return AbrirSesionCajaRequest.createFromRequest(Map.of("montoApertura", monto), ID_USUARIO);
        }

        @Test
        void abreLaCajaANombreDeQuienFirmaConSuFondoInicial() {
            // Arrange
            when(sesionCajaRepository.obtenerAbierta()).thenReturn(Optional.empty());

            // Act
            SesionCajaResponse respuesta = useCase.execute(apertura("150.5"));

            // Assert
            assertEquals(ID_CAJA, respuesta.getId());
            assertEquals(ID_USUARIO, respuesta.getIdUsuario());
            assertEquals(new BigDecimal("150.50"), respuesta.getMontoApertura());
            assertEquals(SesionCaja.ABIERTA, respuesta.getEstado());
            assertNotNull(respuesta.getFechaApertura());
            assertNull(respuesta.getFechaCierre());
        }

        @Test
        void seAdmiteAbrirSinFondo() {
            // Arrange
            when(sesionCajaRepository.obtenerAbierta()).thenReturn(Optional.empty());

            // Act
            SesionCajaResponse respuesta = useCase.execute(apertura(0));

            // Assert
            assertEquals(new BigDecimal("0.00"), respuesta.getMontoApertura());
        }

        @Test
        void noSeAbreUnaSegundaCajaMientrasHayaUnaAbierta() {
            // Arrange
            when(sesionCajaRepository.obtenerAbierta()).thenReturn(Optional.of(cajaAbierta()));

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura("50")));

            // Assert
            assertEquals(409, error.getCodigo());
            assertTrue(error.getMensaje().startsWith("Ya hay una caja abierta"));
            verify(sesionCajaRepository, never()).crear(any());
        }

        @Test
        void unFondoNegativoSeRechaza() {
            // Arrange
            when(sesionCajaRepository.obtenerAbierta()).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura("-1")));

            // Assert
            assertEquals(400, error.getCodigo());
            verify(sesionCajaRepository, never()).crear(any());
        }

        @Test
        void unUsuarioInexistenteNoPuedeAbrirCaja() {
            // Arrange
            when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(apertura("50")));

            // Assert
            assertEquals(404, error.getCodigo());
            verify(sesionCajaRepository, never()).obtenerAbierta();
        }

        @Test
        void laPeticionExigeElMontoDeApertura() {
            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> AbrirSesionCajaRequest.createFromRequest(Map.of(), ID_USUARIO));

            // Assert
            assertEquals(400, error.getCodigo());
        }
    }

    // ================================================================== movimientos manuales

    @Nested
    class MovimientosManuales {

        private RegistrarMovimientoCajaUseCase useCase;

        @BeforeEach
        void crearCasoDeUso() {
            useCase = new RegistrarMovimientoCajaUseCase(sesionCajaRepository, usuarioRepository, ventaRepository,
                    estadoVentaRepository, tipoPagoRepository);

            lenient().when(estadoVentaRepository.obtenerPorNombre("Completada")).thenReturn(Optional.of(COMPLETADA));
            lenient().when(tipoPagoRepository.obtenerVivos()).thenReturn(List.of(
                    TipoPago.restaurar(ID_EFECTIVO, "Efectivo", false),
                    TipoPago.restaurar(ID_TARJETA, "Tarjeta", false)));
            lenient().when(ventaRepository.obtenerPorSesionCaja(any())).thenReturn(List.of());
        }

        private RegistrarMovimientoCajaRequest peticion(String tipo, String monto) {
            return RegistrarMovimientoCajaRequest.createFromRequest(Map.of("tipo", tipo, "monto", monto, "motivo", "Compra de hielo"), ID_USUARIO);
        }

        @Test
        void unEgresoQuedaRegistradoEnLaCajaAbierta() {
            // Arrange
            SesionCaja caja = cajaAbierta();
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(caja));

            // Act
            SesionCajaResponse respuesta = useCase.execute(peticion("egreso", "12.5"));

            // Assert
            assertEquals(1, caja.getMovimientosActivos().size());
            assertEquals(new BigDecimal("12.50"), respuesta.getTotalEgresos());
            assertEquals(new BigDecimal("0.00"), respuesta.getTotalIngresos());
            assertEquals("Compra de hielo", respuesta.getMovimientos().get(0).getMotivo());
            verify(sesionCajaRepository).actualizar(any(), any());
        }

        @Test
        void unIngresoSumaALosIngresosDelTurno() {
            // Arrange
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(cajaAbierta(movimiento(MovimientoCaja.INGRESO, "20"))));

            // Act
            SesionCajaResponse respuesta = useCase.execute(peticion("INGRESO", "30"));

            // Assert
            assertEquals(new BigDecimal("50.00"), respuesta.getTotalIngresos());
        }

        @Test
        void unEgresoQueDejariaLaCajaEnNegativoSeRechaza() {
            // Arrange: 100 de fondo y ninguna venta; un egreso de 100.01 no cabe.
            SesionCaja caja = cajaAbierta();
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(caja));

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(peticion("EGRESO", "100.01")));

            // Assert
            assertEquals(409, error.getCodigo());
            assertTrue(caja.getMovimientosActivos().isEmpty());
            verify(sesionCajaRepository, never()).actualizar(any(), any());
        }

        @Test
        void elEgresoPuedeUsarElEfectivoDeLasVentasCompletadasPeroNoElDeTarjeta() {
            // Arrange: 100 de fondo + 30 en efectivo (la tarjeta y la venta anulada no cuentan) = 130.
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(cajaAbierta()));
            when(ventaRepository.obtenerPorSesionCaja(any())).thenReturn(List.of(
                    venta("95000000-0000-0000-0000-000000000001", COMPLETADA, "80",
                            pago(ID_EFECTIVO, "30", "50"), pago(ID_TARJETA, "50", "50")),
                    venta("95000000-0000-0000-0000-000000000002", ANULADA, "40", pago(ID_EFECTIVO, "40", "40"))));

            // Act
            SesionCajaResponse respuesta = useCase.execute(peticion("EGRESO", "130"));
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(peticion("EGRESO", "0.01")));

            // Assert
            assertEquals(new BigDecimal("130.00"), respuesta.getTotalEgresos());
            assertEquals(409, error.getCodigo());
        }

        @Test
        void sinCajaAbiertaNoSeRegistranMovimientos() {
            // Arrange
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(peticion("EGRESO", "5")));

            // Assert
            assertEquals(409, error.getCodigo());
            verify(sesionCajaRepository, never()).actualizar(any(), any());
        }

        @Test
        void unMovimientoInvalidoNoSeGuarda() {
            // Arrange
            SesionCaja caja = cajaAbierta();
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(caja));

            // Act
            DomainException porTipo = assertThrows(DomainException.class, () -> useCase.execute(peticion("RETIRO", "5")));
            DomainException porMonto = assertThrows(DomainException.class, () -> useCase.execute(peticion("EGRESO", "0")));

            // Assert
            assertEquals(400, porTipo.getCodigo());
            assertEquals(400, porMonto.getCodigo());
            assertTrue(caja.getMovimientosActivos().isEmpty());
            verify(sesionCajaRepository, never()).actualizar(any(), any());
        }

        @Test
        void unUsuarioInexistenteNoPuedeRegistrarMovimientos() {
            // Arrange
            when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(peticion("EGRESO", "5")));

            // Assert
            assertEquals(404, error.getCodigo());
            verify(sesionCajaRepository, never()).obtenerAbiertaParaActualizar();
        }
    }

    // ================================================================== cierre y arqueo

    @Nested
    class Cierre {

        private CerrarSesionCajaUseCase useCase;

        @BeforeEach
        void crearCasoDeUso() {
            useCase = new CerrarSesionCajaUseCase(sesionCajaRepository, usuarioRepository, rolRepository, ventaRepository,
                    estadoVentaRepository, tipoPagoRepository);

            lenient().when(rolRepository.obtenerPorId(any())).thenReturn(Optional.of(Rol.restaurar(ID_ROL, "Operador", false)));
            lenient().when(estadoVentaRepository.obtenerPorNombre("Completada")).thenReturn(Optional.of(COMPLETADA));
            lenient().when(tipoPagoRepository.obtenerVivos()).thenReturn(List.of(
                    TipoPago.restaurar(ID_EFECTIVO, "Efectivo", false),
                    TipoPago.restaurar(ID_TARJETA, "Tarjeta", false),
                    TipoPago.restaurar(ID_QR, "QR", false)));
            lenient().when(ventaRepository.obtenerPorSesionCaja(any())).thenReturn(List.of());
        }

        private CerrarSesionCajaRequest cierre(String declarado) {
            return CerrarSesionCajaRequest.createFromRequest(Map.of("montoCierreDeclarado", declarado, "motivoDiferencia", "Arqueo"), ID_USUARIO);
        }

        @Test
        void elEsperadoEsAperturaMasEfectivoMasIngresosMenosEgresos() {
            // Arrange: 100 de fondo + 30 en efectivo + 20 de ingreso - 5 de egreso = 145.
            SesionCaja caja = cajaAbierta(movimiento(MovimientoCaja.INGRESO, "20"), movimiento(MovimientoCaja.EGRESO, "5"));
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(caja));
            when(ventaRepository.obtenerPorSesionCaja(any())).thenReturn(List.of(
                    venta("95000000-0000-0000-0000-000000000001", COMPLETADA, "30.00", pago(ID_EFECTIVO, "30.00", "50.00")),
                    venta("95000000-0000-0000-0000-000000000002", COMPLETADA, "40.00", pago(ID_TARJETA, "40.00", "40.00"))));

            // Act
            ArqueoCajaResponse arqueo = useCase.execute(cierre("140"));

            // Assert
            assertEquals(new BigDecimal("30.00"), arqueo.getVentasEnEfectivo());
            assertEquals(new BigDecimal("145.00"), arqueo.getMontoCierreEsperado());
            assertEquals(new BigDecimal("140.00"), arqueo.getMontoCierreDeclarado());
            assertEquals(new BigDecimal("-5.00"), arqueo.getDiferencia());
            assertEquals(new BigDecimal("70.00"), arqueo.getTotalVendido());
            assertEquals(2, arqueo.getCantidadVentas());
            assertEquals(0, arqueo.getCantidadVentasAnuladas());
            assertEquals(SesionCaja.CERRADA, arqueo.getSesion().getEstado());
            assertNotNull(arqueo.getSesion().getFechaCierre());
            assertFalse(caja.estaAbierta());
            verify(sesionCajaRepository).actualizar(any(), any());
        }

        @Test
        void elEfectivoCuentaPorLoCobradoNoPorLoRecibido() {
            // Arrange: se cobraron 30 y el cliente entrego 50; el cambio ya salio de la caja.
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(cajaAbierta()));
            when(ventaRepository.obtenerPorSesionCaja(any())).thenReturn(List.of(
                    venta("95000000-0000-0000-0000-000000000001", COMPLETADA, "30.00", pago(ID_EFECTIVO, "30.00", "50.00"))));

            // Act
            ArqueoCajaResponse arqueo = useCase.execute(cierre("130"));

            // Assert
            assertEquals(new BigDecimal("130.00"), arqueo.getMontoCierreEsperado());
            assertEquals(new BigDecimal("0.00"), arqueo.getDiferencia());
        }

        @Test
        void lasVentasAnuladasNoCuentanEnElArqueoPeroSeInforman() {
            // Arrange
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(cajaAbierta()));
            when(ventaRepository.obtenerPorSesionCaja(any())).thenReturn(List.of(
                    venta("95000000-0000-0000-0000-000000000001", COMPLETADA, "10.00", pago(ID_EFECTIVO, "10.00", "10.00")),
                    venta("95000000-0000-0000-0000-000000000002", ANULADA, "99.00", pago(ID_EFECTIVO, "99.00", "99.00"))));

            // Act
            ArqueoCajaResponse arqueo = useCase.execute(cierre("110"));

            // Assert
            assertEquals(new BigDecimal("10.00"), arqueo.getVentasEnEfectivo());
            assertEquals(new BigDecimal("10.00"), arqueo.getTotalVendido());
            assertEquals(1, arqueo.getCantidadVentas());
            assertEquals(1, arqueo.getCantidadVentasAnuladas());
        }

        @Test
        void elDesgloseMuestraTodosLosTiposVivosAunqueNoSeUsaran() {
            // Arrange
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(cajaAbierta()));
            when(ventaRepository.obtenerPorSesionCaja(any())).thenReturn(List.of(
                    venta("95000000-0000-0000-0000-000000000001", COMPLETADA, "25.00",
                            pago(ID_EFECTIVO, "10.00", "10.00"), pago(ID_TARJETA, "15.00", "15.00")),
                    venta("95000000-0000-0000-0000-000000000002", COMPLETADA, "5.00", pago(ID_EFECTIVO, "5.00", "5.00"))));

            // Act
            List<RecaudacionPorTipoPagoResponse> desglose = useCase.execute(cierre("115")).getRecaudacionPorTipoPago();

            // Assert
            assertEquals(List.of("Efectivo", "Tarjeta", "QR"), desglose.stream().map(RecaudacionPorTipoPagoResponse::getNombre).toList());
            assertEquals(new BigDecimal("15.00"), desglose.get(0).getMonto());
            assertEquals(2, desglose.get(0).getCantidadPagos());
            assertEquals(new BigDecimal("15.00"), desglose.get(1).getMonto());
            assertEquals(0, desglose.get(2).getMonto().signum());
            assertEquals(0, desglose.get(2).getCantidadPagos());
        }

        @Test
        void unPagoConUnTipoDadoDeBajaSeAgregaAlDesgloseSinContarComoEfectivo() {
            // Arrange
            String idDadoDeBaja = "91000000-0000-0000-0000-0000000000ff";
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(cajaAbierta()));
            when(ventaRepository.obtenerPorSesionCaja(any())).thenReturn(List.of(
                    venta("95000000-0000-0000-0000-000000000001", COMPLETADA, "8.00", pago(idDadoDeBaja, "8.00", "8.00"))));

            // Act
            ArqueoCajaResponse arqueo = useCase.execute(cierre("100"));

            // Assert
            RecaudacionPorTipoPagoResponse ultimo = arqueo.getRecaudacionPorTipoPago().get(3);
            assertEquals("Tipo de pago dado de baja", ultimo.getNombre());
            assertEquals(new BigDecimal("8.00"), ultimo.getMonto());
            assertEquals(0, arqueo.getVentasEnEfectivo().signum());
        }

        @Test
        void quienNoEsTitularNiAdministradorNoPuedeCerrar() {
            // Arrange: la caja la abrio otro usuario y quien cierra es Operador.
            SesionCaja ajena = SesionCaja.restaurar(ID_CAJA, new UsuarioId(ID_OTRO_USUARIO), AHORA.minusHours(4), null,
                    new BigDecimal("100.00"), null, null, null, null, SesionCaja.ABIERTA, false, new ArrayList<>());
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(ajena));

            // Act
            AutorizacionException error = assertThrows(AutorizacionException.class, () -> useCase.execute(cierre("100")));

            // Assert
            assertEquals(403, error.getCodigo());
            assertTrue(ajena.estaAbierta());
            verify(sesionCajaRepository, never()).actualizar(any(), any());
        }

        @Test
        void unAdministradorPuedeCerrarLaCajaDeOtro() {
            // Arrange
            SesionCaja ajena = SesionCaja.restaurar(ID_CAJA, new UsuarioId(ID_OTRO_USUARIO), AHORA.minusHours(4), null,
                    new BigDecimal("100.00"), null, null, null, null, SesionCaja.ABIERTA, false, new ArrayList<>());
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(ajena));
            when(rolRepository.obtenerPorId(any())).thenReturn(Optional.of(Rol.restaurar(ID_ROL, "administrador", false)));

            // Act
            ArqueoCajaResponse arqueo = useCase.execute(cierre("100"));

            // Assert
            assertEquals(SesionCaja.CERRADA, arqueo.getSesion().getEstado());
        }

        @Test
        void siElRolDelUsuarioNoExisteSeLeTrataComoNoAdministrador() {
            // Arrange
            SesionCaja ajena = SesionCaja.restaurar(ID_CAJA, new UsuarioId(ID_OTRO_USUARIO), AHORA.minusHours(4), null,
                    new BigDecimal("100.00"), null, null, null, null, SesionCaja.ABIERTA, false, new ArrayList<>());
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(ajena));
            when(rolRepository.obtenerPorId(any())).thenReturn(Optional.empty());

            // Act
            AutorizacionException error = assertThrows(AutorizacionException.class, () -> useCase.execute(cierre("100")));

            // Assert
            assertEquals(403, error.getCodigo());
        }

        @Test
        void sinCajaAbiertaNoHayNadaQueCerrar() {
            // Arrange
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(cierre("100")));

            // Assert
            assertEquals(409, error.getCodigo());
        }

        @Test
        void unMontoDeclaradoNegativoSeRechazaYLaCajaSigueAbierta() {
            // Arrange
            SesionCaja caja = cajaAbierta();
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(caja));

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(cierre("-1")));

            // Assert
            assertEquals(400, error.getCodigo());
            assertTrue(caja.estaAbierta());
            verify(sesionCajaRepository, never()).actualizar(any(), any());
        }

        @Test
        void sinElEstadoCompletadaSembradoNoSeCierra() {
            // Arrange
            when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(cajaAbierta()));
            when(estadoVentaRepository.obtenerPorNombre("Completada")).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(cierre("100")));

            // Assert
            assertEquals(404, error.getCodigo());
            verify(sesionCajaRepository, never()).actualizar(any(), any());
        }

        @Test
        void unUsuarioInexistenteNoPuedeCerrar() {
            // Arrange
            when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(cierre("100")));

            // Assert
            assertEquals(404, error.getCodigo());
        }
    }

    // ================================================================== consultas

    @Nested
    class Consultas {

        @Test
        void elEstadoDeCajaInformaLaSesionAbiertaYSuResponsable() {
            // Arrange
            ObtenerEstadoCajaUseCase useCase = new ObtenerEstadoCajaUseCase(sesionCajaRepository, usuarioRepository, personaRepository);
            when(sesionCajaRepository.obtenerAbierta()).thenReturn(Optional.of(cajaAbierta()));
            when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(Persona.restaurar(ID_PERSONA, "Ana", "Rojas", null, "123", false)));

            // Act
            EstadoCajaResponse estado = useCase.execute();

            // Assert
            assertTrue(estado.isAbierta());
            assertEquals(ID_CAJA, estado.getIdSesionCaja());
            assertEquals(ID_USUARIO, estado.getIdResponsable());
            assertEquals("Ana Rojas", estado.getNombreResponsable());
            assertEquals(new BigDecimal("100.00"), estado.getMontoApertura());
        }

        @Test
        void siNoSePuedeResolverElResponsableElNombreQuedaVacio() {
            // Arrange
            ObtenerEstadoCajaUseCase useCase = new ObtenerEstadoCajaUseCase(sesionCajaRepository, usuarioRepository, personaRepository);
            when(sesionCajaRepository.obtenerAbierta()).thenReturn(Optional.of(cajaAbierta()));
            when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

            // Act
            EstadoCajaResponse estado = useCase.execute();

            // Assert
            assertTrue(estado.isAbierta());
            assertNull(estado.getNombreResponsable());
        }

        @Test
        void sinCajaAbiertaElEstadoLoIndicaSinFallar() {
            // Arrange
            ObtenerEstadoCajaUseCase useCase = new ObtenerEstadoCajaUseCase(sesionCajaRepository, usuarioRepository, personaRepository);
            when(sesionCajaRepository.obtenerAbierta()).thenReturn(Optional.empty());

            // Act
            EstadoCajaResponse estado = useCase.execute();

            // Assert
            assertFalse(estado.isAbierta());
            assertNull(estado.getIdSesionCaja());
        }

        @Test
        void obtenerPorIdDevuelveLaSesionOResponde404() {
            // Arrange
            ObtenerSesionCajaPorIdUseCase useCase = new ObtenerSesionCajaPorIdUseCase(sesionCajaRepository);
            when(sesionCajaRepository.obtenerPorId(any())).thenReturn(Optional.of(cajaAbierta())).thenReturn(Optional.empty());

            // Act
            SesionCajaResponse encontrada = useCase.execute(ID_CAJA);
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_CAJA));

            // Assert
            assertEquals(ID_CAJA, encontrada.getId());
            assertEquals(404, error.getCodigo());
        }

        @Test
        void elListadoConservaLaPaginacionDelRepositorio() {
            // Arrange
            ObtenerSesionesCajaUseCase useCase = new ObtenerSesionesCajaUseCase(sesionCajaRepository);
            when(sesionCajaRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(cajaAbierta()), 5, 1, 9, true));

            // Act
            PaginatedResponse<List<SesionCajaResponse>> pagina = useCase.execute(ObtenerSesionesCajaRequest.createFromRequest(Map.of()));

            // Assert
            assertEquals(1, pagina.getData().size());
            assertEquals(ID_CAJA, pagina.getData().get(0).getId());
            assertEquals(5, pagina.getSkip());
            assertEquals(1, pagina.getLimit());
            assertEquals(9, pagina.getTotal());
            assertTrue(pagina.isHasMore());
        }
    }
}
