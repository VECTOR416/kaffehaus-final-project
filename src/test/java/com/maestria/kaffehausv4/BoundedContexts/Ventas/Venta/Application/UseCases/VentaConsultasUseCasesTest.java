package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ActualizarVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ObtenerVentasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.TicketVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;

/**
 * Pruebas unitarias de lo que se puede hacer con una venta ya registrada: reasignar su
 * cliente, consultarla (por id, por ticket o en listado) y comprobar que no se da de baja.
 */
@ExtendWith(MockitoExtension.class)
class VentaConsultasUseCasesTest {

    private static final String ID_VENTA = "0e100000-0000-0000-0000-000000000001";
    private static final String ID_CLIENTE = "0e100000-0000-0000-0000-000000000002";
    private static final String ID_PERSONA_CLIENTE = "0e100000-0000-0000-0000-000000000003";
    private static final String ID_EFECTIVO = "0e100000-0000-0000-0000-000000000004";
    private static final String ID_INEXISTENTE = "0e100000-0000-0000-0000-0000000000ee";
    private static final EstadoVenta COMPLETADA = EstadoVenta.restaurar("0e200000-0000-0000-0000-000000000001", "Completada", false);
    private static final EstadoVenta ANULADA = EstadoVenta.restaurar("0e200000-0000-0000-0000-000000000002", "Anulada", false);

