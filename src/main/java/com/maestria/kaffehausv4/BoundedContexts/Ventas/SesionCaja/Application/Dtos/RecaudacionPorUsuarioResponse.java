package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos;

import java.math.BigDecimal;

/**
 * Lo vendido por un usuario en las ventas completadas de una sesion (logica.md SESIONCAJA-16):
 * el total, la parte cobrada en efectivo y cuantas ventas hizo.
 */
public class RecaudacionPorUsuarioResponse {
    private String idUsuario;
    private String correo;
    private BigDecimal totalVendido;
    private BigDecimal efectivo;
    private long cantidadVentas;

    private RecaudacionPorUsuarioResponse(String idUsuario, String correo, BigDecimal totalVendido, BigDecimal efectivo, long cantidadVentas) {
        this.idUsuario = idUsuario;
        this.correo = correo;
        this.totalVendido = totalVendido;
        this.efectivo = efectivo;
        this.cantidadVentas = cantidadVentas;
    }

    /** 'correo' puede ser nulo si el usuario ya se dio de baja. */
    public static RecaudacionPorUsuarioResponse crear(String idUsuario, String correo, BigDecimal totalVendido, BigDecimal efectivo, long cantidadVentas) {
        return new RecaudacionPorUsuarioResponse(idUsuario, correo, totalVendido, efectivo, cantidadVentas);
    }

    public String getIdUsuario() {return idUsuario;}
    public String getCorreo() {return correo;}
    public BigDecimal getTotalVendido() {return totalVendido;}
    public BigDecimal getEfectivo() {return efectivo;}
    public long getCantidadVentas() {return cantidadVentas;}
}
