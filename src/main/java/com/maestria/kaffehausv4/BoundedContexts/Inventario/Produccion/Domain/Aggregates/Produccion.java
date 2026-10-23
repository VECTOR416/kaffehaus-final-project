package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Raiz del agregado Produccion: una elaboracion por lotes en cocina (MTS).
 *
 * La diferencia NO se recibe desde fuera: es cantidadReal - cantidadEsperada y la calcula la
 * raiz. Una vez registrada, la produccion ya consumio insumos y abrio su lote, asi que sus
 * cantidades y su receta quedan fijas: solo se admite corregir el motivo y la fecha.
 */
public class Produccion {

    /** Escala de NUMERIC(12, 2) en 'produccion'. */
    private static final int ESCALA = 2;

    private ProduccionId id;
    private BigDecimal cantidadEsperada;
    private BigDecimal cantidadReal;
    private BigDecimal diferencia;
    private String motivoDiferencia;
    private LocalDateTime fechaProduccion;
    private RecetaId recetaId;
    private boolean borrado;

    private Produccion(
        ProduccionId id,
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        RecetaId recetaId,
        boolean borrado
    ){
        this.id = id;
        this.cantidadEsperada = cantidadEsperada;
        this.cantidadReal = cantidadReal;
        this.diferencia = diferencia;
        this.motivoDiferencia = motivoDiferencia;
        this.fechaProduccion = fechaProduccion;
        this.recetaId = recetaId;
        this.borrado = borrado;
    }

    public static Produccion restaurar(
        String id,
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        RecetaId recetaId,
        boolean borrado
    ){
        return new Produccion(new ProduccionId(id), cantidadEsperada, cantidadReal, diferencia, motivoDiferencia, fechaProduccion, recetaId, borrado);
    }

    /**
     * Registra una produccion. La cantidad esperada escala la receta (es lo que se planifico
     * elaborar); la real es lo que salio de cocina y puede ser cero si la tanda se perdio.
     */
    public static Produccion crear(
        BigDecimal cantidadEsperada,
        BigDecimal cantidadReal,
        String motivoDiferencia,
        LocalDateTime fechaProduccion,
        RecetaId recetaId
    ){
        if (cantidadEsperada == null || cantidadEsperada.signum() <= 0) {
            throw DomainException.throwException(400, "La cantidad esperada de la produccion debe ser mayor a cero");
        }
        if (cantidadReal == null || cantidadReal.signum() < 0) {
            throw DomainException.throwException(400, "La cantidad real de la produccion no puede ser negativa");
        }
        if (recetaId == null || recetaId.getValue() == null) {
            throw DomainException.throwException(400, "La produccion requiere una receta");
        }

        BigDecimal esperada = aEscala(cantidadEsperada);
        BigDecimal real = aEscala(cantidadReal);

        return new Produccion(null, esperada, real, real.subtract(esperada), motivoDiferencia, fechaProduccion, recetaId, false);
    }

    /**
     * Solo corrige datos descriptivos. Las cantidades y la receta no se tocan: los consumos y
     * el lote que generaron son movimientos de auditoria inmutables.
     */
    public void actualizar(
        String motivoDiferencia,
        LocalDateTime fechaProduccion
    ){
        this.motivoDiferencia = motivoDiferencia != null ? motivoDiferencia : this.motivoDiferencia;
        this.fechaProduccion = fechaProduccion != null ? fechaProduccion : this.fechaProduccion;
    }

    /**
     * Una produccion no se da de baja (logica.md PRODUCCION-11): ya movio el stock del terminado y
     * los insumos. Un error se corrige con el pesaje; la produccion se conserva como registro.
     */
    public void eliminar(){
        throw DomainException.throwException(409,
                "Una produccion no se da de baja: ya movio stock e insumos; corrige las existencias con un pesaje");
    }

    /** Factor por el que se multiplica cada ingrediente: cantidad esperada / rendimiento de la receta. */
    public BigDecimal factorSobre(BigDecimal rendimientoReceta) {
        if (rendimientoReceta == null || rendimientoReceta.signum() <= 0) {
            throw DomainException.throwException(400, "El rendimiento de la receta debe ser mayor a cero");
        }
        return this.cantidadEsperada.divide(rendimientoReceta, 6, RoundingMode.HALF_UP);
    }

    public boolean produjoUnidades() {
        return this.cantidadReal.signum() > 0;
    }

    private static BigDecimal aEscala(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public ProduccionId getId() {return id;}
    public BigDecimal getCantidadEsperada() {return cantidadEsperada;}
    public BigDecimal getCantidadReal() {return cantidadReal;}
    public BigDecimal getDiferencia() {return diferencia;}
    public String getMotivoDiferencia() {return motivoDiferencia;}
    public LocalDateTime getFechaProduccion() {return fechaProduccion;}
    public RecetaId getRecetaId() {return recetaId;}
    public boolean isBorrado() {return borrado;}
}
