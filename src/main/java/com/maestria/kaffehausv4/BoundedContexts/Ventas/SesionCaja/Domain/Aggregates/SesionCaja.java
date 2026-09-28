package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Raiz del agregado SesionCaja: un turno de caja, desde que se abre con un fondo inicial
 * hasta que se cierra con el arqueo del efectivo (arquitectura.md 4.11).
 *
 * Quien la abre es el titular: el responsable del dinero fisico. Mientras esta 'ABIERTA'
 * recibe ventas y movimientos menores de efectivo (MovimientoCaja, su entidad hija). Al
 * cerrarla la raiz calcula el efectivo esperado y la diferencia con lo contado; desde ese
 * momento es inmutable. En el local solo puede haber una abierta (uq_sesion_caja_abierta).
 *
 * El dominio no sabe que pagos fueron en efectivo: quien la cierra le pasa esa suma, calculada
 * sobre las ventas completadas de la sesion.
 */
public class SesionCaja {
    public static final String ABIERTA = "ABIERTA";
    public static final String CERRADA = "CERRADA";

    private SesionCajaId id;
    private UsuarioId usuarioId;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal montoApertura;
    private BigDecimal montoCierreDeclarado;
    private BigDecimal montoCierreEsperado;
    private BigDecimal diferencia;
    private String motivoDiferencia;
    private String estado;
    private boolean borrado;
    private final List<MovimientoCaja> movimientos;

    private SesionCaja(
        SesionCajaId id,
        UsuarioId usuarioId,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        BigDecimal montoApertura,
        BigDecimal montoCierreDeclarado,
        BigDecimal montoCierreEsperado,
        BigDecimal diferencia,
        String motivoDiferencia,
        String estado,
        boolean borrado,
        List<MovimientoCaja> movimientos
    ){
        this.id = id;
        this.usuarioId = usuarioId;
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
        this.montoApertura = montoApertura;
        this.montoCierreDeclarado = montoCierreDeclarado;
        this.montoCierreEsperado = montoCierreEsperado;
        this.diferencia = diferencia;
        this.motivoDiferencia = motivoDiferencia;
        this.estado = estado;
        this.borrado = borrado;
        this.movimientos = movimientos != null ? new ArrayList<>(movimientos) : new ArrayList<>();
    }

    public static SesionCaja restaurar(
        String id,
        UsuarioId usuarioId,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        BigDecimal montoApertura,
        BigDecimal montoCierreDeclarado,
        BigDecimal montoCierreEsperado,
        BigDecimal diferencia,
        String motivoDiferencia,
        String estado,
        boolean borrado,
        List<MovimientoCaja> movimientos
    ){
        return new SesionCaja(new SesionCajaId(id), usuarioId, fechaApertura, fechaCierre, montoApertura,
                montoCierreDeclarado, montoCierreEsperado, diferencia, motivoDiferencia, estado, borrado, movimientos);
    }

    /** Abre un turno a cargo de 'usuarioId' con el fondo de efectivo con el que arranca. */
    public static SesionCaja abrir(UsuarioId usuarioId, BigDecimal montoApertura, LocalDateTime fechaApertura) {
        if (montoApertura == null || montoApertura.signum() < 0) {
            throw DomainException.throwException(400, "El monto de apertura de la caja no puede ser negativo");
        }
        return new SesionCaja(null, usuarioId, fechaApertura, null, aEscalaMonetaria(montoApertura),
                null, null, null, null, ABIERTA, false, null);
    }

    /** Ingreso o egreso menor de efectivo. Solo en una caja abierta. */
    public void registrarMovimiento(UsuarioId usuarioId, String tipo, BigDecimal monto, String motivo) {
        exigirAbierta();
        this.movimientos.add(MovimientoCaja.crear(usuarioId, tipo, monto, motivo));
    }

    /**
     * Solo el titular del turno o un administrador cierran la caja: el cierre fija la
     * diferencia de la que responde el titular.
     */
    public void exigirQuePuedaCerrar(UsuarioId usuarioId, boolean esAdministrador) {
        if (!esAdministrador && !this.usuarioId.getValue().equals(usuarioId.getValue())) {
            throw AutorizacionException.throwException(403,
                    "Solo el titular de la sesion de caja o un Administrador pueden cerrarla");
        }
    }

    /**
     * Cierra el turno con el arqueo del efectivo:
     *
     *   esperado   = apertura + ventas en efectivo + ingresos - egresos
     *   diferencia = declarado - esperado
     *
     * 'ventasEnEfectivo' es la suma de los pagos en efectivo de las ventas completadas de esta
     * sesion (el importe cobrado, no el recibido: el cambio ya salio de la caja).
     */
    public void cerrar(BigDecimal montoCierreDeclarado, BigDecimal ventasEnEfectivo, String motivoDiferencia, LocalDateTime fechaCierre) {
        exigirAbierta();
        if (montoCierreDeclarado == null || montoCierreDeclarado.signum() < 0) {
            throw DomainException.throwException(400, "El monto de cierre declarado no puede ser negativo");
        }
        BigDecimal esperado = this.montoApertura
                .add(ventasEnEfectivo != null ? ventasEnEfectivo : BigDecimal.ZERO)
                .add(getTotalIngresos())
                .subtract(getTotalEgresos());

        this.montoCierreDeclarado = aEscalaMonetaria(montoCierreDeclarado);
        this.montoCierreEsperado = aEscalaMonetaria(esperado);
        this.diferencia = this.montoCierreDeclarado.subtract(this.montoCierreEsperado);
        this.motivoDiferencia = motivoDiferencia;
        this.fechaCierre = fechaCierre;
        this.estado = CERRADA;
    }

    /** Un turno es auditoria del dinero: no se da de baja. */
    public void eliminar() {
        throw DomainException.throwException(409, "Una sesion de caja no se da de baja");
    }

    public boolean estaAbierta() {
        return ABIERTA.equals(this.estado);
    }

    private void exigirAbierta() {
        if (!estaAbierta()) {
            throw DomainException.throwException(409, this.id != null
                    ? "La sesion de caja '" + this.id.getValue() + "' ya esta cerrada"
                    : "La sesion de caja ya esta cerrada");
        }
    }

    private static BigDecimal aEscalaMonetaria(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalIngresos() {
        return sumar(true);
    }

    public BigDecimal getTotalEgresos() {
        return sumar(false);
    }

    private BigDecimal sumar(boolean ingresos) {
        return aEscalaMonetaria(getMovimientosActivos().stream()
                .filter(movimiento -> movimiento.esIngreso() == ingresos)
                .map(MovimientoCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public SesionCajaId getId() {return id;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public LocalDateTime getFechaApertura() {return fechaApertura;}
    public LocalDateTime getFechaCierre() {return fechaCierre;}
    public BigDecimal getMontoApertura() {return montoApertura;}
    public BigDecimal getMontoCierreDeclarado() {return montoCierreDeclarado;}
    public BigDecimal getMontoCierreEsperado() {return montoCierreEsperado;}
    public BigDecimal getDiferencia() {return diferencia;}
    public String getMotivoDiferencia() {return motivoDiferencia;}
    public String getEstado() {return estado;}
    public boolean isBorrado() {return borrado;}

    /** Todos los movimientos, incluidos los dados de baja: es lo que la persistencia escribe. */
    public List<MovimientoCaja> getMovimientos() {return Collections.unmodifiableList(movimientos);}

    public List<MovimientoCaja> getMovimientosActivos() {
        return movimientos.stream()
                .filter(movimiento -> !movimiento.isBorrado())
                .collect(Collectors.toList());
    }
}
