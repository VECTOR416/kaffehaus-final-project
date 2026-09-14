package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class CrearDetalleVentaRequest {
    private BigDecimal precioUnitario;
    private BigDecimal cantidad;
    private BigDecimal subtotal;
    private BigDecimal descuentoAplicado;
    private boolean estado;
    private String idVenta;
    private String idProducto;

    private CrearDetalleVentaRequest(
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        boolean estado,
        String idVenta,
        String idProducto
    ) {
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
        this.descuentoAplicado = descuentoAplicado;
        this.estado = estado;
        this.idVenta = idVenta;
        this.idProducto = idProducto;
    }

    public static CrearDetalleVentaRequest createFromRequest(Map<String, Object> request){
        BigDecimal precioUnitario = BigDecimal.valueOf(Validador.numeroRequerido(request.get("precioUnitario"), "precioUnitario"));
        BigDecimal cantidad = BigDecimal.valueOf(Validador.numeroRequerido(request.get("cantidad"), "cantidad"));
        BigDecimal subtotal = BigDecimal.valueOf(Validador.numeroRequerido(request.get("subtotal"), "subtotal"));
        BigDecimal descuentoAplicado = BigDecimal.valueOf(Validador.numeroRequerido(request.get("descuentoAplicado"), "descuentoAplicado"));
        Boolean estadoTmp = Validador.booleanoOpcional(request.get("estado"), "estado");
        boolean estado = estadoTmp != null ? estadoTmp : true;
        String idVenta = Validador.textoRequerido(request.get("idVenta"), "idVenta");
        String idProducto = Validador.textoRequerido(request.get("idProducto"), "idProducto");

        return new CrearDetalleVentaRequest(precioUnitario, cantidad, subtotal, descuentoAplicado, estado, idVenta, idProducto);
    }

    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public void setPrecioUnitario(BigDecimal precioUnitario) {this.precioUnitario = precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public BigDecimal getSubtotal() {return subtotal;}
    public void setSubtotal(BigDecimal subtotal) {this.subtotal = subtotal;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
    public void setDescuentoAplicado(BigDecimal descuentoAplicado) {this.descuentoAplicado = descuentoAplicado;}
    public boolean isEstado() {return estado;}
    public void setEstado(boolean estado) {this.estado = estado;}
    public String getIdVenta() {return idVenta;}
    public void setIdVenta(String idVenta) {this.idVenta = idVenta;}
    public String getIdProducto() {return idProducto;}
    public void setIdProducto(String idProducto) {this.idProducto = idProducto;}
}
