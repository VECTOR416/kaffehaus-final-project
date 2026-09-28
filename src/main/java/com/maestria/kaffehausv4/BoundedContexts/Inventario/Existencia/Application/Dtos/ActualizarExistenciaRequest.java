package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Correccion de una existencia. Solo admite 'fechaVencimiento' e 'idUsuario': el contenido, el
 * estado, la unidad, el detalle de origen y las fechas de apertura y acabado los mueven la
 * apertura, los consumos, las mermas y los ajustes, cada uno con su movimiento
 * (inventario.md INV-40, INV-43). Se rechazan en lugar de ignorarse.
 */
public class ActualizarExistenciaRequest {
    private static final List<String> CAMPOS_INMUTABLES = List.of(
            "contenidoInicial", "contenidoActual", "fechaAcabado", "fechaApertura", "idDetalleLote", "idEstadoExistencia", "idUnidad");

    private BigDecimal contenidoInicial;
    private BigDecimal contenidoActual;
    private LocalDateTime fechaAcabado;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaApertura;
    private String idDetalleLote;
    private String idEstadoExistencia;
    private String idUnidad;
    private String idUsuario;

    private ActualizarExistenciaRequest(
        BigDecimal contenidoInicial,
        BigDecimal contenidoActual,
        LocalDateTime fechaAcabado,
        LocalDate fechaVencimiento,
        LocalDateTime fechaApertura,
        String idDetalleLote,
        String idEstadoExistencia,
        String idUnidad,
        String idUsuario
    ) {
        this.contenidoInicial = contenidoInicial;
        this.contenidoActual = contenidoActual;
        this.fechaAcabado = fechaAcabado;
        this.fechaVencimiento = fechaVencimiento;
        this.fechaApertura = fechaApertura;
        this.idDetalleLote = idDetalleLote;
        this.idEstadoExistencia = idEstadoExistencia;
        this.idUnidad = idUnidad;
        this.idUsuario = idUsuario;
    }

    public static ActualizarExistenciaRequest createFromRequest(Map<String, Object> request){
        for (String campo : CAMPOS_INMUTABLES) {
            if (request.get(campo) != null) {
                throw DomainException.throwException(400, "El campo '" + campo
                        + "' de una existencia no se modifica a mano; usa la apertura, una merma o un ajuste");
            }
        }

        Double contenidoInicialTmp = Validador.numeroOpcional(request.get("contenidoInicial"), "contenidoInicial");
        BigDecimal contenidoInicial = contenidoInicialTmp != null ? BigDecimal.valueOf(contenidoInicialTmp) : null;
        Double contenidoActualTmp = Validador.numeroOpcional(request.get("contenidoActual"), "contenidoActual");
        BigDecimal contenidoActual = contenidoActualTmp != null ? BigDecimal.valueOf(contenidoActualTmp) : null;
        LocalDateTime fechaAcabado = Validador.fechaOpcional(request.get("fechaAcabado"), "fechaAcabado");
        LocalDateTime fechaVencimientoTmp = Validador.fechaOpcional(request.get("fechaVencimiento"), "fechaVencimiento");
        LocalDate fechaVencimiento = fechaVencimientoTmp != null ? fechaVencimientoTmp.toLocalDate() : null;
        LocalDateTime fechaApertura = Validador.fechaOpcional(request.get("fechaApertura"), "fechaApertura");
        String idDetalleLote = Validador.textoOpcional(request.get("idDetalleLote"), "idDetalleLote");
        String idEstadoExistencia = Validador.textoOpcional(request.get("idEstadoExistencia"), "idEstadoExistencia");
        String idUnidad = Validador.textoOpcional(request.get("idUnidad"), "idUnidad");
        String idUsuario = Validador.textoOpcional(request.get("idUsuario"), "idUsuario");

        return new ActualizarExistenciaRequest(contenidoInicial, contenidoActual, fechaAcabado, fechaVencimiento, fechaApertura, idDetalleLote, idEstadoExistencia, idUnidad, idUsuario);
    }

    public BigDecimal getContenidoInicial() {return contenidoInicial;}
    public BigDecimal getContenidoActual() {return contenidoActual;}
    public LocalDateTime getFechaAcabado() {return fechaAcabado;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public LocalDateTime getFechaApertura() {return fechaApertura;}
    public String getIdDetalleLote() {return idDetalleLote;}
    public String getIdEstadoExistencia() {return idEstadoExistencia;}
    public String getIdUnidad() {return idUnidad;}
    public String getIdUsuario() {return idUsuario;}
}
