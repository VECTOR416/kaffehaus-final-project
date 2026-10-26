package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.time.LocalDateTime;

/**
 * Entrada de RegistrarAperturaUseCase. No viaja por HTTP: la arma AbrirDetalleLoteUseCase con
 * datos ya validados, por eso su factoria es crear(...).
 */
public class RegistrarAperturaRequest {
    private String idDetalleLote;
    private String idUsuario;
    private LocalDateTime fecha;

    private RegistrarAperturaRequest(String idDetalleLote, String idUsuario, LocalDateTime fecha) {
        this.idDetalleLote = idDetalleLote;
        this.idUsuario = idUsuario;
        this.fecha = fecha;
    }

    public static RegistrarAperturaRequest crear(String idDetalleLote, String idUsuario, LocalDateTime fecha) {
        return new RegistrarAperturaRequest(idDetalleLote, idUsuario, fecha != null ? fecha : LocalDateTime.now());
    }

    public String getIdDetalleLote() {return idDetalleLote;}
    public String getIdUsuario() {return idUsuario;}
    public LocalDateTime getFecha() {return fecha;}
}
