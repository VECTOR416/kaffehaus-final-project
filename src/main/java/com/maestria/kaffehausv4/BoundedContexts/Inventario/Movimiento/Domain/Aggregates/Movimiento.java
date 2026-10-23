package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;

/**
 * Raiz del agregado Movimiento: el registro de auditoria de una entrada o salida de inventario
 * (logica.md MOVIMIENTO-20).
 *
 * Su sujeto es exactamente uno: una existencia o una linea de lote (MOVIMIENTO-21). Puede llevar
 * ademas el documento que lo explica, una produccion o una venta, nunca los dos (MOVIMIENTO-04).
 *
 * Es inmutable. Un movimiento registrado no se corrige ni se da de baja, ni siquiera de forma
 * logica: esconderlo romperia la traza que explica la cantidad actual de su sujeto. Un error se
 * corrige registrando un movimiento compensatorio.
 *
 * La cantidad es una magnitud mayor a cero; el tipo de movimiento le da el sentido
 * (MOVIMIENTO-22). La unica excepcion es la apertura de una unidad cerrada, que deja constancia
 * de que se abrio sin mover contenido y por eso registra cantidad 0 (MOVIMIENTO-08).
 */
public class Movimiento {
    private MovimientoId id;
    private BigDecimal cantidad;
    private LocalDateTime fechaMovimiento;
    private String motivo;
    private ExistenciaId existenciaId;
    private DetalleLoteId detalleLoteId;
    private TipoMovimientoId tipoMovimientoId;
    private UsuarioId usuarioId;
    private ProduccionId produccionId;
    private VentaId ventaId;
    private boolean borrado;

    private Movimiento(
        MovimientoId id,
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        DetalleLoteId detalleLoteId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        ProduccionId produccionId,
        VentaId ventaId,
        boolean borrado
    ){
        this.id = id;
        this.cantidad = cantidad;
        this.fechaMovimiento = fechaMovimiento;
        this.motivo = motivo;
        this.existenciaId = existenciaId;
        this.detalleLoteId = detalleLoteId;
        this.tipoMovimientoId = tipoMovimientoId;
        this.usuarioId = usuarioId;
        this.produccionId = produccionId;
        this.ventaId = ventaId;
        this.borrado = borrado;
    }

    public static Movimiento restaurar(
        String id,
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        DetalleLoteId detalleLoteId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        ProduccionId produccionId,
        VentaId ventaId,
        boolean borrado
    ){
        return new Movimiento(new MovimientoId(id), cantidad, fechaMovimiento, motivo, existenciaId, detalleLoteId,
                tipoMovimientoId, usuarioId, produccionId, ventaId, borrado);
    }

    public static Movimiento crear(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        ProduccionId produccionId
    ){
        exigirCantidadPositiva(cantidad);
        return nuevo(cantidad, fechaMovimiento, motivo, existenciaId, null, tipoMovimientoId, usuarioId, produccionId, null);
    }

    /**
     * Movimiento de una operacion sobre el contenido de una existencia: el consumo de una
     * produccion o de una venta, o el reintegro de una venta anulada (logica.md MOVIMIENTO-19,
     * VENTA-25). Lleva el documento que lo explica: una produccion o una venta.
     */
    public static Movimiento registrarSobreExistencia(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        ProduccionId produccionId,
        VentaId ventaId
    ){
        exigirCantidadPositiva(cantidad);
        return nuevo(cantidad, fechaMovimiento, motivo, existenciaId, null, tipoMovimientoId, usuarioId, produccionId, ventaId);
    }

    /**
     * Movimiento de una operacion sobre las unidades de una linea de lote: lo que una produccion o
     * una venta descontaron de ella, o lo que le devolvio el reintegro (logica.md LOTE-17,
     * LOTE-24). Lleva el documento que lo explica: una produccion o una venta.
     */
    public static Movimiento registrarSobreLinea(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        DetalleLoteId detalleLoteId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        ProduccionId produccionId,
        VentaId ventaId
    ){
        exigirCantidadPositiva(cantidad);
        return nuevo(cantidad, fechaMovimiento, motivo, null, detalleLoteId, tipoMovimientoId, usuarioId, produccionId, ventaId);
    }

    private static void exigirCantidadPositiva(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw DomainException.throwException(400, "La cantidad del movimiento debe ser mayor a cero");
        }
    }

    /**
     * Registra la apertura de una unidad cerrada sobre la existencia que nace de ella (logica.md
     * EXISTENCIA-13). La cantidad es 0: solo deja constancia de que se abrio, porque el
     * contenido inicial de la existencia ya dice cuanto trajo.
     */
    public static Movimiento registrarApertura(
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoApertura,
        UsuarioId usuarioId
    ){
        return nuevo(BigDecimal.ZERO, fechaMovimiento, motivo, existenciaId, null, tipoApertura, usuarioId, null, null);
    }

    /**
     * Punto unico de alta: exige exactamente un sujeto y a lo sumo un documento (logica.md
     * MOVIMIENTO-21, MOVIMIENTO-04). Son invariantes internas: ningun movimiento se crea por API
     * (MOVIMIENTO-17), asi que romperlas es un fallo del sistema, no del cliente.
     */
    private static Movimiento nuevo(
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        DetalleLoteId detalleLoteId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        ProduccionId produccionId,
        VentaId ventaId
    ){
        if ((existenciaId == null) == (detalleLoteId == null)) {
            throw SystemException.throwException(500,
                    "Un movimiento debe apuntar a exactamente una existencia o una linea de lote");
        }
        if (produccionId != null && ventaId != null) {
            throw SystemException.throwException(500,
                    "Un movimiento no puede pertenecer a una produccion y a una venta a la vez");
        }
        return new Movimiento(null, cantidad, fechaMovimiento, motivo, existenciaId, detalleLoteId,
                tipoMovimientoId, usuarioId, produccionId, ventaId, false);
    }

    public void actualizar(){
        throw DomainException.throwException(409,
                "Los movimientos son inmutables; registra un movimiento compensatorio para corregirlo");
    }

    public void eliminar(){
        throw DomainException.throwException(409,
                "Los movimientos son inmutables y no se pueden dar de baja; registra un movimiento compensatorio");
    }

    public MovimientoId getId() {return id;}
    public BigDecimal getCantidad() {return cantidad;}
    public LocalDateTime getFechaMovimiento() {return fechaMovimiento;}
    public String getMotivo() {return motivo;}
    public ExistenciaId getExistenciaId() {return existenciaId;}
    public DetalleLoteId getDetalleLoteId() {return detalleLoteId;}
    public TipoMovimientoId getTipoMovimientoId() {return tipoMovimientoId;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public ProduccionId getProduccionId() {return produccionId;}
    public VentaId getVentaId() {return ventaId;}
    public boolean isBorrado() {return borrado;}
}
