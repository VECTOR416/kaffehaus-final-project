package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ActualizarPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.CrearPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ObtenerPedidosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.PedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.RegistrarAnulacionDeVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.RegistrarAnulacionDeVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;

/**
 * Pruebas unitarias de los casos de uso de Pedido: un pedido nace 'Recibido' de una venta
 * completada y avanza por una maquina de estados; cancelarlo anula su venta. La anulacion es
 * un caso de uso simulado.
 */
@ExtendWith(MockitoExtension.class)
class PedidoUseCasesTest {

    private static final String ID_PEDIDO = "0d000000-0000-0000-0000-000000000001";
    private static final String ID_VENTA = "0d000000-0000-0000-0000-000000000002";
    private static final String ID_INEXISTENTE = "0d000000-0000-0000-0000-0000000000ee";
    private static final LocalDateTime ENTREGA = LocalDateTime.of(2026, 10, 12, 18, 30);

    private static final EstadoPedido RECIBIDO = EstadoPedido.restaurar("0d100000-0000-0000-0000-000000000001", "Recibido", false);
    private static final EstadoPedido EN_PREPARACION = EstadoPedido.restaurar("0d100000-0000-0000-0000-000000000002", "En preparacion", false);
    private static final EstadoPedido EN_CAMINO = EstadoPedido.restaurar("0d100000-0000-0000-0000-000000000003", "En camino", false);
    private static final EstadoPedido ENTREGADO = EstadoPedido.restaurar("0d100000-0000-0000-0000-000000000004", "Entregado", false);
    private static final EstadoPedido CANCELADO = EstadoPedido.restaurar("0d100000-0000-0000-0000-000000000005", "Cancelado", false);
    private static final EstadoVenta COMPLETADA = EstadoVenta.restaurar("0d200000-0000-0000-0000-000000000001", "Completada", false);
    private static final EstadoVenta ANULADA = EstadoVenta.restaurar("0d200000-0000-0000-0000-000000000002", "Anulada", false);

    @Mock private IPedidoRepository pedidoRepository;
    @Mock private IVentaRepository ventaRepository;
    @Mock private IEstadoPedidoRepository estadoPedidoRepository;
    @Mock private IEstadoVentaRepository estadoVentaRepository;
    @Mock private RegistrarAnulacionDeVentaUseCase registrarAnulacionDeVentaUseCase;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        for (EstadoPedido estado : List.of(RECIBIDO, EN_PREPARACION, EN_CAMINO, ENTREGADO, CANCELADO)) {
            lenient().when(estadoPedidoRepository.obtenerPorId(argThat(id -> id != null && estado.getId().getValue().equals(id.getValue()))))
                    .thenReturn(Optional.of(estado));
        }
        lenient().when(estadoPedidoRepository.obtenerPorNombre("Recibido")).thenReturn(Optional.of(RECIBIDO));
        lenient().when(estadoVentaRepository.obtenerPorNombre("Completada")).thenReturn(Optional.of(COMPLETADA));
        lenient().when(pedidoRepository.obtenerPorVentaId(any())).thenReturn(Optional.empty());
        lenient().when(pedidoRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(pedidoRepository.crear(any())).thenAnswer(llamada -> {
            Pedido nuevo = llamada.getArgument(0);
            return Pedido.restaurar(ID_PEDIDO, nuevo.getFechaEntrega(), nuevo.getLatitud(), nuevo.getLongitud(), nuevo.getDescripcion(),
                    nuevo.getVentaId(), nuevo.getEstadoId(), false);
        });
    }

    private void hayVentaEn(EstadoVenta estado) {
        lenient().when(ventaRepository.obtenerPorId(any())).thenReturn(Optional.of(Venta.restaurar(ID_VENTA, 3L, AHORA, new BigDecimal("20.00"),
                new UsuarioId(ID_USUARIO), new ClienteId(Cliente.ID_MOSTRADOR), new SesionCajaId("0d300000-0000-0000-0000-000000000001"),
                estado.getId(), null, null, null, false, new ArrayList<>(), new ArrayList<>())));
    }

