package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos;

import java.time.LocalDateTime;

/**
 * Entrada de RegistrarAnulacionDeVentaUseCase. No viaja por HTTP: la arman AnularVentaUseCase
 * y la cancelacion de un pedido (ventas.md VEN-52), por eso su factoria es crear(...).
 */
public class RegistrarAnulacionDeVentaRequest {
    private String idVenta;
    private String idUsuario;
    private String motivo;
    private boolean reintegrar;
    private LocalDateTime fecha;

    private RegistrarAnulacionDeVentaRequest(String idVenta, String idUsuario, String motivo, boolean reintegrar, LocalDateTime fecha) {
        this.idVenta = idVenta;
        this.idUsuario = idUsuario;
        this.motivo = motivo;
        this.reintegrar = reintegrar;
        this.fecha = fecha;
    }

    public static RegistrarAnulacionDeVentaRequest crear(String idVenta, String idUsuario, String motivo, boolean reintegrar, LocalDateTime fecha) {
        return new RegistrarAnulacionDeVentaRequest(idVenta, idUsuario, motivo, reintegrar, fecha != null ? fecha : LocalDateTime.now());
    }

    public String getIdVenta() {return idVenta;}
    public String getIdUsuario() {return idUsuario;}
    public String getMotivo() {return motivo;}
    public boolean isReintegrar() {return reintegrar;}
    public LocalDateTime getFecha() {return fecha;}
}
