package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates;

import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

public class TipoDescuento {
    /**
     * Filas que el sistema reconoce por su nombre (logica.md GENERAL-04): no se renombran ni se dan
     * de baja (GENERAL-19). Se pueden agregar otras filas, que no tienen semantica.
     */
    public static final List<String> NOMBRES_SEMANTICOS = List.of("Porcentaje", "Monto fijo");

    private TipoDescuentoId id;
    private String nombre;
    private boolean borrado;

    private TipoDescuento(
        TipoDescuentoId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static TipoDescuento restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new TipoDescuento(new TipoDescuentoId(id), nombre, borrado);
    }

    public static TipoDescuento crear(
        String nombre
    ){
        return new TipoDescuento(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        if (nombre != null && esSemantico() && !nombre.equalsIgnoreCase(this.nombre)) {
            throw DomainException.throwException(409,
                    "El tipo de descuento '" + this.nombre + "' lo reconoce el sistema por su nombre y no se renombra");
        }
        if (nombre != null && !esSemantico()) {
            this.nombre = nombre;
        }
    }

    public void eliminar(){
        if (esSemantico()) {
            throw DomainException.throwException(409,
                    "El tipo de descuento '" + this.nombre + "' lo reconoce el sistema por su nombre y no se da de baja");
        }
        this.borrado = true;
    }

    /** Es una de las filas que el sistema reconoce por su nombre (GENERAL-19). */
    public boolean esSemantico() {
        return NOMBRES_SEMANTICOS.stream().anyMatch(semantico -> semantico.equalsIgnoreCase(this.nombre));
    }

    /**
     * Semantica del tipo (ventas.md VEN-14). Como los roles, los tipos son filas del catalogo
     * reconocidas por nombre: 'Porcentaje' y 'Monto fijo'. Otro nombre no se puede aplicar.
     */
    public boolean esPorcentaje() {
        return "Porcentaje".equalsIgnoreCase(this.nombre);
    }

    public boolean esMontoFijo() {
        return "Monto fijo".equalsIgnoreCase(this.nombre);
    }

    public boolean esAplicable() {
        return esPorcentaje() || esMontoFijo();
    }

    public TipoDescuentoId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
