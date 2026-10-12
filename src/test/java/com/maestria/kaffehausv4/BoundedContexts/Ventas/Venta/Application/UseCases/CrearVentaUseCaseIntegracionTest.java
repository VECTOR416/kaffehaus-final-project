package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa.MovimientoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa.MovimientoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa.ClienteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa.ClienteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa.ProveedorJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa.ProveedorJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa.DescuentoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa.DescuentoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.CajaDePrueba;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Infrastructure.Persistence.Jpa.TipoDescuentoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ActualizarVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.AnularVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas de integracion de la venta y su integracion con el inventario (ventas.md) contra la
 * base real.
 *
 * Escenario: cafe (12.50) y te (8.00), de stock directo, cada uno con una existencia abierta de
 * 10 unidades compradas a 50.00 la caja de 10 (costo 5.00 por unidad). Requiere el catalogo de
 * POST api/v1/seed ('Completada', 'Anulada', 'Porcentaje', 'Monto fijo', tipos de movimiento,
 * estados de existencia, tipos de pago y el cliente 'Mostrador'), que CajaDePrueba ejecuta
 * antes de cada prueba junto con la apertura de una caja. Cada venta se paga en efectivo por
 * su total exacto (arquitectura.md 4.11, CAJ-11).
 *
 * No se anota @Transactional a proposito: cada caso de uso abre y cierra su propia
 * transaccion, que es justo lo que se quiere observar.
 */
@SpringBootTest
class CrearVentaUseCaseIntegracionTest {

    @Autowired private CrearVentaUseCase crearVentaUseCase;
    @Autowired private ActualizarVentaUseCase actualizarVentaUseCase;
    @Autowired private ObtenerVentaPorIdUseCase obtenerVentaPorIdUseCase;
    @Autowired private EliminarVentaUseCase eliminarVentaUseCase;
    @Autowired private AnularVentaUseCase anularVentaUseCase;

    @Autowired private VentaJpaRepository ventaJpaRepository;
    @Autowired private DetalleVentaJpaRepository detalleVentaJpaRepository;
    @Autowired private ProductoJpaRepository productoJpaRepository;
    @Autowired private TipoJpaRepository tipoJpaRepository;
    @Autowired private CategoriaJpaRepository categoriaJpaRepository;
    @Autowired private UnidadJpaRepository unidadJpaRepository;
    @Autowired private UsuarioJpaRepository usuarioJpaRepository;
    @Autowired private PersonaJpaRepository personaJpaRepository;
    @Autowired private RolJpaRepository rolJpaRepository;
    @Autowired private ClienteJpaRepository clienteJpaRepository;
    @Autowired private ProveedorJpaRepository proveedorJpaRepository;
    @Autowired private LoteJpaRepository loteJpaRepository;
    @Autowired private DetalleLoteJpaRepository detalleLoteJpaRepository;
    @Autowired private EstadoExistenciaJpaRepository estadoExistenciaJpaRepository;
    @Autowired private ExistenciaJpaRepository existenciaJpaRepository;
    @Autowired private MovimientoJpaRepository movimientoJpaRepository;
    @Autowired private DescuentoJpaRepository descuentoJpaRepository;
    @Autowired private TipoDescuentoJpaRepository tipoDescuentoJpaRepository;
    @Autowired private CajaDePrueba cajaDePrueba;

    private static final String PRODUCTO_INEXISTENTE = "00000000-0000-0000-0000-0000000000ff";

    private UUID idRol;
    private UUID idPersona;
    private UUID idUsuario;
    private UUID idCliente;
    private UUID idUnidad;
    private UUID idTipo;
    private UUID idCategoria;
    private UUID idProductoCafe;
    private UUID idProductoTe;
    private UUID idProveedor;
    private UUID idLote;
    private UUID idEstadoExistencia;
    private UUID idExistenciaCafe;
    private UUID idExistenciaTe;
    private UUID idSesionCaja;

    private final List<UUID> productos = new ArrayList<>();
    private final List<UUID> detallesLote = new ArrayList<>();
    private final List<UUID> descuentos = new ArrayList<>();
    private final List<UUID> ventasCreadas = new ArrayList<>();

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
        persona.setTelefono("70000000");
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

