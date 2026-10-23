package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de el tipo. No se da de baja si un producto vivo lo usa: el producto quedaria
 * apuntando a una fila borrada (logica.md GENERAL-18). Solo Administrador (GENERAL-17).
 */
@Service
@Transactional
public class EliminarTipoUseCase {
    private final ITipoRepository tipoRepository;
    private final IProductoRepository productoRepository;

    public EliminarTipoUseCase(
            ITipoRepository tipoRepository,
            IProductoRepository productoRepository) {
        this.tipoRepository = tipoRepository;
        this.productoRepository = productoRepository;
    }

    public void execute(String tipoId) {
        TipoId id = new TipoId(tipoId);

        Tipo tipo = tipoRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo con id '" + tipoId + "' no existe"));
        if (productoRepository.existeConTipo(id)) {
            throw DomainException.throwException(409,
                    "El tipo '" + tipo.getNombre() + "' lo usan productos vivos y no se puede dar de baja");
        }

        tipoRepository.eliminar(id);
    }
}
