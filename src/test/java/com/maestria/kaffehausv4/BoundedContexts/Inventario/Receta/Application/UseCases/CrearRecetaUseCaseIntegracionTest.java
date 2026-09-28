package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

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
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa.CategoriaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa.CategoriaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa.TipoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ActualizarRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.CrearRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.PasoRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.IngredienteRecetaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.PasoRecetaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.RecetaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa.ProductoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa.UnidadJpaRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas de integracion del agregado Receta contra la base real.
 *
 * No se anota @Transactional a proposito: cada caso de uso abre y cierra su propia
 * transaccion, que es justo lo que se quiere observar. Envolver la prueba en una transaccion
 * externa haria que el rollback del caso de uso fuera indistinguible del de la prueba.
 */
@SpringBootTest
class CrearRecetaUseCaseIntegracionTest {

    @Autowired private CrearRecetaUseCase crearRecetaUseCase;
    @Autowired private ActualizarRecetaUseCase actualizarRecetaUseCase;
    @Autowired private ObtenerRecetaPorIdUseCase obtenerRecetaPorIdUseCase;
    @Autowired private EliminarRecetaUseCase eliminarRecetaUseCase;

    @Autowired private RecetaJpaRepository recetaJpaRepository;
    @Autowired private IngredienteRecetaJpaRepository ingredienteRecetaJpaRepository;
    @Autowired private PasoRecetaJpaRepository pasoRecetaJpaRepository;
    @Autowired private ProductoJpaRepository productoJpaRepository;
    @Autowired private TipoJpaRepository tipoJpaRepository;
    @Autowired private CategoriaJpaRepository categoriaJpaRepository;
    @Autowired private UnidadJpaRepository unidadJpaRepository;

    private static final String PRODUCTO_INEXISTENTE = "00000000-0000-0000-0000-0000000000ff";

    private UUID idUnidad;
    private UUID idTipo;
    private UUID idCategoria;
    private UUID idProductoPan;
    private UUID idHarina;
    private UUID idAgua;

    private final List<UUID> recetasCreadas = new ArrayList<>();

    @BeforeEach
    void sembrarDatos() {
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

        idProductoPan = guardarProducto("pan-test");
        idHarina = guardarProducto("harina-test");
        idAgua = guardarProducto("agua-test");
    }

    private UUID guardarProducto(String nombre) {
        UUID id = UUID.randomUUID();
        ProductoJpaModel producto = new ProductoJpaModel();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(new BigDecimal("100.00"));
        producto.setPrecio(new BigDecimal("5.00"));
        producto.setStockMinimo(new BigDecimal("1.00"));
        producto.setVendible(true);
        producto.setEsStockDirecto(true);
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
        // La limpieza de la prueba si borra fisicamente: es basura de test, no datos de negocio.
        for (UUID idReceta : recetasCreadas) {
            ingredienteRecetaJpaRepository.deleteAll(ingredienteRecetaJpaRepository.findAll().stream()
                    .filter(ingrediente -> idReceta.equals(ingrediente.getIdReceta()))
                    .toList());
            pasoRecetaJpaRepository.deleteAll(pasoRecetaJpaRepository.findAll().stream()
                    .filter(paso -> idReceta.equals(paso.getIdReceta()))
                    .toList());
            recetaJpaRepository.deleteById(idReceta);
        }
        recetasCreadas.clear();

        productoJpaRepository.deleteById(idProductoPan);
        productoJpaRepository.deleteById(idHarina);
        productoJpaRepository.deleteById(idAgua);
        tipoJpaRepository.deleteById(idTipo);
        categoriaJpaRepository.deleteById(idCategoria);
        unidadJpaRepository.deleteById(idUnidad);
    }

    private Map<String, Object> ingrediente(UUID idProducto, String cantidad) {
        return Map.of("idProducto", idProducto.toString(), "cantidad", cantidad);
    }

    private Map<String, Object> paso(String descripcion) {
        return Map.of("descripcion", descripcion);
    }

    private Map<String, Object> payloadCon(List<Object> ingredientes, List<Object> pasos) {
        return Map.of(
                "cantidad", "10",
                "titulo", "Pan casero",
                "idProductoObtenido", idProductoPan.toString(),
                "ingredientes", ingredientes,
                "pasos", pasos);
    }

    private RecetaResponse crearRecetaCompleta() {
        RecetaResponse respuesta = crearRecetaUseCase.execute(CrearRecetaRequest.createFromRequest(payloadCon(
                List.of(ingrediente(idHarina, "1.5"), ingrediente(idAgua, "0.75")),
                List.of(paso("Mezclar"), paso("Amasar"), paso("Hornear")))));
        recetasCreadas.add(UUID.fromString(respuesta.getId()));
        return respuesta;
    }

