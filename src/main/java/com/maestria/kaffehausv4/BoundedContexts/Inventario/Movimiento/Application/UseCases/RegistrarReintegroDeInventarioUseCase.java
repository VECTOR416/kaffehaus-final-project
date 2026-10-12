package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Operacion interna: devuelve a inventario lo que descontaron unos movimientos de salida,
 * registrando por cada uno un compensatorio del mismo tipo que la salida
 * (ventas.md VEN-42). No tiene endpoint; la invoca la anulacion con reintegro de una venta, dentro de su transaccion.
 *
 * - Existencia viva y no agotada: se reintegra en la misma, sin superar su contenido inicial.
 * - Existencia agotada: no se revive (inventario.md INV-05); se abre una existencia nueva sobre
 *   el mismo detalle de lote, con la misma unidad y caducidad, y el compensatorio va a ella.
 * - Si lo reintegrado esta vencido, se registra y se da de baja de inmediato como
 *   'Merma' (INV-42): el libro mayor cuadra sin devolver a la venta algo vencido.
 *
 * Es todo o nada (VEN-43): si una parte no se puede reintegrar, lanza 409 y la transaccion del
 * llamador revierte entera. Bloquea las existencias en orden de id y recalcula al final el
 * stock de cada producto tocado (INV-52, INV-53).
 */
@Service
@Transactional
public class RegistrarReintegroDeInventarioUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_ABIERTA = "Abierta";
    private static final String ESTADO_AGOTADA = "Agotada";

    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final ILoteRepository loteRepository;
    private final IProductoRepository productoRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public RegistrarReintegroDeInventarioUseCase(
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            IExistenciaRepository existenciaRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            ILoteRepository loteRepository,
            IProductoRepository productoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.existenciaRepository = existenciaRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.loteRepository = loteRepository;
        this.productoRepository = productoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public void execute(RegistrarReintegroDeInventarioRequest request) {
        TipoMovimiento merma = tipo(TipoMovimiento.MERMA);
        EstadoExistencia abierta = estado(ESTADO_ABIERTA);
        EstadoExistencia agotada = estado(ESTADO_AGOTADA);
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());

        List<Movimiento> salidas = movimientoRepository.obtenerPorMotivo(request.getMotivoOrigen()).stream()
                .sorted(Comparator.comparing((Movimiento movimiento) -> movimiento.getExistenciaId().getValue())
                        .thenComparing(movimiento -> movimiento.getId().getValue()))
                .toList();

        Set<String> productosTocados = new TreeSet<>();
        for (Movimiento salida : salidas) {
            Existencia origen = existenciaRepository.obtenerPorIdParaActualizar(salida.getExistenciaId())
                    .orElseThrow(() -> DomainException.throwException(404,
                            "La existencia con id '" + salida.getExistenciaId().getValue() + "' no existe"));

            Existencia destino;
            if (origen.isAgotada()) {
                destino = existenciaRepository.crear(Existencia.crear(
                        salida.getCantidad(),
                        salida.getCantidad(),
                        null,
                        origen.getFechaVencimiento(),
                        request.getFecha(),
                        origen.getDetalleLoteId(),
                        abierta.getId(),
                        origen.getUnidadId(),
                        usuarioId));
            } else {
                origen.reintegrar(salida.getCantidad());
                destino = existenciaRepository.actualizar(origen.getId(), origen);
            }

            movimientoRepository.crear(Movimiento.crear(
                    salida.getCantidad(), request.getFecha(), request.getMotivoReintegro(), destino.getId(), salida.getTipoMovimientoId(), usuarioId, null));

            if (destino.estaVencida(request.getFecha().toLocalDate())) {
                destino.consumir(salida.getCantidad(), request.getFecha(), agotada.getId());
                existenciaRepository.actualizar(destino.getId(), destino);
                movimientoRepository.crear(Movimiento.crear(
                        salida.getCantidad(), request.getFecha(), request.getMotivoReintegro() + " (vencido)",
                        destino.getId(), merma.getId(), usuarioId, null));
            }

            productosTocados.add(productoBaseDe(origen));
        }

        productosTocados.forEach(recalcularStockUseCase::execute);
    }

    private String productoBaseDe(Existencia existencia) {
        Lote lote = loteRepository.obtenerPorDetalleId(existencia.getDetalleLoteId())
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote de la existencia no existe"));
        Producto producto = productoRepository.obtenerPorId(lote.obtenerDetalle(existencia.getDetalleLoteId().getValue()).getProductoId())
                .orElseThrow(() -> DomainException.throwException(404, "El producto de la existencia no existe"));
        return producto.productoBaseId().getValue();
    }

    private TipoMovimiento tipo(String nombre) {
        return tipoMovimientoRepository.obtenerPorNombre(nombre)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + nombre + "' no esta registrado"));
    }

    private EstadoExistencia estado(String nombre) {
        return estadoExistenciaRepository.obtenerPorNombre(nombre)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia '" + nombre + "' no esta registrado"));
    }
}
