package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;

/**
 * Respuesta del agregado completo: encabezado mas sus lineas vivas. Las lineas dadas de baja
 * no se devuelven, igual que el resto del proyecto oculta lo borrado logicamente.
 */
public class LoteResponse {
    private String id;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String idProveedor;
    private String idUsuario;
    private boolean borrado;
    private List<DetalleLoteResponse> detalles;

    private LoteResponse(
        String id,
        LocalDateTime fechaCompra,
        BigDecimal total,
        String idProveedor,
        String idUsuario,
        boolean borrado,
        List<DetalleLoteResponse> detalles
    ) {
        this.id = id;
        this.fechaCompra = fechaCompra;
        this.total = total;
        this.idProveedor = idProveedor;
        this.idUsuario = idUsuario;
        this.borrado = borrado;
        this.detalles = detalles;
    }

    public static LoteResponse createFromAggregate(Lote lote){
        List<DetalleLoteResponse> detalles = lote.getDetallesActivos().stream()
                .map(DetalleLoteResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new LoteResponse(
            lote.getId().getValue(),
            lote.getFechaCompra(),
            lote.getTotal(),
            lote.getProveedorId() != null ? lote.getProveedorId().getValue() : null,
            lote.getUsuarioId() != null ? lote.getUsuarioId().getValue() : null,
            lote.isBorrado(),
            detalles
        );
    }

    public String getId() {return id;}
    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public BigDecimal getTotal() {return total;}
    public String getIdProveedor() {return idProveedor;}
    public String getIdUsuario() {return idUsuario;}
    public boolean isBorrado() {return borrado;}
    public List<DetalleLoteResponse> getDetalles() {return detalles;}
}
