package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.CrearLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.LoteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Alta de un lote de compra con sus detalles en una sola operacion.
 *
 * El caso de uso resuelve las referencias a otros agregados (proveedor, usuario y productos),
 * arma el agregado Lote completo incorporando cada linea con agregarDetalle y lo entrega una
 * sola vez a ILoteRepository. La @Transactional de la clase cubre encabezado y lineas: si una
 * linea es invalida, la transaccion revierte entera y no queda un lote sin detalle.
 */
@Service
@Transactional
public class CrearLoteUseCase {
    private final ILoteRepository loteRepository;
    private final IProveedorRepository proveedorRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IProductoRepository productoRepository;

    public CrearLoteUseCase(
            ILoteRepository loteRepository,
            IProveedorRepository proveedorRepository,
            IUsuarioRepository usuarioRepository,
            IProductoRepository productoRepository) {
        this.loteRepository = loteRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
    }

    public LoteResponse execute(CrearLoteRequest crearLoteRequest) {
        ProveedorId proveedorId = new ProveedorId(crearLoteRequest.getIdProveedor());
        proveedorRepository.obtenerPorId(proveedorId)
                .orElseThrow(() -> DomainException.throwException(404, "El proveedor con id '" + crearLoteRequest.getIdProveedor() + "' no existe"));
        UsuarioId usuarioId = new UsuarioId(crearLoteRequest.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + crearLoteRequest.getIdUsuario() + "' no existe"));

        Lote lote = Lote.crear(
                crearLoteRequest.getFechaCompra(),
                proveedorId,
                usuarioId);

        agregarDetalles(lote, crearLoteRequest.getDetalles());
        lote.validarRegistrable();

        Lote loteCreado = loteRepository.crear(lote);

        return LoteResponse.createFromAggregate(loteCreado);
    }

    /**
     * Incorpora las lineas a la raiz. Cada producto se valida contra su propio agregado antes
     * de entregarselo a Lote, que a su vez comprueba las invariantes de cantidad y precio.
     */
    private void agregarDetalles(Lote lote, List<DetalleLoteRequest> detalles) {
        for (DetalleLoteRequest detalle : detalles) {
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
    }
}
