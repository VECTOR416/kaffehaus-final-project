package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates;

import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Clasifica un movimiento de inventario; cada tipo tiene un sentido fijo (logica.md MOVIMIENTO-22):
 * 'Produccion' y 'Venta' son los insumos o unidades que consumen una produccion o una venta;
 * 'Reintegro' devuelve lo de una venta anulada; 'Merma' y 'Ajuste' salen del pesaje o del conteo,
 * cuando en la realidad hay menos o mas de lo que dice el sistema; 'Apertura' deja constancia de
 * que se abrio una unidad cerrada; 'Vencimiento' es la baja de lo vencido que decide el usuario
 * (VENCIMIENTO-03). Se reconocen por nombre.
 */
public class TipoMovimiento {
    public static final String PRODUCCION = "Produccion";
    public static final String AJUSTE = "Ajuste";
    public static final String MERMA = "Merma";
    public static final String APERTURA = "Apertura";
    public static final String VENTA = "Venta";
    public static final String REINTEGRO = "Reintegro";
    public static final String VENCIMIENTO = "Vencimiento";

    /**
     * Filas que el sistema reconoce por su nombre (logica.md GENERAL-04): no se renombran ni se dan
     * de baja (GENERAL-19). Se pueden agregar otras filas, que no tienen semantica.
     */
    public static final List<String> NOMBRES_SEMANTICOS = List.of(PRODUCCION, AJUSTE, MERMA, APERTURA, VENTA, REINTEGRO, VENCIMIENTO);

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
        if (nombre != null && esSemantico() && !nombre.equalsIgnoreCase(this.nombre)) {
            throw DomainException.throwException(409,
                    "El tipo de movimiento '" + this.nombre + "' lo reconoce el sistema por su nombre y no se renombra");
        }
        if (nombre != null && !esSemantico()) {
            this.nombre = nombre;
        }
    }

    public void eliminar(){
        if (esSemantico()) {
            throw DomainException.throwException(409,
                    "El tipo de movimiento '" + this.nombre + "' lo reconoce el sistema por su nombre y no se da de baja");
        }
        this.borrado = true;
    }

    /** Es una de las filas que el sistema reconoce por su nombre (GENERAL-19). */
    public boolean esSemantico() {
        return NOMBRES_SEMANTICOS.stream().anyMatch(semantico -> semantico.equalsIgnoreCase(this.nombre));
    }

    public TipoMovimientoId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
