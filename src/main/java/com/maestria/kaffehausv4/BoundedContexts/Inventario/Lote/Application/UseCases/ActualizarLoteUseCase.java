package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ActualizarDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ActualizarLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Modificacion de un lote existente, encabezado y lineas, en una sola transaccion.
 *
 * Cuando el payload trae 'detalles' se concilia por id en lugar de reemplazar la coleccion:
 * las lineas con id se corrigen, las que no lo traen se agregan y las ausentes se dan de baja
 * logica. Conservar los identificadores no es un detalle de implementacion sino un requisito
 * de integridad: 'existencias.id_detalle_lote' es ON DELETE RESTRICT y apunta a estas lineas.
 *
 * Toda mutacion pasa por la raiz, que es quien recalcula el costo del lote y valida que el
 * stock restante siga siendo coherente con lo comprado.
 *
 * Corregir 'cantidadRestante' es un conteo (logica.md LOTE-23): exige 'motivo' y registra sobre
 * la linea un movimiento 'Merma' si se contaron menos o 'Ajuste' si se contaron mas, por la
 * diferencia.
 *
 * El lote se bloquea por su raiz antes de leerlo (logica.md LOTE-27): la diferencia de un conteo
 * se calcula sobre la cantidad vigente y ninguna venta descuenta de sus lineas a la vez.
 */
@Service
@Transactional
public class ActualizarLoteUseCase {
    private final ILoteRepository loteRepository;
    private final IProveedorRepository proveedorRepository;
    private final IProductoRepository productoRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public ActualizarLoteUseCase(
            ILoteRepository loteRepository,
            IProveedorRepository proveedorRepository,
            IProductoRepository productoRepository,
            IExistenciaRepository existenciaRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.loteRepository = loteRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.existenciaRepository = existenciaRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public LoteResponse execute(String loteId, ActualizarLoteRequest actualizarLoteRequest) {
        LoteId id = new LoteId(loteId);

        Lote lote = loteRepository.obtenerPorIdParaActualizar(id)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId + "' no existe"));

        ProveedorId proveedorId = null;
        if (actualizarLoteRequest.getIdProveedor() != null) {
            proveedorId = new ProveedorId(actualizarLoteRequest.getIdProveedor());
            proveedorRepository.obtenerPorId(proveedorId)
                    .orElseThrow(() -> DomainException.throwException(404, "El proveedor con id '" + actualizarLoteRequest.getIdProveedor() + "' no existe"));
        }
        // El usuario que registro la compra no se corrige: lo fijo la sesion al darla de alta.
        lote.actualizar(
                actualizarLoteRequest.getFechaCompra(),
                proveedorId,
                null);

        List<Conteo> conteos = new ArrayList<>();
        if (actualizarLoteRequest.getDetalles() != null) {
            conciliarDetalles(lote, actualizarLoteRequest.getDetalles(), conteos);
        }
        if (!conteos.isEmpty() && (actualizarLoteRequest.getMotivo() == null || actualizarLoteRequest.getMotivo().isBlank())) {
            throw DomainException.throwException(400,
                    "Corregir 'cantidadRestante' es un conteo de inventario y requiere 'motivo'");
        }

        lote.validarRegistrable();

        Lote loteActualizado = loteRepository.actualizar(id, lote);
        registrarConteos(conteos, actualizarLoteRequest);
        // Corregir o dar de baja lineas cambia el stock de sus productos (logica.md PRODUCTO-16).
        CrearLoteUseCase.productosDe(loteActualizado).forEach(recalcularStockUseCase::execute);

