package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.RolDelSistema;

/**
 * Rol tal como lo ve el cliente: el codigo numerico del sistema (1 o 2) y su nombre. No expone
 * el UUID de la fila 'rol', que es un detalle de persistencia.
 */
public class RolResponse {
    private int id;
    private String nombre;

    private RolResponse(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public static RolResponse createFromAggregate(RolDelSistema rol){
        return new RolResponse(rol.getCodigo(), rol.getNombre());
    }

    public int getId() {return id;}
    public String getNombre() {return nombre;}
}
