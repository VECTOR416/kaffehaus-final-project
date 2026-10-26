package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de la unidad. No se da de baja si es la unidad actual de un producto vivo: su stock y
 * sus conversiones dejarian de tener unidad (logica.md GENERAL-18). Ademas, una unidad no borrable
 * no se da de baja (CATALOGO-INV-05). Solo Administrador (GENERAL-17).
 */
@Service
@Transactional
public class EliminarUnidadUseCase {
    private final IUnidadRepository unidadRepository;
    private final IProductoRepository productoRepository;

    public EliminarUnidadUseCase(
            IUnidadRepository unidadRepository,
            IProductoRepository productoRepository) {
        this.unidadRepository = unidadRepository;
        this.productoRepository = productoRepository;
    }

    public void execute(String unidadId) {
        UnidadId id = new UnidadId(unidadId);

        Unidad unidad = unidadRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La unidad con id '" + unidadId + "' no existe"));
        if (productoRepository.existeConUnidad(id)) {
            throw DomainException.throwException(409,
                    "La unidad '" + unidad.getUnidad() + "' la usan productos vivos y no se puede dar de baja");
        }

        unidadRepository.eliminar(id);
    }
}
