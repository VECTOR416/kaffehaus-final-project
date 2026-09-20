package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa.EstadoExistenciaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa.EstadoExistenciaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa.ExistenciaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa.ExistenciaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ActualizarLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.CrearLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DetalleLoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.DetalleLoteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa.LoteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaRepository;
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
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas de integracion del agregado Lote contra la base real.
 *
 * No se anota @Transactional a proposito: cada caso de uso abre y cierra su propia
 * transaccion, que es justo lo que se quiere observar. Envolver la prueba en una transaccion
 * externa haria que el rollback del caso de uso fuera indistinguible del de la prueba.
 */
@SpringBootTest
class CrearLoteUseCaseIntegracionTest {

    @Autowired private CrearLoteUseCase crearLoteUseCase;
    @Autowired private ActualizarLoteUseCase actualizarLoteUseCase;
    @Autowired private ObtenerLotePorIdUseCase obtenerLotePorIdUseCase;
    @Autowired private EliminarLoteUseCase eliminarLoteUseCase;

    @Autowired private LoteJpaRepository loteJpaRepository;
    @Autowired private DetalleLoteJpaRepository detalleLoteJpaRepository;
    @Autowired private ExistenciaJpaRepository existenciaJpaRepository;
    @Autowired private EstadoExistenciaJpaRepository estadoExistenciaJpaRepository;
    @Autowired private ProductoJpaRepository productoJpaRepository;
    @Autowired private TipoJpaRepository tipoJpaRepository;
    @Autowired private CategoriaJpaRepository categoriaJpaRepository;
    @Autowired private UnidadJpaRepository unidadJpaRepository;
    @Autowired private ProveedorJpaRepository proveedorJpaRepository;
    @Autowired private UsuarioJpaRepository usuarioJpaRepository;
    @Autowired private PersonaJpaRepository personaJpaRepository;
    @Autowired private RolJpaRepository rolJpaRepository;

    private static final String PRODUCTO_INEXISTENTE = "00000000-0000-0000-0000-0000000000ff";

    private UUID idRol;
    private UUID idPersona;
    private UUID idUsuario;
    private UUID idProveedor;
    private UUID idUnidad;
    private UUID idTipo;
    private UUID idCategoria;
    private UUID idEstadoExistencia;
    private UUID idProductoCafe;
    private UUID idProductoAzucar;

    private final List<UUID> lotesCreados = new ArrayList<>();
    private final List<UUID> existenciasCreadas = new ArrayList<>();

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
        EstadoExistenciaJpaModel estadoExistencia = new EstadoExistenciaJpaModel();
        estadoExistencia.setId(idEstadoExistencia);
        estadoExistencia.setNombre("ESTADO_TEST_" + idEstadoExistencia);
        estadoExistencia.setFecC(OffsetDateTime.now());
        estadoExistencia.setBorrado(false);
        estadoExistenciaJpaRepository.save(estadoExistencia);

