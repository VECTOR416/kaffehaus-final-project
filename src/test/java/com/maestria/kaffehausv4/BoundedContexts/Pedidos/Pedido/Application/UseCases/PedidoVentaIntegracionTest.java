package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa.CategoriaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa.CategoriaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa.EstadoExistenciaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa.EstadoExistenciaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa.ExistenciaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa.ExistenciaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.DetalleLoteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.DetalleLoteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa.MovimientoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Infrastructure.Persistence.Jpa.EstadoPedidoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ActualizarPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.CrearPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.PedidoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Persistence.Jpa.PedidoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa.ProveedorJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa.ProveedorJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.AnularVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.CajaDePrueba;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.AnularVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.CrearVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.ObtenerVentaPorIdUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas de integracion de los pedidos a domicilio y su efecto sobre la venta y el
 * inventario (ventas.md VEN-50..VEN-53) contra la base real.
 *
 * Escenario: un sandwich de stock directo con una existencia abierta de 10 unidades. Requiere
 * el catalogo que siembra POST api/v1/seed (lo ejecuta CajaDePrueba). Sin @Transactional de clase.
 */
@SpringBootTest
class PedidoVentaIntegracionTest {

    @Autowired private CrearPedidoUseCase crearPedidoUseCase;
    @Autowired private ActualizarPedidoUseCase actualizarPedidoUseCase;
    @Autowired private CrearVentaUseCase crearVentaUseCase;
    @Autowired private CajaDePrueba cajaDePrueba;
    @Autowired private AnularVentaUseCase anularVentaUseCase;
    @Autowired private ObtenerVentaPorIdUseCase obtenerVentaPorIdUseCase;

    @Autowired private PedidoJpaRepository pedidoJpaRepository;
    @Autowired private EstadoPedidoJpaRepository estadoPedidoJpaRepository;
    @Autowired private VentaJpaRepository ventaJpaRepository;
    @Autowired private DetalleVentaJpaRepository detalleVentaJpaRepository;
    @Autowired private RolJpaRepository rolJpaRepository;
    @Autowired private PersonaJpaRepository personaJpaRepository;
    @Autowired private UsuarioJpaRepository usuarioJpaRepository;
    @Autowired private ProveedorJpaRepository proveedorJpaRepository;
    @Autowired private UnidadJpaRepository unidadJpaRepository;
    @Autowired private TipoJpaRepository tipoJpaRepository;
    @Autowired private CategoriaJpaRepository categoriaJpaRepository;
    @Autowired private ProductoJpaRepository productoJpaRepository;
    @Autowired private LoteJpaRepository loteJpaRepository;
    @Autowired private DetalleLoteJpaRepository detalleLoteJpaRepository;
    @Autowired private EstadoExistenciaJpaRepository estadoExistenciaJpaRepository;
    @Autowired private ExistenciaJpaRepository existenciaJpaRepository;
    @Autowired private MovimientoJpaRepository movimientoJpaRepository;

    private UUID idRol;
    private UUID idPersona;
    private UUID idUsuario;
    private UUID idSesionCaja;
    private UUID idProveedor;
    private UUID idUnidad;
    private UUID idTipo;
    private UUID idCategoria;
    private UUID idSandwich;
    private UUID idLote;
    private UUID idDetalle;
    private UUID idEstadoExistencia;
    private UUID idExistencia;

    private final List<UUID> ventas = new ArrayList<>();
    private final List<UUID> pedidos = new ArrayList<>();