    @Mock private IVentaRepository ventaRepository;
    @Mock private IClienteRepository clienteRepository;
    @Mock private IEstadoVentaRepository estadoVentaRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IPersonaRepository personaRepository;
    @Mock private ITipoPagoRepository tipoPagoRepository;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        lenient().when(estadoVentaRepository.obtenerPorNombre("Anulada")).thenReturn(Optional.of(ANULADA));
        lenient().when(estadoVentaRepository.obtenerPorId(argThat(id -> id != null && COMPLETADA.getId().getValue().equals(id.getValue()))))
                .thenReturn(Optional.of(COMPLETADA));
        lenient().when(estadoVentaRepository.obtenerPorId(argThat(id -> id != null && ANULADA.getId().getValue().equals(id.getValue()))))
                .thenReturn(Optional.of(ANULADA));
        lenient().when(clienteRepository.obtenerPorId(argThat(id -> id != null && ID_CLIENTE.equals(id.getValue()))))
                .thenReturn(Optional.of(Cliente.restaurar(ID_CLIENTE, ID_PERSONA_CLIENTE, false)));
        lenient().when(clienteRepository.obtenerPorId(argThat(id -> id != null && Cliente.ID_MOSTRADOR.equals(id.getValue()))))
                .thenReturn(Optional.of(Cliente.restaurar(Cliente.ID_MOSTRADOR, Cliente.ID_MOSTRADOR, false)));
        lenient().when(ventaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    /** Venta de 2 cafes a 10.00 pagada con 50.00 en efectivo. */
    private Venta hayVenta(EstadoVenta estado, String idCliente) {
        Venta venta = Venta.restaurar(ID_VENTA, 42L, AHORA, new BigDecimal("20.00"), new UsuarioId(ID_USUARIO), new ClienteId(idCliente),
                new SesionCajaId("0e300000-0000-0000-0000-000000000001"), estado.getId(), null, null, null, null, false,
                new ArrayList<>(List.of(DetalleVenta.restaurar("0e400000-0000-0000-0000-000000000001", new ProductoId("0e500000-0000-0000-0000-000000000001"),
                        new BigDecimal("10.00"), new BigDecimal("2"), new BigDecimal("20.00"), new BigDecimal("0.00"), true, false))),
                new ArrayList<>(List.of(PagoVenta.restaurar("0e600000-0000-0000-0000-000000000001", new TipoPagoId(ID_EFECTIVO),
                        new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("30.00"), null, false))));
        lenient().when(ventaRepository.obtenerPorId(argThat(id -> id != null && ID_VENTA.equals(id.getValue())))).thenReturn(Optional.of(venta));
        lenient().when(ventaRepository.obtenerPorNumeroTicket(42L)).thenReturn(Optional.of(venta));
        return venta;
    }

    // ------------------------------------------------------------------ reasignar cliente

    @Test
    void aUnaVentaRegistradaSoloSeLePuedeReasignarElCliente() {
        // Arrange
        ActualizarVentaUseCase useCase = new ActualizarVentaUseCase(ventaRepository, clienteRepository, estadoVentaRepository);
        hayVenta(COMPLETADA, Cliente.ID_MOSTRADOR);

        // Act
        VentaResponse respuesta = useCase.execute(ID_VENTA, ActualizarVentaRequest.createFromRequest(Map.of("idCliente", ID_CLIENTE)));

        // Assert
        assertEquals(ID_CLIENTE, respuesta.getIdCliente());
        assertEquals(new BigDecimal("20.00"), respuesta.getTotal());
        assertEquals("Completada", respuesta.getEstadoVenta().getNombre());
        verify(ventaRepository).actualizar(any(), any());
    }

    @Test
    void unaVentaAnuladaNoAdmiteCambios() {
        // Arrange
        ActualizarVentaUseCase useCase = new ActualizarVentaUseCase(ventaRepository, clienteRepository, estadoVentaRepository);
        hayVenta(ANULADA, Cliente.ID_MOSTRADOR);

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID_VENTA, ActualizarVentaRequest.createFromRequest(Map.of("idCliente", ID_CLIENTE))));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(ventaRepository, never()).actualizar(any(), any());
    }

    @Test
    void reasignarValidaQueExistanLaVentaElClienteYElCatalogo() {
        // Arrange
        ActualizarVentaUseCase useCase = new ActualizarVentaUseCase(ventaRepository, clienteRepository, estadoVentaRepository);
        hayVenta(COMPLETADA, Cliente.ID_MOSTRADOR);
        ActualizarVentaRequest aClienteReal = ActualizarVentaRequest.createFromRequest(Map.of("idCliente", ID_CLIENTE));

        // Act
        DomainException sinVenta = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE, aClienteReal));
        DomainException sinCliente = assertThrows(DomainException.class,
                () -> useCase.execute(ID_VENTA, ActualizarVentaRequest.createFromRequest(Map.of("idCliente", ID_INEXISTENTE))));
        when(estadoVentaRepository.obtenerPorNombre("Anulada")).thenReturn(Optional.empty());
        DomainException sinCatalogo = assertThrows(DomainException.class, () -> useCase.execute(ID_VENTA, aClienteReal));

        // Assert
        assertEquals(404, sinVenta.getCodigo());
        assertEquals(404, sinCliente.getCodigo());
        assertEquals(404, sinCatalogo.getCodigo());
        verify(ventaRepository, never()).actualizar(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"total", "fechaVenta", "detalles", "pagos", "idEstadoVenta"})
    void laPeticionRechazaCualquierOtroCampoDeLaVenta(String campo) {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> ActualizarVentaRequest.createFromRequest(Map.of("idCliente", ID_CLIENTE, campo, "1")));

        // Assert
        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("'" + campo + "'"));
    }

    @Test
    void laPeticionExigeElCliente() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> ActualizarVentaRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    // ------------------------------------------------------------------ baja

    @Test
    void unaVentaNoSeDaDeBajaSeAnula() {
        // Arrange
        EliminarVentaUseCase useCase = new EliminarVentaUseCase(ventaRepository);
        hayVenta(COMPLETADA, Cliente.ID_MOSTRADOR);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_VENTA));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("anulala"));
        verify(ventaRepository, never()).eliminar(any());
    }

    @Test
    void darDeBajaUnaVentaInexistenteRespondeNoEncontrado() {
        // Arrange
        EliminarVentaUseCase useCase = new EliminarVentaUseCase(ventaRepository);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

        // Assert
        assertEquals(404, error.getCodigo());
    }

    // ------------------------------------------------------------------ consultas

    @Test
    void obtenerPorIdDevuelveLaVentaConSuEstadoLineasYPagos() {
        // Arrange
        ObtenerVentaPorIdUseCase useCase = new ObtenerVentaPorIdUseCase(ventaRepository, estadoVentaRepository);
        hayVenta(COMPLETADA, Cliente.ID_MOSTRADOR);

        // Act
        VentaResponse respuesta = useCase.execute(ID_VENTA);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

        // Assert
        assertEquals(42L, respuesta.getNumeroTicket());
        assertEquals("Completada", respuesta.getEstadoVenta().getNombre());
        assertEquals(1, respuesta.getDetalles().size());
        assertEquals(1, respuesta.getPagos().size());
        assertEquals(new BigDecimal("30.00"), respuesta.getCambio());
        assertEquals(404, error.getCodigo());
    }

    @Test
    void elListadoResuelveElEstadoDeCadaVentaYConservaLaPaginacion() {
        // Arrange
        ObtenerVentasUseCase useCase = new ObtenerVentasUseCase(ventaRepository, estadoVentaRepository);
        Venta venta = hayVenta(ANULADA, Cliente.ID_MOSTRADOR);
        when(ventaRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(venta), 20, 10, 21, false));

        // Act
        PaginatedResponse<List<VentaResponse>> pagina = useCase.execute(ObtenerVentasRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Anulada", pagina.getData().get(0).getEstadoVenta().getNombre());
        assertEquals(20, pagina.getSkip());
        assertEquals(21, pagina.getTotal());
        assertFalse(pagina.isHasMore());
    }

    // ------------------------------------------------------------------ ticket

    private ObtenerVentaPorTicketUseCase ticket() {
        return new ObtenerVentaPorTicketUseCase(ventaRepository, estadoVentaRepository, clienteRepository, usuarioRepository,
                personaRepository, tipoPagoRepository);
    }

    @Test
    void elTicketReuneLaVentaConLosNombresDeClienteYCajeroYSusPagos() {
        // Arrange
        hayVenta(COMPLETADA, ID_CLIENTE);
        when(personaRepository.obtenerPorId(argThat(id -> id != null && ID_PERSONA_CLIENTE.equals(id.getValue()))))
                .thenReturn(Optional.of(Persona.restaurar(ID_PERSONA_CLIENTE, "Luis", "Mamani", null, "7654321", false)));
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        when(personaRepository.obtenerPorId(argThat(id -> id != null && ID_PERSONA.equals(id.getValue()))))
                .thenReturn(Optional.of(Persona.restaurar(ID_PERSONA, "Ana", "Rojas", null, "1234567", false)));
        when(tipoPagoRepository.obtenerPorId(any())).thenReturn(Optional.of(TipoPago.restaurar(ID_EFECTIVO, "Efectivo", false)));

        // Act
        TicketVentaResponse respuesta = ticket().execute(" 42 ");

        // Assert
        assertEquals(ID_VENTA, respuesta.getIdVenta());
        assertEquals(42L, respuesta.getNumeroTicket());
        assertEquals("Completada", respuesta.getEstadoVenta());
        assertEquals("Luis Mamani", respuesta.getNombreCliente());
        assertEquals("7654321", respuesta.getCarnetCliente());
        assertEquals(ID_USUARIO, respuesta.getIdCajero());
        assertEquals("Ana Rojas", respuesta.getNombreCajero());
        assertEquals(new BigDecimal("20.00"), respuesta.getTotal());
        assertEquals(new BigDecimal("20.00"), respuesta.getTotalPagado());
        assertEquals(new BigDecimal("30.00"), respuesta.getCambio());
        assertEquals(1, respuesta.getDetalles().size());
        assertEquals(1, respuesta.getPagos().size());
    }

    @Test
    void elTicketSeEmiteAunqueFaltenLosDatosDePersonaOElTipoDePago() {
        // Arrange
        hayVenta(COMPLETADA, ID_CLIENTE);
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        when(tipoPagoRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        TicketVentaResponse respuesta = ticket().execute("42");

        // Assert
        assertNull(respuesta.getNombreCliente());
        assertNull(respuesta.getNombreCajero());
        assertEquals(1, respuesta.getPagos().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-3", "4.5", ""})
    void unNumeroDeTicketQueNoEsUnEnteroPositivoSeRechaza(String numero) {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> ticket().execute(numero));

        // Assert
        assertEquals(400, error.getCodigo());
        verifyNoInteractions(ventaRepository);
    }

    @Test
    void unTicketNuloSeRechaza() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> ticket().execute(null));

        // Assert
        assertEquals(400, error.getCodigo());
    }

    @Test
    void unTicketQueNoCorrespondeANingunaVentaRespondeNoEncontrado() {
        // Arrange
        when(ventaRepository.obtenerPorNumeroTicket(999L)).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> ticket().execute("999"));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains("999"));
    }

    @Test
    void elTicketResponde404SiElClienteDeLaVentaYaNoExiste() {
        // Arrange
        hayVenta(COMPLETADA, ID_INEXISTENTE);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> ticket().execute("42"));

        // Assert
        assertEquals(404, error.getCodigo());
    }
}
