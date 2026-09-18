package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.IngredienteRecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Entidad interna del agregado Receta. NO es raiz de agregado.
 *
 * Se declara en el mismo paquete que Receta a proposito: es la senal mas fuerte de que su
 * ciclo de vida pertenece a la raiz. Sus mutadores son package-private, asi que nadie fuera
 * de Receta puede crear un ingrediente suelto ni cambiarle la cantidad.
 *
 * No guarda el RecetaId del padre: dentro del agregado la relacion es la contencion, y la
 * columna 'ingredientes_receta.id_receta' la resuelve el mapper a partir de la raiz.
 *
 * El borrado es logico, como en todo el sistema.
 */
public class IngredienteReceta {

    /** Escala de NUMERIC(12, 4) en 'ingredientes_receta': el insumo admite mas decimales. */
    static final int ESCALA = 4;

    private IngredienteRecetaId id;
    private ProductoId productoId;
    private BigDecimal cantidad;
    private boolean borrado;

    private IngredienteReceta(
        IngredienteRecetaId id,
        ProductoId productoId,
        BigDecimal cantidad,
        boolean borrado
    ){
        this.id = id;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.borrado = borrado;
    }

    /**
     * Reconstruye un ingrediente persistido. La invoca el mapper de infraestructura a traves
     * de Receta.restaurar, nunca la capa de aplicacion.
     */
    public static IngredienteReceta restaurar(
        String id,
        ProductoId productoId,
        BigDecimal cantidad,
        boolean borrado
    ){
        return new IngredienteReceta(new IngredienteRecetaId(id), productoId, cantidad, borrado);
    }

    /** Crea un ingrediente nuevo validando sus invariantes. Package-private: solo Receta. */
    static IngredienteReceta crear(ProductoId productoId, BigDecimal cantidad) {
        if (productoId == null || productoId.getValue() == null) {
            throw DomainException.throwException(400, "El ingrediente de receta requiere un producto");
        }
        exigirCantidadValida(cantidad);

        return new IngredienteReceta(null, productoId, aEscala(cantidad), false);
    }

    /** Corrige la cantidad requerida del insumo. */
    void actualizarCantidad(BigDecimal cantidad) {
        if (cantidad == null) {
            return;
        }
        exigirCantidadValida(cantidad);
        this.cantidad = aEscala(cantidad);
    }

    void eliminar() {
        this.borrado = true;
    }

    private static void exigirCantidadValida(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw DomainException.throwException(400, "La cantidad del ingrediente de receta debe ser mayor a cero");
        }
    }

    private static BigDecimal aEscala(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public IngredienteRecetaId getId() {return id;}
    public ProductoId getProductoId() {return productoId;}
    public BigDecimal getCantidad() {return cantidad;}
    public boolean isBorrado() {return borrado;}
}
