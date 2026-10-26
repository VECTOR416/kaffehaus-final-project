package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Datasources.IVentaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ObtenerVentasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class VentaRepositoryImpl implements IVentaRepository{

    private final IVentaDatasource ventaDatasource;

    public VentaRepositoryImpl(IVentaDatasource ventaDatasource) {
        this.ventaDatasource = ventaDatasource;
    }

    @Override
    public Venta crear(Venta venta) {
        return this.ventaDatasource.crear(venta);
    }

    @Override
    public Venta actualizar(VentaId ventaId, Venta venta) {
        return this.ventaDatasource.actualizar(ventaId, venta);
    }

    @Override
    public Optional<Venta> obtenerPorId(VentaId ventaId) {
        return this.ventaDatasource.obtenerPorId(ventaId);
    }

    @Override
    public PaginatedResponse<List<Venta>> obtenerTodos(ObtenerVentasRequest obtenerVentasRequest) {
        return this.ventaDatasource.obtenerTodos(obtenerVentasRequest);
    }

    @Override
    public Optional<Venta> obtenerPorClaveIdempotencia(String claveIdempotencia) {
        return this.ventaDatasource.obtenerPorClaveIdempotencia(claveIdempotencia);
    }

    @Override
    public void eliminar(VentaId ventaId) {
        this.ventaDatasource.eliminar(ventaId);
    }

    @Override
    public Optional<Venta> obtenerPorNumeroTicket(Long numeroTicket) {
        return this.ventaDatasource.obtenerPorNumeroTicket(numeroTicket);
    }

    @Override
    public List<Venta> obtenerPorSesionCaja(SesionCajaId sesionCajaId) {
        return this.ventaDatasource.obtenerPorSesionCaja(sesionCajaId);
    }
}
