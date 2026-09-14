package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos;

import java.math.BigDecimal;
import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

public class ActualizarDetalleVentaRequest {
    private BigDecimal precioUnitario;
    private BigDecimal cantidad;
    private BigDecimal subtotal;
    private BigDecimal descuentoAplicado;
    private Boolean estado;
    private String idVenta;
    private String idProducto;

    private ActualizarDetalleVentaRequest(
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        Boolean estado,
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

    public static ActualizarDetalleVentaRequest createFromRequest(Map<String, Object> request){
        Double precioUnitarioTmp = Validador.numeroOpcional(request.get("precioUnitario"), "precioUnitario");
        BigDecimal precioUnitario = precioUnitarioTmp != null ? BigDecimal.valueOf(precioUnitarioTmp) : null;
        Double cantidadTmp = Validador.numeroOpcional(request.get("cantidad"), "cantidad");
        BigDecimal cantidad = cantidadTmp != null ? BigDecimal.valueOf(cantidadTmp) : null;
        Double subtotalTmp = Validador.numeroOpcional(request.get("subtotal"), "subtotal");
        BigDecimal subtotal = subtotalTmp != null ? BigDecimal.valueOf(subtotalTmp) : null;
        Double descuentoAplicadoTmp = Validador.numeroOpcional(request.get("descuentoAplicado"), "descuentoAplicado");
        BigDecimal descuentoAplicado = descuentoAplicadoTmp != null ? BigDecimal.valueOf(descuentoAplicadoTmp) : null;
        Boolean estadoTmp = Validador.booleanoOpcional(request.get("estado"), "estado");
        Boolean estado = estadoTmp;
        String idVenta = Validador.textoOpcional(request.get("idVenta"), "idVenta");
        String idProducto = Validador.textoOpcional(request.get("idProducto"), "idProducto");

        return new ActualizarDetalleVentaRequest(precioUnitario, cantidad, subtotal, descuentoAplicado, estado, idVenta, idProducto);
    }

    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public void setPrecioUnitario(BigDecimal precioUnitario) {this.precioUnitario = precioUnitario;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public BigDecimal getSubtotal() {return subtotal;}
    public void setSubtotal(BigDecimal subtotal) {this.subtotal = subtotal;}
    public BigDecimal getDescuentoAplicado() {return descuentoAplicado;}
    public void setDescuentoAplicado(BigDecimal descuentoAplicado) {this.descuentoAplicado = descuentoAplicado;}
    public Boolean getEstado() {return estado;}
    public void setEstado(Boolean estado) {this.estado = estado;}
    public String getIdVenta() {return idVenta;}
    public void setIdVenta(String idVenta) {this.idVenta = idVenta;}
    public String getIdProducto() {return idProducto;}
    public void setIdProducto(String idProducto) {this.idProducto = idProducto;}
}
