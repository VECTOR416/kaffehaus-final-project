package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;

public class LoteResponse {
    private String id;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String idProveedor;
    private String idUsuario;
    private boolean borrado;

    private LoteResponse(
        String id,
        LocalDateTime fechaCompra,
        BigDecimal total,
        String idProveedor,
        String idUsuario,
        boolean borrado
    ) {
        this.id = id;
        this.fechaCompra = fechaCompra;
        this.total = total;
        this.idProveedor = idProveedor;
        this.idUsuario = idUsuario;
        this.borrado = borrado;
    }

    public static LoteResponse createFromAggregate(Lote lote){
        return new LoteResponse(
            lote.getId().getValue(),
            lote.getFechaCompra(),
            lote.getTotal(),
            lote.getProveedorId() != null ? lote.getProveedorId().getValue() : null,
            lote.getUsuarioId() != null ? lote.getUsuarioId().getValue() : null,
            lote.isBorrado()
        );
    }

    public String getId() {return id;}
    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public BigDecimal getTotal() {return total;}
    public String getIdProveedor() {return idProveedor;}
    public String getIdUsuario() {return idUsuario;}
    public boolean isBorrado() {return borrado;}
}
