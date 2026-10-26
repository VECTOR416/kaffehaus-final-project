package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates;

import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

public class EstadoExistencia {
    /**
     * Filas que el sistema reconoce por su nombre (logica.md GENERAL-04): no se renombran ni se dan
     * de baja (GENERAL-19). Se pueden agregar otras filas, que no tienen semantica.
     */
    public static final List<String> NOMBRES_SEMANTICOS = List.of("Abierta", "Agotada");

    private EstadoExistenciaId id;
    private String nombre;
    private boolean borrado;

    private EstadoExistencia(
        EstadoExistenciaId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static EstadoExistencia restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new EstadoExistencia(new EstadoExistenciaId(id), nombre, borrado);
    }

    public static EstadoExistencia crear(
        String nombre
    ){
        return new EstadoExistencia(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        if (nombre != null && esSemantico() && !nombre.equalsIgnoreCase(this.nombre)) {
            throw DomainException.throwException(409,
                    "El estado de existencia '" + this.nombre + "' lo reconoce el sistema por su nombre y no se renombra");
        }
        if (nombre != null && !esSemantico()) {
            this.nombre = nombre;
        }
    }

    public void eliminar(){
        if (esSemantico()) {
            throw DomainException.throwException(409,
                    "El estado de existencia '" + this.nombre + "' lo reconoce el sistema por su nombre y no se da de baja");
        }
        this.borrado = true;
    }

    /** Es una de las filas que el sistema reconoce por su nombre (GENERAL-19). */
    public boolean esSemantico() {
        return NOMBRES_SEMANTICOS.stream().anyMatch(semantico -> semantico.equalsIgnoreCase(this.nombre));
    }

    public EstadoExistenciaId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
