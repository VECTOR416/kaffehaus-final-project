package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entrada de RegistrarReintegroDeInventarioUseCase. No viaja por HTTP: la arma la anulacion de
 * una venta, por eso su factoria es crear(...).
 *
 * 'motivoOrigen' identifica los movimientos a compensar (los de la venta citan
 * "Venta '<id>'"); 'nombresTiposSalida' dice cuales de ellos son salidas que se reintegran.
 */
public class RegistrarReintegroDeInventarioRequest {
    private String motivoOrigen;
    private List<String> nombresTiposSalida;
    private String motivoReintegro;
    private String idUsuario;
    private LocalDateTime fecha;

    private RegistrarReintegroDeInventarioRequest(
        String motivoOrigen,
        List<String> nombresTiposSalida,
        String motivoReintegro,
        String idUsuario,
        LocalDateTime fecha
    ) {
        this.motivoOrigen = motivoOrigen;
        this.nombresTiposSalida = nombresTiposSalida;
        this.motivoReintegro = motivoReintegro;
        this.idUsuario = idUsuario;
        this.fecha = fecha;
    }

    public static RegistrarReintegroDeInventarioRequest crear(
        String motivoOrigen,
        List<String> nombresTiposSalida,
        String motivoReintegro,
        String idUsuario,
        LocalDateTime fecha
    ) {
        return new RegistrarReintegroDeInventarioRequest(
                motivoOrigen, List.copyOf(nombresTiposSalida), motivoReintegro, idUsuario, fecha != null ? fecha : LocalDateTime.now());
    }

    public String getMotivoOrigen() {return motivoOrigen;}
    public List<String> getNombresTiposSalida() {return nombresTiposSalida;}
    public String getMotivoReintegro() {return motivoReintegro;}
    public String getIdUsuario() {return idUsuario;}
    public LocalDateTime getFecha() {return fecha;}
}
