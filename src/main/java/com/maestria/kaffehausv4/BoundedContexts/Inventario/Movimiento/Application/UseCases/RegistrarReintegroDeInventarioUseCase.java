package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Operacion interna: devuelve lo que desconto una venta anulada con reintegro (logica.md
 * VENTA-25). No tiene endpoint; la invoca la anulacion de una venta, dentro de su transaccion.
 *
 * Por cada movimiento 'Venta' de la venta registra un movimiento 'Reintegro' que devuelve la
 * misma cantidad al mismo sujeto, aunque este vencido:
 * - una linea de lote recupera sus unidades (LOTE-24), sin superar su cantidad inicial;
 * - una existencia recupera su contenido y, si estaba agotada, vuelve a 'Abierta' (EXISTENCIA-19).
 * Asi tambien vuelven los insumos de los MTO.
 *
 * Bloquea en el orden de GENERAL-15: primero, de una vez y en orden de id, los lotes de las
 * lineas a las que devuelve (LOTE-27); despues, por producto en orden de id, sus existencias y el
 * producto (al recalcular su stock). Para ordenar sin cargar antes las filas que va a bloquear,
 * averigua el lote y el producto de cada sujeto con consultas que solo leen ids
 * (arquitectura.md 5.7). Es todo o nada: si algo no se puede devolver lanza 409 y la
 * transaccion del llamador revierte entera.
 */
@Service
@Transactional
public class RegistrarReintegroDeInventarioUseCase {
    // Fila de catalogo que el flujo resuelve por nombre (logica.md GENERAL-04).
    private static final String ESTADO_ABIERTA = "Abierta";

    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IProductoRepository productoRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public RegistrarReintegroDeInventarioUseCase(
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IProductoRepository productoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.productoRepository = productoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public void execute(RegistrarReintegroDeInventarioRequest request) {
        VentaId ventaId = new VentaId(request.getIdVenta());
        TipoMovimiento salidaPorVenta = tipo(TipoMovimiento.VENTA);
        TipoMovimiento reintegro = tipo(TipoMovimiento.REINTEGRO);

        List<Movimiento> salidas = movimientoRepository.obtenerPorVenta(ventaId).stream()
                .filter(movimiento -> movimiento.getTipoMovimientoId().getValue().equals(salidaPorVenta.getId().getValue()))
                .toList();
        if (salidas.isEmpty()) {
            throw DomainException.throwException(409, "La venta con id '" + request.getIdVenta()
                    + "' no tiene salidas de inventario registradas que devolver; anulala sin reintegro");
        }

        // Producto -> sujeto -> cantidad, ambos en orden de id (GENERAL-15).
        Map<String, Map<String, Devolucion>> porProducto = new TreeMap<>();
        for (Movimiento salida : salidas) {
            Devolucion devolucion = salida.getDetalleLoteId() != null
                    ? Devolucion.aLinea(salida.getDetalleLoteId())
                    : Devolucion.aExistencia(salida.getExistenciaId());
            porProducto.computeIfAbsent(productoDe(devolucion).getValue(), id -> new TreeMap<>())
                    .computeIfAbsent(devolucion.clave(), clave -> devolucion)
                    .sumar(salida.getCantidad());
        }

        List<Lote> lotes = bloquearLotes(porProducto);

        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        EstadoExistencia abierta = null;
        for (Map.Entry<String, Map<String, Devolucion>> producto : porProducto.entrySet()) {
            for (Devolucion devolucion : producto.getValue().values()) {
                if (devolucion.esLinea()) {
                    devolverALinea(devolucion, lotes, request, reintegro, usuarioId, ventaId);
                } else {
                    abierta = abierta != null ? abierta : estadoAbierta();
                    devolverAExistencia(devolucion, request, reintegro, usuarioId, ventaId, abierta);
                }
            }
            recalcularStockUseCase.execute(producto.getKey());
        }
    }

    /** Los lotes de todas las lineas a las que se devuelve, bloqueados en orden de id (LOTE-27). */
    private List<Lote> bloquearLotes(Map<String, Map<String, Devolucion>> porProducto) {
        List<LoteId> idsLote = porProducto.values().stream()
                .flatMap(devoluciones -> devoluciones.values().stream())
                .filter(Devolucion::esLinea)
                .map(devolucion -> loteRepository.obtenerLoteDeDetalle(devolucion.detalleLoteId)
                        .orElseThrow(() -> lineaInexistente(devolucion.detalleLoteId)))
                .map(LoteId::getValue)
                .distinct()
                .sorted()
                .map(LoteId::new)
                .toList();
        return loteRepository.obtenerPorIdsParaActualizar(idsLote);
    }

    /**
     * El producto cuyo stock cambia: el de la linea, o el producto base de la presentacion de la
     * que se abrio la existencia (PRODUCTO-11). Solo lee ids: no carga las filas que se bloquean.
     */
    private ProductoId productoDe(Devolucion devolucion) {
        if (devolucion.esLinea()) {
            return productoDeLinea(devolucion.detalleLoteId);
        }
        DetalleLoteId origen = existenciaRepository.obtenerDetalleLoteDe(devolucion.existenciaId)
                .orElseThrow(() -> DomainException.throwException(409,
                        "La existencia con id '" + devolucion.existenciaId.getValue() + "' ya no existe; no se puede devolver su contenido"));
        ProductoId presentacionId = productoDeLinea(origen);
        Producto presentacion = productoRepository.obtenerPorId(presentacionId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + presentacionId.getValue() + "' no existe"));
        return presentacion.productoBaseId();
    }

