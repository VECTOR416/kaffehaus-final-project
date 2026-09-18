package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Entidad interna del agregado Lote. NO es raiz de agregado.
 *
 * Se declara en el mismo paquete que Lote a proposito: es la senal mas fuerte de que su ciclo
 * de vida pertenece a la raiz. Sus mutadores son package-private, de modo que nadie fuera de
 * Lote puede crear una linea suelta ni tocar sus cantidades; el unico camino son los metodos
 * de la raiz (agregarDetalle, ajustarCantidadRestante, actualizarDetalle, removerDetalle).
 *
 * No guarda el LoteId del padre: dentro del agregado la relacion es la contencion, y la
 * columna 'detalle_lote.id_lote' la resuelve el mapper a partir de la raiz.
 *
 * El borrado es logico. La FK 'existencias.id_detalle_lote' es ON DELETE RESTRICT, asi que
 * una linea con existencias no puede desaparecer de la tabla; marcarla como borrada es la
 * unica baja posible sin romper el stock ya registrado.
 */
public class DetalleLote {

    /** Escala de NUMERIC(12, 2) en 'detalle_lote'. Todo importe y cantidad se redondea aqui. */
    static final int ESCALA = 2;

    private DetalleLoteId id;
    private ProductoId productoId;
    private BigDecimal cantidadInicial;
    private BigDecimal cantidadRestante;
    private BigDecimal precioUnitario;
    private boolean fraccionable;
    private boolean borrado;

    private DetalleLote(
        DetalleLoteId id,
        ProductoId productoId,
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        boolean fraccionable,
        boolean borrado
    ){
        this.id = id;
        this.productoId = productoId;
        this.cantidadInicial = cantidadInicial;
        this.cantidadRestante = cantidadRestante;
        this.precioUnitario = precioUnitario;
        this.fraccionable = fraccionable;
        this.borrado = borrado;
    }

    /**
     * Reconstruye una linea persistida. La invoca el mapper de infraestructura a traves de
     * Lote.restaurar, nunca la capa de aplicacion.
     */
    public static DetalleLote restaurar(
        String id,
        ProductoId productoId,
        BigDecimal cantidadInicial,
        BigDecimal cantidadRestante,
        BigDecimal precioUnitario,
        boolean fraccionable,
        boolean borrado
    ){
        return new DetalleLote(
                new DetalleLoteId(id),
                productoId,
                cantidadInicial,
                cantidadRestante,
                precioUnitario,
                fraccionable,
                borrado);
    }

    /**
     * Crea una linea nueva. La cantidad restante no se recibe: un lote recien comprado no
     * tiene consumo, asi que nace igual a la cantidad inicial. Bajarla es despues un ajuste
     * explicito de inventario.
     */
    static DetalleLote crear(
        ProductoId productoId,
        BigDecimal cantidadInicial,
        BigDecimal precioUnitario,
        Boolean fraccionable
    ){
        if (productoId == null || productoId.getValue() == null) {
            throw DomainException.throwException(400, "El detalle de lote requiere un producto");
        }
        exigirCantidadInicialValida(cantidadInicial);
        exigirPrecioValido(precioUnitario);

        BigDecimal cantidad = aEscala(cantidadInicial);

        return new DetalleLote(
                null,
                productoId,
                cantidad,
                cantidad,
                aEscala(precioUnitario),
                fraccionable != null && fraccionable,
                false);
    }

    /**
     * Corrige los datos de compra de la linea. La cantidad inicial no puede quedar por debajo
     * de lo que aun queda en almacen: eso dejaria el stock en un estado imposible.
     */
    void actualizarDatosDeCompra(BigDecimal cantidadInicial, BigDecimal precioUnitario, Boolean fraccionable) {
        if (cantidadInicial != null) {
            exigirCantidadInicialValida(cantidadInicial);
            BigDecimal nueva = aEscala(cantidadInicial);
            if (nueva.compareTo(this.cantidadRestante) < 0) {
                throw DomainException.throwException(400,
                        "La cantidad inicial no puede ser menor que la cantidad restante del detalle de lote");
            }
            this.cantidadInicial = nueva;
        }

        if (precioUnitario != null) {
            exigirPrecioValido(precioUnitario);
            this.precioUnitario = aEscala(precioUnitario);
        }

        this.fraccionable = fraccionable != null ? fraccionable : this.fraccionable;
    }

    /** Ajusta el stock restante de la linea. Nunca negativo ni por encima de lo comprado. */
    void ajustarCantidadRestante(BigDecimal nuevaCantidad) {
        if (nuevaCantidad == null || nuevaCantidad.signum() < 0) {
            throw DomainException.throwException(400, "La cantidad restante del detalle de lote no puede ser negativa");
        }

        BigDecimal cantidad = aEscala(nuevaCantidad);
        if (cantidad.compareTo(this.cantidadInicial) > 0) {
            throw DomainException.throwException(400,
                    "La cantidad restante no puede superar la cantidad inicial del detalle de lote");
        }

        this.cantidadRestante = cantidad;
    }

    void eliminar() {
        this.borrado = true;
    }

    /** Costo de la linea: lo que aporta al total del lote. */
    BigDecimal costoTotal() {
        return aEscala(this.cantidadInicial.multiply(this.precioUnitario));
    }

    private static void exigirCantidadInicialValida(BigDecimal cantidadInicial) {
        if (cantidadInicial == null || cantidadInicial.signum() <= 0) {
            throw DomainException.throwException(400, "La cantidad inicial del detalle de lote debe ser mayor a cero");
        }
    }

    private static void exigirPrecioValido(BigDecimal precioUnitario) {
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw DomainException.throwException(400, "El precio unitario del detalle de lote no puede ser negativo");
        }
    }

    private static BigDecimal aEscala(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public DetalleLoteId getId() {return id;}
    public ProductoId getProductoId() {return productoId;}
    public BigDecimal getCantidadInicial() {return cantidadInicial;}
    public BigDecimal getCantidadRestante() {return cantidadRestante;}
    public BigDecimal getPrecioUnitario() {return precioUnitario;}
    public boolean isFraccionable() {return fraccionable;}
    public boolean isBorrado() {return borrado;}
}