        idProductoCafe = guardarProducto("cafe-test");
        idProductoAzucar = guardarProducto("azucar-test");
    }

    private UUID guardarProducto(String nombre) {
        UUID id = UUID.randomUUID();
        ProductoJpaModel producto = new ProductoJpaModel();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(new BigDecimal("100.00"));
        producto.setPrecio(new BigDecimal("15.00"));
        producto.setStockMinimo(new BigDecimal("1.00"));
        producto.setVendible(true);
        producto.setUnidadActual(idUnidad);
        producto.setIdTipo(idTipo);
        producto.setIdCategoria(idCategoria);
        producto.setFecC(OffsetDateTime.now());
        producto.setBorrado(false);
        productoJpaRepository.save(producto);
        return id;
    }

    @AfterEach
    void limpiarDatos() {
        existenciasCreadas.forEach(existenciaJpaRepository::deleteById);
        existenciasCreadas.clear();

        for (UUID idLote : lotesCreados) {
            detalleLoteJpaRepository.deleteAll(detalleLoteJpaRepository.findAll().stream()
                    .filter(detalle -> idLote.equals(detalle.getIdLote()))
                    .toList());
            loteJpaRepository.deleteById(idLote);
        }
        lotesCreados.clear();

        estadoExistenciaJpaRepository.deleteById(idEstadoExistencia);
        productoJpaRepository.deleteById(idProductoCafe);
        productoJpaRepository.deleteById(idProductoAzucar);
        tipoJpaRepository.deleteById(idTipo);
        categoriaJpaRepository.deleteById(idCategoria);
        unidadJpaRepository.deleteById(idUnidad);
        proveedorJpaRepository.deleteById(idProveedor);
        usuarioJpaRepository.deleteById(idUsuario);
        personaJpaRepository.deleteById(idPersona);
        rolJpaRepository.deleteById(idRol);
    }

    private Map<String, Object> detalle(UUID idProducto, String cantidadInicial, String precioUnitario) {
        return Map.of(
                "idProducto", idProducto.toString(),
                "cantidadInicial", cantidadInicial,
                "precioUnitario", precioUnitario);
    }

    private Map<String, Object> payloadCon(List<Object> detalles) {
        return Map.of(
                "idProveedor", idProveedor.toString(),
                "idUsuario", idUsuario.toString(),
                "detalles", detalles);
    }

    private LoteResponse crearLoteDeDosLineas() {
        LoteResponse respuesta = crearLoteUseCase.execute(CrearLoteRequest.createFromRequest(payloadCon(List.of(
                detalle(idProductoCafe, "10", "12.00"),
                detalle(idProductoAzucar, "8", "5.00")))));
        lotesCreados.add(UUID.fromString(respuesta.getId()));
        return respuesta;
    }

    private UUID crearExistenciaSobre(String idDetalleLote) {
        UUID id = UUID.randomUUID();
        ExistenciaJpaModel existencia = new ExistenciaJpaModel();
        existencia.setId(id);
        existencia.setIdDetalleLote(UUID.fromString(idDetalleLote));
        existencia.setContenidoInicial(new BigDecimal("10.0000"));
        existencia.setContenidoActual(new BigDecimal("10.0000"));
        existencia.setIdEstadoExistencia(idEstadoExistencia);
        existencia.setUnidad(idUnidad);
        existencia.setFecC(OffsetDateTime.now());
        existencia.setBorrado(false);
        existenciaJpaRepository.save(existencia);
        existenciasCreadas.add(id);
        return id;
    }

    @Test
    void persisteElLoteConTodosSusDetallesEnUnaSolaOperacion() {
        LoteResponse respuesta = crearLoteDeDosLineas();

        assertEquals(2, respuesta.getDetalles().size());
        assertEquals(new BigDecimal("160.00"), respuesta.getTotal());

        // Se relee desde la base para comprobar que el agregado quedo completo, no solo en memoria.
        LoteResponse releido = obtenerLotePorIdUseCase.execute(respuesta.getId());
        assertEquals(2, releido.getDetalles().size());
        assertEquals(new BigDecimal("160.00"), releido.getTotal());
        assertEquals(2, detalleLoteJpaRepository.findByIdLoteAndBorradoFalse(UUID.fromString(respuesta.getId())).size());
    }

    @Test
    void laCantidadRestantePersistidaNaceIgualALaComprada() {
        LoteResponse respuesta = crearLoteDeDosLineas();

        DetalleLoteResponse cafe = respuesta.getDetalles().get(0);
        assertEquals(cafe.getCantidadInicial(), cafe.getCantidadRestante());
    }

    @Test
    void revierteElLoteCompletoSiUnProductoDelDetalleNoExiste() {
        long lotesAntes = loteJpaRepository.count();
        long detallesAntes = detalleLoteJpaRepository.count();

        CrearLoteRequest request = CrearLoteRequest.createFromRequest(payloadCon(List.of(
                detalle(idProductoCafe, "10", "12.00"),
                Map.of("idProducto", PRODUCTO_INEXISTENTE, "cantidadInicial", "5", "precioUnitario", "3.00"))));

        DomainException error = assertThrows(DomainException.class, () -> crearLoteUseCase.execute(request));

        assertEquals(404, error.getCodigo());
        assertEquals(lotesAntes, loteJpaRepository.count());
        assertEquals(detallesAntes, detalleLoteJpaRepository.count());
    }

    @Test
    void revierteElLoteCompletoSiUnDetalleViolaUnaInvarianteDelAgregado() {
        long lotesAntes = loteJpaRepository.count();
        long detallesAntes = detalleLoteJpaRepository.count();

        CrearLoteRequest request = CrearLoteRequest.createFromRequest(payloadCon(List.of(
                detalle(idProductoCafe, "10", "12.00"),
                detalle(idProductoAzucar, "0", "5.00"))));

        DomainException error = assertThrows(DomainException.class, () -> crearLoteUseCase.execute(request));

        assertEquals(400, error.getCodigo());
        assertEquals(lotesAntes, loteJpaRepository.count());
        assertEquals(detallesAntes, detalleLoteJpaRepository.count());
    }

    @Test
    void rechazaUnLoteSinDetalles() {
        Map<String, Object> payload = Map.of(
                "idProveedor", idProveedor.toString(),
                "idUsuario", idUsuario.toString(),
                "detalles", List.of());

        DomainException error = assertThrows(DomainException.class,
                () -> CrearLoteRequest.createFromRequest(payload));

        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("detalles"));
    }

    @Test
    void actualizarConservaLosIdentificadoresDeLasLineasYNoRompeLasExistencias() {
        LoteResponse creado = crearLoteDeDosLineas();
        String idDetalleCafe = creado.getDetalles().get(0).getId();
        String idDetalleAzucar = creado.getDetalles().get(1).getId();

        // Una existencia cuelga de la linea de cafe: la FK es ON DELETE RESTRICT.
        UUID idExistencia = crearExistenciaSobre(idDetalleCafe);

        Map<String, Object> ajusteCafe = new HashMap<>();
        ajusteCafe.put("id", idDetalleCafe);
        ajusteCafe.put("cantidadRestante", "4");

        Map<String, Object> ajusteAzucar = new HashMap<>();
        ajusteAzucar.put("id", idDetalleAzucar);
        ajusteAzucar.put("precioUnitario", "6.00");

        LoteResponse actualizado = actualizarLoteUseCase.execute(
                creado.getId(),
                ActualizarLoteRequest.createFromRequest(Map.of("detalles", List.of(ajusteCafe, ajusteAzucar))));

        assertEquals(2, actualizado.getDetalles().size());
        assertEquals(idDetalleCafe, actualizado.getDetalles().get(0).getId());
        assertEquals(new BigDecimal("4.00"), actualizado.getDetalles().get(0).getCantidadRestante());
        // El costo de compra no cambia por consumir stock, pero si por corregir un precio.
        assertEquals(new BigDecimal("168.00"), actualizado.getTotal());
        assertTrue(existenciaJpaRepository.findById(idExistencia).isPresent());
    }

    @Test
    void actualizarDaDeBajaLogicaLasLineasAusentesYAgregaLasNuevas() {
        LoteResponse creado = crearLoteDeDosLineas();
        String idDetalleCafe = creado.getDetalles().get(0).getId();
        UUID idLote = UUID.fromString(creado.getId());

        Map<String, Object> conservado = new HashMap<>();
        conservado.put("id", idDetalleCafe);

        LoteResponse actualizado = actualizarLoteUseCase.execute(
                creado.getId(),
                ActualizarLoteRequest.createFromRequest(Map.of("detalles", List.of(
                        conservado,
                        detalle(idProductoAzucar, "2", "7.00")))));

        assertEquals(2, actualizado.getDetalles().size());
        assertEquals(new BigDecimal("134.00"), actualizado.getTotal());

        // La linea ausente no se borro de la tabla: sigue ahi marcada, protegiendo su FK.
        assertEquals(2, detalleLoteJpaRepository.findByIdLoteAndBorradoFalse(idLote).size());
        assertEquals(3, detalleLoteJpaRepository.findAll().stream()
                .filter(detalle -> idLote.equals(detalle.getIdLote()))
                .count());
    }

    @Test
    void eliminarElLoteDaDeBajaTambienSusLineas() {
        LoteResponse creado = crearLoteDeDosLineas();
        UUID idLote = UUID.fromString(creado.getId());

        eliminarLoteUseCase.execute(creado.getId());

        assertTrue(detalleLoteJpaRepository.findByIdLoteAndBorradoFalse(idLote).isEmpty());
        assertTrue(loteJpaRepository.findByIdAndBorradoFalse(idLote).isEmpty());
        // Baja logica: las filas siguen existiendo.
        assertEquals(2, detalleLoteJpaRepository.findAll().stream()
                .filter(detalle -> idLote.equals(detalle.getIdLote()))
                .count());
    }
}
