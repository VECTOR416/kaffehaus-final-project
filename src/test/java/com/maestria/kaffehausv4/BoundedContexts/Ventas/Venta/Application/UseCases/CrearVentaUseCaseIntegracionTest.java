package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa.EstadoVentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa.EstadoVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ActualizarVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas de integracion del alta de ventas contra la base real.
 *
 * No se anota @Transactional a proposito: cada caso de uso abre y cierra su propia
 * transaccion, que es justo lo que se quiere observar. Envolver la prueba en una
 * transaccion externa haria que el rollback del caso de uso fuera indistinguible del
 * rollback de la prueba.
 */
@SpringBootTest
class CrearVentaUseCaseIntegracionTest {

    @Autowired private CrearVentaUseCase crearVentaUseCase;
    @Autowired private ActualizarVentaUseCase actualizarVentaUseCase;
    @Autowired private ObtenerVentaPorIdUseCase obtenerVentaPorIdUseCase;
    @Autowired private EliminarVentaUseCase eliminarVentaUseCase;

    @Autowired private VentaJpaRepository ventaJpaRepository;
    @Autowired private DetalleVentaJpaRepository detalleVentaJpaRepository;
    @Autowired private ProductoJpaRepository productoJpaRepository;
    @Autowired private CategoriaJpaRepository categoriaJpaRepository;
    @Autowired private UnidadJpaRepository unidadJpaRepository;
    @Autowired private UsuarioJpaRepository usuarioJpaRepository;
    @Autowired private PersonaJpaRepository personaJpaRepository;
    @Autowired private RolJpaRepository rolJpaRepository;
    @Autowired private EstadoVentaJpaRepository estadoVentaJpaRepository;

    private static final String PRODUCTO_INEXISTENTE = "00000000-0000-0000-0000-0000000000ff";

    private UUID idRol;
    private UUID idPersona;
    private UUID idUsuario;
    private UUID idEstadoVenta;
    private UUID idUnidad;
    private UUID idCategoria;
    private UUID idProductoCafe;
    private UUID idProductoTe;

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

        idEstadoVenta = UUID.randomUUID();
        EstadoVentaJpaModel estadoVenta = new EstadoVentaJpaModel();
        estadoVenta.setId(idEstadoVenta);
        estadoVenta.setNombre("ESTADO_TEST_" + idEstadoVenta);
        estadoVenta.setFecC(OffsetDateTime.now());
        estadoVenta.setBorrado(false);
        estadoVentaJpaRepository.save(estadoVenta);

        idUnidad = UUID.randomUUID();
        UnidadJpaModel unidad = new UnidadJpaModel();
        unidad.setId(idUnidad);
        unidad.setUnidad("unidad-test");
        unidad.setAbreviatura("ut");
        unidad.setEsBorrable(true);
        unidad.setFecC(OffsetDateTime.now());
        unidad.setBorrado(false);
        unidadJpaRepository.save(unidad);

        idCategoria = UUID.randomUUID();
        CategoriaJpaModel categoria = new CategoriaJpaModel();
        categoria.setId(idCategoria);
        categoria.setNombre("categoria-test");
        categoria.setFecC(OffsetDateTime.now());
        categoria.setBorrado(false);
        categoriaJpaRepository.save(categoria);

