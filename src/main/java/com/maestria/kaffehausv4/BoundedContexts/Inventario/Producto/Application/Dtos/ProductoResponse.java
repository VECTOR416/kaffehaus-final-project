package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos;

import java.math.BigDecimal;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.CategoriaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.TipoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.UnidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;

public class ProductoResponse {
    private String id;
    private String nombre;
    private BigDecimal stock;
    private BigDecimal precio;
    private BigDecimal stockMinimo;
    private boolean vendible;
    private boolean esStockDirecto;
    private UnidadResponse unidadActual;
    private TipoResponse tipo;
    private CategoriaResponse categoria;
    private String idProductoPadre;
    private boolean borrado;

    private ProductoResponse(
        String id,
        String nombre,
        BigDecimal stock,
        BigDecimal precio,
        BigDecimal stockMinimo,
        boolean vendible,
        boolean esStockDirecto,
        UnidadResponse unidadActual,
        TipoResponse tipo,
        CategoriaResponse categoria,
        String idProductoPadre,
        boolean borrado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.stock = stock;
        this.precio = precio;
        this.stockMinimo = stockMinimo;
        this.vendible = vendible;
        this.esStockDirecto = esStockDirecto;
        this.unidadActual = unidadActual;
        this.tipo = tipo;
        this.categoria = categoria;
        this.idProductoPadre = idProductoPadre;
        this.borrado = borrado;
    }

    public static ProductoResponse createFromAggregate(Producto producto, Unidad unidadActual, Tipo tipo, Categoria categoria){
        UnidadResponse unidadActualResponse = unidadActual != null ? UnidadResponse.createFromAggregate(unidadActual) : null;
        TipoResponse tipoResponse = tipo != null ? TipoResponse.createFromAggregate(tipo) : null;
        CategoriaResponse categoriaResponse = categoria != null ? CategoriaResponse.createFromAggregate(categoria) : null;
        return new ProductoResponse(
            producto.getId().getValue(),
            producto.getNombre(),
            producto.getStock(),
            producto.getPrecio(),
            producto.getStockMinimo(),
            producto.isVendible(),
            producto.isEsStockDirecto(),
            unidadActualResponse,
            tipoResponse,
            categoriaResponse,
            producto.getProductoPadreId() != null ? producto.getProductoPadreId().getValue() : null,
            producto.isBorrado()
        );
    }

    public String getId() {return id;}
    public String getNombre() {return nombre;}
    public BigDecimal getStock() {return stock;}
    public BigDecimal getPrecio() {return precio;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public boolean isVendible() {return vendible;}
    public boolean isEsStockDirecto() {return esStockDirecto;}
    public UnidadResponse getUnidadActual() {return unidadActual;}
    public TipoResponse getTipo() {return tipo;}
    public CategoriaResponse getCategoria() {return categoria;}
    public String getIdProductoPadre() {return idProductoPadre;}
    public boolean isBorrado() {return borrado;}
}