    private ProductoId productoDeLinea(DetalleLoteId detalleLoteId) {
        return loteRepository.obtenerProductoDeDetalle(detalleLoteId)
                .orElseThrow(() -> lineaInexistente(detalleLoteId));
    }

    private static DomainException lineaInexistente(DetalleLoteId detalleLoteId) {
        return DomainException.throwException(409,
                "El detalle de lote con id '" + detalleLoteId.getValue() + "' ya no existe; no se pueden devolver sus unidades");
    }

    private void devolverALinea(Devolucion devolucion, List<Lote> lotes, RegistrarReintegroDeInventarioRequest request,
            TipoMovimiento reintegro, UsuarioId usuarioId, VentaId ventaId) {
        String idDetalle = devolucion.detalleLoteId.getValue();
        // El lote ya esta bloqueado; si se dio de baja entre tanto, no se devuelve a el.
        Lote lote = lotes.stream()
                .filter(candidato -> candidato.getDetallesActivos().stream()
                        .anyMatch(detalle -> detalle.getId().getValue().equals(idDetalle)))
                .findFirst()
                .orElseThrow(() -> lineaInexistente(devolucion.detalleLoteId));
        lote.devolverUnidades(idDetalle, devolucion.cantidad);
        loteRepository.actualizar(lote.getId(), lote);
        movimientoRepository.crear(Movimiento.registrarSobreLinea(
                devolucion.cantidad, request.getFecha(), request.getMotivo(), devolucion.detalleLoteId,
                reintegro.getId(), usuarioId, null, ventaId));
    }

    private void devolverAExistencia(Devolucion devolucion, RegistrarReintegroDeInventarioRequest request,
            TipoMovimiento reintegro, UsuarioId usuarioId, VentaId ventaId, EstadoExistencia abierta) {
        String idExistencia = devolucion.existenciaId.getValue();
        Existencia existencia = existenciaRepository.obtenerPorIdParaActualizar(devolucion.existenciaId)
                .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + idExistencia + "' no existe"));
        existencia.reintegrar(devolucion.cantidad, abierta.getId());
        existenciaRepository.actualizar(existencia.getId(), existencia);
        movimientoRepository.crear(Movimiento.registrarSobreExistencia(
                devolucion.cantidad, request.getFecha(), request.getMotivo(), devolucion.existenciaId,
                reintegro.getId(), usuarioId, null, ventaId));
    }

    private TipoMovimiento tipo(String nombre) {
        return tipoMovimientoRepository.obtenerPorNombre(nombre)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + nombre + "' no esta registrado"));
    }

    private EstadoExistencia estadoAbierta() {
        return estadoExistenciaRepository.obtenerPorNombre(ESTADO_ABIERTA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia '" + ESTADO_ABIERTA + "' no esta registrado"));
    }

    /** Lo que vuelve a un sujeto: la suma de lo que la venta desconto de el. */
    private static final class Devolucion {
        private final DetalleLoteId detalleLoteId;
        private final ExistenciaId existenciaId;
        private BigDecimal cantidad = BigDecimal.ZERO;

        private Devolucion(DetalleLoteId detalleLoteId, ExistenciaId existenciaId) {
            this.detalleLoteId = detalleLoteId;
            this.existenciaId = existenciaId;
        }

        private static Devolucion aLinea(DetalleLoteId detalleLoteId) {
            return new Devolucion(detalleLoteId, null);
        }

        private static Devolucion aExistencia(ExistenciaId existenciaId) {
            return new Devolucion(null, existenciaId);
        }

        private boolean esLinea() {
            return detalleLoteId != null;
        }

        private String clave() {
            return esLinea() ? "L" + detalleLoteId.getValue() : "E" + existenciaId.getValue();
        }

        private void sumar(BigDecimal otra) {
            this.cantidad = this.cantidad.add(otra);
        }
    }
}
