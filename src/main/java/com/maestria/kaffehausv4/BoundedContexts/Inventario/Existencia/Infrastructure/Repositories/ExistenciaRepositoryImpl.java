package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Datasources.IExistenciaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ObtenerExistenciasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class ExistenciaRepositoryImpl implements IExistenciaRepository{

    private final IExistenciaDatasource existenciaDatasource;

    public ExistenciaRepositoryImpl(IExistenciaDatasource existenciaDatasource) {
        this.existenciaDatasource = existenciaDatasource;
    }

    @Override
    public Existencia crear(Existencia existencia) {
        return this.existenciaDatasource.crear(existencia);
    }

    @Override
    public Existencia actualizar(ExistenciaId existenciaId, Existencia existencia) {
        return this.existenciaDatasource.actualizar(existenciaId, existencia);
    }

    @Override
    public Optional<Existencia> obtenerPorId(ExistenciaId existenciaId) {
        return this.existenciaDatasource.obtenerPorId(existenciaId);
    }

    @Override
    public PaginatedResponse<List<Existencia>> obtenerTodos(ObtenerExistenciasRequest obtenerExistenciasRequest) {
        return this.existenciaDatasource.obtenerTodos(obtenerExistenciasRequest);
    }

    @Override
    public void eliminar(ExistenciaId existenciaId) {
        this.existenciaDatasource.eliminar(existenciaId);
    }

    @Override
    public List<Existencia> obtenerDisponiblesPorDetallesLote(List<DetalleLoteId> detalleLoteIds) {
        return this.existenciaDatasource.obtenerDisponiblesPorDetallesLote(detalleLoteIds);
    }

    @Override
    public List<Existencia> obtenerDisponiblesParaConsumo(List<DetalleLoteId> detalleLoteIds) {
        return this.existenciaDatasource.obtenerDisponiblesParaConsumo(detalleLoteIds);
    }

    @Override
    public List<Existencia> obtenerVivasParaPesaje(List<DetalleLoteId> detalleLoteIds) {
        return this.existenciaDatasource.obtenerVivasParaPesaje(detalleLoteIds);
    }

    @Override
    public Optional<Existencia> obtenerPorIdParaActualizar(ExistenciaId existenciaId) {
        return this.existenciaDatasource.obtenerPorIdParaActualizar(existenciaId);
    }

    @Override
    public Optional<DetalleLoteId> obtenerDetalleLoteDe(ExistenciaId existenciaId) {
        return this.existenciaDatasource.obtenerDetalleLoteDe(existenciaId);
    }

    @Override
    public boolean existenPorDetallesLote(List<DetalleLoteId> detalleLoteIds) {
        return this.existenciaDatasource.existenPorDetallesLote(detalleLoteIds);
    }

    @Override
    public List<Existencia> obtenerConContenidoQueVencenHasta(LocalDate fecha) {
        return this.existenciaDatasource.obtenerConContenidoQueVencenHasta(fecha);
    }
}
