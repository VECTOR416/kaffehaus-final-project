package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;

/**
 * Forma de pago de una venta (Efectivo, Tarjeta, QR...). El catalogo es datos: el unico tipo
 * con semantica propia es el efectivo, que admite recibir mas de lo cobrado y dar cambio, y
 * es el que cuenta en el arqueo fisico de la caja. Se reconoce por nombre, igual que
 * TipoDescuento reconoce sus porcentajes.
 */
public class TipoPago {
    public static final String EFECTIVO = "Efectivo";

    private TipoPagoId id;
    private String nombre;
    private boolean borrado;

    private TipoPago(
        TipoPagoId id,
        String nombre,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.borrado = borrado;
    }

    public static TipoPago restaurar(
        String id,
        String nombre,
        boolean borrado
    ){
        return new TipoPago(new TipoPagoId(id), nombre, borrado);
    }

    public static TipoPago crear(
        String nombre
    ){
        return new TipoPago(null, nombre, false);
    }

    public void actualizar(
        String nombre
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public boolean esEfectivo() {
        return EFECTIVO.equalsIgnoreCase(this.nombre);
    }

    public TipoPagoId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
