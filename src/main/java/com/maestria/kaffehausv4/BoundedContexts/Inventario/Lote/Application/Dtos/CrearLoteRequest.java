package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearLoteRequest {
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String idProveedor;
    private String idUsuario;

    private CrearLoteRequest(
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

    public static CrearLoteRequest createFromRequest(Map<String, Object> request){
        LocalDateTime fechaCompra = Validador.fechaOpcional(request.get("fechaCompra"), "fechaCompra");
        fechaCompra = fechaCompra != null ? fechaCompra : LocalDateTime.now();
        BigDecimal total = BigDecimal.valueOf(Validador.numeroRequerido(request.get("total"), "total"));
        String idProveedor = Validador.textoRequerido(request.get("idProveedor"), "idProveedor");
        String idUsuario = Validador.textoRequerido(request.get("idUsuario"), "idUsuario");

        return new CrearLoteRequest(fechaCompra, total, idProveedor, idUsuario);
    }

    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public BigDecimal getTotal() {return total;}
    public String getIdProveedor() {return idProveedor;}
    public String getIdUsuario() {return idUsuario;}
}
