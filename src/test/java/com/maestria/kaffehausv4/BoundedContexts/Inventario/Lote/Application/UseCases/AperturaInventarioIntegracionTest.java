package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Persistence.Jpa.ConversionJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Persistence.Jpa.ConversionJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa.EstadoExistenciaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ActualizarExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.AjustarExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.CrearExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarMermaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.AjustarExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.CrearExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.EliminarExistenciaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.RegistrarMermaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa.ExistenciaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa.ExistenciaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.AbrirDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DarDeBajaDetalleVencidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DetalleAperturableResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerDetallesAperturablesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.DetalleLoteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.DetalleLoteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa.MovimientoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa.MovimientoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.CrearProductoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.InventarioProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.CrearProductoUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.ObtenerInventarioProductoUseCase;
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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Infrastructure.Persistence.Jpa.TipoMovimientoJpaRepository;
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
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.CajaDePrueba;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases.CrearVentaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.DetalleVentaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa.VentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas de integracion de la apertura de empaques y del libro mayor (inventario.md, secciones
 * 2 y 4) contra la base real.
 *
 * Escenario: la harina (producto base, en g) llega en dos presentaciones, bolsa de 5 kg y bolsa
 * de 2 kg, con cuatro lineas de compra:
 *   D1  bolsa 5 kg  2 cerradas  vence en 60 dias
 *   D2  bolsa 5 kg  1 cerrada   vence en 30 dias   <- la que toca abrir de 5 kg
 *   D3  bolsa 2 kg  3 cerradas  vence en 45 dias   <- la que toca abrir de 2 kg
 *   D4  bolsa 2 kg  1 cerrada   vencida hace 10 dias
 *
 * Requiere el catalogo que siembra POST api/v1/seed; CajaDePrueba lo ejecuta antes de cada prueba. Sin @Transactional de clase.
 */
@SpringBootTest
class AperturaInventarioIntegracionTest {

    @Autowired private ObtenerDetallesAperturablesUseCase obtenerDetallesAperturablesUseCase;
    @Autowired private AbrirDetalleLoteUseCase abrirDetalleLoteUseCase;
    @Autowired private DarDeBajaDetalleVencidoUseCase darDeBajaDetalleVencidoUseCase;
    @Autowired private EliminarLoteUseCase eliminarLoteUseCase;
    @Autowired private RegistrarMermaUseCase registrarMermaUseCase;
    @Autowired private AjustarExistenciaUseCase ajustarExistenciaUseCase;
    @Autowired private CrearExistenciaUseCase crearExistenciaUseCase;
    @Autowired private EliminarExistenciaUseCase eliminarExistenciaUseCase;
    @Autowired private ObtenerInventarioProductoUseCase obtenerInventarioProductoUseCase;
    @Autowired private CrearProductoUseCase crearProductoUseCase;
    @Autowired private CrearVentaUseCase crearVentaUseCase;
    @Autowired private CajaDePrueba cajaDePrueba;

    @Autowired private RolJpaRepository rolJpaRepository;
    @Autowired private PersonaJpaRepository personaJpaRepository;
    @Autowired private UsuarioJpaRepository usuarioJpaRepository;
    @Autowired private ProveedorJpaRepository proveedorJpaRepository;
    @Autowired private UnidadJpaRepository unidadJpaRepository;
    @Autowired private ConversionJpaRepository conversionJpaRepository;
    @Autowired private TipoJpaRepository tipoJpaRepository;
    @Autowired private CategoriaJpaRepository categoriaJpaRepository;
    @Autowired private ProductoJpaRepository productoJpaRepository;
    @Autowired private RecetaJpaRepository recetaJpaRepository;
    @Autowired private IngredienteRecetaJpaRepository ingredienteRecetaJpaRepository;
    @Autowired private PasoRecetaJpaRepository pasoRecetaJpaRepository;
    @Autowired private LoteJpaRepository loteJpaRepository;
    @Autowired private DetalleLoteJpaRepository detalleLoteJpaRepository;
    @Autowired private ExistenciaJpaRepository existenciaJpaRepository;
    @Autowired private EstadoExistenciaJpaRepository estadoExistenciaJpaRepository;
    @Autowired private MovimientoJpaRepository movimientoJpaRepository;
    @Autowired private TipoMovimientoJpaRepository tipoMovimientoJpaRepository;
    @Autowired private EstadoVentaJpaRepository estadoVentaJpaRepository;
    @Autowired private VentaJpaRepository ventaJpaRepository;
    @Autowired private DetalleVentaJpaRepository detalleVentaJpaRepository;


