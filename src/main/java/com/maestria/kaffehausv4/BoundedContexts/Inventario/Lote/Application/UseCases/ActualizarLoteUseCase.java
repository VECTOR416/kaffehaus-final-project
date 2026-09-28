package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
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
 */
@Service
@Transactional
public class ActualizarLoteUseCase {
    private final ILoteRepository loteRepository;
    private final IProveedorRepository proveedorRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IProductoRepository productoRepository;
    private final IExistenciaRepository existenciaRepository;

    public ActualizarLoteUseCase(
            ILoteRepository loteRepository,
            IProveedorRepository proveedorRepository,
            IUsuarioRepository usuarioRepository,
            IProductoRepository productoRepository,
            IExistenciaRepository existenciaRepository) {
        this.loteRepository = loteRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.existenciaRepository = existenciaRepository;
    }

    public LoteResponse execute(String loteId, ActualizarLoteRequest actualizarLoteRequest) {
        LoteId id = new LoteId(loteId);

        Lote lote = loteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El lote con id '" + loteId + "' no existe"));

        ProveedorId proveedorId = null;
        if (actualizarLoteRequest.getIdProveedor() != null) {
            proveedorId = new ProveedorId(actualizarLoteRequest.getIdProveedor());
            proveedorRepository.obtenerPorId(proveedorId)
                    .orElseThrow(() -> DomainException.throwException(404, "El proveedor con id '" + actualizarLoteRequest.getIdProveedor() + "' no existe"));
        }
        UsuarioId usuarioId = null;
        if (actualizarLoteRequest.getIdUsuario() != null) {
            usuarioId = new UsuarioId(actualizarLoteRequest.getIdUsuario());
            usuarioRepository.obtenerPorId(usuarioId)
                    .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + actualizarLoteRequest.getIdUsuario() + "' no existe"));
        }

        lote.actualizar(
                actualizarLoteRequest.getFechaCompra(),
                proveedorId,
                usuarioId);

        if (actualizarLoteRequest.getDetalles() != null) {
            conciliarDetalles(lote, actualizarLoteRequest.getDetalles());
        }

        lote.validarRegistrable();

        Lote loteActualizado = loteRepository.actualizar(id, lote);

        return LoteResponse.createFromAggregate(loteActualizado);
    }

    private void conciliarDetalles(Lote lote, List<ActualizarDetalleLoteRequest> detalles) {
        Set<String> idsRecibidos = detalles.stream()
                .map(ActualizarDetalleLoteRequest::getId)
                .filter(idDetalle -> idDetalle != null)
                .collect(Collectors.toSet());

        List<String> idsVivos = lote.getDetallesActivos().stream()
                .map(detalle -> detalle.getId().getValue())
                .collect(Collectors.toList());

        for (String idVivo : idsVivos) {
            if (!idsRecibidos.contains(idVivo)) {
                // Una linea con existencias en uso no se da de baja: su stock quedaria sin
                // origen (inventario.md INV-63).
                if (!existenciaRepository.obtenerDisponiblesPorDetallesLote(List.of(new DetalleLoteId(idVivo))).isEmpty()) {
                    throw DomainException.throwException(409,
                            "El detalle con id '" + idVivo + "' tiene existencias con contenido y no se puede dar de baja");
                }
                lote.removerDetalle(idVivo);
            }
        }

        for (ActualizarDetalleLoteRequest detalle : detalles) {
            if (detalle.getId() == null) {
                agregarDetalleNuevo(lote, detalle);
            } else {
                corregirDetalleExistente(lote, detalle);
            }
        }
    }

    private void agregarDetalleNuevo(Lote lote, ActualizarDetalleLoteRequest detalle) {
        if (detalle.getIdProducto() == null || detalle.getCantidadInicial() == null || detalle.getPrecioUnitario() == null) {
            throw DomainException.throwException(400,
                    "Un detalle de lote nuevo requiere 'idProducto', 'cantidadInicial' y 'precioUnitario'");
        }

        ProductoId productoId = new ProductoId(detalle.getIdProducto());
        productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + detalle.getIdProducto() + "' no existe"));

        lote.agregarDetalle(
                productoId,
                detalle.getCantidadInicial(),
                detalle.getPrecioUnitario(),
                detalle.getFraccionable(),
                detalle.getFechaVencimiento());
    }

    private void corregirDetalleExistente(Lote lote, ActualizarDetalleLoteRequest detalle) {
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
            lote.ajustarCantidadRestante(detalle.getId(), detalle.getCantidadRestante());
        }
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
}
