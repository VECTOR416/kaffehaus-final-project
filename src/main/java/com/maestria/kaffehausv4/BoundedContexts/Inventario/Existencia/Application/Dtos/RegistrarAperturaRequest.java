package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.time.LocalDateTime;

/**
 * Entrada de RegistrarAperturaUseCase. No viaja por HTTP: la arman AbrirDetalleLoteUseCase y
 * DarDeBajaDetalleVencidoUseCase con datos ya validados, por eso su factoria es crear(...).
 *
 * 'permitirVencido' solo lo activa la baja de unidades cerradas vencidas, que las abre para
 * registrar la perdida en el libro mayor (inventario.md INV-42). Una apertura normal de un
 * detalle vencido se rechaza (INV-11).
 */
public class RegistrarAperturaRequest {
    private String idDetalleLote;
    private String idUsuario;
    private boolean permitirVencido;
    private LocalDateTime fecha;

    private RegistrarAperturaRequest(String idDetalleLote, String idUsuario, boolean permitirVencido, LocalDateTime fecha) {
        this.idDetalleLote = idDetalleLote;
        this.idUsuario = idUsuario;
        this.permitirVencido = permitirVencido;
        this.fecha = fecha;
    }

    public static RegistrarAperturaRequest crear(String idDetalleLote, String idUsuario, boolean permitirVencido, LocalDateTime fecha) {
        return new RegistrarAperturaRequest(idDetalleLote, idUsuario, permitirVencido, fecha != null ? fecha : LocalDateTime.now());
    }

    public String getIdDetalleLote() {return idDetalleLote;}
    public String getIdUsuario() {return idUsuario;}
    public boolean isPermitirVencido() {return permitirVencido;}
    public LocalDateTime getFecha() {return fecha;}
}
