package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas del agregado Receta. Son de dominio puro: no levantan Spring ni tocan la base,
 * porque las invariantes que se verifican aqui no dependen de ninguna de las dos.
 */
class RecetaTest {

    private static final ProductoId PRODUCTO_PAN = new ProductoId("6f1d3d34-0f4e-4a5c-9f33-0c3f2a0b0000");
    private static final ProductoId HARINA = new ProductoId("6f1d3d34-0f4e-4a5c-9f33-0c3f2a0b1111");
    private static final ProductoId AGUA = new ProductoId("6f1d3d34-0f4e-4a5c-9f33-0c3f2a0b2222");
    private static final String ID_PASO_1 = "44444444-4444-4444-4444-444444444444";
    private static final String ID_PASO_2 = "55555555-5555-5555-5555-555555555555";
    private static final String ID_PASO_3 = "66666666-6666-6666-6666-666666666666";
    private static final String ID_INGREDIENTE_HARINA = "77777777-7777-7777-7777-777777777777";

    private Receta recetaVacia() {
        return Receta.crear(new BigDecimal("10"), "Pan casero", PRODUCTO_PAN);
    }

    private Receta recetaPersistida() {
        return Receta.restaurar(
                "33333333-3333-3333-3333-333333333333",
                new BigDecimal("10.00"),
                "Pan casero",
                PRODUCTO_PAN,
                false,
                List.of(IngredienteReceta.restaurar(ID_INGREDIENTE_HARINA, HARINA, new BigDecimal("1.5000"), false)),
                List.of(
                        PasoReceta.restaurar(ID_PASO_1, 1, "Mezclar", false),
                        PasoReceta.restaurar(ID_PASO_2, 2, "Amasar", false),
                        PasoReceta.restaurar(ID_PASO_3, 3, "Hornear", false)));
    }

