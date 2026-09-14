package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;

public class Venta {
    private VentaId id;
    private LocalDateTime fechaVenta;
    private BigDecimal total;
    private UsuarioId usuarioId;
    private ClienteId clienteId;
    private EstadoVentaId estadoVentaId;

    private Venta(
        VentaId id,
        LocalDateTime fechaVenta,
        BigDecimal total,
        UsuarioId usuarioId,
        ClienteId clienteId,
        EstadoVentaId estadoVentaId
    ){
        this.id = id;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.usuarioId = usuarioId;
        this.clienteId = clienteId;
        this.estadoVentaId = estadoVentaId;
    }

    public static Venta restaurar(
        String id,
        LocalDateTime fechaVenta,
        BigDecimal total,
        UsuarioId usuarioId,
        ClienteId clienteId,
        EstadoVentaId estadoVentaId
    ){
        return new Venta(new VentaId(id), fechaVenta, total, usuarioId, clienteId, estadoVentaId);
    }

    public static Venta crear(
        LocalDateTime fechaVenta,
        BigDecimal total,
        UsuarioId usuarioId,
        ClienteId clienteId,
        EstadoVentaId estadoVentaId
    ){
        return new Venta(null, fechaVenta, total, usuarioId, clienteId, estadoVentaId);
    }

    public void actualizar(
        LocalDateTime fechaVenta,
        BigDecimal total,
        UsuarioId usuarioId,
        ClienteId clienteId,
        EstadoVentaId estadoVentaId
    ){
        this.fechaVenta = fechaVenta != null ? fechaVenta : this.fechaVenta;
        this.total = total != null ? total : this.total;
        this.usuarioId = usuarioId != null ? usuarioId : this.usuarioId;
        this.clienteId = clienteId != null ? clienteId : this.clienteId;
        this.estadoVentaId = estadoVentaId != null ? estadoVentaId : this.estadoVentaId;
    }

    public VentaId getId() {return id;}
    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public BigDecimal getTotal() {return total;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public ClienteId getClienteId() {return clienteId;}
    public EstadoVentaId getEstadoVentaId() {return estadoVentaId;}
}
