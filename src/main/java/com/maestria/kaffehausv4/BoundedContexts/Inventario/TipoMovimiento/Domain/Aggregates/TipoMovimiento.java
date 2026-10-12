package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;

/**
 * Clasifica un movimiento de inventario. El catalogo es corto a proposito, para que el usuario
 * tenga poco que leer: 'Receta' es lo que mueve una receta (los insumos que consume y el
 * producto que ingresa), 'Merma' lo que se pierde (incluido lo vencido) y 'Ajuste' el resto de
 * entradas y salidas de una existencia. El tipo no dice la direccion (entrada o salida):
 * esa se lee del motivo del movimiento. Se reconocen por nombre.
 */
public class TipoMovimiento {
    public static final String RECETA = "Receta";
    public static final String AJUSTE = "Ajuste";
    public static final String MERMA = "Merma";

    private TipoMovimientoId id;
    private String nombre;
    private boolean borrado;

    private TipoMovimiento(
        TipoMovimientoId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static TipoMovimiento restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new TipoMovimiento(new TipoMovimientoId(id), nombre, borrado);
    }

    public static TipoMovimiento crear(
        String nombre
    ){
        return new TipoMovimiento(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public TipoMovimientoId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