    @BeforeEach
    void sembrarDatos() {
        idRol = UUID.randomUUID();
        RolJpaModel rol = new RolJpaModel();
        rol.setId(idRol);
        rol.setNombre("ROL_TEST_" + idRol);
        rol.setFecC(OffsetDateTime.now());
        rol.setBorrado(false);
        rolJpaRepository.save(rol);

        idPersona = UUID.randomUUID();
        PersonaJpaModel persona = new PersonaJpaModel();
        persona.setId(idPersona);
        persona.setNombre("Persona");
        persona.setApellidos("De Prueba");
        persona.setDni("TEST-" + idPersona.toString().substring(0, 8));
        persona.setFecC(OffsetDateTime.now());
        persona.setBorrado(false);
        personaJpaRepository.save(persona);

        idUsuario = UUID.randomUUID();
        UsuarioJpaModel usuario = new UsuarioJpaModel();
        usuario.setId(idUsuario);
        usuario.setIdPersona(idPersona);
        usuario.setCorreo(idUsuario + "@test.local");
        usuario.setContrasena("pbkdf2$1$x$y");
        usuario.setIdRol(idRol);
        usuario.setFecC(OffsetDateTime.now());
        usuario.setBorrado(false);
        usuarioJpaRepository.save(usuario);

        idProveedor = UUID.randomUUID();
        ProveedorJpaModel proveedor = new ProveedorJpaModel();
        proveedor.setId(idProveedor);
        proveedor.setIdPersona(idPersona);
        proveedor.setFecC(OffsetDateTime.now());
        proveedor.setBorrado(false);
        proveedorJpaRepository.save(proveedor);

        idUnidad = UUID.randomUUID();
        UnidadJpaModel unidad = new UnidadJpaModel();
        unidad.setId(idUnidad);
        unidad.setUnidad("unidad-test");
        unidad.setAbreviatura("ut");
        unidad.setEsBorrable(true);
        unidad.setFecC(OffsetDateTime.now());
        unidad.setBorrado(false);
        unidadJpaRepository.save(unidad);

        idTipo = UUID.randomUUID();
        TipoJpaModel tipo = new TipoJpaModel();
        tipo.setId(idTipo);
        tipo.setNombre("tipo-test");
        tipo.setFecC(OffsetDateTime.now());
        tipo.setBorrado(false);
        tipoJpaRepository.save(tipo);

        idCategoria = UUID.randomUUID();
        CategoriaJpaModel categoria = new CategoriaJpaModel();
        categoria.setId(idCategoria);
        categoria.setNombre("categoria-test");
        categoria.setEsBorrable(true);
        categoria.setFecC(OffsetDateTime.now());
        categoria.setBorrado(false);
        categoriaJpaRepository.save(categoria);

        idSandwich = UUID.randomUUID();
        ProductoJpaModel sandwich = new ProductoJpaModel();
        sandwich.setId(idSandwich);
        sandwich.setNombre("sandwich-test");
        sandwich.setStock(new BigDecimal("10.00"));
        sandwich.setPrecio(new BigDecimal("15.00"));
        sandwich.setStockMinimo(new BigDecimal("0.00"));
        sandwich.setVendible(true);
        sandwich.setEsStockDirecto(true);
        sandwich.setUnidadActual(idUnidad);
        sandwich.setIdTipo(idTipo);
        sandwich.setIdCategoria(idCategoria);
        sandwich.setFecC(OffsetDateTime.now());
        sandwich.setBorrado(false);
        productoJpaRepository.save(sandwich);

        idLote = UUID.randomUUID();
        LoteJpaModel lote = new LoteJpaModel();
        lote.setId(idLote);
        lote.setFechaCompra(LocalDateTime.now());
        lote.setTotal(new BigDecimal("0.00"));
        lote.setIdProveedor(idProveedor);
        lote.setIdUsuario(idUsuario);
        lote.setEsProduccion(false);
        lote.setFecC(OffsetDateTime.now());
        lote.setBorrado(false);
        loteJpaRepository.save(lote);

        idDetalle = UUID.randomUUID();
        DetalleLoteJpaModel detalle = new DetalleLoteJpaModel();
        detalle.setId(idDetalle);
        detalle.setIdLote(idLote);
        detalle.setIdProducto(idSandwich);
        detalle.setCantidadInicial(new BigDecimal("1.00"));
        detalle.setCantidadRestante(new BigDecimal("0.00"));
        detalle.setPrecioUnitario(new BigDecimal("60.00"));
        detalle.setFraccionable(false);
        detalle.setFecC(OffsetDateTime.now());
        detalle.setBorrado(false);
        detalleLoteJpaRepository.save(detalle);

        idEstadoExistencia = UUID.randomUUID();
        EstadoExistenciaJpaModel estado = new EstadoExistenciaJpaModel();
        estado.setId(idEstadoExistencia);
        estado.setNombre("ESTADO_EXISTENCIA_TEST_" + idEstadoExistencia);
        estado.setFecC(OffsetDateTime.now());
        estado.setBorrado(false);
        estadoExistenciaJpaRepository.save(estado);

        idExistencia = UUID.randomUUID();
        ExistenciaJpaModel existencia = new ExistenciaJpaModel();
        existencia.setId(idExistencia);
        existencia.setIdDetalleLote(idDetalle);
        existencia.setContenidoInicial(new BigDecimal("10.0000"));
        existencia.setContenidoActual(new BigDecimal("10.0000"));
        existencia.setIdEstadoExistencia(idEstadoExistencia);
        existencia.setUnidad(idUnidad);
        existencia.setIdUsuario(idUsuario);
        existencia.setFecC(OffsetDateTime.now());
        existencia.setBorrado(false);
        existenciaJpaRepository.save(existencia);
        idSesionCaja = cajaDePrueba.prepararCaja(idUsuario);
    }

