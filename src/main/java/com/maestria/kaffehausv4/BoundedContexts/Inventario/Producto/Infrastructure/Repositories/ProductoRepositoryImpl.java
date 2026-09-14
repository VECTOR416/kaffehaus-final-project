package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Datasources.IProductoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ObtenerProductosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class ProductoRepositoryImpl implements IProductoRepository{

    private final IProductoDatasource productoDatasource;

    public ProductoRepositoryImpl(IProductoDatasource productoDatasource) {
        this.productoDatasource = productoDatasource;
    }

    @Override
    public Producto crear(Producto producto) {
        return this.productoDatasource.crear(producto);
    }

    @Override
    public Producto actualizar(ProductoId productoId, Producto producto) {
        return this.productoDatasource.actualizar(productoId, producto);
    }

    @Override
    public Optional<Producto> obtenerPorId(ProductoId productoId) {
        return this.productoDatasource.obtenerPorId(productoId);
    }

    @Override
    public PaginatedResponse<List<Producto>> obtenerTodos(ObtenerProductosRequest obtenerProductosRequest) {
        return this.productoDatasource.obtenerTodos(obtenerProductosRequest);
    }

    @Override
    public void eliminar(ProductoId productoId) {
        this.productoDatasource.eliminar(productoId);
    }

}
