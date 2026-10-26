package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;

public class Descuento {
    private DescuentoId id;
    private BigDecimal descuento;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFinal;
    private boolean estado;
    private ProductoId productoId;
    private TipoDescuentoId tipoDescuentoId;
    private boolean borrado;

    private Descuento(
        DescuentoId id,
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        boolean estado,
        ProductoId productoId,
        TipoDescuentoId tipoDescuentoId,
        boolean borrado
    ){
        this.id = id;
        this.descuento = descuento;
        this.fechaInicio = fechaInicio;
        this.fechaFinal = fechaFinal;
        this.estado = estado;
        this.productoId = productoId;
        this.tipoDescuentoId = tipoDescuentoId;
        this.borrado = borrado;
    }

    public static Descuento restaurar(
        String id,
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        boolean estado,
        ProductoId productoId,
        TipoDescuentoId tipoDescuentoId,
        boolean borrado
    ){
        return new Descuento(new DescuentoId(id), descuento, fechaInicio, fechaFinal, estado, productoId, tipoDescuentoId, borrado);
    }

    public static Descuento crear(
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        boolean estado,
        ProductoId productoId,
        TipoDescuentoId tipoDescuentoId
    ){
        return new Descuento(null, descuento, fechaInicio, fechaFinal, estado, productoId, tipoDescuentoId, false);
    }

    public void actualizar(
        BigDecimal descuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        Boolean estado,
        ProductoId productoId,
        TipoDescuentoId tipoDescuentoId
    ){
        this.descuento = descuento != null ? descuento : this.descuento;
        this.fechaInicio = fechaInicio != null ? fechaInicio : this.fechaInicio;
        this.fechaFinal = fechaFinal != null ? fechaFinal : this.fechaFinal;
        this.estado = estado != null ? estado : this.estado;
        this.productoId = productoId != null ? productoId : this.productoId;
        this.tipoDescuentoId = tipoDescuentoId != null ? tipoDescuentoId : this.tipoDescuentoId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    /**
     * Exige un valor coherente con su tipo y una ventana valida (ventas.md VEN-14): un
     * porcentaje en (0, 100], un monto fijo mayor a cero y fecha_inicio <= fecha_final.
     */
    public void validarCon(TipoDescuento tipo) {
        if (!tipo.esAplicable()) {
            throw DomainException.throwException(409,
                    "El tipo de descuento '" + tipo.getNombre() + "' no es aplicable; usa 'Porcentaje' o 'Monto fijo'");
        }
        if (this.descuento == null || this.descuento.signum() <= 0) {
            throw DomainException.throwException(400, "El descuento debe ser mayor a cero");
        }
        if (tipo.esPorcentaje() && this.descuento.compareTo(new BigDecimal("100")) > 0) {
            throw DomainException.throwException(400, "Un descuento porcentual no puede superar el 100");
        }
        if (this.fechaInicio != null && this.fechaFinal != null && this.fechaInicio.isAfter(this.fechaFinal)) {
            throw DomainException.throwException(400, "La fecha de inicio del descuento no puede ser posterior a la final");
        }
    }

    /**
     * Importe que este descuento resta a una linea (ventas.md VEN-14): porcentaje sobre el
     * importe, o monto fijo por unidad sin superar el precio. Nunca supera precio x cantidad.
     */
    public BigDecimal importeSobre(BigDecimal precioUnitario, BigDecimal cantidad, TipoDescuento tipo) {
        BigDecimal bruto = precioUnitario.multiply(cantidad);
        BigDecimal importe;
        if (tipo.esPorcentaje()) {
            importe = bruto.multiply(this.descuento).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        } else if (tipo.esMontoFijo()) {
            importe = this.descuento.min(precioUnitario).multiply(cantidad);
        } else {
            return BigDecimal.ZERO.setScale(2);
        }
        return importe.min(bruto).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public DescuentoId getId() {return id;}
    public BigDecimal getDescuento() {return descuento;}
    public LocalDateTime getFechaInicio() {return fechaInicio;}
    public LocalDateTime getFechaFinal() {return fechaFinal;}
    public boolean isEstado() {return estado;}
    public ProductoId getProductoId() {return productoId;}
    public TipoDescuentoId getTipoDescuentoId() {return tipoDescuentoId;}
    public boolean isBorrado() {return borrado;}
}