        return LoteResponse.createFromAggregate(loteActualizado);
    }

    private void conciliarDetalles(Lote lote, List<ActualizarDetalleLoteRequest> detalles, List<Conteo> conteos) {
        Set<String> idsRecibidos = detalles.stream()
                .map(ActualizarDetalleLoteRequest::getId)
                .filter(idDetalle -> idDetalle != null)
                .collect(Collectors.toSet());

        List<String> idsVivos = lote.getDetallesActivos().stream()
                .map(detalle -> detalle.getId().getValue())
                .collect(Collectors.toList());

        for (String idVivo : idsVivos) {
            if (!idsRecibidos.contains(idVivo)) {
                // Solo se quita una linea que no se uso y de la que no se abrio nada (logica.md
                // LOTE-26, que amplia LOTE-07).
                lote.exigirQueLaLineaPuedaDarseDeBaja(idVivo);
                if (existenciaRepository.existenPorDetallesLote(List.of(new DetalleLoteId(idVivo)))) {
                    throw DomainException.throwException(409,
                            "Del detalle con id '" + idVivo + "' ya se abrieron existencias y no se puede dar de baja");
                }
                lote.removerDetalle(idVivo);
            }
        }

        for (ActualizarDetalleLoteRequest detalle : detalles) {
            if (detalle.getId() == null) {
                agregarDetalleNuevo(lote, detalle);
            } else {
                corregirDetalleExistente(lote, detalle, conteos);
            }
        }
    }

    private void agregarDetalleNuevo(Lote lote, ActualizarDetalleLoteRequest detalle) {
        if (detalle.getIdProducto() == null || detalle.getCantidadInicial() == null || detalle.getPrecioUnitario() == null) {
            throw DomainException.throwException(400,
                    "Un detalle de lote nuevo requiere 'idProducto', 'cantidadInicial' y 'precioUnitario'");
        }

        ProductoId productoId = new ProductoId(detalle.getIdProducto());
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + detalle.getIdProducto() + "' no existe"));
        CrearLoteUseCase.exigirQueSeCompre(producto, productoRepository);

        lote.agregarDetalle(
                productoId,
                detalle.getCantidadInicial(),
                detalle.getPrecioUnitario(),
                detalle.getFraccionable(),
                detalle.getFechaVencimiento());
    }

    private void corregirDetalleExistente(Lote lote, ActualizarDetalleLoteRequest detalle, List<Conteo> conteos) {
        // Cambiar el producto de una linea ya comprada equivaldria a rehacer la compra y
        // dejaria sin sentido las existencias que ya colgaban de ella.
        if (detalle.getIdProducto() != null && !detalle.getIdProducto().equals(productoActualDe(lote, detalle.getId()))) {
            throw DomainException.throwException(400,
                    "No se puede cambiar el producto de un detalle de lote existente; da de baja la linea y agrega una nueva");
        }

        lote.actualizarDetalle(
                detalle.getId(),
                detalle.getCantidadInicial(),
                detalle.getPrecioUnitario(),
                detalle.getFraccionable(),
                detalle.getFechaVencimiento());

        if (detalle.getCantidadRestante() != null) {
            BigDecimal antes = lote.obtenerDetalle(detalle.getId()).getCantidadRestante();
            lote.ajustarCantidadRestante(detalle.getId(), detalle.getCantidadRestante());
            BigDecimal diferencia = lote.obtenerDetalle(detalle.getId()).getCantidadRestante().subtract(antes);
            if (diferencia.signum() != 0) {
                conteos.add(new Conteo(new DetalleLoteId(detalle.getId()), diferencia));
            }
        }
    }

    /** Un movimiento por linea contada: 'Merma' si hay menos, 'Ajuste' si hay mas (LOTE-23). */
    private void registrarConteos(List<Conteo> conteos, ActualizarLoteRequest request) {
        if (conteos.isEmpty()) {
            return;
        }
        TipoMovimiento merma = tipo(TipoMovimiento.MERMA);
        TipoMovimiento ajuste = tipo(TipoMovimiento.AJUSTE);
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());
        LocalDateTime ahora = LocalDateTime.now();
        for (Conteo conteo : conteos) {
            TipoMovimiento tipo = conteo.diferencia.signum() < 0 ? merma : ajuste;
            movimientoRepository.crear(Movimiento.registrarSobreLinea(
                    conteo.diferencia.abs(), ahora, request.getMotivo(), conteo.detalleLoteId, tipo.getId(), usuarioId, null, null));
        }
    }

    private TipoMovimiento tipo(String nombre) {
        return tipoMovimientoRepository.obtenerPorNombre(nombre)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + nombre + "' no esta registrado"));
    }

    private String productoActualDe(Lote lote, String detalleLoteId) {
        return lote.getDetallesActivos().stream()
                .filter(detalle -> detalle.getId().getValue().equals(detalleLoteId))
                .map(DetalleLote::getProductoId)
                .map(ProductoId::getValue)
                .findFirst()
                .orElseThrow(() -> DomainException.throwException(404,
                        "El detalle con id '" + detalleLoteId + "' no pertenece a este lote"));
    }

    /** La correccion de una linea: cuanto cambio su cantidad restante con el conteo. */
    private static final class Conteo {
        private final DetalleLoteId detalleLoteId;
        private final BigDecimal diferencia;

        private Conteo(DetalleLoteId detalleLoteId, BigDecimal diferencia) {
            this.detalleLoteId = detalleLoteId;
            this.diferencia = diferencia;
        }
    }
}