    private final LocalDate hoy = LocalDate.now();

    private UUID idRol;
    private UUID idPersona;
    private UUID idUsuario;
    private UUID idSesionCaja;
    private UUID idProveedor;
    private UUID idTipo;
    private UUID idCategoria;
    private UUID idEstadoVenta;
    private UUID idLote;
    private UUID idReceta;

    private UUID unidadG;
    private UUID unidadKg;
    private UUID unidadBolsa5;
    private UUID unidadBolsa2;

    private UUID idHarina;
    private UUID idBolsa5;
    private UUID idBolsa2;

    private UUID d1;
    private UUID d2;
    private UUID d3;
    private UUID d4;

    private final List<UUID> unidades = new ArrayList<>();
    private final List<UUID> conversiones = new ArrayList<>();
    private final List<UUID> productos = new ArrayList<>();
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

        idEstadoVenta = UUID.randomUUID();
        EstadoVentaJpaModel estadoVenta = new EstadoVentaJpaModel();
        estadoVenta.setId(idEstadoVenta);
        estadoVenta.setNombre("ESTADO_TEST_" + idEstadoVenta);
        estadoVenta.setFecC(OffsetDateTime.now());
        estadoVenta.setBorrado(false);
        estadoVentaJpaRepository.save(estadoVenta);

        unidadG = guardarUnidad("g-test");
        unidadKg = guardarUnidad("kg-test");
        unidadBolsa5 = guardarUnidad("bolsa-5kg-test");
        unidadBolsa2 = guardarUnidad("bolsa-2kg-test");
        guardarConversion(unidadBolsa5, unidadKg, "5");
        guardarConversion(unidadBolsa2, unidadKg, "2");
        guardarConversion(unidadKg, unidadG, "1000");

        idHarina = guardarProducto("harina-test", unidadG, null, true);
        idBolsa5 = guardarProducto("harina-bolsa-5kg-test", unidadBolsa5, idHarina, true);
        idBolsa2 = guardarProducto("harina-bolsa-2kg-test", unidadBolsa2, idHarina, true);

        idLote = UUID.randomUUID();
        LoteJpaModel lote = new LoteJpaModel();
        lote.setId(idLote);
        lote.setFechaCompra(LocalDateTime.now().minusDays(20));
        lote.setTotal(new BigDecimal("0.00"));
        lote.setIdProveedor(idProveedor);
        lote.setIdUsuario(idUsuario);
        lote.setEsProduccion(false);
        lote.setFecC(OffsetDateTime.now());
        lote.setBorrado(false);
        loteJpaRepository.save(lote);

