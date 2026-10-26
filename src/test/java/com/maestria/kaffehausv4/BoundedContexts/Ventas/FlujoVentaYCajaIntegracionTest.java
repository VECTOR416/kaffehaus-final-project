package com.maestria.kaffehausv4.BoundedContexts.Ventas;

import static com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria.linea;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.AbrirDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases.AbrirDetalleLoteUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarSalidaDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.CrearRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.CrearRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ActualizarPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.CrearPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases.ActualizarPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases.CrearPedidoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Dtos.CrearUsuarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases.CrearUsuarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.UseCases.EliminarUsuarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ArqueoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.CerrarSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RegistrarMovimientoCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.CerrarSesionCajaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases.RegistrarMovimientoCajaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.MovimientoCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.AnularVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.TicketVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.AnularVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.CrearVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.ObtenerVentaPorTicketUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/**
 * Flujo de venta y caja contra la base (logica.md seccion 2): abrir caja, vender, anular con
 * reintegro, registrar egresos y cerrar con arqueo.
 *
 * Escenario: 10 gaseosas compradas a 2.00 y vendidas a 5.00, y un cafe con leche bajo demanda a
 * 8.00 cuya receta lleva 200 ml de leche; la leche se compra en bolsas de 1 l (2 bolsas). La
 * caja abre con 100.00.
 */
class FlujoVentaYCajaIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private EscenarioDeCafeteria escenario;
    @Autowired private CrearVentaUseCase crearVentaUseCase;
    @Autowired private AnularVentaUseCase anularVentaUseCase;
    @Autowired private ObtenerVentaPorTicketUseCase obtenerVentaPorTicketUseCase;
    @Autowired private RegistrarMovimientoCajaUseCase registrarMovimientoCajaUseCase;
    @Autowired private CerrarSesionCajaUseCase cerrarSesionCajaUseCase;
    @Autowired private CrearPedidoUseCase crearPedidoUseCase;
    @Autowired private ActualizarPedidoUseCase actualizarPedidoUseCase;
    @Autowired private IEstadoPedidoRepository estadoPedidoRepository;
    @Autowired private CrearRecetaUseCase crearRecetaUseCase;
    @Autowired private AbrirDetalleLoteUseCase abrirDetalleLoteUseCase;
    @Autowired private IMovimientoRepository movimientoRepository;
    @Autowired private ITipoMovimientoRepository tipoMovimientoRepository;
    @Autowired private ISesionCajaRepository sesionCajaRepository;
    @Autowired private CrearUsuarioUseCase crearUsuarioUseCase;
    @Autowired private EliminarUsuarioUseCase eliminarUsuarioUseCase;
    @Autowired private IRolRepository rolRepository;

    private String usuario;
    private String gaseosa;
    private String leche;
    private String cafeConLeche;
    private String idDetalleBolsasDeLeche;
    private String efectivo;

    @BeforeEach
    void prepararEscenario() {
        usuario = escenario.sembrar();
        String proveedor = escenario.proveedor();
        gaseosa = escenario.producto("Gaseosa", "5", true, true, "Unidad", null);
        leche = escenario.producto("Leche", "0", false, true, "Mililitro", null);
        String bolsaDeLeche = escenario.producto("Leche 1 l", "9", false, true, "Litro", leche);
        cafeConLeche = escenario.producto("Cafe con leche", "8", true, false, "Unidad", null);
        recetaDeCafeConLeche();
        escenario.comprar(proveedor, linea(gaseosa, "10", "2", LocalDate.now().plusDays(90).toString()));
        idDetalleBolsasDeLeche = escenario.comprar(proveedor, linea(bolsaDeLeche, "2", "9", LocalDate.now().plusDays(10).toString()))
                .getDetalles().get(0).getId();
        efectivo = escenario.idTipoPago("Efectivo");
        escenario.abrirCaja("100");
    }

    private void recetaDeCafeConLeche() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("cantidad", "1");
        datos.put("titulo", "Cafe con leche");
        datos.put("idProductoObtenido", cafeConLeche);
        datos.put("ingredientes", List.of(Map.of("idProducto", leche, "cantidad", "200")));
        datos.put("pasos", List.of(Map.of("descripcion", "Calentar la leche"), Map.of("descripcion", "Servir")));
        crearRecetaUseCase.execute(CrearRecetaRequest.createFromRequest(datos));
    }

    private void abrirBolsaDeLeche() {
        abrirDetalleLoteUseCase.execute(idDetalleBolsasDeLeche, AbrirDetalleLoteRequest.createFromRequest(Map.of(), usuario));
    }

    private List<Movimiento> movimientosDeLaVenta(String idVenta, String tipo) {
        String idTipo = tipoMovimientoRepository.obtenerPorNombre(tipo).orElseThrow().getId().getValue();
        return movimientoRepository.obtenerPorVenta(new VentaId(idVenta)).stream()
                .filter(movimiento -> movimiento.getTipoMovimientoId().getValue().equals(idTipo))
                .toList();
    }

    @SafeVarargs
    private VentaResponse vender(String recibido, Map<String, Object>... lineas) {
        return vender(recibido, null, lineas);
    }

    @SafeVarargs
    private VentaResponse vender(String recibido, String claveIdempotencia, Map<String, Object>... lineas) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> linea : lineas) {
            BigDecimal precio = linea.get("idProducto").equals(gaseosa) ? new BigDecimal("5") : new BigDecimal("8");
            total = total.add(precio.multiply(new BigDecimal(linea.get("cantidad").toString())));
        }
        Map<String, Object> datos = new HashMap<>();
        datos.put("detalles", List.of(lineas));
        datos.put("pagos", List.of(Map.of("idTipoPago", efectivo, "monto", total.toPlainString(), "montoRecibido", recibido)));
        if (claveIdempotencia != null) {
            datos.put("claveIdempotencia", claveIdempotencia);
        }
        return crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(datos, usuario));
    }

    private static Map<String, Object> de(String idProducto, String cantidad) {
        return Map.of("idProducto", idProducto, "cantidad", cantidad);
    }

    @Test
    void unaVentaDescuentaLasUnidadesYLosInsumosDelMtoYRecibeSuTicket() {
        // Arrange
        abrirBolsaDeLeche();

        // Act
        VentaResponse venta = vender("30", de(gaseosa, "2"), de(cafeConLeche, "1"));

        // Assert: 2 gaseosas a 5 + 1 cafe a 8 = 18; el cafe consume 200 ml de leche (VENTA-24).
        assertEquals(0, new BigDecimal("18").compareTo(venta.getTotal()));
        assertEquals(0, new BigDecimal("12").compareTo(venta.getCambio()));
        assertEquals("Completada", venta.getEstadoVenta().getNombre());
        assertNotNull(venta.getNumeroTicket());
        assertEquals(0, new BigDecimal("8").compareTo(escenario.stock(gaseosa)));
        assertEquals(0, new BigDecimal("800").compareTo(escenario.stock(leche)));

        // Un movimiento 'Venta' por la linea de gaseosas y otro por la existencia de leche.
        List<Movimiento> salidas = movimientosDeLaVenta(venta.getId(), TipoMovimiento.VENTA);
        assertEquals(2, salidas.size());
        assertEquals(1, salidas.stream().filter(movimiento -> movimiento.getDetalleLoteId() != null).count());
        assertEquals(1, salidas.stream().filter(movimiento -> movimiento.getExistenciaId() != null).count());
    }

    @Test
    void sinLecheAbiertaLaVentaPideAbrirUnaBolsaYAlReintentarSeRegistraUnaSolaVez() {
        // Act 1: nada abierto, pero hay 2 bolsas cerradas (VENTA-30).
        DomainException error = assertThrows(DomainException.class, () -> vender("8", "pos-1", de(cafeConLeche, "1")));

        // Assert 1
        assertEquals(409, error.getCodigo());
        assertEquals(RegistrarSalidaDeInventarioUseCase.APERTURA_REQUERIDA, error.getError());
        @SuppressWarnings("unchecked")
        Map<String, Object> faltante = ((List<Map<String, Object>>) error.getDetalle().get("faltantes")).get(0);
        assertEquals(leche, faltante.get("idProducto"));
        assertEquals(1, faltante.get("bolsasNecesarias"));
        @SuppressWarnings("unchecked")
        Map<String, Object> bolsa = (Map<String, Object>) faltante.get("bolsaSugerida");
        assertEquals(idDetalleBolsasDeLeche, bolsa.get("idDetalleLote"));
        assertEquals(0, new BigDecimal("1000").compareTo((BigDecimal) bolsa.get("contenido")));

        // Act 2: el cliente abre la bolsa sugerida y reintenta. Aqui toda la prueba es una sola
        // transaccion y el intento rechazado no se revierte, asi que el reintento no reusa la clave:
        // que la clave quede libre lo garantiza la reversion real de la venta (GENERAL-05).
        abrirDetalleLoteUseCase.execute((String) bolsa.get("idDetalleLote"), AbrirDetalleLoteRequest.createFromRequest(Map.of(), usuario));
        VentaResponse venta = vender("8", "pos-2", de(cafeConLeche, "1"));

        // Assert 2
        assertEquals("Completada", venta.getEstadoVenta().getNombre());
        assertEquals(0, new BigDecimal("800").compareTo(escenario.stock(leche)));
    }

    @Test
    void losTicketsSonCorrelativosYSeBuscanPorSuNumero() {
        // Act
        VentaResponse primera = vender("5", de(gaseosa, "1"));
        VentaResponse segunda = vender("5", de(gaseosa, "1"));
        TicketVentaResponse ticket = obtenerVentaPorTicketUseCase.execute(String.valueOf(segunda.getNumeroTicket()));

        // Assert
        assertEquals(primera.getNumeroTicket() + 1, segunda.getNumeroTicket());
        assertEquals(segunda.getId(), ticket.getIdVenta());
        assertNotNull(ticket.getNombreCajero());
    }

    @Test
    void sinUnidadesSuficientesLaVentaSeRechaza() {
        // Act
        DomainException error = assertThrows(DomainException.class, () -> vender("55", de(gaseosa, "11")));

        // Assert
        assertEquals(409, error.getCodigo());
        assertEquals(RegistrarSalidaDeInventarioUseCase.STOCK_INSUFICIENTE, error.getError());
        assertTrue(error.getMensaje().startsWith("Stock insuficiente de 'Gaseosa'"));
    }

    @Test
    void anularConReintegroDevuelveLaLecheALaMismaExistencia() {
        // Arrange
        abrirBolsaDeLeche();
        VentaResponse venta = vender("16", de(cafeConLeche, "2"));

        // Act
        anularVentaUseCase.execute(venta.getId(),
                AnularVentaRequest.createFromRequest(Map.of("motivo", "Pedido equivocado", "reintegrar", true), usuario));

        // Assert (VENTA-25)
        assertEquals(0, new BigDecimal("1000").compareTo(escenario.stock(leche)));
        Movimiento salida = movimientosDeLaVenta(venta.getId(), TipoMovimiento.VENTA).get(0);
        Movimiento reintegro = movimientosDeLaVenta(venta.getId(), TipoMovimiento.REINTEGRO).get(0);
        assertEquals(salida.getExistenciaId().getValue(), reintegro.getExistenciaId().getValue());
        assertEquals(0, salida.getCantidad().compareTo(reintegro.getCantidad()));
    }

    @Test
    void anularUnaVentaDeUnTurnoCerradoDevuelveElEfectivoDesdeLaCajaAbierta() {
        // Arrange: se vende en un turno que se cierra, y se abre otro.
        VentaResponse venta = vender("10", de(gaseosa, "2"));
        cerrarSesionCajaUseCase.execute(CerrarSesionCajaRequest.createFromRequest(Map.of("montoCierreDeclarado", "110"), usuario));
        escenario.abrirCaja("50");

        // Act
        anularVentaUseCase.execute(venta.getId(), AnularVentaRequest.createFromRequest(Map.of("motivo", "Reclamo"), usuario));

        // Assert (VENTA-27, SESIONCAJA-13)
        List<MovimientoCaja> movimientos = sesionCajaRepository.obtenerAbierta().orElseThrow().getMovimientosActivos();
        assertEquals(1, movimientos.size());
        assertFalse(movimientos.get(0).esIngreso());
        assertEquals(0, new BigDecimal("10").compareTo(movimientos.get(0).getMonto()));
        assertEquals(venta.getId(), movimientos.get(0).getVentaId().getValue());
        assertEquals("Devolucion del ticket " + venta.getNumeroTicket(), movimientos.get(0).getMotivo());
    }

    @Test
    void anularConReintegroDevuelveLasUnidades() {
        // Arrange
        VentaResponse venta = vender("10", de(gaseosa, "2"));

        // Act
        VentaResponse anulada = anularVentaUseCase.execute(venta.getId(),
                AnularVentaRequest.createFromRequest(Map.of("motivo", "Cobro duplicado", "reintegrar", true), usuario));

        // Assert
        assertEquals("Anulada", anulada.getEstadoVenta().getNombre());
        assertEquals(0, new BigDecimal("10").compareTo(escenario.stock(gaseosa)));
    }

    @Test
    void elArqueoCuentaElEfectivoDeLasVentasCompletadasYLosMovimientosDeCaja() {
        // Arrange: una venta de 10 completada, otra de 5 anulada y un egreso de 30.
        vender("10", de(gaseosa, "2"));
        VentaResponse anulada = vender("5", de(gaseosa, "1"));
        anularVentaUseCase.execute(anulada.getId(), AnularVentaRequest.createFromRequest(Map.of("motivo", "Error"), usuario));
        registrarMovimientoCajaUseCase.execute(RegistrarMovimientoCajaRequest.createFromRequest(
                Map.of("tipo", "EGRESO", "monto", "30", "motivo", "Hielo"), usuario));

        // Act
        ArqueoCajaResponse arqueo = cerrarSesionCajaUseCase.execute(CerrarSesionCajaRequest.createFromRequest(
                Map.of("montoCierreDeclarado", "80"), usuario));

        // Assert: 100 de fondo + 10 en efectivo - 30 de egreso = 80; la venta anulada no cuenta.
        assertEquals(0, new BigDecimal("80").compareTo(arqueo.getMontoCierreEsperado()));
        assertEquals(0, arqueo.getDiferencia().signum());
        assertEquals(1, arqueo.getCantidadVentasAnuladas());
        assertEquals("CERRADA", arqueo.getSesion().getEstado());
    }

    @Test
    void unEgresoQueDejariaLaCajaEnNegativoSeRechaza() {
        // Arrange: 100 de fondo + 10 vendidos en efectivo = 110.
        vender("10", de(gaseosa, "2"));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> registrarMovimientoCajaUseCase.execute(
                RegistrarMovimientoCajaRequest.createFromRequest(Map.of("tipo", "EGRESO", "monto", "110.01", "motivo", "Retiro"), usuario)));

        // Assert
        assertEquals(409, error.getCodigo());
    }

    @Test
    void cancelarUnPedidoRecibidoAnulaSuVentaYDevuelveLasUnidades() {
        // Arrange
        VentaResponse venta = vender("15", de(gaseosa, "3"));
        Map<String, Object> pedido = new HashMap<>();
        pedido.put("fechaEntrega", LocalDateTime.now().plusHours(2).toString());
        pedido.put("latitud", "-17.78");
        pedido.put("longitud", "-63.18");
        pedido.put("idVenta", venta.getId());
        String idPedido = crearPedidoUseCase.execute(CrearPedidoRequest.createFromRequest(pedido)).getId();
        String cancelado = estadoPedidoRepository.obtenerPorNombre("Cancelado").orElseThrow().getId().getValue();

        // Act
        actualizarPedidoUseCase.execute(idPedido, ActualizarPedidoRequest.createFromRequest(Map.of("idEstado", cancelado, "motivo", "El cliente no estaba"), usuario));

        // Assert
        assertEquals(0, new BigDecimal("10").compareTo(escenario.stock(gaseosa)));
    }

    /** Un cajero con rol Operador, que no es el titular de la caja (la abrio el administrador). */
    private String operador() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombres", "Luis");
        datos.put("apellidos", "Paz");
        datos.put("carnet", "OP-1");
        datos.put("correo", "luis@kaffe.com");
        datos.put("contrasena", "clave-luis");
        datos.put("idRol", rolRepository.obtenerPorNombre("Operador").orElseThrow().getId().getValue());
        return crearUsuarioUseCase.execute(CrearUsuarioRequest.createFromRequest(datos)).getId();
    }

    @Test
    void seVendePorUnidadesEnteras() {
        // Act (logica.md GENERAL-16)
        DomainException error = assertThrows(DomainException.class, () -> vender("10", de(gaseosa, "1.5")));

        // Assert
        assertEquals(400, error.getCodigo());
        assertEquals(0, new BigDecimal("10").compareTo(escenario.stock(gaseosa)));
    }

    @Test
    void unCajeroQueNoEsTitularRegistraIngresosPeroNoEgresos() {
        // Arrange (logica.md SESIONCAJA-15)
        String cajero = operador();

        // Act
        registrarMovimientoCajaUseCase.execute(RegistrarMovimientoCajaRequest.createFromRequest(
                Map.of("tipo", "INGRESO", "monto", "20", "motivo", "Cambio"), cajero));
        AutorizacionException error = assertThrows(AutorizacionException.class, () -> registrarMovimientoCajaUseCase.execute(
                RegistrarMovimientoCajaRequest.createFromRequest(Map.of("tipo", "EGRESO", "monto", "5", "motivo", "Taxi"), cajero)));

        // Assert
        assertEquals(403, error.getCodigo());
        assertEquals(1, sesionCajaRepository.obtenerAbierta().orElseThrow().getMovimientosActivos().size());
    }

    @Test
    void elArqueoDesglosaLoVendidoPorCadaCajero() {
        // Arrange (logica.md SESIONCAJA-16): el administrador vende 2 gaseosas y el cajero 1.
        String cajero = operador();
        vender("10", de(gaseosa, "2"));
        Map<String, Object> datos = new HashMap<>();
        datos.put("detalles", List.of(de(gaseosa, "1")));
        datos.put("pagos", List.of(Map.of("idTipoPago", efectivo, "monto", "5", "montoRecibido", "5")));
        crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(datos, cajero));

        // Act
        ArqueoCajaResponse arqueo = cerrarSesionCajaUseCase.execute(
                CerrarSesionCajaRequest.createFromRequest(Map.of("montoCierreDeclarado", "115"), usuario));

        // Assert
        assertEquals(2, arqueo.getRecaudacionPorUsuario().size());
        assertEquals(0, new BigDecimal("10").compareTo(arqueo.getRecaudacionPorUsuario().get(0).getTotalVendido()));
        assertEquals("luis@kaffe.com", arqueo.getRecaudacionPorUsuario().get(1).getCorreo());
        assertEquals(0, new BigDecimal("5").compareTo(arqueo.getRecaudacionPorUsuario().get(1).getEfectivo()));
    }

    @Test
    void unOperadorNoCancelaUnPedidoPorqueAnulariaSuVenta() {
        // Arrange (logica.md PEDIDO-09)
        String cajero = operador();
        VentaResponse venta = vender("15", de(gaseosa, "3"));
        Map<String, Object> pedido = new HashMap<>();
        pedido.put("fechaEntrega", LocalDateTime.now().plusHours(2).toString());
        pedido.put("latitud", "-17.78");
        pedido.put("longitud", "-63.18");
        pedido.put("idVenta", venta.getId());
        String idPedido = crearPedidoUseCase.execute(CrearPedidoRequest.createFromRequest(pedido)).getId();
        String cancelado = estadoPedidoRepository.obtenerPorNombre("Cancelado").orElseThrow().getId().getValue();

        // Act
        AutorizacionException error = assertThrows(AutorizacionException.class, () -> actualizarPedidoUseCase.execute(idPedido,
                ActualizarPedidoRequest.createFromRequest(Map.of("idEstado", cancelado, "motivo", "No llego"), cajero)));

        // Assert
        assertEquals(403, error.getCodigo());
        assertEquals(0, new BigDecimal("7").compareTo(escenario.stock(gaseosa)));
    }

    @Test
    void niElUltimoAdministradorNiElTitularDeLaCajaSeDanDeBaja() {
        // Arrange (logica.md USUARIO-04): el administrador sembrado es el unico y abrio la caja.
        String cajero = operador();

        // Act
        DomainException ultimoAdministrador = assertThrows(DomainException.class, () -> eliminarUsuarioUseCase.execute(usuario, cajero));
        DomainException aSiMismo = assertThrows(DomainException.class, () -> eliminarUsuarioUseCase.execute(cajero, cajero));
        eliminarUsuarioUseCase.execute(cajero, usuario);

        // Assert
        assertEquals(409, ultimoAdministrador.getCodigo());
        assertTrue(ultimoAdministrador.getMensaje().contains("ultimo Administrador"));
        assertEquals(409, aSiMismo.getCodigo());
    }
}
