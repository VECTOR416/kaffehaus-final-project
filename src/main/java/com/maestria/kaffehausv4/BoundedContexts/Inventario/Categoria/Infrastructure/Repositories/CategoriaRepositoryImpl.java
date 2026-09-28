package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Datasources.ICategoriaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Dtos.ObtenerCategoriasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class CategoriaRepositoryImpl implements ICategoriaRepository{

    private final ICategoriaDatasource categoriaDatasource;

    public CategoriaRepositoryImpl(ICategoriaDatasource categoriaDatasource) {
        this.categoriaDatasource = categoriaDatasource;
    }

    @Override
    public Categoria crear(Categoria categoria) {
        return this.categoriaDatasource.crear(categoria);
    }

    @Override
    public Categoria actualizar(CategoriaId categoriaId, Categoria categoria) {
        return this.categoriaDatasource.actualizar(categoriaId, categoria);
    }

    @Override
    public Optional<Categoria> obtenerPorId(CategoriaId categoriaId) {
        return this.categoriaDatasource.obtenerPorId(categoriaId);
    }

    @Override
    public PaginatedResponse<List<Categoria>> obtenerTodos(ObtenerCategoriasRequest obtenerCategoriasRequest) {
        return this.categoriaDatasource.obtenerTodos(obtenerCategoriasRequest);
    }

    @Override
    public void eliminar(CategoriaId categoriaId) {
        this.categoriaDatasource.eliminar(categoriaId);
    }

    @Override
    public Optional<Categoria> obtenerPorNombre(String nombre) {
        return this.categoriaDatasource.obtenerPorNombre(nombre);
    }
}
