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
    private boolean borrado;

    private CrearLoteRequest(
        LocalDateTime fechaCompra,
        BigDecimal total,
        String idProveedor,
        String idUsuario,
        boolean borrado
    ) {
        this.fechaCompra = fechaCompra;
        this.total = total;
        this.idProveedor = idProveedor;
        this.idUsuario = idUsuario;
        this.borrado = borrado;
    }

    public static CrearLoteRequest createFromRequest(Map<String, Object> request){
        LocalDateTime fechaCompra = Validador.fechaOpcional(request.get("fechaCompra"), "fechaCompra");
        fechaCompra = fechaCompra != null ? fechaCompra : LocalDateTime.now();
        BigDecimal total = BigDecimal.valueOf(Validador.numeroRequerido(request.get("total"), "total"));
        String idProveedor = Validador.textoRequerido(request.get("idProveedor"), "idProveedor");
        String idUsuario = Validador.textoRequerido(request.get("idUsuario"), "idUsuario");

        return new CrearLoteRequest(fechaCompra, total, idProveedor, idUsuario, false);
    }

    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public void setFechaCompra(LocalDateTime fechaCompra) {this.fechaCompra = fechaCompra;}
    public BigDecimal getTotal() {return total;}
    public void setTotal(BigDecimal total) {this.total = total;}
    public String getIdProveedor() {return idProveedor;}
    public void setIdProveedor(String idProveedor) {this.idProveedor = idProveedor;}
    public String getIdUsuario() {return idUsuario;}
    public void setIdUsuario(String idUsuario) {this.idUsuario = idUsuario;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
