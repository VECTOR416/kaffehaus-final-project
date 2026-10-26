package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerVencimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.VencimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;

/**
 * Alerta de vencimiento (logica.md VENCIMIENTO-04): las lineas de lote con unidades y las
 * existencias con contenido que ya vencieron o vencen hasta hoy + 'dias', ordenadas por fecha de
 * vencimiento. Es una lectura: no bloquea nada. Lo que la alerta muestra se da de baja con
 * VENCIMIENTO-03 cuando el usuario lo decide.
 */
@Service
@Transactional(readOnly = true)
public class ObtenerVencimientosUseCase {
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IProductoRepository productoRepository;

    public ObtenerVencimientosUseCase(
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository,
            IProductoRepository productoRepository) {
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
        this.productoRepository = productoRepository;
    }

    public List<VencimientoResponse> execute(ObtenerVencimientosRequest request) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(request.getDias());
        Map<String, Optional<Producto>> productos = new HashMap<>();
        List<VencimientoResponse> filas = new ArrayList<>();

        for (Lote lote : loteRepository.obtenerConUnidadesQueVencenHasta(limite)) {
            for (DetalleLote detalle : lote.getDetallesActivos()) {
                if (!detalle.tieneUnidadesDisponibles() || detalle.getFechaVencimiento() == null
                        || detalle.getFechaVencimiento().isAfter(limite)) {
                    continue;
                }
                filas.add(VencimientoResponse.deLinea(lote, detalle, producto(detalle.getProductoId(), productos).orElse(null), hoy));
            }
        }

        for (Existencia existencia : existenciaRepository.obtenerConContenidoQueVencenHasta(limite)) {
            // Una existencia es del producto base de la presentacion que se abrio (PRODUCTO-14).
            Producto base = loteRepository.obtenerProductoDeDetalle(existencia.getDetalleLoteId())
                    .flatMap(presentacionId -> producto(presentacionId, productos))
                    .map(presentacion -> presentacion.esPresentacion() ? producto(presentacion.productoBaseId(), productos).orElse(null) : presentacion)
                    .orElse(null);
            filas.add(VencimientoResponse.deExistencia(
                    existencia, loteRepository.obtenerLoteDeDetalle(existencia.getDetalleLoteId()).orElse(null), base, hoy));
        }

        filas.sort(Comparator.comparing(VencimientoResponse::getFechaVencimiento)
                .thenComparing(VencimientoResponse::getId));
        return filas;
    }

    private Optional<Producto> producto(ProductoId productoId, Map<String, Optional<Producto>> productos) {
        return productos.computeIfAbsent(productoId.getValue(), id -> productoRepository.obtenerPorId(productoId));
    }
}
