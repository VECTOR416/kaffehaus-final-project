package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de una receta con sus ingredientes y pasos (logica.md RECETA-09; solo Administrador,
 * GENERAL-17). No se da de baja la ultima receta viva de un MTO vendible: sin ella el producto ya
 * no se podria vender (RECETA-08). Las producciones que la usaron la conservan como historia.
 */
@Service
@Transactional
public class EliminarRecetaUseCase {
    private final IRecetaRepository recetaRepository;
    private final IProductoRepository productoRepository;

    public EliminarRecetaUseCase(
            IRecetaRepository recetaRepository,
            IProductoRepository productoRepository) {
        this.recetaRepository = recetaRepository;
        this.productoRepository = productoRepository;
    }

    public void execute(String recetaId) {
        RecetaId id = new RecetaId(recetaId);

        Receta receta = recetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + recetaId + "' no existe"));

        Producto obtenido = productoRepository.obtenerPorId(receta.getProductoObtenidoId()).orElse(null);
        boolean esMtoVendible = obtenido != null && !obtenido.isEsStockDirecto() && obtenido.isVendible();
        if (esMtoVendible && recetaRepository.obtenerVivasPorProductoObtenido(obtenido.getId()).size() <= 1) {
            throw DomainException.throwException(409,
                    "La receta es la ultima del producto '" + obtenido.getNombre()
                    + "', que se vende bajo demanda: registra otra receta antes de darla de baja");
        }

        recetaRepository.eliminar(id);
    }
}
