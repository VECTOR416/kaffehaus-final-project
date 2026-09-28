package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

public class Producto {
    private ProductoId id;
    private String nombre;
    private BigDecimal stock;
    private BigDecimal precio;
    private BigDecimal stockMinimo;
    private boolean vendible;
    private boolean esStockDirecto;
    private UnidadId unidadActualId;
    private TipoId tipoId;
    private CategoriaId categoriaId;
    private ProductoId productoPadreId;
    private boolean borrado;

    private Producto(
        ProductoId id,
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        boolean esStockDirecto,
        UnidadId unidadActualId,
        TipoId tipoId,
        CategoriaId categoriaId,
        ProductoId productoPadreId,
        boolean borrado
    ){
        this.id = id;
        this.nombre = nombre;
        this.stock = stock;
        this.precio = precio;
        this.stockMinimo = stockMinimo;
        this.vendible = vendible;
        this.esStockDirecto = esStockDirecto;
        this.unidadActualId = unidadActualId;
        this.tipoId = tipoId;
        this.categoriaId = categoriaId;
        this.productoPadreId = productoPadreId;
        this.borrado = borrado;
    }

    public static Producto restaurar(
        String id,
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        boolean esStockDirecto,
        UnidadId unidadActualId,
        TipoId tipoId,
        CategoriaId categoriaId,
        ProductoId productoPadreId,
        boolean borrado
    ){
        return new Producto(new ProductoId(id), nombre, stock, precio, stockMinimo, vendible, esStockDirecto, unidadActualId, tipoId, categoriaId, productoPadreId, borrado);
    }

    /**
     * El stock no se recibe: es la proyeccion del contenido abierto de sus existencias
     * (inventario.md INV-20), asi que todo producto nace con stock cero.
     */
    public static Producto crear(
        String nombre,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        boolean esStockDirecto,
        UnidadId unidadActualId,
        TipoId tipoId,
        CategoriaId categoriaId,
        ProductoId productoPadreId
    ){
        return new Producto(null, nombre, BigDecimal.ZERO.setScale(2), precio, stockMinimo, vendible, esStockDirecto, unidadActualId, tipoId, categoriaId, productoPadreId, false);
    }

    public void actualizar(
        String nombre,
        BigDecimal precio,
        BigDecimal stockMinimo,
        Boolean vendible,
        Boolean esStockDirecto,
        UnidadId unidadActualId,
        TipoId tipoId,
        CategoriaId categoriaId,
        ProductoId productoPadreId
    ){
        this.nombre = nombre != null ? nombre : this.nombre;
        this.precio = precio != null ? precio : this.precio;
        this.stockMinimo = stockMinimo != null ? stockMinimo : this.stockMinimo;
        this.vendible = vendible != null ? vendible : this.vendible;
        this.esStockDirecto = esStockDirecto != null ? esStockDirecto : this.esStockDirecto;
        this.unidadActualId = unidadActualId != null ? unidadActualId : this.unidadActualId;
        this.tipoId = tipoId != null ? tipoId : this.tipoId;
        this.categoriaId = categoriaId != null ? categoriaId : this.categoriaId;
        this.productoPadreId = productoPadreId != null ? productoPadreId : this.productoPadreId;
    }

    public void eliminar(){
        this.borrado = true;
    }

    /**
     * Fija el stock proyectado a partir del contenido abierto de sus existencias. Es la unica
     * forma de cambiar 'stock' (inventario.md INV-20, INV-58). Un producto bajo demanda no
     * tiene stock propio.
     */
    public void proyectarStock(BigDecimal contenidoAbierto) {
        if (!this.esStockDirecto) {
            throw DomainException.throwException(409, "El producto '" + this.nombre + "' se prepara bajo demanda y no tiene stock propio");
        }
        if (contenidoAbierto == null || contenidoAbierto.signum() < 0) {
            throw DomainException.throwException(409, "El stock del producto '" + this.nombre + "' no puede ser negativo");
        }
        this.stock = contenidoAbierto.setScale(2, RoundingMode.HALF_UP);
    }

    /** Una presentacion es la forma de compra de un producto base (inventario.md INV-06). */
    public boolean esPresentacion() {
        return this.productoPadreId != null;
    }

    /** El producto que contienen sus existencias: el padre si es presentacion, o el mismo. */
    public ProductoId productoBaseId() {
        return esPresentacion() ? this.productoPadreId : this.id;
    }

    public ProductoId getId() {return id;}
    public String getNombre() {return nombre;}
    public BigDecimal getStock() {return stock;}
    public BigDecimal getPrecio() {return precio;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public boolean isVendible() {return vendible;}

    /**
     * TRUE: tiene stock fisico propio (MTS, reventa) y al venderse descuenta sus existencias.
     * FALSE: se prepara al pedirlo (MTO) y al venderse descuenta los insumos de su receta.
     */
    public boolean isEsStockDirecto() {return esStockDirecto;}
    public UnidadId getUnidadActualId() {return unidadActualId;}
    public TipoId getTipoId() {return tipoId;}
    public CategoriaId getCategoriaId() {return categoriaId;}
    public ProductoId getProductoPadreId() {return productoPadreId;}
    public boolean isBorrado() {return borrado;}
}
