package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.time.LocalDateTime;

/**
 * Entrada de RegistrarReintegroDeInventarioUseCase. No viaja por HTTP: la arma la anulacion de
 * una venta, por eso su factoria es crear(...).
 *
 * Solo lleva la venta: lo que se devuelve sale de sus movimientos 'Venta', no de sus lineas
 * (logica.md VENTA-25).
 */
public class RegistrarReintegroDeInventarioRequest {
    private final String idVenta;
    private final String idUsuario;
    private final String motivo;
    private final LocalDateTime fecha;

    private RegistrarReintegroDeInventarioRequest(String idVenta, String idUsuario, String motivo, LocalDateTime fecha) {
        this.idVenta = idVenta;
        this.idUsuario = idUsuario;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    /** 'idUsuario' es quien anula; 'fecha', el momento de la anulacion. */
    public static RegistrarReintegroDeInventarioRequest crear(String idVenta, String idUsuario, String motivo, LocalDateTime fecha) {
        return new RegistrarReintegroDeInventarioRequest(idVenta, idUsuario, motivo, fecha != null ? fecha : LocalDateTime.now());
    }

    public String getIdVenta() {return idVenta;}
    public String getIdUsuario() {return idUsuario;}
    public String getMotivo() {return motivo;}
    public LocalDateTime getFecha() {return fecha;}
}
