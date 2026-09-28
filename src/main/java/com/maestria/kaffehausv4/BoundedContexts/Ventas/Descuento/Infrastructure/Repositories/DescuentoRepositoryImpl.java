package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Datasources.IDescuentoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ObtenerDescuentosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class DescuentoRepositoryImpl implements IDescuentoRepository{

    private final IDescuentoDatasource descuentoDatasource;

    public DescuentoRepositoryImpl(IDescuentoDatasource descuentoDatasource) {
        this.descuentoDatasource = descuentoDatasource;
    }

    @Override
    public Descuento crear(Descuento descuento) {
        return this.descuentoDatasource.crear(descuento);
    }

    @Override
    public Descuento actualizar(DescuentoId descuentoId, Descuento descuento) {
        return this.descuentoDatasource.actualizar(descuentoId, descuento);
    }

    @Override
    public Optional<Descuento> obtenerPorId(DescuentoId descuentoId) {
        return this.descuentoDatasource.obtenerPorId(descuentoId);
    }

    @Override
    public PaginatedResponse<List<Descuento>> obtenerTodos(ObtenerDescuentosRequest obtenerDescuentosRequest) {
        return this.descuentoDatasource.obtenerTodos(obtenerDescuentosRequest);
    }

    @Override
    public void eliminar(DescuentoId descuentoId) {
        this.descuentoDatasource.eliminar(descuentoId);
    }

    @Override
    public List<Descuento> obtenerVigentes(ProductoId productoId, LocalDateTime fecha) {
        return this.descuentoDatasource.obtenerVigentes(productoId, fecha);
    }

}
