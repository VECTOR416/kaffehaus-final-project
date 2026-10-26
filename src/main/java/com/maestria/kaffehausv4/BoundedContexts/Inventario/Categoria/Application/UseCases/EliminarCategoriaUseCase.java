package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de la categoria. No se da de baja si un producto vivo la usa: el producto quedaria
 * apuntando a una fila borrada (logica.md GENERAL-18). Ademas, una categoria no borrable no se da
 * de baja (CATALOGO-INV-05). Solo Administrador (GENERAL-17).
 */
@Service
@Transactional
public class EliminarCategoriaUseCase {
    private final ICategoriaRepository categoriaRepository;
    private final IProductoRepository productoRepository;

    public EliminarCategoriaUseCase(
            ICategoriaRepository categoriaRepository,
            IProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    public void execute(String categoriaId) {
        CategoriaId id = new CategoriaId(categoriaId);

        Categoria categoria = categoriaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La categoria con id '" + categoriaId + "' no existe"));
        if (productoRepository.existeConCategoria(id)) {
            throw DomainException.throwException(409,
                    "La categoria '" + categoria.getNombre() + "' la usan productos vivos y no se puede dar de baja");
        }

        categoriaRepository.eliminar(id);
    }
}