    private List<String> descripcionesEnOrden(RecetaResponse receta) {
        return receta.getPasos().stream().map(PasoRecetaResponse::getDescripcion).collect(Collectors.toList());
    }

    @Test
    void persisteLaRecetaConSusIngredientesYPasosEnUnaSolaOperacion() {
        RecetaResponse respuesta = crearRecetaCompleta();
        UUID idReceta = UUID.fromString(respuesta.getId());

        assertEquals(2, respuesta.getIngredientes().size());
        assertEquals(3, respuesta.getPasos().size());
        assertEquals(List.of("Mezclar", "Amasar", "Hornear"), descripcionesEnOrden(respuesta));

        // Se relee desde la base para comprobar que el agregado quedo completo, no solo en memoria.
        RecetaResponse releida = obtenerRecetaPorIdUseCase.execute(respuesta.getId());
        assertEquals(2, releida.getIngredientes().size());
        assertEquals(List.of(1, 2, 3), releida.getPasos().stream().map(PasoRecetaResponse::getPasos).collect(Collectors.toList()));
        assertEquals(2, ingredienteRecetaJpaRepository.findByIdRecetaAndBorradoFalse(idReceta).size());
        assertEquals(3, pasoRecetaJpaRepository.findByIdRecetaAndBorradoFalseOrderByPasosAsc(idReceta).size());
    }

    @Test
    void revierteLaRecetaCompletaSiUnIngredienteApuntaAUnProductoInexistente() {
        long recetasAntes = recetaJpaRepository.count();
        long ingredientesAntes = ingredienteRecetaJpaRepository.count();
        long pasosAntes = pasoRecetaJpaRepository.count();

        CrearRecetaRequest request = CrearRecetaRequest.createFromRequest(payloadCon(
                List.of(ingrediente(idHarina, "1.5"), Map.of("idProducto", PRODUCTO_INEXISTENTE, "cantidad", "1")),
                List.of(paso("Mezclar"))));

        DomainException error = assertThrows(DomainException.class, () -> crearRecetaUseCase.execute(request));

        assertEquals(404, error.getCodigo());
        assertEquals(recetasAntes, recetaJpaRepository.count());
        assertEquals(ingredientesAntes, ingredienteRecetaJpaRepository.count());
        assertEquals(pasosAntes, pasoRecetaJpaRepository.count());
    }

    @Test
    void revierteLaRecetaCompletaSiUnPasoViolaUnaInvariante() {
        long recetasAntes = recetaJpaRepository.count();
        long pasosAntes = pasoRecetaJpaRepository.count();

        Map<String, Object> payload = new HashMap<>(payloadCon(
                List.of(ingrediente(idHarina, "1.5")),
                List.of(paso("Mezclar"))));
        // Un paso sin descripcion: lo rechaza el dominio, no la base.
        payload.put("pasos", List.of(paso("Mezclar"), Map.of("descripcion", "   ")));

        DomainException error = assertThrows(DomainException.class,
                () -> crearRecetaUseCase.execute(CrearRecetaRequest.createFromRequest(payload)));

        assertEquals(400, error.getCodigo());
        assertEquals(recetasAntes, recetaJpaRepository.count());
        assertEquals(pasosAntes, pasoRecetaJpaRepository.count());
    }

    @Test
    void revierteLaRecetaCompletaSiSeRepiteUnIngrediente() {
        long recetasAntes = recetaJpaRepository.count();
        long ingredientesAntes = ingredienteRecetaJpaRepository.count();

        CrearRecetaRequest request = CrearRecetaRequest.createFromRequest(payloadCon(
                List.of(ingrediente(idHarina, "1.5"), ingrediente(idHarina, "2")),
                List.of(paso("Mezclar"))));

        DomainException error = assertThrows(DomainException.class, () -> crearRecetaUseCase.execute(request));

        assertEquals(400, error.getCodigo());
        assertEquals(recetasAntes, recetaJpaRepository.count());
        assertEquals(ingredientesAntes, ingredienteRecetaJpaRepository.count());
    }

