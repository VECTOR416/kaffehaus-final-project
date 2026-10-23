package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.AHORA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_USUARIO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.presentacion;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.productoDirecto;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.SalidaDeInventarioResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarSalidaDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.PagoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias del registro de una venta (ventas.md VEN-10..VEN-23, CAJ-02, CAJ-10..13).
 * El inventario es un caso de uso simulado: aqui solo se comprueba que la venta lo invoca bien
 * y que usa el costo que devuelve.
 *
 * Escenario: cafe y te a 10.00 cada uno, de stock directo y vendibles; una caja abierta.
 */
@ExtendWith(MockitoExtension.class)
class CrearVentaUseCaseTest {

    private static final String ID_CAFE = "10000000-0000-0000-0000-00000000000b";
    private static final String ID_TE = "10000000-0000-0000-0000-00000000000a";
    private static final String ID_VENTA = "20000000-0000-0000-0000-000000000001";
    private static final String ID_CAJA = "30000000-0000-0000-0000-000000000001";
    private static final String ID_CLIENTE = "40000000-0000-0000-0000-000000000001";
    private static final String ID_EFECTIVO = "50000000-0000-0000-0000-000000000001";
    private static final String ID_TARJETA = "50000000-0000-0000-0000-000000000002";
    private static final String ID_PORCENTAJE = "60000000-0000-0000-0000-000000000001";
    private static final String ID_MONTO_FIJO = "60000000-0000-0000-0000-000000000002";

    private static final EstadoVenta COMPLETADA = EstadoVenta.restaurar("70000000-0000-0000-0000-000000000001", "Completada", false);

    @Mock private IVentaRepository ventaRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IClienteRepository clienteRepository;
    @Mock private IEstadoVentaRepository estadoVentaRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private IDescuentoRepository descuentoRepository;
    @Mock private ITipoDescuentoRepository tipoDescuentoRepository;
    @Mock private ISesionCajaRepository sesionCajaRepository;
    @Mock private ITipoPagoRepository tipoPagoRepository;
    @Mock private RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase;

