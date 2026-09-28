package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.CrearProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ProduccionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases.CrearProduccionUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Infrastructure.Persistence.Jpa.ProduccionJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.IngredienteRecetaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.IngredienteRecetaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.PasoRecetaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.PasoRecetaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.RecetaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.RecetaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa.PersonaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa.ProveedorJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa.ProveedorJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa.RolJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa.UsuarioJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa.EstadoVentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa.EstadoVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.CrearVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas de integracion del flujo de inventario contra la base real: produccion MTS con su
 * lote y su existencia, venta del producto terminado y backflush de un producto MTO.
 *
 * Usa las filas de catalogo que el flujo resuelve por nombre ('Consumo produccion',
 * 'Ingreso produccion', 'Venta', 'Consumo venta', 'Abierta', 'Agotada'): deben existir, como
 * deja la seccion 7 de databasev4.sql.
 *
 * Sin @Transactional de clase, igual que el resto de pruebas de integracion, para observar el
 * rollback de cada caso de uso.
 */
@SpringBootTest
class FlujoInventarioIntegracionTest {

    @Autowired private CrearProduccionUseCase crearProduccionUseCase;
    @Autowired private CrearVentaUseCase crearVentaUseCase;

    @Autowired private RolJpaRepository rolJpaRepository;
    @Autowired private PersonaJpaRepository personaJpaRepository;
    @Autowired private UsuarioJpaRepository usuarioJpaRepository;
    @Autowired private ProveedorJpaRepository proveedorJpaRepository;
    @Autowired private UnidadJpaRepository unidadJpaRepository;
    @Autowired private TipoJpaRepository tipoJpaRepository;
    @Autowired private CategoriaJpaRepository categoriaJpaRepository;
    @Autowired private ProductoJpaRepository productoJpaRepository;
    @Autowired private RecetaJpaRepository recetaJpaRepository;
    @Autowired private IngredienteRecetaJpaRepository ingredienteRecetaJpaRepository;
    @Autowired private PasoRecetaJpaRepository pasoRecetaJpaRepository;
    @Autowired private LoteJpaRepository loteJpaRepository;
    @Autowired private DetalleLoteJpaRepository detalleLoteJpaRepository;
    @Autowired private EstadoExistenciaJpaRepository estadoExistenciaJpaRepository;
    @Autowired private ExistenciaJpaRepository existenciaJpaRepository;
    @Autowired private MovimientoJpaRepository movimientoJpaRepository;
    @Autowired private ProduccionJpaRepository produccionJpaRepository;
    @Autowired private EstadoVentaJpaRepository estadoVentaJpaRepository;
    @Autowired private VentaJpaRepository ventaJpaRepository;
    @Autowired private DetalleVentaJpaRepository detalleVentaJpaRepository;

    private UUID idRol;
    private UUID idPersona;
    private UUID idUsuario;
    private UUID idProveedor;
    private UUID idUnidad;
    private UUID idTipo;
    private UUID idCategoria;
    private UUID idEstadoExistencia;
    private UUID idEstadoVenta;
    private UUID idLoteCompra;

    private UUID idPan;
    private UUID idJamon;
    private UUID idSandwich;
    private UUID idGrano;
    private UUID idVaso;
    private UUID idAmericano;

    private UUID idRecetaSandwich;
    private UUID idRecetaAmericano;
    private UUID idRecetaDelMto;

    private UUID existenciaPan;
    private UUID existenciaJamon;
    private UUID existenciaGrano;
    private UUID existenciaVaso;

    private final List<UUID> productos = new ArrayList<>();
    private final List<UUID> detallesCompra = new ArrayList<>();
    private final List<UUID> producciones = new ArrayList<>();
    private final List<UUID> ventas = new ArrayList<>();

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

        idEstadoExistencia = UUID.randomUUID();
        EstadoExistenciaJpaModel estado = new EstadoExistenciaJpaModel();
        estado.setId(idEstadoExistencia);
        estado.setNombre("ESTADO_EXISTENCIA_TEST_" + idEstadoExistencia);
        estado.setFecC(OffsetDateTime.now());
        estado.setBorrado(false);
        estadoExistenciaJpaRepository.save(estado);

        idEstadoVenta = UUID.randomUUID();
        EstadoVentaJpaModel estadoVenta = new EstadoVentaJpaModel();
        estadoVenta.setId(idEstadoVenta);
        estadoVenta.setNombre("ESTADO_TEST_" + idEstadoVenta);
        estadoVenta.setFecC(OffsetDateTime.now());
        estadoVenta.setBorrado(false);
        estadoVentaJpaRepository.save(estadoVenta);

