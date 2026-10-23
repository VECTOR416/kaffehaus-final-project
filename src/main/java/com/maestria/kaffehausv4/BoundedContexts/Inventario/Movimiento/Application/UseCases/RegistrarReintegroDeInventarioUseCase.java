package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Operacion interna: devuelve a sus lineas de lote las unidades que desconto una venta anulada
 * con reintegro (logica.md VENTA-22, LOTE-18). No tiene endpoint; la invoca la anulacion de una
 * venta, dentro de su transaccion.
 *
 * - Producto por unidades: las unidades vuelven a las lineas vivas con espacio, primero la que
 *   vence antes, sin superar la cantidad inicial de ninguna. Cada linea se bloquea antes.
 * - Producto bajo demanda: su venta no desconto nada (VENTA-23) y no devuelve nada.
 *
 * Es todo o nada: si no caben todas las unidades lanza 409 y la transaccion del llamador revierte
 * entera. Procesa los productos en orden de id (INV-53) y recalcula el stock de cada uno.
 */
@Service
@Transactional
public class RegistrarReintegroDeInventarioUseCase {
    /** Escala de NUMERIC(12, 2) en 'detalle_lote'. */
    private static final int ESCALA_UNIDADES = 2;

    private final ILoteRepository loteRepository;
    private final IProductoRepository productoRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public RegistrarReintegroDeInventarioUseCase(
            ILoteRepository loteRepository,
            IProductoRepository productoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.loteRepository = loteRepository;
        this.productoRepository = productoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public void execute(RegistrarReintegroDeInventarioRequest request) {
        List<RegistrarReintegroDeInventarioRequest.Linea> enOrden = request.getLineas().stream()
                .sorted(Comparator.comparing(RegistrarReintegroDeInventarioRequest.Linea::getIdProducto))
                .toList();
        for (RegistrarReintegroDeInventarioRequest.Linea linea : enOrden) {
            Producto producto = productoRepository.obtenerPorId(new ProductoId(linea.getIdProducto()))
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + linea.getIdProducto() + "' no existe"));
            if (producto.isEsStockDirecto()) {
                devolver(producto, linea.getCantidad());
            }
        }
    }

    private void devolver(Producto producto, BigDecimal cantidad) {
        BigDecimal pendiente = cantidad.setScale(ESCALA_UNIDADES, RoundingMode.HALF_UP);
        if (pendiente.signum() <= 0) {
            return;
        }

        List<DetalleLote> lineas = Lote.conEspacioEnOrden(
                loteRepository.obtenerPorProductos(List.of(producto.getId())), producto.getId());
        BigDecimal espacio = lineas.stream()
                .map(DetalleLote::espacioDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (espacio.compareTo(pendiente) < 0) {
            throw DomainException.throwException(409, "No se pueden devolver " + pendiente.stripTrailingZeros().toPlainString()
                    + " unidades de '" + producto.getNombre() + "': sus lotes solo admiten " + espacio.stripTrailingZeros().toPlainString());
        }

        for (DetalleLote linea : lineas) {
            if (pendiente.signum() == 0) {
                break;
            }
            String idDetalle = linea.getId().getValue();
            Lote lote = loteRepository.obtenerPorDetalleIdParaActualizar(linea.getId())
                    .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + idDetalle + "' no existe"));
            // Releida con el bloqueo tomado: otra transaccion pudo cambiarla antes.
            BigDecimal devuelto = pendiente.min(lote.obtenerDetalle(idDetalle).espacioDisponible());
            if (devuelto.signum() <= 0) {
                continue;
            }
            lote.devolverUnidades(idDetalle, devuelto);
            loteRepository.actualizar(lote.getId(), lote);
            pendiente = pendiente.subtract(devuelto);
        }

        if (pendiente.signum() > 0) {
            throw DomainException.throwException(409,
                    "No se pudieron devolver todas las unidades de '" + producto.getNombre() + "': otra operacion cambio sus lotes a la vez; reintenta");
        }
        recalcularStockUseCase.execute(producto.getId().getValue());
    }
}
