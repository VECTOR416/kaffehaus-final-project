package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Operacion interna: recalcula 'producto.stock' de un producto de stock directo como la suma
 * del contenido abierto de sus existencias y de las de sus presentaciones (inventario.md
 * INV-20). No tiene endpoint; la invocan, dentro de su transaccion, todas las operaciones que
 * cambian 'contenido_actual': apertura, consumo, ingreso de produccion, merma, vencimiento y
 * ajuste.
 *
 * Recalcula en lugar de sumar o restar a proposito. Primero bloquea la fila del producto
 * (INV-52) y despues suma: la suma se lee ya con el bloqueo tomado, asi que ve lo que otra
 * transaccion haya confirmado, y el resultado no depende de un valor de 'stock' leido antes y
 * quiza desactualizado. Por eso el producto se bloquea el ultimo (INV-53).
 *
 * Un producto bajo demanda no tiene stock propio y se deja como esta.
 */
@Service
@Transactional
public class RecalcularStockUseCase {
    private final IProductoRepository productoRepository;
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;

    public RecalcularStockUseCase(
            IProductoRepository productoRepository,
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository) {
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
    }

    public void execute(String idProducto) {
        Producto producto = productoRepository.obtenerPorIdParaActualizar(new ProductoId(idProducto))
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + idProducto + "' no existe"));

        if (!producto.isEsStockDirecto()) {
            return;
        }

        List<ProductoId> fuentes = new ArrayList<>();
        fuentes.add(producto.getId());
        productoRepository.obtenerPresentaciones(producto.getId()).forEach(presentacion -> fuentes.add(presentacion.getId()));
        Set<String> idsFuente = fuentes.stream().map(ProductoId::getValue).collect(Collectors.toSet());

        List<DetalleLoteId> detalles = loteRepository.obtenerPorProductos(fuentes).stream()
                .flatMap(lote -> lote.getDetallesActivos().stream())
                .filter(detalle -> idsFuente.contains(detalle.getProductoId().getValue()))
                .map(DetalleLote::getId)
                .collect(Collectors.toList());

        String unidad = producto.getUnidadActualId().getValue();
        BigDecimal contenidoAbierto = existenciaRepository.obtenerDisponiblesPorDetallesLote(detalles).stream()
                .filter(existencia -> existencia.getUnidadId().getValue().equals(unidad))
                .map(Existencia::getContenidoActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        producto.proyectarStock(contenidoAbierto);
        productoRepository.actualizarStock(producto);
    }
}
