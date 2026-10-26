package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Datasources.ISesionCajaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.ObtenerSesionesCajaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class SesionCajaRepositoryImpl implements ISesionCajaRepository {

    private final ISesionCajaDatasource sesionCajaDatasource;

    public SesionCajaRepositoryImpl(ISesionCajaDatasource sesionCajaDatasource) {
        this.sesionCajaDatasource = sesionCajaDatasource;
    }

    @Override
    public SesionCaja crear(SesionCaja sesionCaja) {
        return this.sesionCajaDatasource.crear(sesionCaja);
    }

    @Override
    public SesionCaja actualizar(SesionCajaId sesionCajaId, SesionCaja sesionCaja) {
        return this.sesionCajaDatasource.actualizar(sesionCajaId, sesionCaja);
    }

    @Override
    public Optional<SesionCaja> obtenerPorId(SesionCajaId sesionCajaId) {
        return this.sesionCajaDatasource.obtenerPorId(sesionCajaId);
    }

    @Override
    public PaginatedResponse<List<SesionCaja>> obtenerTodos(ObtenerSesionesCajaRequest obtenerSesionesCajaRequest) {
        return this.sesionCajaDatasource.obtenerTodos(obtenerSesionesCajaRequest);
    }

    @Override
    public Optional<SesionCaja> obtenerAbierta() {
        return this.sesionCajaDatasource.obtenerAbierta();
    }

    @Override
    public Optional<SesionCaja> obtenerAbiertaParaActualizar() {
        return this.sesionCajaDatasource.obtenerAbiertaParaActualizar();
    }

    @Override
    public Optional<SesionCaja> obtenerAbiertaParaVenta() {
        return this.sesionCajaDatasource.obtenerAbiertaParaVenta();
    }
}