        idPan = guardarProducto("pan-test", true);
        idJamon = guardarProducto("jamon-test", true);
        idSandwich = guardarProducto("sandwich-test", true);
        idGrano = guardarProducto("grano-test", true);
        idVaso = guardarProducto("vaso-test", true);
        idAmericano = guardarProducto("americano-test", false);

        // Sandwich (MTS): 2 panes y 50 g de jamon por unidad.
        idRecetaSandwich = guardarReceta(idSandwich, "Sandwich de prueba",
                Map.of(idPan, new BigDecimal("2"), idJamon, new BigDecimal("50")));
        // Americano (MTO): 18 g de grano y 1 vaso por taza.
        idRecetaAmericano = guardarReceta(idAmericano, "Americano de prueba",
                Map.of(idGrano, new BigDecimal("18"), idVaso, new BigDecimal("1")));

        idLoteCompra = UUID.randomUUID();
        LoteJpaModel lote = new LoteJpaModel();
        lote.setId(idLoteCompra);
        lote.setFechaCompra(LocalDateTime.now());
        lote.setTotal(new BigDecimal("0.00"));
        lote.setIdProveedor(idProveedor);
        lote.setIdUsuario(idUsuario);
        lote.setEsProduccion(false);
        lote.setFecC(OffsetDateTime.now());
        lote.setBorrado(false);
        loteJpaRepository.save(lote);

