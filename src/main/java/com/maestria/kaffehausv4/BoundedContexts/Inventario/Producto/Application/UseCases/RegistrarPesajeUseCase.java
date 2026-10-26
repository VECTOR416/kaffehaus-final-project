package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.RegistrarPesajeRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pesaje de las existencias abiertas de un producto base (logica.md EXISTENCIA-16,
 * EXISTENCIA-18). El usuario informa el peso total real; el sistema lo compara con la suma del
 * contenido de sus existencias vivas y decide:
 *
 * - Menos que el sistema: 'Merma'. Se descuenta empezando por la existencia del ultimo movimiento,
 *   vaciandola antes de pasar a la anterior.
 * - Mas que el sistema: 'Ajuste'. Se rellena en el mismo orden hasta el contenido inicial de cada
 *   una, incluidas las agotadas, que vuelven a 'Abierta' (EXISTENCIA-15). Si no cabe, 409.
 * - Igual: no registra nada.
 *
 * Un movimiento por existencia tocada, por la cantidad que le toco. Las existencias se bloquean
 * antes de leerlas (INV-52) y al final se recalcula el stock del producto base.
 */
@Service
@Transactional
public class RegistrarPesajeUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_ABIERTA = "Abierta";
    private static final String ESTADO_AGOTADA = "Agotada";

    /** Escala de NUMERIC(12, 4) en 'existencias'. */
    private static final int ESCALA_CONTENIDO = 4;

    private final IProductoRepository productoRepository;
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IUnidadRepository unidadRepository;
    private final IUsuarioRepository usuarioRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public RegistrarPesajeUseCase(
            IProductoRepository productoRepository,
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IUnidadRepository unidadRepository,
            IUsuarioRepository usuarioRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.unidadRepository = unidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public List<ExistenciaResponse> execute(String idProducto, RegistrarPesajeRequest request) {
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + request.getIdUsuario() + "' no existe"));

        Producto base = productoRepository.obtenerPorId(new ProductoId(idProducto))
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + idProducto + "' no existe"));
        List<Producto> presentaciones = productoRepository.obtenerPresentaciones(base.getId());
        if (!base.isEsStockDirecto() || presentaciones.isEmpty()) {
            throw DomainException.throwException(400,
                    "El producto '" + base.getNombre() + "' no es un producto base con presentaciones: no tiene existencias que pesar");
        }

        List<Existencia> existencias = existenciasDe(base, presentaciones);
        if (existencias.isEmpty()) {
            throw DomainException.throwException(409, "El producto '" + base.getNombre() + "' no tiene existencias abiertas que pesar");
        }

        BigDecimal enSistema = existencias.stream().map(Existencia::getContenidoActual).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diferencia = request.getPesoTotal().setScale(ESCALA_CONTENIDO, RoundingMode.HALF_UP).subtract(enSistema);
        if (diferencia.signum() == 0) {
            return List.of();
        }

        List<Existencia> enOrden = desdeElUltimoMovimiento(existencias);
        LocalDateTime ahora = LocalDateTime.now();
        List<Existencia> tocadas = diferencia.signum() < 0
                ? mermar(enOrden, diferencia.negate(), request.getMotivo(), usuarioId, ahora)
                : ajustar(enOrden, diferencia, request.getMotivo(), usuarioId, ahora);

        recalcularStockUseCase.execute(base.getId().getValue());

        Unidad unidad = unidadRepository.obtenerPorId(base.getUnidadActualId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad del producto base no existe"));
        List<ExistenciaResponse> respuesta = new ArrayList<>();
        for (Existencia existencia : tocadas) {
            EstadoExistencia estado = estadoExistenciaRepository.obtenerPorId(existencia.getEstadoExistenciaId())
                    .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia asociado a la existencia no existe"));
            respuesta.add(ExistenciaResponse.createFromAggregate(existencia, estado, unidad));
        }
        return respuesta;
    }

    /** Existencias vivas del producto base, abiertas desde cualquiera de sus presentaciones. */
    private List<Existencia> existenciasDe(Producto base, List<Producto> presentaciones) {
        List<ProductoId> fuentes = new ArrayList<>();
        fuentes.add(base.getId());
        presentaciones.forEach(presentacion -> fuentes.add(presentacion.getId()));
        Set<String> idsFuente = fuentes.stream().map(ProductoId::getValue).collect(Collectors.toSet());

        List<DetalleLoteId> detalles = loteRepository.obtenerPorProductos(fuentes).stream()
                .flatMap(lote -> lote.getDetallesActivos().stream())
                .filter(detalle -> idsFuente.contains(detalle.getProductoId().getValue()))
                .map(DetalleLote::getId)
                .collect(Collectors.toList());

        String unidad = base.getUnidadActualId().getValue();
        return existenciaRepository.obtenerVivasParaPesaje(detalles).stream()
                .filter(existencia -> existencia.getUnidadId().getValue().equals(unidad))
                .collect(Collectors.toList());
    }

    /**
     * Primero la existencia cuyo ultimo movimiento es el mas reciente, y asi hacia atras. A igual
     * fecha, la abierta mas tarde y, como desempate estable, la de mayor id.
     */
    private List<Existencia> desdeElUltimoMovimiento(List<Existencia> existencias) {
        Map<String, LocalDateTime> ultimoMovimiento = new HashMap<>();
        movimientoRepository.obtenerPorExistencias(existencias.stream().map(Existencia::getId).toList())
                .forEach(movimiento -> ultimoMovimiento.merge(
                        movimiento.getExistenciaId().getValue(), movimiento.getFechaMovimiento(), (a, b) -> a.isAfter(b) ? a : b));

        Comparator<Existencia> porUltimoMovimiento = Comparator.comparing(
                existencia -> ultimoMovimiento.getOrDefault(existencia.getId().getValue(), existencia.getFechaApertura()),
                Comparator.nullsFirst(Comparator.naturalOrder()));
        Comparator<Existencia> porApertura = Comparator.comparing(
                Existencia::getFechaApertura, Comparator.nullsFirst(Comparator.naturalOrder()));
        Comparator<Existencia> porId = Comparator.comparing(existencia -> existencia.getId().getValue());

        return existencias.stream()
                .sorted(porUltimoMovimiento.thenComparing(porApertura).thenComparing(porId).reversed())
                .collect(Collectors.toList());
    }

    private List<Existencia> mermar(List<Existencia> enOrden, BigDecimal faltante, String motivo, UsuarioId usuarioId, LocalDateTime ahora) {
        TipoMovimiento merma = tipo(TipoMovimiento.MERMA);
        EstadoExistencia agotada = estado(ESTADO_AGOTADA);
        List<Existencia> tocadas = new ArrayList<>();
        BigDecimal pendiente = faltante;
        for (Existencia existencia : enOrden) {
            if (pendiente.signum() == 0) {
                break;
            }
            BigDecimal cantidad = pendiente.min(existencia.getContenidoActual());
            if (cantidad.signum() <= 0) {
                continue;
            }
            existencia.consumir(cantidad, ahora, agotada.getId());
            registrar(existencia, cantidad, merma, motivo, usuarioId, ahora);
            tocadas.add(existencia);
            pendiente = pendiente.subtract(cantidad);
        }
        return tocadas;
    }

    private List<Existencia> ajustar(List<Existencia> enOrden, BigDecimal sobrante, String motivo, UsuarioId usuarioId, LocalDateTime ahora) {
        BigDecimal capacidad = enOrden.stream().map(Existencia::espacioDisponible).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (capacidad.compareTo(sobrante) < 0) {
            throw DomainException.throwException(409, "El peso informado supera en " + sobrante.subtract(capacidad).stripTrailingZeros().toPlainString()
                    + " lo que caben en las existencias abiertas; revisa el pesaje o abre la bolsa que falta registrar");
        }

        TipoMovimiento ajuste = tipo(TipoMovimiento.AJUSTE);
        EstadoExistencia abierta = estado(ESTADO_ABIERTA);
        List<Existencia> tocadas = new ArrayList<>();
        BigDecimal pendiente = sobrante;
        for (Existencia existencia : enOrden) {
            if (pendiente.signum() == 0) {
                break;
            }
            BigDecimal cantidad = pendiente.min(existencia.espacioDisponible());
            if (cantidad.signum() <= 0) {
                continue;
            }
            existencia.rellenar(cantidad, abierta.getId());
            registrar(existencia, cantidad, ajuste, motivo, usuarioId, ahora);
            tocadas.add(existencia);
            pendiente = pendiente.subtract(cantidad);
        }
        return tocadas;
    }

    private void registrar(Existencia existencia, BigDecimal cantidad, TipoMovimiento tipo, String motivo, UsuarioId usuarioId, LocalDateTime ahora) {
        existenciaRepository.actualizar(existencia.getId(), existencia);
        movimientoRepository.crear(Movimiento.crear(cantidad, ahora, motivo, existencia.getId(), tipo.getId(), usuarioId, null));
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