        d1 = guardarDetalle(idBolsa5, "2", "50.00", hoy.plusDays(60));
        d2 = guardarDetalle(idBolsa5, "1", "50.00", hoy.plusDays(30));
        d3 = guardarDetalle(idBolsa2, "3", "24.00", hoy.plusDays(45));
        d4 = guardarDetalle(idBolsa2, "1", "24.00", hoy.minusDays(10));
        idSesionCaja = cajaDePrueba.prepararCaja(idUsuario);
    }

    private UUID guardarUnidad(String nombre) {
        UUID id = UUID.randomUUID();
        UnidadJpaModel unidad = new UnidadJpaModel();
        unidad.setId(id);
        unidad.setUnidad(nombre);
        unidad.setAbreviatura(nombre.substring(0, 2));
        unidad.setEsBorrable(true);
        unidad.setFecC(OffsetDateTime.now());
        unidad.setBorrado(false);
        unidadJpaRepository.save(unidad);
        unidades.add(id);
        return id;
    }

    private void guardarConversion(UUID origen, UUID destino, String factor) {
        UUID id = UUID.randomUUID();
        ConversionJpaModel conversion = new ConversionJpaModel();
        conversion.setId(id);
        conversion.setIdUnidadOrigen(origen);
        conversion.setIdUnidadDestino(destino);
        conversion.setFactorConversion(new BigDecimal(factor));
        conversion.setEsBorrable(true);
        conversion.setFecC(OffsetDateTime.now());
        conversion.setBorrado(false);
        conversionJpaRepository.save(conversion);
        conversiones.add(id);
    }

    private UUID guardarProducto(String nombre, UUID unidad, UUID padre, boolean esStockDirecto) {
        UUID id = UUID.randomUUID();
        ProductoJpaModel producto = new ProductoJpaModel();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(new BigDecimal("0.00"));
        producto.setPrecio(new BigDecimal("10.00"));
        producto.setStockMinimo(new BigDecimal("0.00"));
        producto.setVendible(true);
        producto.setEsStockDirecto(esStockDirecto);
        producto.setUnidadActual(unidad);
        producto.setIdTipo(idTipo);
        producto.setIdCategoria(idCategoria);
        producto.setIdProductoPadre(padre);
        producto.setFecC(OffsetDateTime.now());
        producto.setBorrado(false);
        productoJpaRepository.save(producto);
        productos.add(id);
        return id;
    }

    private UUID guardarDetalle(UUID idProducto, String cerradas, String precio, LocalDate vence) {
        UUID id = UUID.randomUUID();
        DetalleLoteJpaModel detalle = new DetalleLoteJpaModel();
        detalle.setId(id);
        detalle.setIdLote(idLote);
        detalle.setIdProducto(idProducto);
        detalle.setCantidadInicial(new BigDecimal(cerradas));
        detalle.setCantidadRestante(new BigDecimal(cerradas));
        detalle.setPrecioUnitario(new BigDecimal(precio));
        detalle.setFraccionable(true);
        detalle.setFechaVencimiento(vence);
        detalle.setFecC(OffsetDateTime.now());
        detalle.setBorrado(false);
        detalleLoteJpaRepository.save(detalle);
        return id;
    }

    @AfterEach
    void limpiarDatos() {
        // La limpieza de la prueba si borra fisicamente: es basura de test, no datos de negocio.
        List<UUID> detalles = List.of(d1, d2, d3, d4);
        List<UUID> existencias = existenciaJpaRepository.findAll().stream()
                .filter(existencia -> detalles.contains(existencia.getIdDetalleLote()))
                .map(ExistenciaJpaModel::getId)
                .toList();
        movimientoJpaRepository.deleteAll(movimientoJpaRepository.findAll().stream()
                .filter(movimiento -> existencias.contains(movimiento.getIdExistencia()))
                .toList());
        existenciaJpaRepository.deleteAllById(existencias);
        detalleLoteJpaRepository.deleteAllById(detalles);
        loteJpaRepository.deleteById(idLote);

        for (UUID idVenta : ventas) {
            detalleVentaJpaRepository.deleteAll(detalleVentaJpaRepository.findByIdVentaAndBorradoFalse(idVenta));
            ventaJpaRepository.deleteById(idVenta);
        }
        cajaDePrueba.retirarCaja(idSesionCaja);
        if (idReceta != null) {
            ingredienteRecetaJpaRepository.deleteAll(ingredienteRecetaJpaRepository.findByIdRecetaAndBorradoFalse(idReceta));
            pasoRecetaJpaRepository.deleteAll(pasoRecetaJpaRepository.findByIdRecetaAndBorradoFalseOrderByPasosAsc(idReceta));
            recetaJpaRepository.deleteById(idReceta);
        }

        // Las presentaciones antes que su producto base.
        List<UUID> enOrden = new ArrayList<>(productos);
        java.util.Collections.reverse(enOrden);
        productoJpaRepository.deleteAllById(enOrden);
        conversionJpaRepository.deleteAllById(conversiones);
        unidadJpaRepository.deleteAllById(unidades);
        proveedorJpaRepository.deleteById(idProveedor);
        estadoVentaJpaRepository.deleteById(idEstadoVenta);
        categoriaJpaRepository.deleteById(idCategoria);
        tipoJpaRepository.deleteById(idTipo);
        usuarioJpaRepository.deleteById(idUsuario);
        personaJpaRepository.deleteById(idPersona);
        rolJpaRepository.deleteById(idRol);

        unidades.clear();
        conversiones.clear();
        productos.clear();
        ventas.clear();
        idReceta = null;
    }

    private ExistenciaResponse abrir(UUID idDetalle) {
        return abrirDetalleLoteUseCase.execute(idDetalle.toString(),
                AbrirDetalleLoteRequest.createFromRequest(Map.of("idUsuario", idUsuario.toString()), idUsuario.toString()));
    }

    private VentaResponse venderHarina(String cantidad) {
        VentaResponse venta = crearVentaUseCase.execute(CrearVentaRequest.createFromRequest(Map.of(
                "idUsuario", idUsuario.toString(),
                "detalles", List.of(Map.of("idProducto", idHarina.toString(), "cantidad", cantidad)),
                "pagos", cajaDePrueba.enEfectivo(aPagar("10.00", cantidad))), idUsuario.toString()));
        ventas.add(UUID.fromString(venta.getId()));
        return venta;
    }

    private BigDecimal stockDeHarina() {
        return productoJpaRepository.findById(idHarina).orElseThrow().getStock();
    }

    private String nombreDeTipo(MovimientoJpaModel movimiento) {
        return tipoMovimientoJpaRepository.findById(movimiento.getIdTipoMovimiento()).orElseThrow().getNombre();
    }

    private List<MovimientoJpaModel> movimientosDe(String idExistencia) {
        UUID id = UUID.fromString(idExistencia);
        return movimientoJpaRepository.findAll().stream().filter(movimiento -> id.equals(movimiento.getIdExistencia())).toList();
    }

    // ------------------------------------------------------------------ INV-13

    @Test
    void listaUnaFilaPorPresentacionConElDetalleMasProximoAVencer() {
        List<DetalleAperturableResponse> filas = obtenerDetallesAperturablesUseCase.execute(
                ObtenerDetallesAperturablesRequest.createFromRequest(Map.of("idProducto", idHarina.toString())));

        assertEquals(2, filas.size());
        // Primero la bolsa de 5 kg, cuyo detalle D2 vence antes que el D3 de 2 kg.
        assertEquals(d2.toString(), filas.get(0).getIdDetalleLote());
        assertEquals(0, new BigDecimal("5000").compareTo(filas.get(0).getContenidoPorUnidad()));
        assertEquals(0, new BigDecimal("3").compareTo(filas.get(0).getUnidadesCerradasTotales()));
        assertEquals(d3.toString(), filas.get(1).getIdDetalleLote());
        assertEquals(0, new BigDecimal("2000").compareTo(filas.get(1).getContenidoPorUnidad()));
        // El D4 vencido no cuenta.
        assertEquals(0, new BigDecimal("3").compareTo(filas.get(1).getUnidadesCerradasTotales()));
        assertEquals(0, new BigDecimal("0.012").compareTo(filas.get(1).getCostoPorUnidadDeContenido()));
    }

    // ------------------------------------------------------------------ INV-14 / INV-10

    @Test
    void rechazaAbrirUnDetalleQueNoEsElMasProximoAVencerDeSuPresentacion() {
        DomainException error = assertThrows(DomainException.class, () -> abrir(d1));

        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains(d2.toString()));
        assertEquals(0, new BigDecimal("2").compareTo(detalleLoteJpaRepository.findById(d1).orElseThrow().getCantidadRestante()));
    }

    @Test
    void abrirConvierteLaUnidadALaUnidadMinimaDelProductoBase() {
        ExistenciaResponse abierta = abrir(d3);

        assertEquals(0, new BigDecimal("2000").compareTo(abierta.getContenidoInicial()));
        assertEquals(unidadG.toString(), abierta.getUnidad().getId());
        assertEquals("Abierta", abierta.getEstadoExistencia().getNombre());
        assertEquals(0, new BigDecimal("2").compareTo(detalleLoteJpaRepository.findById(d3).orElseThrow().getCantidadRestante()));

        List<MovimientoJpaModel> movimientos = movimientosDe(abierta.getId());
        assertEquals(1, movimientos.size());
        assertEquals("Ajuste", nombreDeTipo(movimientos.get(0)));
        assertEquals(0, new BigDecimal("2000").compareTo(movimientos.get(0).getCantidad()));
        assertEquals(0, new BigDecimal("2000").compareTo(stockDeHarina()));
    }

    @Test
    void noSeAbreUnDetalleVencido() {
        DomainException error = assertThrows(DomainException.class, () -> abrir(d4));

        assertEquals(409, error.getCodigo());
    }

    // ------------------------------------------------------------------ INV-34

    @Test
    void laVentaNoAbreEmpaquesYPideAbrirUnaPresentacion() {
        DomainException error = assertThrows(DomainException.class, () -> venderHarina("100"));

        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("abre una presentacion"));
        assertTrue(error.getMensaje().contains(idHarina.toString()));

        ExistenciaResponse abierta = abrir(d2);
        venderHarina("100");

        assertEquals(0, new BigDecimal("4900").compareTo(existenciaJpaRepository.findById(UUID.fromString(abierta.getId())).orElseThrow().getContenidoActual()));
        assertEquals(0, new BigDecimal("4900").compareTo(stockDeHarina()));
    }

    // ------------------------------------------------------------------ INV-42 / INV-43 / INV-45

    @Test
    void mermasYAjustesQuedanEnElLibroMayorYElSaldoCuadra() {
        ExistenciaResponse abierta = abrir(d3);
        String id = abierta.getId();

        registrarMermaUseCase.execute(id, RegistrarMermaRequest.createFromRequest(Map.of(
                "cantidad", "100", "motivo", "Se derramo", "idUsuario", idUsuario.toString()), idUsuario.toString()));
        ajustarExistenciaUseCase.execute(id, AjustarExistenciaRequest.createFromRequest(Map.of(
                "contenidoContado", "1500", "motivo", "Conteo semanal", "idUsuario", idUsuario.toString()), idUsuario.toString()));
        ExistenciaResponse ajustada = ajustarExistenciaUseCase.execute(id, AjustarExistenciaRequest.createFromRequest(Map.of(
                "contenidoContado", "1800", "motivo", "Recuento", "idUsuario", idUsuario.toString()), idUsuario.toString()));

        assertEquals(0, new BigDecimal("1800").compareTo(ajustada.getContenidoActual()));
        assertEquals(0, new BigDecimal("1800").compareTo(stockDeHarina()));

        List<MovimientoJpaModel> movimientos = movimientosDe(id);
        assertEquals(List.of("Ajuste", "Merma", "Ajuste", "Ajuste"),
                movimientos.stream()
                        .sorted((a, b) -> a.getFechaMovimiento().compareTo(b.getFechaMovimiento()))
                        .map(this::nombreDeTipo)
                        .toList());

        // INV-45: el libro mayor explica el saldo: +2000 (apertura), -100 (merma), -400 y +300 (ajustes).
        assertEquals(List.of("2000", "100", "400", "300"),
                movimientos.stream()
                        .sorted((a, b) -> a.getFechaMovimiento().compareTo(b.getFechaMovimiento()))
                        .map(movimiento -> movimiento.getCantidad().stripTrailingZeros().toPlainString())
                        .toList());
        assertEquals(0, new BigDecimal("1800").compareTo(existenciaJpaRepository.findById(UUID.fromString(id)).orElseThrow().getContenidoActual()));
    }

    @Test
    void laBajaDeUnDetalleVencidoAbreSusUnidadesYLasDejaAgotadas() {
        List<ExistenciaResponse> bajas = darDeBajaDetalleVencidoUseCase.execute(d4.toString(),
                DarDeBajaDetalleVencidoRequest.createFromRequest(Map.of("idUsuario", idUsuario.toString()), idUsuario.toString()));

        assertEquals(1, bajas.size());
        assertEquals("Agotada", bajas.get(0).getEstadoExistencia().getNombre());
        assertEquals(0, detalleLoteJpaRepository.findById(d4).orElseThrow().getCantidadRestante().signum());
        assertEquals(List.of("Ajuste", "Merma"),
                movimientosDe(bajas.get(0).getId()).stream()
                        .sorted((a, b) -> a.getFechaMovimiento().compareTo(b.getFechaMovimiento()))
                        .map(this::nombreDeTipo)
                        .toList());
        assertEquals(0, stockDeHarina().signum());
    }

    // ------------------------------------------------------------------ INV-30 / INV-31 / INV-46

    @Test
    void elInventarioSeparaLoAbiertoDeLoCerradoYCalculaLaDisponibilidadMto() {
        abrir(d3);
        UUID idPan = guardarProducto("pan-test", unidadG, null, false);
        idReceta = UUID.randomUUID();
        RecetaJpaModel receta = new RecetaJpaModel();
        receta.setId(idReceta);
        receta.setIdProductoObtenido(idPan);
        receta.setCantidad(new BigDecimal("1.00"));
        receta.setTitulo("Pan de prueba");
        receta.setFecC(OffsetDateTime.now());
        receta.setBorrado(false);
        recetaJpaRepository.save(receta);
        IngredienteRecetaJpaModel ingrediente = new IngredienteRecetaJpaModel();
        ingrediente.setId(UUID.randomUUID());
        ingrediente.setIdReceta(idReceta);
        ingrediente.setIdProducto(idHarina);
        ingrediente.setCantidad(new BigDecimal("500"));
        ingrediente.setFecC(OffsetDateTime.now());
        ingrediente.setBorrado(false);
        ingredienteRecetaJpaRepository.save(ingrediente);
        PasoRecetaJpaModel paso = new PasoRecetaJpaModel();
        paso.setId(UUID.randomUUID());
        paso.setIdReceta(idReceta);
        paso.setPasos(1);
        paso.setDescripcion("Hornear");
        paso.setFecC(OffsetDateTime.now());
        paso.setBorrado(false);
        pasoRecetaJpaRepository.save(paso);

        InventarioProductoResponse harina = obtenerInventarioProductoUseCase.execute(idHarina.toString());
        // Cerrado: D1 2x5000 + D2 1x5000 + D3 2x2000 + D4 1x2000 (vencido, se cuenta pero no esta disponible).
        assertEquals(0, new BigDecimal("2000").compareTo(harina.getStockAbierto()));
        assertEquals(0, new BigDecimal("21000").compareTo(harina.getStockCerrado()));
        assertEquals(0, new BigDecimal("23000").compareTo(harina.getStockTotal()));
        assertEquals(0, new BigDecimal("2000").compareTo(harina.getDisponible()));
        assertEquals(0, new BigDecimal("21000").compareTo(harina.getDisponibleTrasAbrir()));
        assertEquals(0, new BigDecimal("24.00").compareTo(harina.getValorInventario()));

        InventarioProductoResponse pan = obtenerInventarioProductoUseCase.execute(idPan.toString());
        assertEquals(0, new BigDecimal("4").compareTo(pan.getDisponible()));
        assertEquals(0, new BigDecimal("42").compareTo(pan.getDisponibleTrasAbrir()));
    }

    // ------------------------------------------------------------------ INV-07 / INV-12 / INV-63

    @Test
    void lasExistenciasNoSeCreanNiSeCorrigenNiSeBorranAMano() {
        Map<String, Object> alta = new HashMap<>();
        alta.put("contenidoInicial", "10");
        alta.put("contenidoActual", "10");
        alta.put("idDetalleLote", d3.toString());
        alta.put("idEstadoExistencia", estadoExistenciaJpaRepository.findAll().get(0).getId().toString());
        alta.put("idUnidad", unidadG.toString());
        DomainException alCrear = assertThrows(DomainException.class,
                () -> crearExistenciaUseCase.execute(CrearExistenciaRequest.createFromRequest(alta)));
        assertEquals(409, alCrear.getCodigo());

        DomainException alCorregir = assertThrows(DomainException.class,
                () -> ActualizarExistenciaRequest.createFromRequest(Map.of("contenidoActual", "5")));
        assertEquals(400, alCorregir.getCodigo());

        ExistenciaResponse abierta = abrir(d3);
        DomainException alBorrar = assertThrows(DomainException.class, () -> eliminarExistenciaUseCase.execute(abierta.getId()));
        assertEquals(409, alBorrar.getCodigo());
    }

    @Test
    void unLoteConExistenciasEnUsoNoSeDaDeBaja() {
        abrir(d3);

        DomainException error = assertThrows(DomainException.class, () -> eliminarLoteUseCase.execute(idLote.toString()));

        assertEquals(409, error.getCodigo());
        assertTrue(loteJpaRepository.findByIdAndBorradoFalse(idLote).isPresent());
    }

    @Test
    void unaPresentacionNoPuedeSerProductoBaseDeOtra() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nombre", "harina-bolsa-de-bolsa-test");
        payload.put("precio", "1");
        payload.put("stockMinimo", "0");
        payload.put("idUnidadActual", unidadBolsa5.toString());
        payload.put("idTipo", idTipo.toString());
        payload.put("idCategoria", idCategoria.toString());
        payload.put("idProductoPadre", idBolsa5.toString());

        DomainException error = assertThrows(DomainException.class,
                () -> crearProductoUseCase.execute(CrearProductoRequest.createFromRequest(payload)));

        assertEquals(409, error.getCodigo());
    }

    /** Total que cobra el sistema: todos los productos de la prueba tienen un unico precio y ningun descuento. */
    private static String aPagar(String precio, String cantidad) {
        return new BigDecimal(precio).multiply(new BigDecimal(cantidad)).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