    private CrearVentaUseCase useCase;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        useCase = new CrearVentaUseCase(ventaRepository, usuarioRepository, clienteRepository, estadoVentaRepository,
                productoRepository, descuentoRepository, tipoDescuentoRepository, sesionCajaRepository, tipoPagoRepository,
                registrarSalidaDeInventarioUseCase);

        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(sesionCajaRepository.obtenerAbiertaParaVenta()).thenReturn(Optional.of(SesionCaja.restaurar(
                ID_CAJA, new UsuarioId(ID_USUARIO), AHORA.minusHours(2), null, new BigDecimal("100.00"),
                null, null, null, null, SesionCaja.ABIERTA, false, new ArrayList<>())));
        lenient().when(clienteRepository.obtenerPorId(any())).thenAnswer(llamada -> {
            String id = ((com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId) llamada.getArgument(0)).getValue();
            return Optional.of(Cliente.restaurar(id, id, false));
        });
        lenient().when(estadoVentaRepository.obtenerPorNombre("Completada")).thenReturn(Optional.of(COMPLETADA));
        lenient().when(estadoVentaRepository.obtenerPorId(any())).thenReturn(Optional.of(COMPLETADA));
        hayProducto(productoDirecto(ID_CAFE, "Cafe"));
        hayProducto(productoDirecto(ID_TE, "Te"));
        lenient().when(descuentoRepository.obtenerVigentes(any(), any())).thenReturn(List.of());
        lenient().when(tipoDescuentoRepository.obtenerPorId(argThat(id -> id != null && ID_PORCENTAJE.equals(id.getValue()))))
                .thenReturn(Optional.of(TipoDescuento.restaurar(ID_PORCENTAJE, "Porcentaje", false)));
        lenient().when(tipoDescuentoRepository.obtenerPorId(argThat(id -> id != null && ID_MONTO_FIJO.equals(id.getValue()))))
                .thenReturn(Optional.of(TipoDescuento.restaurar(ID_MONTO_FIJO, "Monto fijo", false)));
        lenient().when(tipoPagoRepository.obtenerPorId(argThat(id -> id != null && ID_EFECTIVO.equals(id.getValue()))))
                .thenReturn(Optional.of(TipoPago.restaurar(ID_EFECTIVO, "Efectivo", false)));
        lenient().when(tipoPagoRepository.obtenerPorId(argThat(id -> id != null && ID_TARJETA.equals(id.getValue()))))
                .thenReturn(Optional.of(TipoPago.restaurar(ID_TARJETA, "Tarjeta", false)));
        lenient().when(ventaRepository.crear(any())).thenAnswer(llamada -> persistida(llamada.getArgument(0)));
        lenient().when(ventaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(registrarSalidaDeInventarioUseCase.execute(any()))
                .thenReturn(SalidaDeInventarioResponse.create(List.of()));
        lenient().when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of());
    }

    private void hayProducto(Producto producto) {
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && producto.getId().getValue().equals(id.getValue()))))
                .thenReturn(Optional.of(producto));
    }

    /** Lo que hace la persistencia al guardar: asignar id y ticket a la venta e id a sus hijas. */
    private static Venta persistida(Venta venta) {
        List<DetalleVenta> detalles = new ArrayList<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            detalles.add(DetalleVenta.restaurar("21000000-0000-0000-0000-00000000000" + detalles.size(), detalle.getProductoId(),
                    detalle.getPrecioUnitario(), detalle.getCantidad(), detalle.getSubtotal(), detalle.getDescuentoAplicado(),
                    detalle.isEstado(), detalle.isBorrado()));
        }
        List<PagoVenta> pagos = new ArrayList<>();
        for (PagoVenta pago : venta.getPagos()) {
            pagos.add(PagoVenta.restaurar("22000000-0000-0000-0000-00000000000" + pagos.size(), pago.getTipoPagoId(), pago.getMonto(),
                    pago.getMontoRecibido(), pago.getCambio(), pago.getReferencia(), pago.isBorrado()));
        }
        return Venta.restaurar(ID_VENTA, 1L, venta.getFechaVenta(), venta.getTotal(), venta.getUsuarioId(), venta.getClienteId(),
                venta.getSesionCajaId(), venta.getEstadoVentaId(), venta.getClaveIdempotencia(), null, null, false, detalles, pagos);
    }

    private static Map<String, Object> linea(String idProducto, String cantidad) {
        return Map.of("idProducto", idProducto, "cantidad", cantidad);
    }

    private static Map<String, Object> pago(String idTipoPago, String monto) {
        return Map.of("idTipoPago", idTipoPago, "monto", monto);
    }

    private static CrearVentaRequest venta(List<Map<String, Object>> lineas, List<Map<String, Object>> pagos) {
        return venta(lineas, pagos, Map.of());
    }

    private static CrearVentaRequest venta(List<Map<String, Object>> lineas, List<Map<String, Object>> pagos, Map<String, Object> extra) {
        Map<String, Object> cuerpo = new HashMap<>(extra);
        cuerpo.put("fechaVenta", AHORA);
        cuerpo.put("detalles", lineas);
        cuerpo.put("pagos", pagos);
        return CrearVentaRequest.createFromRequest(cuerpo, ID_USUARIO);
    }

    // ------------------------------------------------------------------ camino feliz

    @Test
    void registraLaVentaConElPrecioDeListaYLaDejaCompletada() {
        // Arrange
        CrearVentaRequest peticion = venta(List.of(linea(ID_CAFE, "2")), List.of(pago(ID_EFECTIVO, "20")));

        // Act
        VentaResponse respuesta = useCase.execute(peticion);

        // Assert
        assertEquals(ID_VENTA, respuesta.getId());
        assertEquals(new BigDecimal("20.00"), respuesta.getTotal());
        assertEquals("Completada", respuesta.getEstadoVenta().getNombre());
        assertEquals(ID_USUARIO, respuesta.getIdUsuario());
        assertEquals(ID_CAJA, respuesta.getIdSesionCaja());
        assertEquals(1, respuesta.getDetalles().size());
        assertEquals(1, respuesta.getPagos().size());
        verify(ventaRepository).crear(any());
    }

    @Test
    void sinClienteLaVentaVaAlClienteMostrador() {
        // Act
        VentaResponse respuesta = useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10"))));

        // Assert
        assertEquals(Cliente.ID_MOSTRADOR, respuesta.getIdCliente());
    }

    @Test
    void conClienteLaVentaQuedaASuNombre() {
        // Act
        VentaResponse respuesta = useCase.execute(
                venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")), Map.of("idCliente", ID_CLIENTE)));

        // Assert
        assertEquals(ID_CLIENTE, respuesta.getIdCliente());
    }

    @Test
    void descuentaElInventarioDeCadaLineaComoSalidaDeVenta() {
        // Arrange
        ArgumentCaptor<RegistrarSalidaDeInventarioRequest> salida = ArgumentCaptor.forClass(RegistrarSalidaDeInventarioRequest.class);

        // Act
        useCase.execute(venta(List.of(linea(ID_CAFE, "2")), List.of(pago(ID_EFECTIVO, "20"))));

        // Assert
        verify(registrarSalidaDeInventarioUseCase).execute(salida.capture());
        assertEquals(ID_CAFE, salida.getValue().getIdProducto());
        assertEquals(0, new BigDecimal("2").compareTo(salida.getValue().getCantidad()));
        assertFalse(salida.getValue().isExplotarBajoDemanda());
        assertEquals(ID_USUARIO, salida.getValue().getIdUsuario());
        assertEquals(AHORA, salida.getValue().getFecha());
    }

    @Test
    void lasLineasSeDescuentanEnOrdenDeIdDeProducto() {
        // Arrange: el cafe se pide primero, pero el id del te es menor.
        CrearVentaRequest peticion = venta(List.of(linea(ID_CAFE, "1"), linea(ID_TE, "1")), List.of(pago(ID_EFECTIVO, "20")));

        // Act
        useCase.execute(peticion);

        // Assert
        InOrder orden = inOrder(registrarSalidaDeInventarioUseCase);
        orden.verify(registrarSalidaDeInventarioUseCase).execute(argThat(salida -> ID_TE.equals(salida.getIdProducto())));
        orden.verify(registrarSalidaDeInventarioUseCase).execute(argThat(salida -> ID_CAFE.equals(salida.getIdProducto())));
    }

    @Test
    void laVentaSeGuardaAntesDeDescontarElInventarioYNoSeVuelveAGuardar() {
        // Act
        useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10"))));

        // Assert: sin costo que completar, la venta no se actualiza despues (logica.md VENTA-20).
        InOrder orden = inOrder(ventaRepository, registrarSalidaDeInventarioUseCase);
        orden.verify(ventaRepository).crear(any());
        orden.verify(registrarSalidaDeInventarioUseCase).execute(any());
        verify(ventaRepository, never()).actualizar(any(), any());
    }

    // ------------------------------------------------------------------ descuentos

    @Test
    void aplicaSoloElMejorDescuentoVigente() {
        // Arrange: 10% sobre 2 x 10.00 = 2.00; monto fijo de 3.00 por unidad = 6.00. Gana el fijo.
        ProductoId cafe = new ProductoId(ID_CAFE);
        when(descuentoRepository.obtenerVigentes(any(), any())).thenReturn(List.of(
                Descuento.restaurar("61000000-0000-0000-0000-000000000001", new BigDecimal("10"), AHORA.minusDays(1), AHORA.plusDays(1),
                        true, cafe, new TipoDescuentoId(ID_PORCENTAJE), false),
                Descuento.restaurar("61000000-0000-0000-0000-000000000002", new BigDecimal("3"), AHORA.minusDays(1), AHORA.plusDays(1),
                        true, cafe, new TipoDescuentoId(ID_MONTO_FIJO), false)));

        // Act
        VentaResponse respuesta = useCase.execute(venta(List.of(linea(ID_CAFE, "2")), List.of(pago(ID_EFECTIVO, "14"))));

        // Assert
        assertEquals(new BigDecimal("14.00"), respuesta.getTotal());
        assertEquals(new BigDecimal("6.00"), respuesta.getDetalles().get(0).getDescuentoAplicado());
    }

    @Test
    void unDescuentoCuyoTipoNoExisteImpideLaVenta() {
        // Arrange
        when(descuentoRepository.obtenerVigentes(any(), any())).thenReturn(List.of(
                Descuento.restaurar("61000000-0000-0000-0000-000000000001", new BigDecimal("10"), AHORA.minusDays(1), AHORA.plusDays(1),
                        true, new ProductoId(ID_CAFE), new TipoDescuentoId("60000000-0000-0000-0000-0000000000ff"), false)));
        when(tipoDescuentoRepository.obtenerPorId(argThat(id -> id != null && id.getValue().endsWith("ff")))).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")))));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(ventaRepository, never()).crear(any());
    }

    // ------------------------------------------------------------------ pagos

    @Test
    void elEfectivoAdmiteRecibirMasYLaVentaInformaElCambio() {
        // Arrange
        Map<String, Object> conCambio = Map.of("idTipoPago", ID_EFECTIVO, "monto", "20", "montoRecibido", "50");

        // Act
        VentaResponse respuesta = useCase.execute(venta(List.of(linea(ID_CAFE, "2")), List.of(conCambio)));

        // Assert
        assertEquals(new BigDecimal("30.00"), respuesta.getCambio());
    }

    @Test
    void unaVentaPuedePagarseConVariosMedios() {
        // Act
        VentaResponse respuesta = useCase.execute(
                venta(List.of(linea(ID_CAFE, "2")), List.of(pago(ID_EFECTIVO, "5"), pago(ID_TARJETA, "15"))));

        // Assert
        assertEquals(2, respuesta.getPagos().size());
        assertEquals(new BigDecimal("20.00"), respuesta.getTotal());
    }

    @Test
    void siLosPagosNoSumanElTotalNoSeGuardaNiSeDescuentaNada() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "2")), List.of(pago(ID_EFECTIVO, "19.99")))));

        // Assert
        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("no coincide con el total"));
        verify(ventaRepository, never()).crear(any());
        verifyNoInteractions(registrarSalidaDeInventarioUseCase);
    }

    @Test
    void unaVentaSinPagosConTotalMayorQueCeroSeRechaza() {
        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of())));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(ventaRepository, never()).crear(any());
    }

    @Test
    void unTipoDePagoInexistenteImpideLaVenta() {
        // Arrange
        when(tipoPagoRepository.obtenerPorId(argThat(id -> id != null && id.getValue().endsWith("ff")))).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(
                venta(List.of(linea(ID_CAFE, "1")), List.of(pago("50000000-0000-0000-0000-0000000000ff", "10")))));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(ventaRepository, never()).crear(any());
    }

    @Test
    void unaTarjetaNoPuedeRecibirMasQueSuMonto() {
        // Arrange
        Map<String, Object> tarjetaConExceso = Map.of("idTipoPago", ID_TARJETA, "monto", "10", "montoRecibido", "20");

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(tarjetaConExceso))));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(ventaRepository, never()).crear(any());
    }

    // ------------------------------------------------------------------ productos

    @Test
    void unProductoInexistenteImpideLaVenta() {
        // Arrange
        when(productoRepository.obtenerPorId(argThat(id -> id != null && id.getValue().endsWith("ff")))).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(
                venta(List.of(linea("10000000-0000-0000-0000-0000000000ff", "1")), List.of(pago(ID_EFECTIVO, "10")))));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(ventaRepository, never()).crear(any());
    }

    @Test
    void unProductoNoVendibleNoSePuedeVender() {
        // Arrange
        String idInsumo = "10000000-0000-0000-0000-0000000000c1";
        hayProducto(Producto.restaurar(idInsumo, "Leche", BigDecimal.ZERO, new BigDecimal("1.00"), BigDecimal.ZERO, false, true,
                new UnidadId("0a000000-0000-0000-0000-000000000001"), new TipoId("0a000000-0000-0000-0000-000000000003"),
                new CategoriaId("0a000000-0000-0000-0000-000000000004"), null, false));

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(idInsumo, "1")), List.of(pago(ID_EFECTIVO, "1")))));

        // Assert
        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("no es vendible"));
        verify(ventaRepository, never()).crear(any());
    }

    @Test
    void unaPresentacionSeVendePorUnidad() {
        // Arrange: una bolsa de cafe cerrada (logica.md VENTA-21).
        String idBolsa = "10000000-0000-0000-0000-0000000000c2";
        hayProducto(presentacion(idBolsa, "Bolsa de cafe", ID_CAFE));
        ArgumentCaptor<RegistrarSalidaDeInventarioRequest> salida = ArgumentCaptor.forClass(RegistrarSalidaDeInventarioRequest.class);

        // Act
        useCase.execute(venta(List.of(linea(idBolsa, "1")), List.of(pago(ID_EFECTIVO, "10"))));

        // Assert
        verify(registrarSalidaDeInventarioUseCase).execute(salida.capture());
        assertEquals(idBolsa, salida.getValue().getIdProducto());
    }

    @Test
    void unProductoBaseConPresentacionesNoSeVende() {
        // Arrange: el cafe se compra en bolsas; su stock vive en existencias.
        when(productoRepository.obtenerPresentaciones(argThat(id -> id != null && ID_CAFE.equals(id.getValue()))))
                .thenReturn(List.of(presentacion("10000000-0000-0000-0000-0000000000c2", "Bolsa de cafe", ID_CAFE)));

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")))));

        // Assert
        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("tiene presentaciones"));
        verify(ventaRepository, never()).crear(any());
    }

    @Test
    void siElInventarioNoAlcanzaElErrorSePropagaYLaVentaNoSeConfirma() {
        // Arrange
        when(registrarSalidaDeInventarioUseCase.execute(any()))
                .thenThrow(DomainException.throwException(409, "Stock insuficiente de 'Cafe'"));

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")))));

        // Assert: la transaccion del caso de uso revierte el alta; no llega a actualizarse.
        assertEquals(409, error.getCodigo());
        verify(ventaRepository, never()).actualizar(any(), any());
    }

    // ------------------------------------------------------------------ precondiciones

    @Test
    void unaClaveDeIdempotenciaRepetidaDevuelveLaVentaYaRegistrada() {
        // Arrange
        Venta existente = persistida(Venta.crear(AHORA, new UsuarioId(ID_USUARIO),
                new com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId(Cliente.ID_MOSTRADOR),
                new com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId(ID_CAJA),
                COMPLETADA.getId(), "clave-1"));
        when(ventaRepository.obtenerPorClaveIdempotencia("clave-1")).thenReturn(Optional.of(existente));

        // Act
        VentaResponse respuesta = useCase.execute(
                venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")), Map.of("claveIdempotencia", "clave-1")));

        // Assert
        assertEquals(ID_VENTA, respuesta.getId());
        verify(ventaRepository, never()).crear(any());
        verifyNoInteractions(registrarSalidaDeInventarioUseCase, sesionCajaRepository);
    }

    @Test
    void unaClaveDeIdempotenciaNuevaRegistraLaVentaConEsaClave() {
        // Arrange
        when(ventaRepository.obtenerPorClaveIdempotencia("clave-2")).thenReturn(Optional.empty());

        // Act
        VentaResponse respuesta = useCase.execute(
                venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")), Map.of("claveIdempotencia", "clave-2")));

        // Assert
        assertEquals("clave-2", respuesta.getClaveIdempotencia());
        verify(ventaRepository, times(1)).crear(any());
    }

    @Test
    void sinCajaAbiertaNoSeVende() {
        // Arrange
        when(sesionCajaRepository.obtenerAbiertaParaVenta()).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")))));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("No hay una caja abierta"));
        verify(ventaRepository, never()).crear(any());
    }

    @Test
    void unUsuarioInexistenteNoPuedeVender() {
        // Arrange
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")))));

        // Assert
        assertEquals(404, error.getCodigo());
        verifyNoInteractions(sesionCajaRepository);
    }

    @Test
    void unClienteInexistenteImpideLaVenta() {
        // Arrange
        doReturn(Optional.empty()).when(clienteRepository).obtenerPorId(any());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(
                venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")), Map.of("idCliente", ID_CLIENTE))));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID_CLIENTE));
    }

    @Test
    void sinElClienteMostradorSembradoElErrorIndicaInicializarElSistema() {
        // Arrange
        doReturn(Optional.empty()).when(clienteRepository).obtenerPorId(any());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")))));

        // Assert
        assertEquals(404, error.getCodigo());
        assertTrue(error.getMensaje().contains("api/v1/seed"));
    }

    @Test
    void sinElEstadoCompletadaSembradoNoSeVende() {
        // Arrange
        when(estadoVentaRepository.obtenerPorNombre("Completada")).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(venta(List.of(linea(ID_CAFE, "1")), List.of(pago(ID_EFECTIVO, "10")))));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(ventaRepository, never()).crear(any());
    }

    // ------------------------------------------------------------------ peticion

    @Test
    void laPeticionRechazaLosCamposQueAsignaElSistema() {
        // Arrange
        Map<String, Object> conTotal = Map.of("total", 99, "detalles", List.of(linea(ID_CAFE, "1")));
        Map<String, Object> conPrecio = Map.of("detalles", List.of(Map.of("idProducto", ID_CAFE, "cantidad", "1", "precioUnitario", 1)));

        // Act
        DomainException porTotal = assertThrows(DomainException.class, () -> CrearVentaRequest.createFromRequest(conTotal, ID_USUARIO));
        DomainException porPrecio = assertThrows(DomainException.class, () -> CrearVentaRequest.createFromRequest(conPrecio, ID_USUARIO));

        // Assert
        assertEquals(400, porTotal.getCodigo());
        assertEquals(400, porPrecio.getCodigo());
    }

    @Test
    void laPeticionExigeAlMenosUnaLineaYElUsuarioDeLaSesion() {
        // Act
        DomainException sinLineas = assertThrows(DomainException.class,
                () -> CrearVentaRequest.createFromRequest(Map.of("detalles", List.of()), ID_USUARIO));
        DomainException sinUsuario = assertThrows(DomainException.class,
                () -> CrearVentaRequest.createFromRequest(Map.of("detalles", List.of(linea(ID_CAFE, "1"))), null));

        // Assert
        assertEquals(400, sinLineas.getCodigo());
        assertEquals(400, sinUsuario.getCodigo());
    }
}
