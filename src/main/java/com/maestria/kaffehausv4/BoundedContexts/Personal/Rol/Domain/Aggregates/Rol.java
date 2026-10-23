package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates;

import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

public class Rol {
    public static final String ADMINISTRADOR = "Administrador";
    public static final String OPERADOR = "Operador";

    /**
     * Filas que el sistema reconoce por su nombre (logica.md GENERAL-04): no se renombran ni se dan
     * de baja (GENERAL-19). Se pueden agregar otras filas, que no tienen semantica.
     */
    public static final List<String> NOMBRES_SEMANTICOS = List.of(ADMINISTRADOR, OPERADOR);

    private RolId id;
    private String nombre;
    private boolean borrado;

    private Rol(RolId id, String nombre, boolean borrado){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static Rol restaurar(String id, String nombre, boolean borrado){
        return new Rol(new RolId(id), nombre, borrado);
    }

    public static Rol crear(String nombre){
        return new Rol(null, nombre, false);
    }

    public void actualizar(String nombre){
        if (nombre != null && esSemantico() && !nombre.equalsIgnoreCase(this.nombre)) {
            throw DomainException.throwException(409,
                    "El rol '" + this.nombre + "' lo reconoce el sistema por su nombre y no se renombra");
        }
        if (nombre != null && !esSemantico()) {
            this.nombre = nombre;
        }
    }

    public void eliminar(){
        if (esSemantico()) {
            throw DomainException.throwException(409,
                    "El rol '" + this.nombre + "' lo reconoce el sistema por su nombre y no se da de baja");
        }
        this.borrado = true;
    }

    /** Es una de las filas que el sistema reconoce por su nombre (GENERAL-19). */
    public boolean esSemantico() {
        return NOMBRES_SEMANTICOS.stream().anyMatch(semantico -> semantico.equalsIgnoreCase(this.nombre));
    }

    public RolId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
