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
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
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
    private final RecalcularStockUseCase recalcularStockUseCase;

    public CrearLoteUseCase(
            ILoteRepository loteRepository,
            IProveedorRepository proveedorRepository,
            IUsuarioRepository usuarioRepository,
            IProductoRepository productoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.loteRepository = loteRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
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
        // Las unidades compradas suben el stock de cada producto (logica.md PRODUCTO-16).
        productosDe(loteCreado).forEach(recalcularStockUseCase::execute);

        return LoteResponse.createFromAggregate(loteCreado);
    }

    /**
     * Incorpora las lineas a la raiz. Cada producto se valida contra su propio agregado antes
     * de entregarselo a Lote, que a su vez comprueba las invariantes de cantidad y precio.
     */
    private void agregarDetalles(Lote lote, List<DetalleLoteRequest> detalles) {
        for (DetalleLoteRequest detalle : detalles) {
            ProductoId productoId = new ProductoId(detalle.getIdProducto());
            Producto producto = productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + detalle.getIdProducto() + "' no existe"));
            exigirQueSeCompre(producto, productoRepository);

            lote.agregarDetalle(
                    productoId,
                    detalle.getCantidadInicial(),
                    detalle.getPrecioUnitario(),
                    detalle.getFraccionable(),
                    detalle.getFechaVencimiento());
        }
    }

    /**
     * Un lote compra presentaciones o productos por unidades; un producto base con presentaciones
     * se compra a traves de una de ellas (logica.md LOTE-13), y un MTO no se compra: no tiene
     * inventario propio (PRODUCTO-18).
     */
    static void exigirQueSeCompre(Producto producto, IProductoRepository productoRepository) {
        if (!producto.isEsStockDirecto()) {
            throw DomainException.throwException(400,
                    "El producto '" + producto.getNombre() + "' se prepara bajo demanda (MTO) y no se compra: se compran sus insumos");
        }
        if (!productoRepository.obtenerPresentaciones(producto.getId()).isEmpty()) {
            throw DomainException.throwException(400,
                    "El producto '" + producto.getNombre() + "' tiene presentaciones: compra una de ellas o registra una presentacion para comprarlo suelto");
        }
    }

    /** Productos de las lineas del lote, en orden de id para bloquearlos siempre igual (INV-53). */
    static List<String> productosDe(Lote lote) {
        return lote.getDetalles().stream()
                .map(detalle -> detalle.getProductoId().getValue())
                .distinct()
                .sorted()
                .toList();
    }
}
