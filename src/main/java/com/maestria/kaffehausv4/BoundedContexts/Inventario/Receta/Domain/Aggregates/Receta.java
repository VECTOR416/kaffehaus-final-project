package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Raiz del agregado Receta.
 *
 * Receta gobierna dos colecciones hijas: los ingredientes (que insumos y cuanto) y los pasos
 * (en que orden se ejecutan). Es la unica puerta de entrada a ambas; se exponen como vistas
 * de solo lectura y toda mutacion pasa por sus metodos.
 *
 * Invariantes que sostiene la raiz:
 * - Un mismo producto no puede aparecer dos veces entre los ingredientes vivos.
 * - Los pasos vivos forman siempre la secuencia 1..N, sin huecos ni repetidos. El numero de
 *   paso no se recibe nunca desde fuera: lo asigna la raiz al agregar y lo recalcula al
 *   remover o reordenar.
 * - Una receta registrada tiene al menos un ingrediente y al menos un paso.
 *
 * El borrado es logico en todo el sistema, tambien aqui: las lineas dadas de baja siguen en
 * sus colecciones marcadas, porque esa baja debe llegar a la base como un UPDATE.
 */
public class Receta {

    /** Escala de NUMERIC(12, 2) en 'receta'. */
    private static final int ESCALA_RENDIMIENTO = 2;

    private RecetaId id;
    private BigDecimal cantidad;
    private String titulo;
    private ProductoId productoObtenidoId;
    private boolean borrado;
    private final List<IngredienteReceta> ingredientes;
    private final List<PasoReceta> pasos;

    private Receta(
        RecetaId id,
        BigDecimal cantidad,
        String titulo,
        ProductoId productoObtenidoId,
        boolean borrado,
        List<IngredienteReceta> ingredientes,
        List<PasoReceta> pasos
    ){
        this.id = id;
        this.cantidad = cantidad;
        this.titulo = titulo;
        this.productoObtenidoId = productoObtenidoId;
        this.borrado = borrado;
        this.ingredientes = ingredientes != null ? new ArrayList<>(ingredientes) : new ArrayList<>();
        this.pasos = pasos != null ? new ArrayList<>(pasos) : new ArrayList<>();
    }

    /** Reconstruye el agregado completo desde persistencia, con sus dos colecciones. */
    public static Receta restaurar(
        String id,
        BigDecimal cantidad,
        String titulo,
        ProductoId productoObtenidoId,
        boolean borrado,
        List<IngredienteReceta> ingredientes,
        List<PasoReceta> pasos
    ){
        return new Receta(new RecetaId(id), cantidad, titulo, productoObtenidoId, borrado, ingredientes, pasos);
    }

    /**
     * Crea una receta sin ingredientes ni pasos. Ambos se incorporan despues con
     * agregarIngrediente(...) y agregarPaso(...), que son los que sostienen las invariantes.
     */
    public static Receta crear(
        BigDecimal cantidad,
        String titulo,
        ProductoId productoObtenidoId
    ){
        exigirCantidadValida(cantidad);

        return new Receta(null, aEscalaRendimiento(cantidad), titulo, productoObtenidoId, false, null, null);
    }

    public void actualizar(
        BigDecimal cantidad,
        String titulo,
        ProductoId productoObtenidoId
    ){
        if (cantidad != null) {
            exigirCantidadValida(cantidad);
            this.cantidad = aEscalaRendimiento(cantidad);
        }
        this.titulo = titulo != null ? titulo : this.titulo;
        this.productoObtenidoId = productoObtenidoId != null ? productoObtenidoId : this.productoObtenidoId;
    }

    // ---------------------------------------------------------------- ingredientes

    /** Unica via para incorporar un insumo. Rechaza repetir un producto ya presente. */
    public void agregarIngrediente(ProductoId productoId, BigDecimal cantidad) {
        if (productoId != null && productoId.getValue() != null && contieneProductoVivo(productoId.getValue())) {
            throw DomainException.throwException(400,
                    "El producto '" + productoId.getValue() + "' ya es un ingrediente de esta receta");
        }

        this.ingredientes.add(IngredienteReceta.crear(productoId, cantidad));
    }

    /** Corrige la cantidad de un ingrediente vivo. El producto de un ingrediente no se cambia. */
    public void actualizarIngrediente(String ingredienteRecetaId, BigDecimal cantidad) {
        ingredienteVivo(ingredienteRecetaId).actualizarCantidad(cantidad);
    }

    /** Da de baja logica un ingrediente. */
    public void removerIngrediente(String ingredienteRecetaId) {
        ingredienteVivo(ingredienteRecetaId).eliminar();
    }

    // ---------------------------------------------------------------- pasos

    /** Unica via para incorporar un paso: se anade al final de la secuencia. */
    public void agregarPaso(String descripcion) {
        this.pasos.add(PasoReceta.crear(0, descripcion));
        renumerarPasos();
    }

    /** Corrige el texto de un paso vivo sin alterar su posicion. */
    public void actualizarPaso(String pasoRecetaId, String descripcion) {
        pasoVivo(pasoRecetaId).actualizarDescripcion(descripcion);
    }

    /** Da de baja logica un paso y renumera los restantes para no dejar huecos. */
    public void removerPaso(String pasoRecetaId) {
        pasoVivo(pasoRecetaId).eliminar();
        renumerarPasos();
    }