    private List<String> descripcionesEnOrden(Receta receta) {
        return receta.getPasosActivos().stream()
                .map(PasoReceta::getDescripcion)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ ingredientes

    @Test
    void agregarIngredienteIncorporaElInsumo() {
        Receta receta = recetaVacia();

        receta.agregarIngrediente(HARINA, new BigDecimal("1.5"));
        receta.agregarIngrediente(AGUA, new BigDecimal("0.75"));

        assertEquals(2, receta.getIngredientesActivos().size());
        assertEquals(new BigDecimal("1.5000"), receta.getIngredientesActivos().get(0).getCantidad());
    }

    @Test
    void rechazaDosIngredientesDelMismoProducto() {
        Receta receta = recetaVacia();
        receta.agregarIngrediente(HARINA, new BigDecimal("1.5"));

        DomainException error = assertThrows(DomainException.class,
                () -> receta.agregarIngrediente(HARINA, new BigDecimal("2")));

        assertEquals(400, error.getCodigo());
        assertEquals(1, receta.getIngredientesActivos().size());
    }

    @Test
    void permiteReponerUnProductoDespuesDeDarloDeBaja() {
        Receta receta = recetaPersistida();

        receta.removerIngrediente(ID_INGREDIENTE_HARINA);
        receta.agregarIngrediente(HARINA, new BigDecimal("2"));

        assertEquals(1, receta.getIngredientesActivos().size());
        assertEquals(new BigDecimal("2.0000"), receta.getIngredientesActivos().get(0).getCantidad());
    }

    @Test
    void rechazaUnaCantidadDeIngredienteNoPositiva() {
        Receta receta = recetaVacia();

        DomainException error = assertThrows(DomainException.class,
                () -> receta.agregarIngrediente(HARINA, BigDecimal.ZERO));

        assertEquals(400, error.getCodigo());
        assertTrue(receta.getIngredientesActivos().isEmpty());
    }

    @Test
    void removerIngredienteEsBajaLogica() {
        Receta receta = recetaPersistida();

        receta.removerIngrediente(ID_INGREDIENTE_HARINA);

        assertTrue(receta.getIngredientesActivos().isEmpty());
        assertEquals(1, receta.getIngredientes().size());
        assertTrue(receta.getIngredientes().get(0).isBorrado());
    }

    // ------------------------------------------------------------------ pasos

    @Test
    void agregarPasoAsignaLaSecuenciaEnOrden() {
        Receta receta = recetaVacia();

        receta.agregarPaso("Mezclar");
        receta.agregarPaso("Amasar");
        receta.agregarPaso("Hornear");

        assertEquals(List.of(1, 2, 3), receta.getPasosActivos().stream().map(PasoReceta::getPasos).collect(Collectors.toList()));
        assertEquals(List.of("Mezclar", "Amasar", "Hornear"), descripcionesEnOrden(receta));
    }

    @Test
    void removerPasoRenumeraLosRestantesSinDejarHuecos() {
        Receta receta = recetaPersistida();

        receta.removerPaso(ID_PASO_1);

        assertEquals(List.of(1, 2), receta.getPasosActivos().stream().map(PasoReceta::getPasos).collect(Collectors.toList()));
        assertEquals(List.of("Amasar", "Hornear"), descripcionesEnOrden(receta));
        // Baja logica: el paso retirado sigue en la coleccion marcado.
        assertEquals(3, receta.getPasos().size());
    }

    @Test
    void reordenarPasosPermutaLaSecuencia() {
        Receta receta = recetaPersistida();

        receta.reordenarPasos(List.of(ID_PASO_3, ID_PASO_1, ID_PASO_2));

        assertEquals(List.of("Hornear", "Mezclar", "Amasar"), descripcionesEnOrden(receta));
        assertEquals(List.of(1, 2, 3), receta.getPasosActivos().stream().map(PasoReceta::getPasos).collect(Collectors.toList()));
    }

    @Test
    void reordenarPasosRechazaUnConjuntoIncompleto() {
        Receta receta = recetaPersistida();

        DomainException error = assertThrows(DomainException.class,
                () -> receta.reordenarPasos(List.of(ID_PASO_3, ID_PASO_1)));

        assertEquals(400, error.getCodigo());
        assertEquals(List.of("Mezclar", "Amasar", "Hornear"), descripcionesEnOrden(receta));
    }

    @Test
    void reordenarPasosRechazaRepetidos() {
        Receta receta = recetaPersistida();

        DomainException error = assertThrows(DomainException.class,
                () -> receta.reordenarPasos(List.of(ID_PASO_1, ID_PASO_1, ID_PASO_2)));

        assertEquals(400, error.getCodigo());
    }

    @Test
    void moverPasoAlFinalReconstruyeLaSecuencia() {
        Receta receta = recetaPersistida();

        receta.moverPasoAlFinal(ID_PASO_1);

        assertEquals(List.of("Amasar", "Hornear", "Mezclar"), descripcionesEnOrden(receta));
        assertEquals(List.of(1, 2, 3), receta.getPasosActivos().stream().map(PasoReceta::getPasos).collect(Collectors.toList()));
    }

    @Test
    void rechazaUnPasoSinDescripcion() {
        Receta receta = recetaVacia();

        DomainException error = assertThrows(DomainException.class, () -> receta.agregarPaso("   "));

        assertEquals(400, error.getCodigo());
        assertTrue(receta.getPasosActivos().isEmpty());
    }

    @Test
    void removerUnPasoAjenoALaRecetaFalla() {
        Receta receta = recetaPersistida();

        DomainException error = assertThrows(DomainException.class,
                () -> receta.removerPaso("99999999-9999-9999-9999-999999999999"));

        assertEquals(404, error.getCodigo());
    }

    // ------------------------------------------------------------------ agregado

    @Test
    void lasColeccionesNoSePuedenMutarDesdeFuera() {
        Receta receta = recetaPersistida();

        List<IngredienteReceta> ingredientes = receta.getIngredientes();
        List<PasoReceta> pasos = receta.getPasos();

        assertThrows(UnsupportedOperationException.class, () -> ingredientes.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> pasos.remove(0));
    }

    @Test
    void unaRecetaSinIngredientesNoEsRegistrable() {
        Receta receta = recetaVacia();
        receta.agregarPaso("Mezclar");

        DomainException error = assertThrows(DomainException.class, receta::validarRegistrable);

        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("ingrediente"));
    }

    @Test
    void unaRecetaSinPasosNoEsRegistrable() {
        Receta receta = recetaVacia();
        receta.agregarIngrediente(HARINA, new BigDecimal("1.5"));

        DomainException error = assertThrows(DomainException.class, receta::validarRegistrable);

        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("paso"));
    }

    @Test
    void eliminarLaRecetaArrastraAmbasColecciones() {
        Receta receta = recetaPersistida();

        receta.eliminar();

        assertTrue(receta.isBorrado());
        assertTrue(receta.getIngredientesActivos().isEmpty());
        assertTrue(receta.getPasosActivos().isEmpty());
        assertTrue(receta.getIngredientes().stream().allMatch(IngredienteReceta::isBorrado));
        assertTrue(receta.getPasos().stream().allMatch(PasoReceta::isBorrado));
    }

    @Test
    void rechazaUnRendimientoNoPositivo() {
        DomainException error = assertThrows(DomainException.class,
                () -> Receta.crear(BigDecimal.ZERO, "Pan casero", PRODUCTO_PAN));

        assertEquals(400, error.getCodigo());
    }
}
