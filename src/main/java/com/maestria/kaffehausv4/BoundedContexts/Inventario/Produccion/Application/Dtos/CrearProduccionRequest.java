package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Alta de una produccion MTS.
 *
 * No admite 'diferencia': es derivada (cantidadReal - cantidadEsperada) y la calcula la raiz.
 * 'idUsuario' firma el lote y los movimientos que genera, y 'fechaVencimiento' es la
 * caducidad del producto terminado, que se copia a su detalle de lote y a su existencia.
 */
public class CrearProduccionRequest {
    private BigDecimal cantidadEsperada;
    private BigDecimal cantidadReal;
    private String motivoDiferencia;
    private LocalDateTime fechaProduccion;
    private LocalDate fechaVencimiento;
    private String idReceta;
    private String idUsuario;

    private CrearProduccionRequest(
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        LocalDate fechaVencimiento,
        String idReceta,
        String idUsuario
    ) {
        this.cantidadEsperada = cantidadEsperada;
        this.cantidadReal = cantidadReal;
        this.motivoDiferencia = motivoDiferencia;
        this.fechaProduccion = fechaProduccion;
        this.fechaVencimiento = fechaVencimiento;
        this.idReceta = idReceta;
        this.idUsuario = idUsuario;
    }

    public static CrearProduccionRequest createFromRequest(Map<String, Object> request){
        BigDecimal cantidadEsperada = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidadEsperada"), "cantidadEsperada"));
        BigDecimal cantidadReal = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidadReal"), "cantidadReal"));
        String motivoDiferencia = Validador.textoOpcional(request.get("motivoDiferencia"), "motivoDiferencia");
        LocalDateTime fechaProduccion = Validador.fechaOpcional(request.get("fechaProduccion"), "fechaProduccion");
        fechaProduccion = fechaProduccion != null ? fechaProduccion : LocalDateTime.now();
        LocalDate fechaVencimiento = Validador.fechaRequerida(request.get("fechaVencimiento"), "fechaVencimiento").toLocalDate();
        String idReceta = Validador.textoRequerido(request.get("idReceta"), "idReceta");
        String idUsuario = Validador.textoRequerido(request.get("idUsuario"), "idUsuario");

        return new CrearProduccionRequest(cantidadEsperada, cantidadReal, motivoDiferencia, fechaProduccion, fechaVencimiento, idReceta, idUsuario);
    }

    public BigDecimal getCantidadEsperada() {return cantidadEsperada;}
    public BigDecimal getCantidadReal() {return cantidadReal;}
    public String getMotivoDiferencia() {return motivoDiferencia;}
    public LocalDateTime getFechaProduccion() {return fechaProduccion;}
    public LocalDate getFechaVencimiento() {return fechaVencimiento;}
    public String getIdReceta() {return idReceta;}
    public String getIdUsuario() {return idUsuario;}
}
