package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates;

import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Forma de pago de una venta (Efectivo, Tarjeta, QR...). El catalogo es datos: el unico tipo
 * con semantica propia es el efectivo, que admite recibir mas de lo cobrado y dar cambio, y
 * es el que cuenta en el arqueo fisico de la caja. Se reconoce por nombre, igual que
 * TipoDescuento reconoce sus porcentajes.
 */
public class TipoPago {
    public static final String EFECTIVO = "Efectivo";

    /**
     * Filas que el sistema reconoce por su nombre (logica.md GENERAL-04): no se renombran ni se dan
     * de baja (GENERAL-19). Se pueden agregar otras filas, que no tienen semantica.
     */
    public static final List<String> NOMBRES_SEMANTICOS = List.of(EFECTIVO);

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
        if (nombre != null && esSemantico() && !nombre.equalsIgnoreCase(this.nombre)) {
            throw DomainException.throwException(409,
                    "El tipo de pago '" + this.nombre + "' lo reconoce el sistema por su nombre y no se renombra");
        }
        if (nombre != null && !esSemantico()) {
            this.nombre = nombre;
        }
    }

    public void eliminar(){
        if (esSemantico()) {
            throw DomainException.throwException(409,
                    "El tipo de pago '" + this.nombre + "' lo reconoce el sistema por su nombre y no se da de baja");
        }
        this.borrado = true;
    }

    /** Es una de las filas que el sistema reconoce por su nombre (GENERAL-19). */
    public boolean esSemantico() {
        return NOMBRES_SEMANTICOS.stream().anyMatch(semantico -> semantico.equalsIgnoreCase(this.nombre));
    }

    public boolean esEfectivo() {
        return EFECTIVO.equalsIgnoreCase(this.nombre);
    }

    public TipoPagoId getId() {return id;}
    public String getNombre() {return nombre;}
    public boolean isBorrado() {return borrado;}
}
