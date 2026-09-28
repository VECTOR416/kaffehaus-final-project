package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa.EstadoVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.CajaDePrueba;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.AbrirSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ArqueoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.CerrarSesionCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.EstadoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RecaudacionPorTipoPagoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.RegistrarMovimientoCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.SesionCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.MovimientoCajaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.SesionCajaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa.SesionCajaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.TicketVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.CrearVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.ObtenerVentaPorTicketUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.PagoVentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.PagoVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Inicializacion.SembrarSistemaUseCase;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Sesion de caja, arqueo y ticket contra la base real (arquitectura.md 4.11 y CAJ-14).
 *
 * Las ventas del arqueo se siembran directamente como filas (venta + pago_venta): lo que se
 * prueba aqui es la caja, no el descuento de inventario, que ya cubren los tests de venta.
 *
 * Como en el local solo puede haber una caja abierta, estas pruebas necesitan empezar sin
 * ninguna; si la base tiene una abierta, se omiten en lugar de tocar una caja ajena.
 */
@SpringBootTest
class CajaIntegracionTest {

    @Autowired private AbrirSesionCajaUseCase abrirSesionCajaUseCase;
    @Autowired private RegistrarMovimientoCajaUseCase registrarMovimientoCajaUseCase;
    @Autowired private CerrarSesionCajaUseCase cerrarSesionCajaUseCase;
    @Autowired private ObtenerEstadoCajaUseCase obtenerEstadoCajaUseCase;
    @Autowired private CrearVentaUseCase crearVentaUseCase;
    @Autowired private ObtenerVentaPorTicketUseCase obtenerVentaPorTicketUseCase;
    @Autowired private SembrarSistemaUseCase sembrarSistemaUseCase;
    @Autowired private CajaDePrueba cajaDePrueba;

    @Autowired private SesionCajaJpaRepository sesionCajaJpaRepository;
    @Autowired private MovimientoCajaJpaRepository movimientoCajaJpaRepository;
    @Autowired private VentaJpaRepository ventaJpaRepository;
    @Autowired private PagoVentaJpaRepository pagoVentaJpaRepository;
    @Autowired private EstadoVentaJpaRepository estadoVentaJpaRepository;
    @Autowired private UsuarioJpaRepository usuarioJpaRepository;
    @Autowired private PersonaJpaRepository personaJpaRepository;
    @Autowired private RolJpaRepository rolJpaRepository;

    private UUID idOperador;
    private UUID idOtroOperador;
    private UUID idAdministrador;
    private final List<UUID> personas = new ArrayList<>();
    private final List<UUID> ventas = new ArrayList<>();

    @BeforeEach
    void sembrarDatos() {
        sembrarSistemaUseCase.execute();
        assumeTrue(sesionCajaJpaRepository.findFirstByEstadoAndBorradoFalse(SesionCaja.ABIERTA).isEmpty(),
                "La base tiene una caja abierta; estas pruebas necesitan empezar sin ninguna");

        idOperador = guardarUsuario("Olga", "Operadora", "Operador");
        idOtroOperador = guardarUsuario("Oscar", "Operador", "Operador");
        idAdministrador = guardarUsuario("Ada", "Administradora", "Administrador");
    }

    private UUID guardarUsuario(String nombre, String apellidos, String rol) {
        UUID idPersona = UUID.randomUUID();
        PersonaJpaModel persona = new PersonaJpaModel();
        persona.setId(idPersona);
        persona.setNombre(nombre);
        persona.setApellidos(apellidos);
        persona.setDni("TEST-" + idPersona.toString().substring(0, 8));
        persona.setFecC(OffsetDateTime.now());
        persona.setBorrado(false);
        personaJpaRepository.save(persona);
        personas.add(idPersona);

        UUID id = UUID.randomUUID();
        UsuarioJpaModel usuario = new UsuarioJpaModel();
        usuario.setId(id);
        usuario.setIdPersona(idPersona);
        usuario.setCorreo(id + "@test.local");
        usuario.setContrasena("pbkdf2$1$x$y");
        usuario.setIdRol(rolJpaRepository.findFirstByNombreIgnoreCaseAndBorradoFalse(rol).orElseThrow().getId());
        usuario.setFecC(OffsetDateTime.now());
        usuario.setBorrado(false);
        usuarioJpaRepository.save(usuario);
        return id;
    }

    @AfterEach
    void limpiarDatos() {
        // La limpieza si borra fisicamente: es basura de test. pago_venta cae en cascada.
        ventaJpaRepository.deleteAllById(ventas);
        List<SesionCajaJpaModel> sesiones = sesionCajaJpaRepository.findAll().stream()
                .filter(sesion -> List.of(idOperador, idOtroOperador, idAdministrador).contains(sesion.getIdUsuario()))
                .toList();
        for (SesionCajaJpaModel sesion : sesiones) {
            movimientoCajaJpaRepository.deleteAll(movimientoCajaJpaRepository.findByIdSesionCajaAndBorradoFalseOrderByFecCAsc(sesion.getId()));
        }
        sesionCajaJpaRepository.deleteAll(sesiones);
        usuarioJpaRepository.deleteAllById(List.of(idOperador, idOtroOperador, idAdministrador));
        personaJpaRepository.deleteAllById(personas);
        personas.clear();
        ventas.clear();
    }

