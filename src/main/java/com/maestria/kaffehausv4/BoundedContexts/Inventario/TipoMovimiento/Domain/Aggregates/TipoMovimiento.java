package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;

/**
 * Clasifica un movimiento de inventario; cada tipo tiene un sentido fijo (logica.md MOVIMIENTO-22):
 * 'Produccion' y 'Venta' son los insumos o unidades que consumen una produccion o una venta;
 * 'Reintegro' devuelve lo de una venta anulada; 'Merma' y 'Ajuste' salen del pesaje o del conteo,
 * cuando en la realidad hay menos o mas de lo que dice el sistema; 'Apertura' deja constancia de
 * que se abrio una unidad cerrada; 'Vencimiento' queda sembrado para la baja de lo vencido, que
 * aun no existe (VENCIMIENTO-01). Se reconocen por nombre.
 */
public class TipoMovimiento {
    public static final String PRODUCCION = "Produccion";
    public static final String AJUSTE = "Ajuste";
    public static final String MERMA = "Merma";
    public static final String APERTURA = "Apertura";
    public static final String VENTA = "Venta";
    public static final String REINTEGRO = "Reintegro";
    public static final String VENCIMIENTO = "Vencimiento";

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
