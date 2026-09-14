package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

public class Lote {
    private LoteId id;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private ProveedorId proveedorId;
    private UsuarioId usuarioId;
    private boolean borrado;

    private Lote(
        LoteId id,
        LocalDateTime fechaCompra,
        BigDecimal total,
        ProveedorId proveedorId,
        UsuarioId usuarioId,
        boolean borrado
    ){
        this.id = id;
        this.fechaCompra = fechaCompra;
        this.total = total;
        this.proveedorId = proveedorId;
        this.usuarioId = usuarioId;
        this.borrado = borrado;
    }

    public static Lote restaurar(
        String id,
        LocalDateTime fechaCompra,
        BigDecimal total,
        ProveedorId proveedorId,
        UsuarioId usuarioId,
        boolean borrado
    ){
        return new Lote(new LoteId(id), fechaCompra, total, proveedorId, usuarioId, borrado);
    }

    public static Lote crear(
        LocalDateTime fechaCompra,
        BigDecimal total,
        ProveedorId proveedorId,
        UsuarioId usuarioId
    ){
        return new Lote(null, fechaCompra, total, proveedorId, usuarioId, false);
    }

    public void actualizar(
        LocalDateTime fechaCompra,
        BigDecimal total,
        ProveedorId proveedorId,
        UsuarioId usuarioId
    ){
        this.fechaCompra = fechaCompra != null ? fechaCompra : this.fechaCompra;
        this.total = total != null ? total : this.total;
        this.proveedorId = proveedorId != null ? proveedorId : this.proveedorId;
        this.usuarioId = usuarioId != null ? usuarioId : this.usuarioId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    public void reactivar(){
        this.borrado = false;
    }

    public LoteId getId() {return id;}
    public void setId(LoteId id) {this.id = id;}
    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public void setFechaCompra(LocalDateTime fechaCompra) {this.fechaCompra = fechaCompra;}
    public BigDecimal getTotal() {return total;}
    public void setTotal(BigDecimal total) {this.total = total;}
    public ProveedorId getProveedorId() {return proveedorId;}
    public void setProveedorId(ProveedorId proveedorId) {this.proveedorId = proveedorId;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public void setUsuarioId(UsuarioId usuarioId) {this.usuarioId = usuarioId;}
    public boolean isBorrado() {return borrado;}
    public void setBorrado(boolean borrado) {this.borrado = borrado;}
}