    private SesionCajaResponse abrir(UUID idUsuario, String monto) {
        return abrirSesionCajaUseCase.execute(AbrirSesionCajaRequest.createFromRequest(Map.of("montoApertura", monto), idUsuario.toString()));
    }

    private void moverEfectivo(String tipo, String monto) {
        registrarMovimientoCajaUseCase.execute(RegistrarMovimientoCajaRequest.createFromRequest(
                Map.of("tipo", tipo, "monto", monto, "motivo", "Prueba"), idOperador.toString()));
    }

    private ArqueoCajaResponse cerrar(UUID idUsuario, String declarado) {
        return cerrarSesionCajaUseCase.execute(CerrarSesionCajaRequest.createFromRequest(
                Map.of("montoCierreDeclarado", declarado), idUsuario.toString()));
    }

    /** Una venta ya cobrada en la sesion, con sus pagos: [tipo de pago, monto, recibido]. */
    private VentaJpaModel guardarVenta(UUID idSesionCaja, String estado, String total, String[]... pagos) {
        UUID id = UUID.randomUUID();
        VentaJpaModel venta = new VentaJpaModel();
        venta.setId(id);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setTotal(new BigDecimal(total));
        venta.setIdUsuario(idOperador);
        venta.setIdCliente(UUID.fromString(Cliente.ID_MOSTRADOR));
        venta.setIdSesionCaja(idSesionCaja);
        venta.setIdEstadoVenta(estadoVentaJpaRepository.findFirstByNombreIgnoreCaseAndBorradoFalse(estado).orElseThrow().getId());
        venta.setFecC(OffsetDateTime.now());
        venta.setBorrado(false);
        VentaJpaModel guardada = ventaJpaRepository.saveAndFlush(venta);
        ventas.add(id);

        for (String[] pago : pagos) {
            PagoVentaJpaModel fila = new PagoVentaJpaModel();
            fila.setId(UUID.randomUUID());
            fila.setIdVenta(id);
            fila.setIdTipoPago(cajaDePrueba.idTipoPago(pago[0]));
            fila.setMonto(new BigDecimal(pago[1]));
            fila.setMontoRecibido(new BigDecimal(pago[2]));
            fila.setCambio(new BigDecimal(pago[2]).subtract(new BigDecimal(pago[1])));
            fila.setFecC(OffsetDateTime.now());
            fila.setBorrado(false);
            pagoVentaJpaRepository.save(fila);
        }
        return guardada;
    }

