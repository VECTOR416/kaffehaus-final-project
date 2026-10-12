package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.time.LocalDateTime;

/**
 * Entrada de RegistrarReintegroDeInventarioUseCase. No viaja por HTTP: la arma la anulacion de
 * una venta, por eso su factoria es crear(...).
 *
 * 'motivoOrigen' identifica los movimientos a compensar (los de la venta citan
 * "Venta '<id>'"); solo las salidas de la venta llevan ese motivo, y todas se reintegran.
 */
public class RegistrarReintegroDeInventarioRequest {
    private String motivoOrigen;
    private String motivoReintegro;
    private String idUsuario;
    private LocalDateTime fecha;

    private RegistrarReintegroDeInventarioRequest(
        String motivoOrigen,
        String motivoReintegro,
        String idUsuario,
        LocalDateTime fecha
    ) {
        this.motivoOrigen = motivoOrigen;
        this.motivoReintegro = motivoReintegro;
        this.idUsuario = idUsuario;
        this.fecha = fecha;
    }

    public static RegistrarReintegroDeInventarioRequest crear(
        String motivoOrigen,
        String motivoReintegro,
        String idUsuario,
        LocalDateTime fecha
    ) {
        return new RegistrarReintegroDeInventarioRequest(
                motivoOrigen, motivoReintegro, idUsuario, fecha != null ? fecha : LocalDateTime.now());
    }

    public String getMotivoOrigen() {return motivoOrigen;}
    public String getMotivoReintegro() {return motivoReintegro;}
    public String getIdUsuario() {return idUsuario;}
    public LocalDateTime getFecha() {return fecha;}
}
