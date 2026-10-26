package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AHORA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_USUARIO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarReintegroDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.MovimientoCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.AnularVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.RegistrarAnulacionDeVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de la anulacion de una venta (logica.md VENTA-11, VENTA-25, VENTA-27): con y
 * sin reintegro de inventario, y la devolucion del efectivo de una venta de un turno cerrado.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarAnulacionDeVentaUseCaseTest {

    private static final String ID_VENTA = "80000000-0000-0000-0000-000000000001";
    private static final String ID_PAN = "80000000-0000-0000-0000-000000000002";
    private static final String ID_CAJA_DE_LA_VENTA = "82000000-0000-0000-0000-000000000001";
    private static final String ID_CAJA_ABIERTA = "82000000-0000-0000-0000-000000000002";
    private static final String ID_EFECTIVO = "84000000-0000-0000-0000-000000000001";
    private static final String ID_TARJETA = "84000000-0000-0000-0000-000000000002";
    private static final EstadoVenta COMPLETADA = EstadoVenta.restaurar("81000000-0000-0000-0000-000000000001", "Completada", false);
    private static final EstadoVenta ANULADA = EstadoVenta.restaurar("81000000-0000-0000-0000-000000000002", "Anulada", false);

    @Mock private IVentaRepository ventaRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IEstadoVentaRepository estadoVentaRepository;
    @Mock private ISesionCajaRepository sesionCajaRepository;
    @Mock private ITipoPagoRepository tipoPagoRepository;
    @Mock private RegistrarReintegroDeInventarioUseCase registrarReintegroDeInventarioUseCase;

    private RegistrarAnulacionDeVentaUseCase useCase;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new RegistrarAnulacionDeVentaUseCase(ventaRepository, usuarioRepository, estadoVentaRepository,
                sesionCajaRepository, tipoPagoRepository, registrarReintegroDeInventarioUseCase);

        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(estadoVentaRepository.obtenerPorNombre("Completada")).thenReturn(Optional.of(COMPLETADA));
        lenient().when(estadoVentaRepository.obtenerPorNombre("Anulada")).thenReturn(Optional.of(ANULADA));
        lenient().when(ventaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(tipoPagoRepository.obtenerVivos()).thenReturn(List.of(
                TipoPago.restaurar(ID_EFECTIVO, "Efectivo", false), TipoPago.restaurar(ID_TARJETA, "Tarjeta", false)));
        lenient().when(sesionCajaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    private Venta hayVentaEn(EstadoVenta estado, PagoVenta... pagos) {
        Venta venta = Venta.restaurar(ID_VENTA, 7L, AHORA.minusHours(1), new BigDecimal("20.00"), new UsuarioId(ID_USUARIO),
                new ClienteId(Cliente.ID_MOSTRADOR), new SesionCajaId(ID_CAJA_DE_LA_VENTA), estado.getId(),
                null, null, null, null, false,
                new ArrayList<>(List.of(DetalleVenta.restaurar("83000000-0000-0000-0000-000000000001", new ProductoId(ID_PAN),
                        new BigDecimal("10.00"), new BigDecimal("2"), new BigDecimal("20.00"), new BigDecimal("0.00"), true, false))),
                new ArrayList<>(List.of(pagos)));
        lenient().when(ventaRepository.obtenerPorId(any())).thenReturn(Optional.of(venta));
        return venta;
    }

    private static PagoVenta pago(String idTipoPago, String monto) {
        return PagoVenta.restaurar("85000000-0000-0000-0000-00000000000" + idTipoPago.charAt(idTipoPago.length() - 1),
                new TipoPagoId(idTipoPago), new BigDecimal(monto), new BigDecimal(monto), BigDecimal.ZERO, null, false);
    }

    private static SesionCaja caja(String id, String estado, String montoApertura) {
        return SesionCaja.restaurar(id, new UsuarioId(ID_USUARIO), AHORA.minusDays(1), null, new BigDecimal(montoApertura),
                null, null, null, null, estado, false, new ArrayList<>());
    }

    /** La caja de la venta ya se cerro; hay otra abierta con 'fondo' de efectivo y sin ventas. */
    private SesionCaja laCajaDeLaVentaEstaCerradaYHayOtraAbiertaCon(String fondo) {
        when(sesionCajaRepository.obtenerPorId(argThat(id -> ID_CAJA_DE_LA_VENTA.equals(id.getValue()))))
                .thenReturn(Optional.of(caja(ID_CAJA_DE_LA_VENTA, SesionCaja.CERRADA, "50.00")));
        SesionCaja abierta = caja(ID_CAJA_ABIERTA, SesionCaja.ABIERTA, fondo);
        lenient().when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.of(abierta));
        lenient().when(ventaRepository.obtenerPorSesionCaja(any())).thenReturn(List.of());
        return abierta;
    }

    private static RegistrarAnulacionDeVentaRequest anulacion(String motivo, boolean reintegrar) {
        return RegistrarAnulacionDeVentaRequest.crear(ID_VENTA, ID_USUARIO, motivo, reintegrar, null, AHORA);
    }

    @Test
    void anularDejaLaVentaAnuladaConSuMotivoYQuienLaAnulo() {
        // Arrange
        Venta venta = hayVentaEn(COMPLETADA);

        // Act
        VentaResponse respuesta = useCase.execute(anulacion("Cobro duplicado", false));

        // Assert
        assertTrue(venta.estaEn(ANULADA.getId()));
        assertEquals("Anulada", respuesta.getEstadoVenta().getNombre());
        assertEquals("Cobro duplicado", respuesta.getMotivoAnulacion());
        assertEquals(ID_USUARIO, respuesta.getIdUsuarioAnulacion());
        verify(ventaRepository).actualizar(any(), any());
    }

    @Test
    void sinReintegroNoSeTocaElInventario() {
        // Arrange
        hayVentaEn(COMPLETADA);

        // Act
        useCase.execute(anulacion("Cobro duplicado", false));

        // Assert
        verifyNoInteractions(registrarReintegroDeInventarioUseCase);
    }

    @Test
    void conReintegroSeDevuelveAlInventarioLoQueDescontoLaVenta() {
        // Arrange
        hayVentaEn(COMPLETADA);
        ArgumentCaptor<RegistrarReintegroDeInventarioRequest> reintegro = ArgumentCaptor.forClass(RegistrarReintegroDeInventarioRequest.class);

        // Act
        useCase.execute(anulacion("Pedido equivocado", true));

        // Assert
        verify(registrarReintegroDeInventarioUseCase).execute(reintegro.capture());
        assertEquals(ID_VENTA, reintegro.getValue().getIdVenta());
        assertEquals(ID_USUARIO, reintegro.getValue().getIdUsuario());
        assertEquals("Reintegro del ticket 7", reintegro.getValue().getMotivo());
        assertEquals(AHORA, reintegro.getValue().getFecha());
    }

    // ------------------------------------------------------------------ devolucion del efectivo (VENTA-27)

    @Test
    void elEfectivoDeUnaVentaDeLaCajaAbiertaNoRegistraNadaMas() {
        // Arrange
        hayVentaEn(COMPLETADA, pago(ID_EFECTIVO, "20.00"));
        when(sesionCajaRepository.obtenerPorId(any())).thenReturn(Optional.of(caja(ID_CAJA_DE_LA_VENTA, SesionCaja.ABIERTA, "50.00")));

        // Act
        useCase.execute(anulacion("Cobro duplicado", false));

        // Assert
        verify(sesionCajaRepository, never()).obtenerAbiertaParaActualizar();
        verify(sesionCajaRepository, never()).actualizar(any(), any());
    }

    @Test
    void elEfectivoDeUnaVentaDeUnTurnoCerradoSaleDeLaCajaAbiertaComoEgresoConLaVenta() {
        // Arrange: pago mixto, 15 en efectivo y 5 con tarjeta.
        hayVentaEn(COMPLETADA, pago(ID_EFECTIVO, "15.00"), pago(ID_TARJETA, "5.00"));
        SesionCaja abierta = laCajaDeLaVentaEstaCerradaYHayOtraAbiertaCon("100.00");

        // Act
        useCase.execute(anulacion("Reclamo del cliente", false));

        // Assert
        verify(sesionCajaRepository).actualizar(argThat(id -> ID_CAJA_ABIERTA.equals(id.getValue())), any());
        MovimientoCaja egreso = abierta.getMovimientosActivos().get(0);
        assertEquals(MovimientoCaja.EGRESO, egreso.getTipo());
        assertEquals(new BigDecimal("15.00"), egreso.getMonto());
        assertEquals("Devolucion del ticket 7", egreso.getMotivo());
        assertEquals(ID_VENTA, egreso.getVentaId().getValue());
        assertEquals(ID_USUARIO, egreso.getUsuarioId().getValue());
    }

    @Test
    void sinCajaAbiertaNoSeAnulaUnaVentaDeUnTurnoCerradoPagadaEnEfectivo() {
        // Arrange
        hayVentaEn(COMPLETADA, pago(ID_EFECTIVO, "20.00"));
        when(sesionCajaRepository.obtenerPorId(any())).thenReturn(Optional.of(caja(ID_CAJA_DE_LA_VENTA, SesionCaja.CERRADA, "50.00")));
        when(sesionCajaRepository.obtenerAbiertaParaActualizar()).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(anulacion("Reclamo", true)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("abre la caja"));
        verifyNoInteractions(registrarReintegroDeInventarioUseCase);
    }

    @Test
    void laDevolucionNoPuedeDejarLaCajaAbiertaEnNegativo() {
        // Arrange (SESIONCAJA-10): la caja abierta solo tiene 5 de fondo.
        hayVentaEn(COMPLETADA, pago(ID_EFECTIVO, "20.00"));
        laCajaDeLaVentaEstaCerradaYHayOtraAbiertaCon("5.00");

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(anulacion("Reclamo", false)));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(sesionCajaRepository, never()).actualizar(any(), any());
    }

    @Test
    void unaVentaDeUnTurnoCerradoPagadaSoloConTarjetaNoTocaLaCaja() {
        // Arrange
        hayVentaEn(COMPLETADA, pago(ID_TARJETA, "20.00"));

        // Act
        useCase.execute(anulacion("Reclamo", false));

        // Assert
        verifyNoInteractions(sesionCajaRepository);
    }

    @Test
    void laReferenciaDelReembolsoConTarjetaQuedaEnLaVentaSinTocarLaCaja() {
        // Arrange (logica.md VENTA-27)
        hayVentaEn(COMPLETADA, pago(ID_TARJETA, "20.00"));

        // Act
        VentaResponse respuesta = useCase.execute(RegistrarAnulacionDeVentaRequest.crear(
                ID_VENTA, ID_USUARIO, "Reclamo", false, "POS-REV-0042", AHORA));

        // Assert
        assertEquals("POS-REV-0042", respuesta.getReferenciaReembolso());
        verifyNoInteractions(sesionCajaRepository);
    }

    @Test
    void laReferenciaDelReembolsoNoPasaDe255Caracteres() {
        // Arrange
        hayVentaEn(COMPLETADA, pago(ID_TARJETA, "20.00"));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(RegistrarAnulacionDeVentaRequest.crear(
                ID_VENTA, ID_USUARIO, "Reclamo", false, "x".repeat(256), AHORA)));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(ventaRepository, never()).actualizar(any(), any());
    }

    @Test
    void laCajaSeTocaAntesQueElInventario() {
        // Arrange (GENERAL-15)
        hayVentaEn(COMPLETADA, pago(ID_EFECTIVO, "20.00"));
        laCajaDeLaVentaEstaCerradaYHayOtraAbiertaCon("100.00");

        // Act
        useCase.execute(anulacion("Reclamo", true));

        // Assert
        InOrder orden = inOrder(sesionCajaRepository, registrarReintegroDeInventarioUseCase);
        orden.verify(sesionCajaRepository).obtenerAbiertaParaActualizar();
        orden.verify(registrarReintegroDeInventarioUseCase).execute(any());
    }

    @Test
    void unaVentaYaAnuladaNoSeVuelveAAnular() {
        // Arrange
        hayVentaEn(ANULADA);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(anulacion("Otra vez", true)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("esta anulada"));
        verify(ventaRepository, never()).actualizar(any(), any());
        verifyNoInteractions(registrarReintegroDeInventarioUseCase);
    }

    @Test
    void soloSeAnulaUnaVentaCompletada() {
        // Arrange
        hayVentaEn(EstadoVenta.restaurar("81000000-0000-0000-0000-000000000003", "Pendiente", false));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(anulacion("Motivo", false)));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(ventaRepository, never()).actualizar(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void laAnulacionExigeUnMotivo(String motivo) {
        // Arrange
        Venta venta = hayVentaEn(COMPLETADA);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(anulacion(motivo, false)));

        // Assert
        assertEquals(400, error.getCodigo());
        assertTrue(venta.estaEn(COMPLETADA.getId()));
        verify(ventaRepository, never()).actualizar(any(), any());
    }

    @Test
    void unUsuarioInexistenteNoPuedeAnular() {
        // Arrange
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(anulacion("Motivo", false)));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(ventaRepository, never()).obtenerPorId(any());
    }

    @Test
    void unaVentaInexistenteRespondeNoEncontrado() {
        // Arrange
        when(ventaRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(anulacion("Motivo", false)));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID_VENTA));
    }

    @Test
    void sinLosEstadosSembradosNoSeAnula() {
        // Arrange
        hayVentaEn(COMPLETADA);
        when(estadoVentaRepository.obtenerPorNombre("Anulada")).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(anulacion("Motivo", false)));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(ventaRepository, never()).actualizar(any(), any());
    }

    @Test
    void siElReintegroFallaElErrorSePropaga() {
        // Arrange
        hayVentaEn(COMPLETADA);
        doThrow(DomainException.throwException(409, "El reintegro supera el contenido inicial"))
                .when(registrarReintegroDeInventarioUseCase).execute(any());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(anulacion("Motivo", true)));

        // Assert
        assertEquals(409, error.getCodigo());
    }

    // ------------------------------------------------------------------ fachada HTTP

    @Test
    void anularVentaUseCaseTraduceLaPeticionHttpYDelega() {
        // Arrange
        RegistrarAnulacionDeVentaUseCase interno = org.mockito.Mockito.mock(RegistrarAnulacionDeVentaUseCase.class);
        AnularVentaUseCase fachada = new AnularVentaUseCase(interno);
        AnularVentaRequest peticion = AnularVentaRequest.createFromRequest(
                Map.of("motivo", "Cobro duplicado", "reintegrar", true, "referenciaReembolso", "QR-1"), ID_USUARIO);
        ArgumentCaptor<RegistrarAnulacionDeVentaRequest> enviado = ArgumentCaptor.forClass(RegistrarAnulacionDeVentaRequest.class);

        // Act
        fachada.execute(ID_VENTA, peticion);

        // Assert
        verify(interno).execute(enviado.capture());
        assertEquals(ID_VENTA, enviado.getValue().getIdVenta());
        assertEquals(ID_USUARIO, enviado.getValue().getIdUsuario());
        assertEquals("Cobro duplicado", enviado.getValue().getMotivo());
        assertTrue(enviado.getValue().isReintegrar());
        assertEquals("QR-1", enviado.getValue().getReferenciaReembolso());
    }

    @Test
    void laPeticionDeAnulacionNoReintegraPorDefectoYExigeMotivo() {
        // Act
        AnularVentaRequest sinReintegro = AnularVentaRequest.createFromRequest(Map.of("motivo", "Cobro duplicado"), ID_USUARIO);
        DomainException sinMotivo = assertThrows(DomainException.class, () -> AnularVentaRequest.createFromRequest(Map.of(), ID_USUARIO));

        // Assert
        assertFalse(sinReintegro.isReintegrar());
        assertEquals(400, sinMotivo.getCodigo());
    }
}