    private BigDecimal recaudado(ArqueoCajaResponse arqueo, String tipo) {
        return arqueo.getRecaudacionPorTipoPago().stream()
                .filter(porTipo -> tipo.equals(porTipo.getNombre()))
                .map(RecaudacionPorTipoPagoResponse::getMonto)
                .findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ CAJ-20, CAJ-25

    @Test
    void soloPuedeHaberUnaCajaAbiertaYElEstadoNombraASuResponsable() {
        assertFalse(obtenerEstadoCajaUseCase.execute().isAbierta());

        SesionCajaResponse sesion = abrir(idOperador, "150");

        DomainException otra = assertThrows(DomainException.class, () -> abrir(idAdministrador, "10"));
        assertEquals(409, otra.getCodigo());

        EstadoCajaResponse estado = obtenerEstadoCajaUseCase.execute();
        assertTrue(estado.isAbierta());
        assertEquals(sesion.getId(), estado.getIdSesionCaja());
        assertEquals("Olga Operadora", estado.getNombreResponsable());
        assertEquals(new BigDecimal("150.00"), estado.getMontoApertura());
    }

    @Test
    void elIndiceUnicoImpideDosCajasAbiertasAunqueSeSalteElCasoDeUso() {
        abrir(idOperador, "0");

        SesionCajaJpaModel segunda = new SesionCajaJpaModel();
        segunda.setId(UUID.randomUUID());
        segunda.setIdUsuario(idAdministrador);
        segunda.setFechaApertura(LocalDateTime.now());
        segunda.setMontoApertura(BigDecimal.ZERO);
        segunda.setEstado(SesionCaja.ABIERTA);
        segunda.setFecC(OffsetDateTime.now());
        segunda.setBorrado(false);

        assertThrows(DataIntegrityViolationException.class, () -> sesionCajaJpaRepository.saveAndFlush(segunda));
    }

    // ------------------------------------------------------------------ CAJ-01

    @Test
    void sinCajaAbiertaNoSeVende() {
        DomainException error = assertThrows(DomainException.class, () -> crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(Map.of(
                "detalles", List.of(Map.of("idProducto", UUID.randomUUID().toString(), "cantidad", "1")),
                "pagos", cajaDePrueba.enEfectivo("10.00")), idOperador.toString())));

        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("caja"));
    }

    // ------------------------------------------------------------------ CAJ-21..CAJ-24

    @Test
    void elArqueoSumaElEfectivoDeLasCompletadasYLosMovimientos() {
        UUID idSesion = UUID.fromString(abrir(idOperador, "100").getId());
        guardarVenta(idSesion, "Completada", "50.00", new String[] {"Efectivo", "30.00", "50.00"}, new String[] {"Tarjeta", "20.00", "20.00"});
        guardarVenta(idSesion, "Completada", "12.00", new String[] {"QR", "12.00", "12.00"});
        guardarVenta(idSesion, "Completada", "8.00", new String[] {"Efectivo", "8.00", "10.00"});
        // Anulada: el dinero se devolvio, no cuenta.
        guardarVenta(idSesion, "Anulada", "40.00", new String[] {"Efectivo", "40.00", "40.00"});
        moverEfectivo("INGRESO", "20");
        moverEfectivo("EGRESO", "5.50");

        ArqueoCajaResponse arqueo = cerrar(idOperador, "150");

        // 100 + (30 + 8) + 20 - 5.50 = 152.50
        assertEquals(new BigDecimal("38.00"), arqueo.getVentasEnEfectivo().setScale(2));
        assertEquals(new BigDecimal("152.50"), arqueo.getMontoCierreEsperado());
        assertEquals(new BigDecimal("-2.50"), arqueo.getDiferencia());
        assertEquals(new BigDecimal("20.00"), arqueo.getIngresosManuales());
        assertEquals(new BigDecimal("5.50"), arqueo.getEgresosManuales());
        assertEquals(0, new BigDecimal("70.00").compareTo(arqueo.getTotalVendido()));
        assertEquals(3, arqueo.getCantidadVentas());
        assertEquals(1, arqueo.getCantidadVentasAnuladas());
        assertEquals(0, new BigDecimal("38.00").compareTo(recaudado(arqueo, "Efectivo")));
        assertEquals(0, new BigDecimal("20.00").compareTo(recaudado(arqueo, "Tarjeta")));
        assertEquals(0, new BigDecimal("12.00").compareTo(recaudado(arqueo, "QR")));

        assertEquals(SesionCaja.CERRADA, arqueo.getSesion().getEstado());
        assertFalse(obtenerEstadoCajaUseCase.execute().isAbierta());
        SesionCajaJpaModel guardada = sesionCajaJpaRepository.findById(idSesion).orElseThrow();
        assertEquals(0, new BigDecimal("152.50").compareTo(guardada.getMontoCierreEsperado()));
        assertEquals(SesionCaja.CERRADA, guardada.getEstado());
    }

    @Test
    void soloElTitularOUnAdministradorCierranLaCaja() {
        abrir(idOperador, "100");

        AutorizacionException ajeno = assertThrows(AutorizacionException.class, () -> cerrar(idOtroOperador, "100"));
        assertEquals(403, ajeno.getCodigo());
        assertTrue(obtenerEstadoCajaUseCase.execute().isAbierta());

        ArqueoCajaResponse porAdministrador = cerrar(idAdministrador, "100");
        assertEquals(new BigDecimal("0.00"), porAdministrador.getDiferencia());
    }

    @Test
    void sinCajaAbiertaNoHayMovimientosNiCierre() {
        assertEquals(409, assertThrows(DomainException.class, () -> moverEfectivo("INGRESO", "1")).getCodigo());
        assertEquals(409, assertThrows(DomainException.class, () -> cerrar(idOperador, "0")).getCodigo());
    }

    // ------------------------------------------------------------------ CAJ-14

    @Test
    void elTicketDevuelveLaVentaConClienteCajeroYTiposDePago() {
        UUID idSesion = UUID.fromString(abrir(idOperador, "0").getId());
        VentaJpaModel venta = guardarVenta(idSesion, "Completada", "25.00",
                new String[] {"Efectivo", "15.00", "20.00"}, new String[] {"Tarjeta", "10.00", "10.00"});
        assertTrue(venta.getNumeroTicket() != null && venta.getNumeroTicket() > 0, "La base numera el ticket");

        TicketVentaResponse ticket = obtenerVentaPorTicketUseCase.execute(venta.getNumeroTicket().toString());

        assertEquals(venta.getId().toString(), ticket.getIdVenta());
        assertEquals("Cliente Casual / Mostrador", ticket.getNombreCliente());
        assertEquals("Olga Operadora", ticket.getNombreCajero());
        assertEquals("Completada", ticket.getEstadoVenta());
        assertEquals(0, new BigDecimal("5.00").compareTo(ticket.getCambio()));
        assertEquals(List.of("Efectivo", "Tarjeta"), ticket.getPagos().stream().map(pago -> pago.getNombreTipoPago()).sorted().toList());

        assertEquals(400, assertThrows(DomainException.class, () -> obtenerVentaPorTicketUseCase.execute("abc")).getCodigo());
        assertEquals(404, assertThrows(DomainException.class, () -> obtenerVentaPorTicketUseCase.execute("999999999")).getCodigo());
    }
}