    /**
     * Reordena la secuencia completa. La lista recibida debe ser exactamente el conjunto de
     * pasos vivos, sin ausencias ni repetidos: reordenar es permutar, no dar de alta ni de baja.
     */
    public void reordenarPasos(List<String> idsEnOrden) {
        if (idsEnOrden == null) {
            throw DomainException.throwException(400, "El nuevo orden de los pasos es obligatorio");
        }

        Set<String> idsRecibidos = new LinkedHashSet<>(idsEnOrden);
        if (idsRecibidos.size() != idsEnOrden.size()) {
            throw DomainException.throwException(400, "El nuevo orden de los pasos no puede repetir un mismo paso");
        }

        Set<String> idsVivos = getPasosActivos().stream()
                .map(paso -> paso.getId().getValue())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!idsRecibidos.equals(idsVivos)) {
            throw DomainException.throwException(400,
                    "El nuevo orden debe incluir exactamente los pasos vivos de la receta");
        }

        List<PasoReceta> reordenados = idsEnOrden.stream()
                .map(this::pasoVivo)
                .collect(Collectors.toList());

        this.pasos.removeAll(reordenados);
        this.pasos.addAll(reordenados);
        renumerarPasos();
    }

    /**
     * Mueve un paso vivo al final de la secuencia. Aplicado en cadena sobre los pasos en el
     * orden deseado, reconstruye cualquier secuencia; es lo que permite que el array del
     * payload defina el orden incluso cuando se mezclan pasos existentes y nuevos.
     */
    public void moverPasoAlFinal(String pasoRecetaId) {
        PasoReceta paso = pasoVivo(pasoRecetaId);

        this.pasos.remove(paso);
        this.pasos.add(paso);
        renumerarPasos();
    }

    // ---------------------------------------------------------------- agregado

    /** Baja logica del agregado entero: el encabezado, sus ingredientes y sus pasos. */
    public void eliminar(){
        this.borrado = true;
        this.ingredientes.forEach(IngredienteReceta::eliminar);
        this.pasos.forEach(PasoReceta::eliminar);
    }

    /**
     * Invariante de agregado: una receta registrada dice con que se hace y como se hace. La
     * comprueba el caso de uso antes de persistir, en el alta y tras modificar las colecciones.
     */
    public void validarRegistrable() {
        if (getIngredientesActivos().isEmpty()) {
            throw DomainException.throwException(400, "La receta debe tener al menos un ingrediente");
        }
        if (getPasosActivos().isEmpty()) {
            throw DomainException.throwException(400, "La receta debe tener al menos un paso");
        }
    }

    /**
     * Reasigna 1..N siguiendo el orden de la coleccion, que es el orden real de la secuencia.
     * No se apoya en los numeros previos: justo despues de mover un paso serian enganosos.
     */
    private void renumerarPasos() {
        int numero = 1;
        for (PasoReceta paso : this.pasos) {
            if (!paso.isBorrado()) {
                paso.reubicarEn(numero++);
            }
        }
    }

    private boolean contieneProductoVivo(String idProducto) {
        return getIngredientesActivos().stream()
                .anyMatch(ingrediente -> ingrediente.getProductoId() != null
                        && idProducto.equals(ingrediente.getProductoId().getValue()));
    }

    private IngredienteReceta ingredienteVivo(String ingredienteRecetaId) {
        return this.ingredientes.stream()
                .filter(ingrediente -> !ingrediente.isBorrado())
                .filter(ingrediente -> ingrediente.getId() != null
                        && ingrediente.getId().getValue().equals(ingredienteRecetaId))
                .findFirst()
                .orElseThrow(() -> DomainException.throwException(404,
                        "El ingrediente con id '" + ingredienteRecetaId + "' no pertenece a esta receta"));
    }

    private PasoReceta pasoVivo(String pasoRecetaId) {
        return this.pasos.stream()
                .filter(paso -> !paso.isBorrado())
                .filter(paso -> paso.getId() != null && paso.getId().getValue().equals(pasoRecetaId))
                .findFirst()
                .orElseThrow(() -> DomainException.throwException(404,
                        "El paso con id '" + pasoRecetaId + "' no pertenece a esta receta"));
    }

    private static void exigirCantidadValida(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw DomainException.throwException(400, "La cantidad que rinde la receta debe ser mayor a cero");
        }
    }

    private static BigDecimal aEscalaRendimiento(BigDecimal valor) {
        return valor.setScale(ESCALA_RENDIMIENTO, RoundingMode.HALF_UP);
    }

    public RecetaId getId() {return id;}
    public BigDecimal getCantidad() {return cantidad;}
    public String getTitulo() {return titulo;}
    public ProductoId getProductoObtenidoId() {return productoObtenidoId;}
    public boolean isBorrado() {return borrado;}

    /** Todos los ingredientes, incluidos los dados de baja: es lo que la persistencia escribe. */
    public List<IngredienteReceta> getIngredientes() {return Collections.unmodifiableList(ingredientes);}

    /** Todos los pasos, incluidos los dados de baja: es lo que la persistencia escribe. */
    public List<PasoReceta> getPasos() {return Collections.unmodifiableList(pasos);}

    /** Solo los ingredientes vivos: es lo que ve el exterior. */
    public List<IngredienteReceta> getIngredientesActivos() {
        return ingredientes.stream()
                .filter(ingrediente -> !ingrediente.isBorrado())
                .collect(Collectors.toList());
    }

    /** Solo los pasos vivos, siempre en su orden de ejecucion. */
    public List<PasoReceta> getPasosActivos() {
        return pasos.stream()
                .filter(paso -> !paso.isBorrado())
                .sorted(Comparator.comparing(PasoReceta::getPasos))
                .collect(Collectors.toList());
    }
}
