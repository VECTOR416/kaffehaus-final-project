package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ActualizarRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.CrearRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.PasoRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.ActualizarRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases.CrearRecetaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Soporte.EscenarioDeCafeteria;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/**
 * Persistencia de Receta contra la base: tres tablas, pasos renumerados en dos fases y
 * conciliacion por id de cada coleccion (arquitectura.md 5.13; logica.md RECETA-04, RECETA-05).
 */
class RecetaDatasourceImplIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private EscenarioDeCafeteria escenario;
    @Autowired private CrearRecetaUseCase crearRecetaUseCase;
    @Autowired private ActualizarRecetaUseCase actualizarRecetaUseCase;
    @Autowired private IRecetaRepository recetaRepository;

    private String harina;
    private String levadura;
    private String pan;

    @BeforeEach
    void prepararEscenario() {
        escenario.sembrar();
        harina = escenario.producto("Harina", "0", false, true, "Gramo", null);
        levadura = escenario.producto("Levadura", "0", false, true, "Gramo", null);
        pan = escenario.producto("Pan", "1.5", true, true, "Unidad", null);
    }

    private RecetaResponse recetaConTresPasos() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("cantidad", "10");
        datos.put("titulo", "Pan de molde");
        datos.put("idProductoObtenido", pan);
        datos.put("ingredientes", List.of(Map.of("idProducto", harina, "cantidad", "400"), Map.of("idProducto", levadura, "cantidad", "10")));
        datos.put("pasos", List.of(Map.of("descripcion", "Amasar"), Map.of("descripcion", "Leudar"), Map.of("descripcion", "Hornear")));
        return crearRecetaUseCase.execute(CrearRecetaRequest.createFromRequest(datos));
    }

    private static List<String> descripcionesEnOrden(Receta receta) {
        return receta.getPasosActivos().stream().map(paso -> paso.getDescripcion()).toList();
    }

    @Test
    void laRecetaSeGuardaConSusIngredientesYSusPasosNumerados() {
        // Act
        RecetaResponse creada = recetaConTresPasos();

        // Assert
        Receta leida = recetaRepository.obtenerPorId(new RecetaId(creada.getId())).orElseThrow();
        assertEquals(2, leida.getIngredientesActivos().size());
        assertEquals(List.of("Amasar", "Leudar", "Hornear"), descripcionesEnOrden(leida));
        assertEquals(List.of(1, 2, 3), leida.getPasosActivos().stream().map(paso -> paso.getPasos()).toList());
    }

    @Test
    void reordenarPasosExistentesYAgregarUnoNuevoSeEscribeSinChocarConLaSecuencia() {
        // Arrange
        RecetaResponse creada = recetaConTresPasos();
        List<PasoRecetaResponse> pasos = creada.getPasos();
        String amasar = pasos.get(0).getId();
        String leudar = pasos.get(1).getId();
        String hornear = pasos.get(2).getId();

        // Act: (Amasar, Leudar, Hornear) pasa a (Hornear, Enfriar, Amasar) y Leudar se da de baja.
        Map<String, Object> datos = new HashMap<>();
        datos.put("pasos", List.of(Map.of("id", hornear), Map.of("descripcion", "Enfriar"), Map.of("id", amasar)));
        actualizarRecetaUseCase.execute(creada.getId(), ActualizarRecetaRequest.createFromRequest(datos));

        // Assert
        Receta leida = recetaRepository.obtenerPorId(new RecetaId(creada.getId())).orElseThrow();
        assertEquals(List.of("Hornear", "Enfriar", "Amasar"), descripcionesEnOrden(leida));
        assertEquals(List.of(1, 2, 3), leida.getPasosActivos().stream().map(paso -> paso.getPasos()).toList());
        assertEquals(hornear, leida.getPasosActivos().get(0).getId().getValue());
        assertNotEquals(leudar, leida.getPasosActivos().get(1).getId().getValue());
    }

    @Test
    void unIngredienteDadoDeBajaPuedeVolverAAgregarseEnElMismoPut() {
        // Arrange
        RecetaResponse creada = recetaConTresPasos();
        String idHarina = creada.getIngredientes().stream().filter(ingrediente -> ingrediente.getIdProducto().equals(harina))
                .findFirst().orElseThrow().getId();
        String idLevadura = creada.getIngredientes().stream().filter(ingrediente -> ingrediente.getIdProducto().equals(levadura))
                .findFirst().orElseThrow().getId();

        // Act: la harina sale y vuelve a entrar como linea nueva con otra cantidad.
        Map<String, Object> datos = new HashMap<>();
        datos.put("ingredientes", List.of(Map.of("id", idLevadura), Map.of("idProducto", harina, "cantidad", "500")));
        actualizarRecetaUseCase.execute(creada.getId(), ActualizarRecetaRequest.createFromRequest(datos));

        // Assert
        Receta leida = recetaRepository.obtenerPorId(new RecetaId(creada.getId())).orElseThrow();
        assertEquals(2, leida.getIngredientesActivos().size());
        var nuevaHarina = leida.getIngredientesActivos().stream()
                .filter(ingrediente -> ingrediente.getProductoId().getValue().equals(harina)).findFirst().orElseThrow();
        assertNotEquals(idHarina, nuevaHarina.getId().getValue());
        assertEquals(0, new BigDecimal("500").compareTo(nuevaHarina.getCantidad()));
        assertEquals(3, leida.getPasosActivos().size());
    }
}