    @AfterEach
    void limpiarDatos() {
        // La limpieza de la prueba si borra fisicamente: es basura de test, no datos de negocio.
        pedidoJpaRepository.deleteAllById(pedidos);
        List<UUID> existencias = existenciaJpaRepository.findAll().stream()
                .filter(existencia -> idDetalle.equals(existencia.getIdDetalleLote()))
                .map(ExistenciaJpaModel::getId)
                .toList();
        movimientoJpaRepository.deleteAll(movimientoJpaRepository.findAll().stream()
                .filter(movimiento -> existencias.contains(movimiento.getIdExistencia()))
                .toList());
        existenciaJpaRepository.deleteAllById(existencias);
        for (UUID idVenta : ventas) {
            detalleVentaJpaRepository.deleteAll(detalleVentaJpaRepository.findByIdVentaAndBorradoFalse(idVenta));
            ventaJpaRepository.deleteById(idVenta);
        }
        cajaDePrueba.retirarCaja(idSesionCaja);
        detalleLoteJpaRepository.deleteById(idDetalle);
        loteJpaRepository.deleteById(idLote);
        estadoExistenciaJpaRepository.deleteById(idEstadoExistencia);
        productoJpaRepository.deleteById(idSandwich);
        categoriaJpaRepository.deleteById(idCategoria);
        tipoJpaRepository.deleteById(idTipo);
        unidadJpaRepository.deleteById(idUnidad);
        proveedorJpaRepository.deleteById(idProveedor);
        usuarioJpaRepository.deleteById(idUsuario);
        personaJpaRepository.deleteById(idPersona);
        rolJpaRepository.deleteById(idRol);

        ventas.clear();
        pedidos.clear();
    }

