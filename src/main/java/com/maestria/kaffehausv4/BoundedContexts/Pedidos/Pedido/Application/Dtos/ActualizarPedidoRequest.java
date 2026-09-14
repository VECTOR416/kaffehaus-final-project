package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarPedidoRequest {
    private LocalDateTime fechaEntrega;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String descripcion;
    private String idVenta;
    private String idEstado;

    private ActualizarPedidoRequest(
        LocalDateTime fechaEntrega,
        BigDecimal latitud,
        BigDecimal longitud,
        String descripcion,
        String idVenta,
        String idEstado
    ) {
        this.fechaEntrega = fechaEntrega;
        this.latitud = latitud;
        this.longitud = longitud;
        this.descripcion = descripcion;
        this.idVenta = idVenta;
        this.idEstado = idEstado;
    }

    public static ActualizarPedidoRequest createFromRequest(Map<String, Object> request){
        LocalDateTime fechaEntrega = Validador.fechaOpcional(request.get("fechaEntrega"), "fechaEntrega");
        Double latitudTmp = Validador.numeroOpcional(request.get("latitud"), "latitud");
        BigDecimal latitud = latitudTmp != null ? BigDecimal.valueOf(latitudTmp) : null;
        Double longitudTmp = Validador.numeroOpcional(request.get("longitud"), "longitud");
        BigDecimal longitud = longitudTmp != null ? BigDecimal.valueOf(longitudTmp) : null;
        String descripcion = Validador.textoOpcional(request.get("descripcion"), "descripcion");
        String idVenta = Validador.textoOpcional(request.get("idVenta"), "idVenta");
        String idEstado = Validador.textoOpcional(request.get("idEstado"), "idEstado");

        return new ActualizarPedidoRequest(fechaEntrega, latitud, longitud, descripcion, idVenta, idEstado);
    }

    public LocalDateTime getFechaEntrega() {return fechaEntrega;}
    public void setFechaEntrega(LocalDateTime fechaEntrega) {this.fechaEntrega = fechaEntrega;}
    public BigDecimal getLatitud() {return latitud;}
    public void setLatitud(BigDecimal latitud) {this.latitud = latitud;}
    public BigDecimal getLongitud() {return longitud;}
    public void setLongitud(BigDecimal longitud) {this.longitud = longitud;}
    public String getDescripcion() {return descripcion;}
    public void setDescripcion(String descripcion) {this.descripcion = descripcion;}
    public String getIdVenta() {return idVenta;}
    public void setIdVenta(String idVenta) {this.idVenta = idVenta;}
    public String getIdEstado() {return idEstado;}
    public void setIdEstado(String idEstado) {this.idEstado = idEstado;}
}