        // Costo por unidad de contenido = precio / contenido de la unidad comprada.
        existenciaPan = abrirCompra(idPan, "10.00", "10");       // 1.00 por pan
        existenciaJamon = abrirCompra(idJamon, "50.00", "1000"); // 0.05 por gramo
        existenciaGrano = abrirCompra(idGrano, "240.00", "2000");// 0.12 por gramo
        existenciaVaso = abrirCompra(idVaso, "30.00", "100");    // 0.30 por vaso
    }

    private UUID guardarProducto(String nombre, boolean esStockDirecto) {
        UUID id = UUID.randomUUID();
        ProductoJpaModel producto = new ProductoJpaModel();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(new BigDecimal("0.00"));
        producto.setPrecio(new BigDecimal("10.00"));
        producto.setStockMinimo(new BigDecimal("0.00"));
        producto.setVendible(true);
        producto.setEsStockDirecto(esStockDirecto);
        producto.setUnidadActual(idUnidad);
        producto.setIdTipo(idTipo);
        producto.setIdCategoria(idCategoria);
        producto.setFecC(OffsetDateTime.now());
        producto.setBorrado(false);
        productoJpaRepository.save(producto);
        productos.add(id);
        return id;
    }

    private UUID guardarReceta(UUID idProductoObtenido, String titulo, Map<UUID, BigDecimal> ingredientes) {
        UUID id = UUID.randomUUID();
        RecetaJpaModel receta = new RecetaJpaModel();
        receta.setId(id);
        receta.setIdProductoObtenido(idProductoObtenido);
        receta.setCantidad(new BigDecimal("1.00"));
        receta.setTitulo(titulo);
        receta.setFecC(OffsetDateTime.now());
        receta.setBorrado(false);
        recetaJpaRepository.save(receta);

        ingredientes.forEach((idProducto, cantidad) -> {
            IngredienteRecetaJpaModel ingrediente = new IngredienteRecetaJpaModel();
            ingrediente.setId(UUID.randomUUID());
            ingrediente.setIdReceta(id);
            ingrediente.setIdProducto(idProducto);
            ingrediente.setCantidad(cantidad);
            ingrediente.setFecC(OffsetDateTime.now());
            ingrediente.setBorrado(false);
            ingredienteRecetaJpaRepository.save(ingrediente);
        });

        PasoRecetaJpaModel paso = new PasoRecetaJpaModel();
        paso.setId(UUID.randomUUID());
        paso.setIdReceta(id);
        paso.setPasos(1);
        paso.setDescripcion("Preparar");
        paso.setFecC(OffsetDateTime.now());
        paso.setBorrado(false);
        pasoRecetaJpaRepository.save(paso);

        return id;
    }

    private UUID abrirCompra(UUID idProducto, String precio, String contenido) {
        UUID idDetalle = UUID.randomUUID();
        DetalleLoteJpaModel detalle = new DetalleLoteJpaModel();
        detalle.setId(idDetalle);
        detalle.setIdLote(idLoteCompra);
        detalle.setIdProducto(idProducto);
        detalle.setCantidadInicial(new BigDecimal("1.00"));
        detalle.setCantidadRestante(new BigDecimal("0.00"));
        detalle.setPrecioUnitario(new BigDecimal(precio));
        detalle.setFraccionable(true);
        detalle.setFecC(OffsetDateTime.now());
        detalle.setBorrado(false);
        detalleLoteJpaRepository.save(detalle);
        detallesCompra.add(idDetalle);

        UUID idExistencia = UUID.randomUUID();
        ExistenciaJpaModel existencia = new ExistenciaJpaModel();
        existencia.setId(idExistencia);
        existencia.setIdDetalleLote(idDetalle);
        existencia.setContenidoInicial(new BigDecimal(contenido));
        existencia.setContenidoActual(new BigDecimal(contenido));
        existencia.setIdEstadoExistencia(idEstadoExistencia);
        existencia.setUnidad(idUnidad);
        existencia.setIdUsuario(idUsuario);
        existencia.setFecC(OffsetDateTime.now());
        existencia.setBorrado(false);
        existenciaJpaRepository.save(existencia);
        return idExistencia;
    }

    @AfterEach
    void limpiarDatos() {
        // La limpieza de la prueba si borra fisicamente: es basura de test, no datos de negocio.
        List<UUID> lotes = new ArrayList<>(List.of(idLoteCompra));
        loteJpaRepository.findAll().stream()
                .filter(lote -> lote.getIdProduccion() != null && producciones.contains(lote.getIdProduccion()))
                .forEach(lote -> lotes.add(lote.getId()));
        List<UUID> detalles = detalleLoteJpaRepository.findAll().stream()
                .filter(detalle -> lotes.contains(detalle.getIdLote()))
                .map(DetalleLoteJpaModel::getId)
                .toList();
        List<UUID> existencias = existenciaJpaRepository.findAll().stream()
                .filter(existencia -> detalles.contains(existencia.getIdDetalleLote()))
                .map(ExistenciaJpaModel::getId)
                .toList();

        movimientoJpaRepository.deleteAll(movimientoJpaRepository.findAll().stream()
                .filter(movimiento -> existencias.contains(movimiento.getIdExistencia()))
                .toList());
        existenciaJpaRepository.deleteAllById(existencias);
        detalleLoteJpaRepository.deleteAllById(detalles);
        loteJpaRepository.deleteAllById(lotes);
        produccionJpaRepository.deleteAllById(producciones);

        for (UUID idVenta : ventas) {
            detalleVentaJpaRepository.deleteAll(detalleVentaJpaRepository.findByIdVentaAndBorradoFalse(idVenta));
            ventaJpaRepository.deleteById(idVenta);
        }

        List<UUID> recetas = new ArrayList<>(List.of(idRecetaSandwich, idRecetaAmericano));
        if (idRecetaDelMto != null) {
            recetas.add(idRecetaDelMto);
        }
        for (UUID idReceta : recetas) {
            ingredienteRecetaJpaRepository.deleteAll(ingredienteRecetaJpaRepository.findByIdRecetaAndBorradoFalse(idReceta));
            pasoRecetaJpaRepository.deleteAll(pasoRecetaJpaRepository.findByIdRecetaAndBorradoFalseOrderByPasosAsc(idReceta));
            recetaJpaRepository.deleteById(idReceta);
        }

        productoJpaRepository.deleteAllById(productos);
        proveedorJpaRepository.deleteById(idProveedor);
        estadoExistenciaJpaRepository.deleteById(idEstadoExistencia);
        estadoVentaJpaRepository.deleteById(idEstadoVenta);
        categoriaJpaRepository.deleteById(idCategoria);
        tipoJpaRepository.deleteById(idTipo);
        unidadJpaRepository.deleteById(idUnidad);
        usuarioJpaRepository.deleteById(idUsuario);
        personaJpaRepository.deleteById(idPersona);
        rolJpaRepository.deleteById(idRol);

        productos.clear();
        detallesCompra.clear();
        producciones.clear();
        ventas.clear();
    }

    private ProduccionResponse producir(UUID idReceta, String esperada, String real) {
        ProduccionResponse respuesta = crearProduccionUseCase.execute(CrearProduccionRequest.createFromRequest(Map.of(
                "idReceta", idReceta.toString(),
                "cantidadEsperada", esperada,
                "cantidadReal", real,
                "fechaVencimiento", "2026-09-29",
                "idUsuario", idUsuario.toString())));
        producciones.add(UUID.fromString(respuesta.getId()));
        return respuesta;
    }

    private VentaResponse vender(UUID idProducto, String cantidad) {
        VentaResponse respuesta = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(Map.of(
                "idUsuario", idUsuario.toString(),
                "idEstadoVenta", idEstadoVenta.toString(),
                "detalles", List.of(Map.of(
                        "idProducto", idProducto.toString(),
                        "precioUnitario", "10.00",
                        "cantidad", cantidad)))));
        ventas.add(UUID.fromString(respuesta.getId()));
        return respuesta;
    }

    private BigDecimal contenido(UUID idExistencia) {
        return existenciaJpaRepository.findById(idExistencia).orElseThrow().getContenidoActual();
    }

    @Test
    void laProduccionConsumeInsumosYAbreUnLoteDeProduccionConSuExistencia() {
        ProduccionResponse produccion = producir(idRecetaSandwich, "4", "4");
        UUID idProduccion = UUID.fromString(produccion.getId());

        // Insumos: 8 panes y 200 g de jamon.
        assertEquals(0, new BigDecimal("2").compareTo(contenido(existenciaPan)));
        assertEquals(0, new BigDecimal("800").compareTo(contenido(existenciaJamon)));

        LoteJpaModel lote = loteJpaRepository.findAll().stream()
                .filter(candidato -> idProduccion.equals(candidato.getIdProduccion()))
                .findFirst()
                .orElseThrow();
        assertTrue(lote.getEsProduccion());
        assertNull(lote.getIdProveedor());

        DetalleLoteJpaModel detalle = detalleLoteJpaRepository.findByIdLoteAndBorradoFalse(lote.getId()).get(0);
        assertEquals(idSandwich, detalle.getIdProducto());
        // Costo: 8 x 1.00 + 200 x 0.05 = 18.00, entre 4 sandwiches.
        assertEquals(new BigDecimal("4.50"), detalle.getPrecioUnitario());
        assertEquals(LocalDate.of(2026, 9, 29), detalle.getFechaVencimiento());

        ExistenciaJpaModel terminado = existenciaJpaRepository.findAll().stream()
                .filter(existencia -> existencia.getIdDetalleLote().equals(detalle.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("4").compareTo(terminado.getContenidoActual()));

        List<MovimientoJpaModel> movimientos = movimientoJpaRepository.findAll().stream()
                .filter(movimiento -> idProduccion.equals(movimiento.getIdProduccion()))
                .toList();
        // Dos consumos (pan y jamon) y el ingreso del producto terminado.
        assertEquals(3, movimientos.size());
    }

    @Test
    void elProductoProducidoSeVendeDesdeSuExistenciaDeProduccion() {
        producir(idRecetaSandwich, "4", "4");

        vender(idSandwich, "3");

        ExistenciaJpaModel terminado = existenciaJpaRepository.findAll().stream()
                .filter(existencia -> !detallesCompra.contains(existencia.getIdDetalleLote()))
                .filter(existencia -> existencia.getUnidad().equals(idUnidad))
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("1").compareTo(terminado.getContenidoActual()));
    }

    @Test
    void laVentaDeUnProductoBajoDemandaDescuentaLosInsumosDeSuReceta() {
        VentaResponse venta = vender(idAmericano, "2");

        assertEquals(0, new BigDecimal("1964").compareTo(contenido(existenciaGrano)));
        assertEquals(0, new BigDecimal("98").compareTo(contenido(existenciaVaso)));

        List<MovimientoJpaModel> movimientos = movimientoJpaRepository.findAll().stream()
                .filter(movimiento -> movimiento.getMotivo() != null && movimiento.getMotivo().contains(venta.getId()))
                .toList();
        assertEquals(2, movimientos.size());
        assertTrue(movimientos.stream().allMatch(movimiento -> movimiento.getIdProduccion() == null));
    }

    @Test
    void unaExistenciaQueSeVaciaQuedaAgotada() {
        vender(idAmericano, "100");

        ExistenciaJpaModel vasos = existenciaJpaRepository.findById(existenciaVaso).orElseThrow();
        assertEquals(0, vasos.getContenidoActual().signum());
        assertTrue(vasos.getFechaAcabado() != null);
        assertEquals("Agotada", estadoExistenciaJpaRepository.findById(vasos.getIdEstadoExistencia()).orElseThrow().getNombre());
    }

    @Test
    void noSeProducePorLotesUnProductoBajoDemanda() {
        idRecetaDelMto = guardarReceta(idAmericano, "Americano por lotes", Map.of(idGrano, new BigDecimal("18")));
        long produccionesAntes = produccionJpaRepository.count();

        DomainException error = assertThrows(DomainException.class,
                () -> producir(idRecetaDelMto, "10", "10"));

        assertEquals(409, error.getCodigo());
        assertEquals(produccionesAntes, produccionJpaRepository.count());
    }

    @Test
    void revierteLaProduccionCompletaSiFaltaUnInsumo() {
        long produccionesAntes = produccionJpaRepository.count();

        // 6 sandwiches piden 12 panes y solo hay 10.
        DomainException error = assertThrows(DomainException.class,
                () -> producir(idRecetaSandwich, "6", "6"));

        assertEquals(409, error.getCodigo());
        assertEquals(produccionesAntes, produccionJpaRepository.count());
        assertEquals(0, new BigDecimal("10").compareTo(contenido(existenciaPan)));
        assertEquals(0, new BigDecimal("1000").compareTo(contenido(existenciaJamon)));
    }
}