    private VentaResponse venderSandwiches(String cantidad) {
        VentaResponse venta = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(Map.of(
                "idUsuario", idUsuario.toString(),
                "detalles", List.of(Map.of("idProducto", idSandwich.toString(), "cantidad", cantidad)),
                "pagos", cajaDePrueba.enEfectivo(aPagar("15.00", cantidad))), idUsuario.toString()));
        ventas.add(UUID.fromString(venta.getId()));
        return venta;
    }

    private PedidoResponse pedirDomicilio(String idVenta) {
        PedidoResponse pedido = crearPedidoUseCase.execute(CrearPedidoRequest.createFromRequest(Map.of(
                "idVenta", idVenta,
                "fechaEntrega", LocalDateTime.now().plusHours(1).toString(),
                "latitud", "-16.5",
                "longitud", "-68.15")));
        pedidos.add(UUID.fromString(pedido.getId()));
        return pedido;
    }

    private String estado(String nombre) {
        return estadoPedidoJpaRepository.findAll().stream()
                .filter(estado -> nombre.equalsIgnoreCase(estado.getNombre()))
                .findFirst().orElseThrow().getId().toString();
    }

    /** 'conSesion' simula si el controller resolvio el usuario del token (@UsuarioEnSesion). */
    private PedidoResponse pasarA(String idPedido, String nombreEstado, boolean conSesion) {
        Map<String, Object> cambio = new HashMap<>();
        cambio.put("idEstado", estado(nombreEstado));
        String idUsuarioEnSesion = conSesion ? idUsuario.toString() : null;
        return actualizarPedidoUseCase.execute(idPedido, ActualizarPedidoRequest.createFromRequest(cambio, idUsuarioEnSesion));
    }

    private BigDecimal sandwichesEnStock() {
        return existenciaJpaRepository.findAll().stream()
                .filter(existencia -> idDetalle.equals(existencia.getIdDetalleLote()))
                .map(ExistenciaJpaModel::getContenidoActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void unPedidoNaceRecibidoYElInventarioYaSalioAlVender() {
        VentaResponse venta = venderSandwiches("3");

        PedidoResponse pedido = pedirDomicilio(venta.getId());

        assertEquals("Recibido", pedido.getEstado().getNombre());
        assertEquals(0, new BigDecimal("7").compareTo(sandwichesEnStock()));
    }

    @Test
    void elEstadoSoloAvanzaPorSusTransiciones() {
        PedidoResponse pedido = pedirDomicilio(venderSandwiches("1").getId());

        DomainException salto = assertThrows(DomainException.class, () -> pasarA(pedido.getId(), "Entregado", true));
        assertEquals(409, salto.getCodigo());

        pasarA(pedido.getId(), "En preparacion", true);
        pasarA(pedido.getId(), "En camino", true);
        PedidoResponse entregado = pasarA(pedido.getId(), "Entregado", true);
        assertEquals("Entregado", entregado.getEstado().getNombre());

        DomainException final_ = assertThrows(DomainException.class, () -> pasarA(pedido.getId(), "Cancelado", true));
        assertEquals(409, final_.getCodigo());
    }

    @Test
    void cancelarUnPedidoRecibidoAnulaSuVentaYReintegraElInventario() {
        VentaResponse venta = venderSandwiches("3");
        PedidoResponse pedido = pedirDomicilio(venta.getId());

        pasarA(pedido.getId(), "Cancelado", true);

        assertEquals("Anulada", obtenerVentaPorIdUseCase.execute(venta.getId()).getEstadoVenta().getNombre());
        assertEquals(0, new BigDecimal("10").compareTo(sandwichesEnStock()));
    }

    @Test
    void cancelarUnPedidoEnPreparacionAnulaSuVentaSinReintegro() {
        VentaResponse venta = venderSandwiches("3");
        PedidoResponse pedido = pedirDomicilio(venta.getId());
        pasarA(pedido.getId(), "En preparacion", true);

        pasarA(pedido.getId(), "Cancelado", true);

        assertEquals("Anulada", obtenerVentaPorIdUseCase.execute(venta.getId()).getEstadoVenta().getNombre());
        assertEquals(0, new BigDecimal("7").compareTo(sandwichesEnStock()));
    }

    @Test
    void sinUsuarioEnSesionNoSeModificaElPedido() {
        PedidoResponse pedido = pedirDomicilio(venderSandwiches("1").getId());

        DomainException error = assertThrows(DomainException.class, () -> pasarA(pedido.getId(), "Cancelado", false));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void unaVentaTieneAloSumoUnPedidoYNuncaSiEstaAnulada() {
        VentaResponse venta = venderSandwiches("1");
        pedirDomicilio(venta.getId());

        DomainException segundo = assertThrows(DomainException.class, () -> pedirDomicilio(venta.getId()));
        assertEquals(409, segundo.getCodigo());

        VentaResponse otra = venderSandwiches("1");
        anularVentaUseCase.execute(otra.getId(), AnularVentaRequest.createFromRequest(Map.of(
                "idUsuario", idUsuario.toString(), "motivo", "Error de cobro"), idUsuario.toString()));
        DomainException sobreAnulada = assertThrows(DomainException.class, () -> pedirDomicilio(otra.getId()));
        assertEquals(409, sobreAnulada.getCodigo());
    }

    /** Total que cobra el sistema: todos los productos de la prueba tienen un unico precio y ningun descuento. */
    private static String aPagar(String precio, String cantidad) {
        return new BigDecimal(precio).multiply(new BigDecimal(cantidad)).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
