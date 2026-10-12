package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AHORA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_USUARIO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarReintegroDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.AnularVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.RegistrarAnulacionDeVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas unitarias de la anulacion de una venta (VEN-40..43), con y sin reintegro de inventario. */
@ExtendWith(MockitoExtension.class)
class RegistrarAnulacionDeVentaUseCaseTest {

    private static final String ID_VENTA = "80000000-0000-0000-0000-000000000001";
    private static final EstadoVenta COMPLETADA = EstadoVenta.restaurar("81000000-0000-0000-0000-000000000001", "Completada", false);
    private static final EstadoVenta ANULADA = EstadoVenta.restaurar("81000000-0000-0000-0000-000000000002", "Anulada", false);

    @Mock private IVentaRepository ventaRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IEstadoVentaRepository estadoVentaRepository;
    @Mock private RegistrarReintegroDeInventarioUseCase registrarReintegroDeInventarioUseCase;

    private RegistrarAnulacionDeVentaUseCase useCase;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new RegistrarAnulacionDeVentaUseCase(ventaRepository, usuarioRepository, estadoVentaRepository,
                registrarReintegroDeInventarioUseCase);

        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(estadoVentaRepository.obtenerPorNombre("Completada")).thenReturn(Optional.of(COMPLETADA));
        lenient().when(estadoVentaRepository.obtenerPorNombre("Anulada")).thenReturn(Optional.of(ANULADA));
        lenient().when(ventaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    private Venta hayVentaEn(EstadoVenta estado) {
        Venta venta = Venta.restaurar(ID_VENTA, 7L, AHORA.minusHours(1), new BigDecimal("20.00"), new UsuarioId(ID_USUARIO),
                new ClienteId(Cliente.ID_MOSTRADOR), new SesionCajaId("82000000-0000-0000-0000-000000000001"), estado.getId(),
                null, null, null, false, new ArrayList<>(), new ArrayList<>());
        lenient().when(ventaRepository.obtenerPorId(any())).thenReturn(Optional.of(venta));
        return venta;
    }

    private static RegistrarAnulacionDeVentaRequest anulacion(String motivo, boolean reintegrar) {
        return RegistrarAnulacionDeVentaRequest.crear(ID_VENTA, ID_USUARIO, motivo, reintegrar, AHORA);
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
        assertEquals("Venta '" + ID_VENTA + "'", reintegro.getValue().getMotivoOrigen());
        assertEquals("Anulacion de la venta '" + ID_VENTA + "'", reintegro.getValue().getMotivoReintegro());
        assertEquals(ID_USUARIO, reintegro.getValue().getIdUsuario());
        assertEquals(AHORA, reintegro.getValue().getFecha());
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
        AnularVentaRequest peticion = AnularVentaRequest.createFromRequest(Map.of("motivo", "Cobro duplicado", "reintegrar", true), ID_USUARIO);
        ArgumentCaptor<RegistrarAnulacionDeVentaRequest> enviado = ArgumentCaptor.forClass(RegistrarAnulacionDeVentaRequest.class);

        // Act
        fachada.execute(ID_VENTA, peticion);

        // Assert
        verify(interno).execute(enviado.capture());
        assertEquals(ID_VENTA, enviado.getValue().getIdVenta());
        assertEquals(ID_USUARIO, enviado.getValue().getIdUsuario());
        assertEquals("Cobro duplicado", enviado.getValue().getMotivo());
        assertTrue(enviado.getValue().isReintegrar());
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
