package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.MovimientoCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Ingreso o egreso menor de efectivo durante un turno (cambio que trae el dueno, compra de
 * hielo pagada de la caja...). Entidad hija de SesionCaja: solo la raiz la crea.
 *
 * Es inmutable, como los movimientos de inventario: un error se corrige con el movimiento
 * contrario. 'fecha' es el instante en que se registro (la columna fec_c); la asigna la
 * persistencia y es nula hasta guardarse.
 *
 * 'ventaId' solo lo pone el sistema, en el egreso que devuelve el efectivo de una venta de un
 * turno cerrado al anularla (logica.md SESIONCAJA-13, VENTA-27); en el resto es nulo.
 */
public class MovimientoCaja {
    public static final String INGRESO = "INGRESO";
    public static final String EGRESO = "EGRESO";
    static final int LONGITUD_MAXIMA_MOTIVO = 255;

    private MovimientoCajaId id;
    private UsuarioId usuarioId;
    private String tipo;
    private BigDecimal monto;
    private String motivo;
    private LocalDateTime fecha;
    private VentaId ventaId;
    private boolean borrado;

    private MovimientoCaja(
        MovimientoCajaId id,
        UsuarioId usuarioId,
        String tipo,
        BigDecimal monto,
        String motivo,
        LocalDateTime fecha,
        VentaId ventaId,
        boolean borrado
    ){
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.monto = monto;
        this.motivo = motivo;
        this.fecha = fecha;
        this.ventaId = ventaId;
        this.borrado = borrado;
    }

    public static MovimientoCaja restaurar(
        String id,
        UsuarioId usuarioId,
        String tipo,
        BigDecimal monto,
        String motivo,
        LocalDateTime fecha,
        VentaId ventaId,
        boolean borrado
    ){
        return new MovimientoCaja(new MovimientoCajaId(id), usuarioId, tipo, monto, motivo, fecha, ventaId, borrado);
    }

    /**
     * Egreso que devuelve el efectivo de una venta de un turno cerrado al anularla (logica.md
     * VENTA-27, SESIONCAJA-13). Es el unico movimiento que lleva 'ventaId'.
     */
    static MovimientoCaja crearDevolucion(UsuarioId usuarioId, BigDecimal monto, String motivo, VentaId ventaId) {
        if (ventaId == null) {
            throw DomainException.throwException(400, "Una devolucion de caja requiere la venta que la origina");
        }
        MovimientoCaja egreso = crear(usuarioId, EGRESO, monto, motivo);
        return new MovimientoCaja(null, egreso.usuarioId, egreso.tipo, egreso.monto, egreso.motivo, null, ventaId, false);
    }

    static MovimientoCaja crear(UsuarioId usuarioId, String tipo, BigDecimal monto, String motivo) {
        String tipoNormalizado = tipo != null ? tipo.trim().toUpperCase() : null;
        if (!INGRESO.equals(tipoNormalizado) && !EGRESO.equals(tipoNormalizado)) {
            throw DomainException.throwException(400, "El campo 'tipo' debe ser '" + INGRESO + "' o '" + EGRESO + "'");
        }
        if (monto == null || monto.signum() <= 0) {
            throw DomainException.throwException(400, "El monto de un movimiento de caja debe ser mayor que cero");
        }
        if (motivo == null || motivo.isBlank()) {
            throw DomainException.throwException(400, "Un movimiento de caja requiere un motivo");
        }
        if (motivo.length() > LONGITUD_MAXIMA_MOTIVO) {
            throw DomainException.throwException(400,
                    "El motivo de un movimiento de caja no puede superar los " + LONGITUD_MAXIMA_MOTIVO + " caracteres");
        }
        return new MovimientoCaja(null, usuarioId, tipoNormalizado, monto.setScale(2, RoundingMode.HALF_UP), motivo, null, null, false);
    }

    public boolean esIngreso() {
        return INGRESO.equals(this.tipo);
    }

    public MovimientoCajaId getId() {return id;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public String getTipo() {return tipo;}
    public BigDecimal getMonto() {return monto;}
    public String getMotivo() {return motivo;}
    public LocalDateTime getFecha() {return fecha;}
    public VentaId getVentaId() {return ventaId;}
    public boolean isBorrado() {return borrado;}
}
