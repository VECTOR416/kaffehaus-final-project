package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarLoteRequest {
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String idProveedor;
    private String idUsuario;

    private ActualizarLoteRequest(
        LocalDateTime fechaCompra,
        BigDecimal total,
        String idProveedor,
        String idUsuario
    ) {
        this.fechaCompra = fechaCompra;
        this.total = total;
        this.idProveedor = idProveedor;
        this.idUsuario = idUsuario;
    }

    public static ActualizarLoteRequest createFromRequest(Map<String, Object> request){
        LocalDateTime fechaCompra = Validador.fechaOpcional(request.get("fechaCompra"), "fechaCompra");
        Double totalTmp = Validador.numeroOpcional(request.get("total"), "total");
        BigDecimal total = totalTmp != null ? BigDecimal.valueOf(totalTmp) : null;
        String idProveedor = Validador.textoOpcional(request.get("idProveedor"), "idProveedor");
        String idUsuario = Validador.textoOpcional(request.get("idUsuario"), "idUsuario");

        return new ActualizarLoteRequest(fechaCompra, total, idProveedor, idUsuario);
    }

    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public BigDecimal getTotal() {return total;}
    public String getIdProveedor() {return idProveedor;}
    public String getIdUsuario() {return idUsuario;}
}
