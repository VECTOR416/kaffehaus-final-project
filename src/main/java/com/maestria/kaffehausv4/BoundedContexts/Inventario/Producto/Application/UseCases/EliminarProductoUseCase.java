package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Baja logica de un producto (logica.md PRODUCTO-20). Se rechaza con 409 si aun tiene inventario
 * o si otro registro vivo lo necesita para operar:
 *
 * - stock distinto de 0, lineas de lote vivas con unidades o existencias con contenido abiertas
 *   desde sus lineas: su inventario quedaria sin producto;
 * - presentaciones vivas: perderian su producto base;
 * - ingrediente de una receta viva: la receta ya no podria consumirlo.
 *
 * Lo agotado no impide la baja. Sus recetas vivas (como producto obtenido) y sus descuentos vivos
 * se dan de baja en la misma transaccion (DESCUENTO-05). El producto se bloquea antes de revisar
 * su inventario, como hace todo recalculo de su stock.
 */
@Service
@Transactional
public class EliminarProductoUseCase {
    private final IProductoRepository productoRepository;
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IRecetaRepository recetaRepository;
    private final IDescuentoRepository descuentoRepository;

    public EliminarProductoUseCase(
            IProductoRepository productoRepository,
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository,
            IRecetaRepository recetaRepository,
            IDescuentoRepository descuentoRepository) {
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
        this.recetaRepository = recetaRepository;
        this.descuentoRepository = descuentoRepository;
    }

    public void execute(String productoId) {
        ProductoId id = new ProductoId(productoId);

        Producto producto = productoRepository.obtenerPorIdParaActualizar(id)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + productoId + "' no existe"));

        exigirSinInventario(producto);
        if (!productoRepository.obtenerPresentaciones(id).isEmpty()) {
            throw DomainException.throwException(409,
                    "El producto '" + producto.getNombre() + "' es producto base de presentaciones vivas y no se puede dar de baja");
        }
        List<Receta> recetasQueLoUsan = recetaRepository.obtenerPorIngrediente(id);
        if (!recetasQueLoUsan.isEmpty()) {
            throw DomainException.throwException(409,
                    "El producto '" + producto.getNombre() + "' es ingrediente de la receta '" + recetasQueLoUsan.get(0).getTitulo()
                    + "' y no se puede dar de baja");
        }

        recetaRepository.obtenerVivasPorProductoObtenido(id).stream()
                .map(Receta::getId)
                .forEach(recetaRepository::eliminar);
        descuentoRepository.obtenerPorProducto(id).stream()
                .map(Descuento::getId)
                .forEach(descuentoRepository::eliminar);
        productoRepository.eliminar(id);
    }

    private void exigirSinInventario(Producto producto) {
        if (producto.getStock() != null && producto.getStock().signum() != 0) {
            throw DomainException.throwException(409,
                    "El producto '" + producto.getNombre() + "' tiene stock y no se puede dar de baja");
        }

        String idProducto = producto.getId().getValue();
        List<DetalleLote> lineas = loteRepository.obtenerPorProductos(List.of(producto.getId())).stream()
                .filter(lote -> !lote.isBorrado())
                .flatMap(lote -> lote.getDetallesActivos().stream())
                .filter(detalle -> idProducto.equals(detalle.getProductoId().getValue()))
                .toList();
        if (lineas.stream().anyMatch(DetalleLote::tieneUnidadesDisponibles)) {
            throw DomainException.throwException(409,
                    "El producto '" + producto.getNombre() + "' tiene unidades en lotes y no se puede dar de baja");
        }
        if (!existenciaRepository.obtenerDisponiblesPorDetallesLote(lineas.stream().map(DetalleLote::getId).toList()).isEmpty()) {
            throw DomainException.throwException(409,
                    "El producto '" + producto.getNombre() + "' tiene existencias abiertas con contenido y no se puede dar de baja");
        }
    }
}
