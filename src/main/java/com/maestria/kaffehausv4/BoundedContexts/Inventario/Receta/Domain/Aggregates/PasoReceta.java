package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.PasoRecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Entidad interna del agregado Receta. NO es raiz de agregado.
 *
 * Se declara en el mismo paquete que Receta a proposito: es la senal mas fuerte de que su
 * ciclo de vida pertenece a la raiz. Sus mutadores son package-private, asi que nadie fuera
 * de Receta puede crear un paso suelto ni cambiarle su numero de orden.
 *
 * El numero de paso NO se recibe desde fuera: lo asigna y lo mantiene la raiz, que es la
 * unica que puede garantizar la secuencia 1..N sin huecos ni repetidos. La base refuerza esa
 * regla con el indice unico parcial uq_receta_paso sobre los pasos vivos.
 *
 * El borrado es logico, como en todo el sistema.
 */
public class PasoReceta {

    private PasoRecetaId id;
    private Integer pasos;
    private String descripcion;
    private boolean borrado;

    private PasoReceta(
        PasoRecetaId id,
        Integer pasos,
        String descripcion,
        boolean borrado
    ){
        this.id = id;
        this.pasos = pasos;
        this.descripcion = descripcion;
        this.borrado = borrado;
    }

    /**
     * Reconstruye un paso persistido. La invoca el mapper de infraestructura a traves de
     * Receta.restaurar, nunca la capa de aplicacion.
     */
    public static PasoReceta restaurar(
        String id,
        Integer pasos,
        String descripcion,
        boolean borrado
    ){
        return new PasoReceta(new PasoRecetaId(id), pasos, descripcion, borrado);
    }

    /** Crea un paso nuevo en la posicion que le indica la raiz. Package-private: solo Receta. */
    static PasoReceta crear(Integer pasos, String descripcion) {
        exigirDescripcionValida(descripcion);

        return new PasoReceta(null, pasos, descripcion.trim(), false);
    }

    /** Corrige el texto de la instruccion sin tocar su posicion. */
    void actualizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return;
        }
        exigirDescripcionValida(descripcion);
        this.descripcion = descripcion.trim();
    }

    /** Reubica el paso en la secuencia. Solo la raiz decide este numero. */
    void reubicarEn(int numeroDePaso) {
        this.pasos = numeroDePaso;
    }

    void eliminar() {
        this.borrado = true;
    }

    private static void exigirDescripcionValida(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw DomainException.throwException(400, "La descripcion del paso de receta es obligatoria");
        }
    }

    public PasoRecetaId getId() {return id;}
    public Integer getPasos() {return pasos;}
    public String getDescripcion() {return descripcion;}
    public boolean isBorrado() {return borrado;}
}