        idProductoCafe = guardarProducto("cafe-test", new BigDecimal("12.50"));
        idProductoTe = guardarProducto("te-test", new BigDecimal("8.00"));
    }

    private UUID guardarProducto(String nombre, BigDecimal precio) {
        UUID id = UUID.randomUUID();
        ProductoJpaModel producto = new ProductoJpaModel();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(new BigDecimal("100.00"));
        producto.setPrecio(precio);
        producto.setStockMinimo(new BigDecimal("1.00"));
        producto.setVendible(true);
        producto.setUnidadActual(idUnidad);
        producto.setIdCategoria(idCategoria);
        producto.setFecC(OffsetDateTime.now());
        producto.setBorrado(false);
        productoJpaRepository.save(producto);
        return id;
    }

    @AfterEach
    void limpiarDatos() {
        // La limpieza de la prueba si borra fisicamente: es basura de test, no datos de negocio.
        for (UUID idVenta : ventasCreadas) {
            detalleVentaJpaRepository.deleteAll(detalleVentaJpaRepository.findAll().stream()
                    .filter(detalle -> idVenta.equals(detalle.getIdVenta()))
                    .toList());
            ventaJpaRepository.deleteById(idVenta);
        }
        ventasCreadas.clear();

        productoJpaRepository.deleteById(idProductoCafe);
        productoJpaRepository.deleteById(idProductoTe);
        categoriaJpaRepository.deleteById(idCategoria);
        unidadJpaRepository.deleteById(idUnidad);
        estadoVentaJpaRepository.deleteById(idEstadoVenta);
        usuarioJpaRepository.deleteById(idUsuario);
        personaJpaRepository.deleteById(idPersona);
        rolJpaRepository.deleteById(idRol);
    }

    private Map<String, Object> detalle(UUID idProducto, String precio, String cantidad, String descuento) {
        return Map.of(
                "idProducto", idProducto.toString(),
                "precioUnitario", precio,
                "cantidad", cantidad,
                "descuentoAplicado", descuento);
    }

    private Map<String, Object> payloadCon(List<Object> detalles) {
        return Map.of(
                "idUsuario", idUsuario.toString(),
                "idEstadoVenta", idEstadoVenta.toString(),
                "detalles", detalles);
    }

    @Test
    void persisteLaVentaConTodosSusDetallesEnUnaSolaOperacion() {
        VentaResponse respuesta = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(payloadCon(List.of(
                detalle(idProductoCafe, "12.50", "2", "0"),
                detalle(idProductoTe, "8.00", "3", "4.00")))));

        ventasCreadas.add(UUID.fromString(respuesta.getId()));

        assertEquals(2, respuesta.getDetalles().size());
        assertEquals(new BigDecimal("45.00"), respuesta.getTotal());

        // Se relee desde la base para comprobar que el agregado quedo completo, no solo en memoria.
        VentaResponse releida = obtenerVentaPorIdUseCase.execute(respuesta.getId());
        assertEquals(2, releida.getDetalles().size());
        assertEquals(new BigDecimal("45.00"), releida.getTotal());
        assertEquals(2, detalleVentaJpaRepository.findByIdVentaAndBorradoFalse(UUID.fromString(respuesta.getId())).size());
    }

    @Test
    void revierteLaVentaCompletaSiUnProductoDelDetalleNoExiste() {
        long ventasAntes = ventaJpaRepository.count();
        long detallesAntes = detalleVentaJpaRepository.count();

        CrearVentaRequest request = CrearVentaRequest.createFromRequest(payloadCon(List.of(
                detalle(idProductoCafe, "12.50", "2", "0"),
                Map.of("idProducto", PRODUCTO_INEXISTENTE, "precioUnitario", "5.00", "cantidad", "1"))));

        DomainException error = assertThrows(DomainException.class, () -> crearVentaUseCase.execute(request));

        assertEquals(404, error.getCodigo());
        assertEquals(ventasAntes, ventaJpaRepository.count());
        assertEquals(detallesAntes, detalleVentaJpaRepository.count());
    }

    @Test
    void revierteLaVentaCompletaSiUnDetalleViolaUnaInvarianteDelAgregado() {
        long ventasAntes = ventaJpaRepository.count();
        long detallesAntes = detalleVentaJpaRepository.count();

        CrearVentaRequest request = CrearVentaRequest.createFromRequest(payloadCon(List.of(
                detalle(idProductoCafe, "12.50", "2", "0"),
                detalle(idProductoTe, "8.00", "0", "0"))));

        DomainException error = assertThrows(DomainException.class, () -> crearVentaUseCase.execute(request));

        assertEquals(400, error.getCodigo());
        assertEquals(ventasAntes, ventaJpaRepository.count());
        assertEquals(detallesAntes, detalleVentaJpaRepository.count());
    }

    @Test
    void rechazaUnaVentaSinDetalles() {
        Map<String, Object> payload = Map.of(
                "idUsuario", idUsuario.toString(),
                "idEstadoVenta", idEstadoVenta.toString(),
                "detalles", List.of());

        DomainException error = assertThrows(DomainException.class,
                () -> CrearVentaRequest.createFromRequest(payload));

        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("detalles"));
    }

    @Test
    void actualizarConservaLosIdentificadoresDeLasLineasQueSiguenVivas() {
        VentaResponse creada = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(payloadCon(List.of(
                detalle(idProductoCafe, "12.50", "2", "0"),
                detalle(idProductoTe, "8.00", "3", "4.00")))));

        UUID idVenta = UUID.fromString(creada.getId());
        ventasCreadas.add(idVenta);
        String idDetalleCafe = creada.getDetalles().get(0).getId();

        Map<String, Object> correccion = new HashMap<>();
        correccion.put("id", idDetalleCafe);
        correccion.put("cantidad", "3");

        VentaResponse actualizada = actualizarVentaUseCase.execute(
                creada.getId(),
                ActualizarVentaRequest.createFromRequest(Map.of("detalles", List.of(correccion))));

        // La linea de te no vino en el payload: baja logica. La de cafe conserva su id.
        assertEquals(1, actualizada.getDetalles().size());
        assertEquals(idDetalleCafe, actualizada.getDetalles().get(0).getId());
        assertEquals(new BigDecimal("37.50"), actualizada.getTotal());
        assertEquals(1, detalleVentaJpaRepository.findByIdVentaAndBorradoFalse(idVenta).size());
    }

    @Test
    void eliminarLaVentaEsBajaLogicaYArrastraSusLineas() {
        VentaResponse creada = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(payloadCon(List.of(
                detalle(idProductoCafe, "12.50", "2", "0")))));

        UUID idVenta = UUID.fromString(creada.getId());
        ventasCreadas.add(idVenta);

        eliminarVentaUseCase.execute(creada.getId());

        assertTrue(ventaJpaRepository.findByIdAndBorradoFalse(idVenta).isEmpty());
        assertTrue(detalleVentaJpaRepository.findByIdVentaAndBorradoFalse(idVenta).isEmpty());
        // Baja logica: las filas siguen existiendo.
        assertTrue(ventaJpaRepository.findById(idVenta).isPresent());
    }
}
