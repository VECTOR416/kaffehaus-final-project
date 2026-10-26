package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates;

import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

public class EstadoPedido {
    /**
     * Filas que el sistema reconoce por su nombre (logica.md GENERAL-04): no se renombran ni se dan
     * de baja (GENERAL-19). Se pueden agregar otras filas, que no tienen semantica.
     */
    public static final List<String> NOMBRES_SEMANTICOS = List.of("Recibido", "En preparacion", "En camino", "Entregado", "Cancelado");

    private EstadoPedidoId id;
    private String nombre;
    private boolean borrado;

    private EstadoPedido(
        EstadoPedidoId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static EstadoPedido restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new EstadoPedido(new EstadoPedidoId(id), nombre, borrado);
    }

    public static EstadoPedido crear(
        String nombre
    ){
        return new EstadoPedido(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        if (nombre != null && esSemantico() && !nombre.equalsIgnoreCase(this.nombre)) {
            throw DomainException.throwException(409,
                    "El estado de pedido '" + this.nombre + "' lo reconoce el sistema por su nombre y no se renombra");
        }
        if (nombre != null && !esSemantico()) {
            this.nombre = nombre;
        }
    }

    public void eliminar(){
        if (esSemantico()) {
            throw DomainException.throwException(409,
                    "El estado de pedido '" + this.nombre + "' lo reconoce el sistema por su nombre y no se da de baja");
        }
        this.borrado = true;
    }

    /** Es una de las filas que el sistema reconoce por su nombre (GENERAL-19). */
    public boolean esSemantico() {
        return NOMBRES_SEMANTICOS.stream().anyMatch(semantico -> semantico.equalsIgnoreCase(this.nombre));
    }

    public EstadoPedidoId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
