package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Raiz del agregado Movimiento: el registro de auditoria de una entrada o salida de una
 * existencia.
 *
 * Es inmutable. Un movimiento registrado no se corrige ni se da de baja, ni siquiera de forma
 * logica: esconderlo romperia la traza que explica el contenido actual de la existencia. Un
 * error se corrige registrando un movimiento compensatorio.
 *
 * La cantidad es siempre una magnitud positiva; el tipo de movimiento solo lo clasifica
 * ('Receta', 'Ajuste', 'Merma') y la direccion (entrada o salida) se lee del motivo.
 * 'produccionId' solo se informa en los consumos de insumos y la entrada del producto terminado
 * de una produccion.
 */
public class Movimiento {
    private MovimientoId id;
    private BigDecimal cantidad;
    private LocalDateTime fechaMovimiento;
    private String motivo;
    private ExistenciaId existenciaId;
    private TipoMovimientoId tipoMovimientoId;
    private UsuarioId usuarioId;
    private ProduccionId produccionId;
    private boolean borrado;

    private Movimiento(
        MovimientoId id,
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        ProduccionId produccionId,
        boolean borrado
    ){
        this.id = id;
        this.cantidad = cantidad;
        this.fechaMovimiento = fechaMovimiento;
        this.motivo = motivo;
        this.existenciaId = existenciaId;
        this.tipoMovimientoId = tipoMovimientoId;
        this.usuarioId = usuarioId;
        this.produccionId = produccionId;
        this.borrado = borrado;
    }

    public static Movimiento restaurar(
        String id,
        BigDecimal cantidad,
        LocalDateTime fechaMovimiento,
        String motivo,
        ExistenciaId existenciaId,
        TipoMovimientoId tipoMovimientoId,
        UsuarioId usuarioId,
        ProduccionId produccionId,
        boolean borrado
    ){
        return new Movimiento(new MovimientoId(id), cantidad, fechaMovimiento, motivo, existenciaId, tipoMovimientoId, usuarioId, produccionId, borrado);
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
        if (cantidad == null || cantidad.signum() <= 0) {
            throw DomainException.throwException(400, "La cantidad del movimiento debe ser mayor a cero");
        }
        return new Movimiento(null, cantidad, fechaMovimiento, motivo, existenciaId, tipoMovimientoId, usuarioId, produccionId, false);
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
    public TipoMovimientoId getTipoMovimientoId() {return tipoMovimientoId;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public ProduccionId getProduccionId() {return produccionId;}
    public boolean isBorrado() {return borrado;}
}