    @Test
    void rechazaUnaRecetaSinIngredientesOSinPasos() {
        Map<String, Object> sinIngredientes = new HashMap<>(payloadCon(List.of(ingrediente(idHarina, "1")), List.of(paso("Mezclar"))));
        sinIngredientes.put("ingredientes", List.of());

        DomainException error = assertThrows(DomainException.class,
                () -> CrearRecetaRequest.createFromRequest(sinIngredientes));

        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("ingredientes"));
    }

    @Test
    void actualizarReordenaLosPasosSinChocarConElIndiceUnico() {
        RecetaResponse creada = crearRecetaCompleta();
        UUID idReceta = UUID.fromString(creada.getId());
        String idMezclar = creada.getPasos().get(0).getId();
        String idAmasar = creada.getPasos().get(1).getId();
        String idHornear = creada.getPasos().get(2).getId();

        // Rotacion completa: (1,2,3) -> (3,1,2). Sin la renumeracion en dos fases, el UPDATE
        // chocaria con uq_receta_paso a medio camino.
        RecetaResponse actualizada = actualizarRecetaUseCase.execute(
                creada.getId(),
                ActualizarRecetaRequest.createFromRequest(Map.of("pasos", List.of(
                        Map.of("id", idHornear),
                        Map.of("id", idMezclar),
                        Map.of("id", idAmasar)))));

        assertEquals(List.of("Hornear", "Mezclar", "Amasar"), descripcionesEnOrden(actualizada));
        assertEquals(List.of(1, 2, 3), actualizada.getPasos().stream().map(PasoRecetaResponse::getPasos).collect(Collectors.toList()));
        assertEquals(3, pasoRecetaJpaRepository.findByIdRecetaAndBorradoFalseOrderByPasosAsc(idReceta).size());
    }

    @Test
    void actualizarDaDeBajaUnPasoYReocupaSuNumeroConUnoNuevo() {
        RecetaResponse creada = crearRecetaCompleta();
        UUID idReceta = UUID.fromString(creada.getId());
        String idMezclar = creada.getPasos().get(0).getId();

        // Se conserva solo el primero y se agrega uno nuevo: el nuevo pasa a ocupar el numero 2,
        // que quedo libre al dar de baja 'Amasar'. Con el indice unico total esto fallaria.
        RecetaResponse actualizada = actualizarRecetaUseCase.execute(
                creada.getId(),
                ActualizarRecetaRequest.createFromRequest(Map.of("pasos", List.of(
                        Map.of("id", idMezclar),
                        Map.of("descripcion", "Reposar")))));

        assertEquals(List.of("Mezclar", "Reposar"), descripcionesEnOrden(actualizada));
        assertEquals(List.of(1, 2), actualizada.getPasos().stream().map(PasoRecetaResponse::getPasos).collect(Collectors.toList()));
        assertEquals(2, pasoRecetaJpaRepository.findByIdRecetaAndBorradoFalseOrderByPasosAsc(idReceta).size());
        // Los dos pasos dados de baja siguen en la tabla.
        assertEquals(4, pasoRecetaJpaRepository.findAll().stream()
                .filter(paso -> idReceta.equals(paso.getIdReceta()))
                .count());
    }

    @Test
    void actualizarConservaLosIdentificadoresDeLosIngredientesQueSiguenVivos() {
        RecetaResponse creada = crearRecetaCompleta();
        String idIngredienteHarina = creada.getIngredientes().get(0).getId();

        Map<String, Object> correccion = new HashMap<>();
        correccion.put("id", idIngredienteHarina);
        correccion.put("cantidad", "3");

        RecetaResponse actualizada = actualizarRecetaUseCase.execute(
                creada.getId(),
                ActualizarRecetaRequest.createFromRequest(Map.of("ingredientes", List.of(correccion))));

        assertEquals(1, actualizada.getIngredientes().size());
        assertEquals(idIngredienteHarina, actualizada.getIngredientes().get(0).getId());
        assertEquals(new BigDecimal("3.0000"), actualizada.getIngredientes().get(0).getCantidad());
        // Los pasos no se mencionaron en el payload: quedan intactos.
        assertEquals(3, actualizada.getPasos().size());
    }

    @Test
    void eliminarLaRecetaEsBajaLogicaYArrastraAmbasColecciones() {
        RecetaResponse creada = crearRecetaCompleta();
        UUID idReceta = UUID.fromString(creada.getId());

        eliminarRecetaUseCase.execute(creada.getId());

        assertTrue(recetaJpaRepository.findByIdAndBorradoFalse(idReceta).isEmpty());
        assertTrue(ingredienteRecetaJpaRepository.findByIdRecetaAndBorradoFalse(idReceta).isEmpty());
        assertTrue(pasoRecetaJpaRepository.findByIdRecetaAndBorradoFalseOrderByPasosAsc(idReceta).isEmpty());
        // Baja logica: las filas siguen existiendo.
        assertTrue(recetaJpaRepository.findById(idReceta).isPresent());
    }
}