    private Pedido hayPedidoEn(EstadoPedido estado) {
        Pedido pedido = Pedido.restaurar(ID_PEDIDO, ENTREGA, new BigDecimal("-17.78"), new BigDecimal("-63.18"), "Sin azucar",
                new VentaId(ID_VENTA), new EstadoPedidoId(estado.getId().getValue()), false);
        lenient().when(pedidoRepository.obtenerPorId(argThat(id -> id != null && ID_PEDIDO.equals(id.getValue())))).thenReturn(Optional.of(pedido));
        return pedido;
    }

    private static Map<String, Object> alta() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("fechaEntrega", "2026-10-12T18:30:00");
        datos.put("latitud", "-17.78");
        datos.put("longitud", "-63.18");
        datos.put("descripcion", "Sin azucar");
        datos.put("idVenta", ID_VENTA);
        return datos;
    }

    private CrearPedidoUseCase crear() {
        return new CrearPedidoUseCase(pedidoRepository, ventaRepository, estadoPedidoRepository, estadoVentaRepository);
    }

    private ActualizarPedidoUseCase actualizar() {
        return new ActualizarPedidoUseCase(pedidoRepository, estadoPedidoRepository, registrarAnulacionDeVentaUseCase);
    }

    private static ActualizarPedidoRequest pasarA(EstadoPedido estado) {
        return ActualizarPedidoRequest.createFromRequest(Map.of("idEstado", estado.getId().getValue()), ID_USUARIO);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void unPedidoNaceRecibidoYLigadoASuVenta() {
        // Arrange
        hayVentaEn(COMPLETADA);

        // Act
        PedidoResponse respuesta = crear().execute(CrearPedidoRequest.createFromRequest(alta()));

        // Assert
        assertEquals(ID_PEDIDO, respuesta.getId());
        assertEquals("Recibido", respuesta.getEstado().getNombre());
        assertEquals(ID_VENTA, respuesta.getIdVenta());
        assertEquals(ENTREGA, respuesta.getFechaEntrega());
        assertEquals("Sin azucar", respuesta.getDescripcion());
    }

    @Test
    void soloSeRegistraUnPedidoDeUnaVentaCompletada() {
        // Arrange
        hayVentaEn(ANULADA);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> crear().execute(CrearPedidoRequest.createFromRequest(alta())));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(pedidoRepository, never()).crear(any());
    }

    @Test
    void unaVentaNoPuedeTenerDosPedidos() {
        // Arrange
        hayVentaEn(COMPLETADA);
        Pedido previo = hayPedidoEn(RECIBIDO);
        when(pedidoRepository.obtenerPorVentaId(any())).thenReturn(Optional.of(previo));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> crear().execute(CrearPedidoRequest.createFromRequest(alta())));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("ya tiene un pedido"));
        verify(pedidoRepository, never()).crear(any());
    }

    @Test
    void crearValidaLaVentaYLosCatalogos() {
        // Arrange
        CrearPedidoRequest peticion = CrearPedidoRequest.createFromRequest(alta());
        hayVentaEn(COMPLETADA);

        // Act
        when(estadoPedidoRepository.obtenerPorNombre("Recibido")).thenReturn(Optional.empty());
        DomainException sinEstadoPedido = assertThrows(DomainException.class, () -> crear().execute(peticion));
        when(estadoVentaRepository.obtenerPorNombre("Completada")).thenReturn(Optional.empty());
        DomainException sinEstadoVenta = assertThrows(DomainException.class, () -> crear().execute(peticion));
        when(ventaRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        DomainException sinVenta = assertThrows(DomainException.class, () -> crear().execute(peticion));

        // Assert
        assertEquals(404, sinEstadoPedido.getCodigo());
        assertEquals(404, sinEstadoVenta.getCodigo());
        assertEquals(404, sinVenta.getCodigo());
        verify(pedidoRepository, never()).crear(any());
    }

    @Test
    void laPeticionDeAltaNoAdmiteEstadoYExigeEntregaCoordenadasYVenta() {
        // Arrange
        Map<String, Object> conEstado = alta();
        conEstado.put("idEstado", RECIBIDO.getId().getValue());
        Map<String, Object> sinEntrega = alta();
        sinEntrega.remove("fechaEntrega");
        Map<String, Object> sinVenta = alta();
        sinVenta.remove("idVenta");

        // Act
        DomainException porEstado = assertThrows(DomainException.class, () -> CrearPedidoRequest.createFromRequest(conEstado));
        DomainException porEntrega = assertThrows(DomainException.class, () -> CrearPedidoRequest.createFromRequest(sinEntrega));
        DomainException porVenta = assertThrows(DomainException.class, () -> CrearPedidoRequest.createFromRequest(sinVenta));

        // Assert
        assertEquals(400, porEstado.getCodigo());
        assertEquals(400, porEntrega.getCodigo());
        assertEquals(400, porVenta.getCodigo());
    }

    // ------------------------------------------------------------------ maquina de estados

    @ParameterizedTest
    @CsvSource({"Recibido,En preparacion", "En preparacion,En camino", "En camino,Entregado"})
    void elPedidoAvanzaPorSusEstadosEnOrden(String desde, String hasta) {
        // Arrange
        hayPedidoEn(estado(desde));

        // Act
        PedidoResponse respuesta = actualizar().execute(ID_PEDIDO, pasarA(estado(hasta)));

        // Assert
        assertEquals(hasta, respuesta.getEstado().getNombre());
        verify(pedidoRepository).actualizar(any(), any());
        verifyNoInteractions(registrarAnulacionDeVentaUseCase);
    }

    @ParameterizedTest
    @CsvSource({"Recibido,En camino", "Recibido,Entregado", "En preparacion,Recibido", "En preparacion,Entregado", "En camino,Recibido", "Recibido,Recibido"})
    void noSePuedeSaltarNiRetrocederDeEstado(String desde, String hasta) {
        // Arrange
        hayPedidoEn(estado(desde));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> actualizar().execute(ID_PEDIDO, pasarA(estado(hasta))));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(pedidoRepository, never()).actualizar(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Entregado", "Cancelado"})
    void unPedidoEnEstadoFinalNoAdmiteNingunCambio(String estadoFinal) {
        // Arrange
        hayPedidoEn(estado(estadoFinal));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> actualizar().execute(ID_PEDIDO,
                ActualizarPedidoRequest.createFromRequest(Map.of("descripcion", "Otra nota"), ID_USUARIO)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("estado final"));
        verify(pedidoRepository, never()).actualizar(any(), any());
    }

    @Test
    void cancelarUnPedidoRecibidoAnulaSuVentaConReintegroDeInventario() {
        // Arrange
        hayPedidoEn(RECIBIDO);
        ArgumentCaptor<RegistrarAnulacionDeVentaRequest> anulacion = ArgumentCaptor.forClass(RegistrarAnulacionDeVentaRequest.class);

        // Act
        PedidoResponse respuesta = actualizar().execute(ID_PEDIDO, pasarA(CANCELADO));

        // Assert
        assertEquals("Cancelado", respuesta.getEstado().getNombre());
        verify(registrarAnulacionDeVentaUseCase).execute(anulacion.capture());
        assertEquals(ID_VENTA, anulacion.getValue().getIdVenta());
        assertEquals(ID_USUARIO, anulacion.getValue().getIdUsuario());
        assertTrue(anulacion.getValue().isReintegrar());
        assertTrue(anulacion.getValue().getMotivo().contains(ID_PEDIDO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"En preparacion", "En camino"})
    void cancelarUnPedidoYaEnMarchaAnulaLaVentaSinReintegrar(String desde) {
        // Arrange: lo preparado ya se consumio, no vuelve al inventario.
        hayPedidoEn(estado(desde));
        ArgumentCaptor<RegistrarAnulacionDeVentaRequest> anulacion = ArgumentCaptor.forClass(RegistrarAnulacionDeVentaRequest.class);

        // Act
        actualizar().execute(ID_PEDIDO, pasarA(CANCELADO));

        // Assert
        verify(registrarAnulacionDeVentaUseCase).execute(anulacion.capture());
        assertFalse(anulacion.getValue().isReintegrar());
    }

    @Test
    void siLaAnulacionDeLaVentaFallaElPedidoNoSeCancela() {
        // Arrange
        hayPedidoEn(RECIBIDO);
        when(registrarAnulacionDeVentaUseCase.execute(any())).thenThrow(DomainException.throwException(409, "La venta esta anulada"));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> actualizar().execute(ID_PEDIDO, pasarA(CANCELADO)));

        // Assert
        assertEquals(409, error.getCodigo());
        verify(pedidoRepository, never()).actualizar(any(), any());
    }

    @Test
    void sePuedenCorregirLosDatosDeEntregaSinCambiarDeEstado() {
        // Arrange
        hayPedidoEn(EN_PREPARACION);

        // Act
        PedidoResponse respuesta = actualizar().execute(ID_PEDIDO,
                ActualizarPedidoRequest.createFromRequest(Map.of("descripcion", "Tocar el timbre", "latitud", "-17.80"), ID_USUARIO));

        // Assert
        assertEquals("Tocar el timbre", respuesta.getDescripcion());
        assertEquals(0, new BigDecimal("-17.80").compareTo(respuesta.getLatitud()));
        assertEquals("En preparacion", respuesta.getEstado().getNombre());
        assertEquals(ID_VENTA, respuesta.getIdVenta());
    }

    @Test
    void actualizarValidaQueExistanElPedidoYElEstadoDestino() {
        // Arrange
        hayPedidoEn(RECIBIDO);

        // Act
        DomainException sinPedido = assertThrows(DomainException.class, () -> actualizar().execute(ID_INEXISTENTE, pasarA(EN_PREPARACION)));
        DomainException sinEstado = assertThrows(DomainException.class, () -> actualizar().execute(ID_PEDIDO,
                ActualizarPedidoRequest.createFromRequest(Map.of("idEstado", ID_INEXISTENTE), ID_USUARIO)));

        // Assert
        assertEquals(404, sinPedido.getCodigo());
        assertEquals(404, sinEstado.getCodigo());
        verify(pedidoRepository, never()).actualizar(any(), any());
    }

    @Test
    void laPeticionDeCorreccionNoAdmiteCambiarLaVentaYExigeElUsuarioDeLaSesion() {
        // Act
        DomainException porVenta = assertThrows(DomainException.class,
                () -> ActualizarPedidoRequest.createFromRequest(Map.of("idVenta", ID_VENTA), ID_USUARIO));
        DomainException porUsuario = assertThrows(DomainException.class,
                () -> ActualizarPedidoRequest.createFromRequest(Map.of("descripcion", "x"), null));

        // Assert
        assertEquals(400, porVenta.getCodigo());
        assertEquals(400, porUsuario.getCodigo());
    }

    private static EstadoPedido estado(String nombre) {
        return List.of(RECIBIDO, EN_PREPARACION, EN_CAMINO, ENTREGADO, CANCELADO).stream()
                .filter(estado -> estado.getNombre().equals(nombre)).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ eliminar y consultas

    @Test
    void eliminarDaDeBajaElExistenteYRechazaElInexistente() {
        // Arrange
        EliminarPedidoUseCase useCase = new EliminarPedidoUseCase(pedidoRepository);
        hayPedidoEn(RECIBIDO);

        // Act
        useCase.execute(ID_PEDIDO);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

        // Assert
        verify(pedidoRepository, times(1)).eliminar(argThat(id -> ID_PEDIDO.equals(id.getValue())));
        assertEquals(404, error.getCodigo());
    }

    @Test
    void obtenerPorIdDevuelveElPedidoConSuEstado() {
        // Arrange
        ObtenerPedidoPorIdUseCase useCase = new ObtenerPedidoPorIdUseCase(pedidoRepository, estadoPedidoRepository);
        hayPedidoEn(EN_CAMINO);

        // Act
        PedidoResponse respuesta = useCase.execute(ID_PEDIDO);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

        // Assert
        assertEquals("En camino", respuesta.getEstado().getNombre());
        assertEquals(404, error.getCodigo());
    }

    @Test
    void elListadoResuelveElEstadoDeCadaPedidoYConservaLaPaginacion() {
        // Arrange
        ObtenerPedidosUseCase useCase = new ObtenerPedidosUseCase(pedidoRepository, estadoPedidoRepository);
        Pedido pedido = hayPedidoEn(RECIBIDO);
        when(pedidoRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(pedido), 0, 10, 1, false));

        // Act
        PaginatedResponse<List<PedidoResponse>> pagina = useCase.execute(ObtenerPedidosRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Recibido", pagina.getData().get(0).getEstado().getNombre());
        assertEquals(1, pagina.getTotal());
    }
}