        idCliente = UUID.randomUUID();
        ClienteJpaModel cliente = new ClienteJpaModel();
        cliente.setId(idCliente);
        cliente.setIdPersona(idPersona);
        cliente.setFecC(OffsetDateTime.now());
        cliente.setBorrado(false);
        clienteJpaRepository.save(cliente);

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

        idProductoCafe = guardarProducto("cafe-test", "12.50", true, null);
        idProductoTe = guardarProducto("te-test", "8.00", true, null);

        sembrarStock();
        idSesionCaja = cajaDePrueba.prepararCaja(idUsuario);
    }

    private UUID guardarProducto(String nombre, String precio, boolean vendible, UUID padre) {
        UUID id = UUID.randomUUID();
        ProductoJpaModel producto = new ProductoJpaModel();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(new BigDecimal("10.00"));
        producto.setPrecio(new BigDecimal(precio));
        producto.setStockMinimo(new BigDecimal("1.00"));
        producto.setVendible(vendible);
        producto.setEsStockDirecto(true);
        producto.setUnidadActual(idUnidad);
        producto.setIdTipo(idTipo);
        producto.setIdCategoria(idCategoria);
        producto.setIdProductoPadre(padre);
        producto.setFecC(OffsetDateTime.now());
        producto.setBorrado(false);
        productoJpaRepository.save(producto);
        productos.add(id);
        return id;
    }

    /** Un lote de compra con una existencia abierta de 10 unidades por producto. */
    private void sembrarStock() {
        idProveedor = UUID.randomUUID();
        ProveedorJpaModel proveedor = new ProveedorJpaModel();
        proveedor.setId(idProveedor);
        proveedor.setIdPersona(idPersona);
        proveedor.setFecC(OffsetDateTime.now());
        proveedor.setBorrado(false);
        proveedorJpaRepository.save(proveedor);

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

        idEstadoExistencia = UUID.randomUUID();
        EstadoExistenciaJpaModel estado = new EstadoExistenciaJpaModel();
        estado.setId(idEstadoExistencia);
        estado.setNombre("ESTADO_EXISTENCIA_TEST_" + idEstadoExistencia);
        estado.setFecC(OffsetDateTime.now());
        estado.setBorrado(false);
        estadoExistenciaJpaRepository.save(estado);

        idExistenciaCafe = abrirExistencia(idProductoCafe);
        idExistenciaTe = abrirExistencia(idProductoTe);
    }

    private UUID abrirExistencia(UUID idProducto) {
        UUID idDetalle = UUID.randomUUID();
        DetalleLoteJpaModel detalle = new DetalleLoteJpaModel();
        detalle.setId(idDetalle);
        detalle.setIdLote(idLote);
        detalle.setIdProducto(idProducto);
        detalle.setCantidadInicial(new BigDecimal("1.00"));
        detalle.setCantidadRestante(new BigDecimal("0.00"));
        detalle.setPrecioUnitario(new BigDecimal("50.00"));
        detalle.setFraccionable(false);
        detalle.setFecC(OffsetDateTime.now());
        detalle.setBorrado(false);
        detalleLoteJpaRepository.save(detalle);
        detallesLote.add(idDetalle);

        UUID idExistencia = UUID.randomUUID();
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
        return idExistencia;
    }

    private UUID tipoDescuento(String nombre) {
        return tipoDescuentoJpaRepository.findAll().stream()
                .filter(tipo -> nombre.equalsIgnoreCase(tipo.getNombre()) && !tipo.getBorrado())
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private void guardarDescuento(UUID idProducto, String tipo, String valor, LocalDateTime inicio, LocalDateTime fin, boolean activo) {
        UUID id = UUID.randomUUID();
        DescuentoJpaModel descuento = new DescuentoJpaModel();
        descuento.setId(id);
        descuento.setIdProducto(idProducto);
        descuento.setIdTipoDescuento(tipoDescuento(tipo));
        descuento.setDescuento(new BigDecimal(valor));
        descuento.setFechaInicio(inicio);
        descuento.setFechaFinal(fin);
        descuento.setEstado(activo);
        descuento.setFecC(OffsetDateTime.now());
        descuento.setBorrado(false);
        descuentoJpaRepository.save(descuento);
        descuentos.add(id);
    }

    @AfterEach
    void limpiarDatos() {
        // La limpieza de la prueba si borra fisicamente: es basura de test, no datos de negocio.
        List<UUID> existencias = existenciaJpaRepository.findAll().stream()
                .filter(existencia -> detallesLote.contains(existencia.getIdDetalleLote()))
                .map(ExistenciaJpaModel::getId)
                .toList();
        movimientoJpaRepository.deleteAll(movimientoJpaRepository.findAll().stream()
                .filter(movimiento -> existencias.contains(movimiento.getIdExistencia()))
                .toList());
        existenciaJpaRepository.deleteAllById(existencias);
        detalleLoteJpaRepository.deleteAllById(detallesLote);
        loteJpaRepository.deleteById(idLote);
        proveedorJpaRepository.deleteById(idProveedor);
        estadoExistenciaJpaRepository.deleteById(idEstadoExistencia);

        for (UUID idVenta : ventasCreadas) {
            detalleVentaJpaRepository.deleteAll(detalleVentaJpaRepository.findByIdVentaAndBorradoFalse(idVenta));
            ventaJpaRepository.deleteById(idVenta);
        }
        descuentoJpaRepository.deleteAllById(descuentos);
        cajaDePrueba.retirarCaja(idSesionCaja);

        List<UUID> enOrden = new ArrayList<>(productos);
        java.util.Collections.reverse(enOrden);
        productoJpaRepository.deleteAllById(enOrden);
        tipoJpaRepository.deleteById(idTipo);
        categoriaJpaRepository.deleteById(idCategoria);
        unidadJpaRepository.deleteById(idUnidad);
        clienteJpaRepository.deleteById(idCliente);
        usuarioJpaRepository.deleteById(idUsuario);
        personaJpaRepository.deleteById(idPersona);
        rolJpaRepository.deleteById(idRol);

        productos.clear();
        detallesLote.clear();
        descuentos.clear();
        ventasCreadas.clear();
    }

    private Map<String, Object> linea(UUID idProducto, String cantidad) {
        return Map.of("idProducto", idProducto.toString(), "cantidad", cantidad);
    }

    private Map<String, Object> payloadCon(List<Object> detalles, String total) {
        return Map.of("idUsuario", idUsuario.toString(), "detalles", detalles, "pagos", cajaDePrueba.enEfectivo(total));
    }

    /** Vende y paga en efectivo 'total', que el test calcula a mano: el sistema exige que cuadre. */
    private VentaResponse vender(List<Object> detalles, String total) {
        VentaResponse respuesta = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(payloadCon(detalles, total), idUsuario.toString()));
        ventasCreadas.add(UUID.fromString(respuesta.getId()));
        return respuesta;
    }

    private VentaResponse anular(String idVenta, boolean reintegrar) {
        return anularVentaUseCase.execute(idVenta, AnularVentaRequest.createFromRequest(Map.of(
                "idUsuario", idUsuario.toString(), "motivo", "Prueba de anulacion", "reintegrar", reintegrar), idUsuario.toString()));
    }

    private BigDecimal contenido(UUID idExistencia) {
        return existenciaJpaRepository.findById(idExistencia).orElseThrow().getContenidoActual();
    }

    private long movimientos() {
        List<UUID> existencias = existenciaJpaRepository.findAll().stream()
                .filter(existencia -> detallesLote.contains(existencia.getIdDetalleLote()))
                .map(ExistenciaJpaModel::getId)
                .toList();
        return movimientoJpaRepository.findAll().stream()
                .filter(movimiento -> existencias.contains(movimiento.getIdExistencia()))
                .count();
    }

    // ------------------------------------------------------------------ VEN-01, VEN-13, VEN-21, VEN-23

    @Test
    void registraLaVentaCompletadaConPrecioDeListaYDescuentaSuInventario() {
        VentaResponse venta = vender(List.of(linea(idProductoCafe, "2"), linea(idProductoTe, "3")), "49.00");

        assertEquals("Completada", venta.getEstadoVenta().getNombre());
        // 2 x 12.50 + 3 x 8.00, sin descuentos vigentes.
        assertEquals(new BigDecimal("49.00"), venta.getTotal());
        // Costo: 5.00 por unidad (50.00 / 10) x 5 unidades.
        assertEquals(0, new BigDecimal("25").compareTo(venta.getCostoTotal()));
        assertEquals(0, new BigDecimal("8").compareTo(contenido(idExistenciaCafe)));
        assertEquals(0, new BigDecimal("7").compareTo(contenido(idExistenciaTe)));
        assertEquals(0, new BigDecimal("8").compareTo(productoJpaRepository.findById(idProductoCafe).orElseThrow().getStock()));

        VentaResponse releida = obtenerVentaPorIdUseCase.execute(venta.getId());
        assertEquals(2, releida.getDetalles().size());
        assertEquals(0, new BigDecimal("25").compareTo(releida.getCostoTotal()));
    }

    // ------------------------------------------------------------------ VEN-14

    @Test
    void aplicaSoloElDescuentoVigenteMasFavorable() {
        LocalDateTime ahora = LocalDateTime.now();
        guardarDescuento(idProductoCafe, "Porcentaje", "10", ahora.minusDays(1), ahora.plusDays(1), true);
        guardarDescuento(idProductoCafe, "Monto fijo", "2.00", ahora.minusDays(1), ahora.plusDays(1), true);
        guardarDescuento(idProductoCafe, "Porcentaje", "50", ahora.minusDays(10), ahora.minusDays(5), true);
        guardarDescuento(idProductoCafe, "Porcentaje", "80", ahora.minusDays(1), ahora.plusDays(1), false);

        VentaResponse venta = vender(List.of(linea(idProductoCafe, "2")), "21.00");

        // 10 % de 25.00 = 2.50; 2.00 por unidad = 4.00. El vencido y el inactivo no cuentan.
        assertEquals(new BigDecimal("4.00"), venta.getDetalles().get(0).getDescuentoAplicado());
        assertEquals(new BigDecimal("21.00"), venta.getTotal());
    }

    // ------------------------------------------------------------------ VEN-02, VEN-10..13

    @Test
    void rechazaQueElPuntoDeVentaEnviePrecioDescuentoOEstado() {
        DomainException conPrecio = assertThrows(DomainException.class, () -> CrearVentaRequest.createFromRequest(payloadCon(List.of(
                Map.of("idProducto", idProductoCafe.toString(), "cantidad", "1", "precioUnitario", "1.00")), "12.50"), idUsuario.toString()));
        DomainException conDescuento = assertThrows(DomainException.class, () -> CrearVentaRequest.createFromRequest(payloadCon(List.of(
                Map.of("idProducto", idProductoCafe.toString(), "cantidad", "1", "descuentoAplicado", "1.00")), "12.50"), idUsuario.toString()));
        DomainException conEstado = assertThrows(DomainException.class, () -> CrearVentaRequest.createFromRequest(Map.of(
                "idUsuario", idUsuario.toString(), "idEstadoVenta", UUID.randomUUID().toString(),
                "detalles", List.of(linea(idProductoCafe, "1"))), idUsuario.toString()));

        assertEquals(400, conPrecio.getCodigo());
        assertEquals(400, conDescuento.getCodigo());
        assertEquals(400, conEstado.getCodigo());
    }

    @Test
    void noSeVendeUnProductoNoVendibleNiUnaPresentacion() {
        UUID noVendible = guardarProducto("insumo-test", "1.00", false, null);
        UUID presentacion = guardarProducto("cafe-caja-test", "100.00", true, idProductoCafe);
        long ventasAntes = ventaJpaRepository.count();

        DomainException alNoVendible = assertThrows(DomainException.class, () -> vender(List.of(linea(noVendible, "1")), "1.00"));
        DomainException alaPresentacion = assertThrows(DomainException.class, () -> vender(List.of(linea(presentacion, "1")), "100.00"));

        assertEquals(400, alNoVendible.getCodigo());
        assertEquals(400, alaPresentacion.getCodigo());
        assertEquals(ventasAntes, ventaJpaRepository.count());
    }

    @Test
    void revierteLaVentaCompletaSiUnProductoDelDetalleNoExiste() {
        long ventasAntes = ventaJpaRepository.count();

        DomainException error = assertThrows(DomainException.class, () -> vender(List.of(
                linea(idProductoCafe, "2"),
                Map.of("idProducto", PRODUCTO_INEXISTENTE, "cantidad", "1")), "25.00"));

        assertEquals(404, error.getCodigo());
        assertEquals(ventasAntes, ventaJpaRepository.count());
        assertEquals(0, new BigDecimal("10").compareTo(contenido(idExistenciaCafe)));
    }

    // ------------------------------------------------------------------ VEN-22

    @Test
    void revierteLaVentaCompletaSiNoHayStockSuficiente() {
        long ventasAntes = ventaJpaRepository.count();

        DomainException error = assertThrows(DomainException.class, () -> vender(List.of(
                linea(idProductoCafe, "2"),
                linea(idProductoTe, "11")), "113.00"));

        assertEquals(409, error.getCodigo());
        assertEquals(ventasAntes, ventaJpaRepository.count());
        // El cafe se habia descontado antes de fallar el te: el rollback lo devuelve.
        assertEquals(0, new BigDecimal("10").compareTo(contenido(idExistenciaCafe)));
    }

    @Test
    void rechazaUnaVentaSinDetalles() {
        DomainException error = assertThrows(DomainException.class,
                () -> CrearVentaRequest.createFromRequest(payloadCon(List.of(), "1.00"), idUsuario.toString()));

        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("detalles"));
    }

    // ------------------------------------------------------------------ VEN-30..33

    @Test
    void unaVentaRegistradaSoloAdmiteAsociarleElCliente() {
        VentaResponse venta = vender(List.of(linea(idProductoCafe, "2")), "25.00");

        DomainException conLineas = assertThrows(DomainException.class,
                () -> ActualizarVentaRequest.createFromRequest(Map.of("detalles", List.of(linea(idProductoCafe, "5")))));
        assertEquals(400, conLineas.getCodigo());

        VentaResponse conCliente = actualizarVentaUseCase.execute(venta.getId(),
                ActualizarVentaRequest.createFromRequest(Map.of("idCliente", idCliente.toString())));

        assertEquals(idCliente.toString(), conCliente.getIdCliente());
        assertEquals(new BigDecimal("25.00"), conCliente.getTotal());
    }

    @Test
    void unaVentaNoSeDaDeBaja() {
        VentaResponse venta = vender(List.of(linea(idProductoCafe, "2")), "25.00");

        DomainException error = assertThrows(DomainException.class, () -> eliminarVentaUseCase.execute(venta.getId()));

        assertEquals(409, error.getCodigo());
        assertTrue(ventaJpaRepository.findByIdAndBorradoFalse(UUID.fromString(venta.getId())).isPresent());
    }

    // ------------------------------------------------------------------ VEN-40..43

    @Test
    void anularSinReintegroNoTocaElInventario() {
        VentaResponse venta = vender(List.of(linea(idProductoCafe, "2")), "25.00");

        VentaResponse anulada = anular(venta.getId(), false);

        assertEquals("Anulada", anulada.getEstadoVenta().getNombre());
        assertEquals("Prueba de anulacion", anulada.getMotivoAnulacion());
        assertEquals(idUsuario.toString(), anulada.getIdUsuarioAnulacion());
        assertEquals(0, new BigDecimal("8").compareTo(contenido(idExistenciaCafe)));
        assertEquals(1, movimientos());
    }

    @Test
    void anularConReintegroDevuelveLoDescontadoConCompensatorios() {
        VentaResponse venta = vender(List.of(linea(idProductoCafe, "2"), linea(idProductoTe, "3")), "49.00");

        anular(venta.getId(), true);

        assertEquals(0, new BigDecimal("10").compareTo(contenido(idExistenciaCafe)));
        assertEquals(0, new BigDecimal("10").compareTo(contenido(idExistenciaTe)));
        assertEquals(4, movimientos());
        assertEquals(0, new BigDecimal("10").compareTo(productoJpaRepository.findById(idProductoTe).orElseThrow().getStock()));

        DomainException otraVez = assertThrows(DomainException.class, () -> anular(venta.getId(), true));
        assertEquals(409, otraVez.getCodigo());
    }

    @Test
    void elReintegroDeUnaExistenciaAgotadaAbreUnaNueva() {
        VentaResponse venta = vender(List.of(linea(idProductoTe, "10")), "80.00");
        assertEquals(0, contenido(idExistenciaTe).signum());

        anular(venta.getId(), true);

        // La agotada no se revive (INV-05): el reintegro abre otra sobre el mismo detalle.
        assertEquals(0, contenido(idExistenciaTe).signum());
        UUID idDetalleTe = existenciaJpaRepository.findById(idExistenciaTe).orElseThrow().getIdDetalleLote();
        List<ExistenciaJpaModel> nuevas = existenciaJpaRepository.findAll().stream()
                .filter(existencia -> idDetalleTe.equals(existencia.getIdDetalleLote()))
                .filter(existencia -> !existencia.getId().equals(idExistenciaTe))
                .toList();
        assertEquals(1, nuevas.size());
        assertEquals(0, new BigDecimal("10").compareTo(nuevas.get(0).getContenidoActual()));
        assertEquals(0, new BigDecimal("10").compareTo(productoJpaRepository.findById(idProductoTe).orElseThrow().getStock()));
    }

    // ------------------------------------------------------------------ VEN-66

    @Test
    void unReintentoConLaMismaClaveNoCobraDosVeces() {
        Map<String, Object> payload = new HashMap<>(payloadCon(List.of(linea(idProductoCafe, "2")), "25.00"));
        payload.put("claveIdempotencia", "pos-1-" + UUID.randomUUID());

        VentaResponse primera = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(payload, idUsuario.toString()));
        ventasCreadas.add(UUID.fromString(primera.getId()));
        VentaResponse reintento = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(payload, idUsuario.toString()));

        assertEquals(primera.getId(), reintento.getId());
        assertEquals(0, new BigDecimal("8").compareTo(contenido(idExistenciaCafe)));
        List<MovimientoJpaModel> movimientos = movimientoJpaRepository.findAll().stream()
                .filter(movimiento -> movimiento.getIdExistencia().equals(idExistenciaCafe))
                .toList();
        assertEquals(1, movimientos.size());
    }

    // ------------------------------------------------------------------ CAJ-01, CAJ-02, CAJ-10..CAJ-14

    @Test
    void sinClienteLaVentaEsDelMostradorYGuardaSusPagosMixtosConCambio() {
        Map<String, Object> payload = Map.of(
                "detalles", List.of(linea(idProductoCafe, "2")),
                "pagos", List.of(
                        Map.of("idTipoPago", cajaDePrueba.idTipoPago("Efectivo").toString(), "monto", "15.00", "montoRecibido", "20"),
                        Map.of("idTipoPago", cajaDePrueba.idTipoPago("Tarjeta").toString(), "monto", "10.00", "referencia", "VISA-1")));

        VentaResponse venta = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(payload, idUsuario.toString()));
        ventasCreadas.add(UUID.fromString(venta.getId()));

        assertEquals(Cliente.ID_MOSTRADOR, venta.getIdCliente());
        assertTrue(venta.getNumeroTicket() != null && venta.getNumeroTicket() > 0);
        assertTrue(venta.getIdSesionCaja() != null);
        assertEquals(2, venta.getPagos().size());
        assertEquals(new BigDecimal("5.00"), venta.getCambio());

        VentaResponse releida = obtenerVentaPorIdUseCase.execute(venta.getId());
        assertEquals(2, releida.getPagos().size());
        assertEquals(venta.getNumeroTicket(), releida.getNumeroTicket());
    }

    @Test
    void unosPagosQueNoCuadranNoRegistranLaVentaNiTocanElInventario() {
        long ventasAntes = ventaJpaRepository.count();

        DomainException error = assertThrows(DomainException.class,
                () -> vender(List.of(linea(idProductoCafe, "2")), "20.00"));

        assertEquals(400, error.getCodigo());
        assertEquals(ventasAntes, ventaJpaRepository.count());
        assertEquals(0, new BigDecimal("10").compareTo(contenido(idExistenciaCafe)));
    }
}
