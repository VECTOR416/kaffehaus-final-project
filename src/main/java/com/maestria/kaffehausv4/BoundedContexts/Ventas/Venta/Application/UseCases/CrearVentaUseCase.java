package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarSalidaDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.DetalleVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Alta de una venta con sus detalles en una sola operacion.
 *
 * El caso de uso resuelve las referencias a otros agregados (usuario, cliente, estado y
 * productos), arma el agregado Venta completo incorporando cada linea con agregarDetalle y
 * lo entrega una sola vez a IVentaRepository. La @Transactional de la clase cubre encabezado
 * y lineas: si una linea es invalida, la excepcion se propaga antes o durante el guardado y
 * la transaccion revierte entera, de modo que nunca queda una venta sin detalle.
 *
 * Tras guardar la venta descuenta el inventario de cada linea en la misma transaccion: un
 * producto de stock directo (MTS) sale de sus existencias; uno bajo demanda (MTO) descuenta
 * los insumos de su receta (backflush). Si falta stock, la venta tampoco se registra. Los
 * movimientos no guardan la linea de venta: se reconocen por su tipo y por la venta que
 * citan en el motivo.
 */
@Service
@Transactional
public class CrearVentaUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String TIPO_VENTA = "Venta";
    private static final String TIPO_CONSUMO_VENTA = "Consumo venta";

    private final IVentaRepository ventaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IClienteRepository clienteRepository;
    private final IEstadoVentaRepository estadoVentaRepository;
    private final IProductoRepository productoRepository;
    private final RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase;

    public CrearVentaUseCase(
            IVentaRepository ventaRepository,
            IUsuarioRepository usuarioRepository,
            IClienteRepository clienteRepository,
            IEstadoVentaRepository estadoVentaRepository,
            IProductoRepository productoRepository,
            RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.estadoVentaRepository = estadoVentaRepository;
        this.productoRepository = productoRepository;
        this.registrarSalidaDeInventarioUseCase = registrarSalidaDeInventarioUseCase;
    }

    public VentaResponse execute(CrearVentaRequest crearVentaRequest) {
        UsuarioId usuarioId = new UsuarioId(crearVentaRequest.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + crearVentaRequest.getIdUsuario() + "' no existe"));
        ClienteId clienteId = null;
        if (crearVentaRequest.getIdCliente() != null) {
            clienteId = new ClienteId(crearVentaRequest.getIdCliente());
            clienteRepository.obtenerPorId(clienteId)
                    .orElseThrow(() -> DomainException.throwException(404, "El cliente con id '" + crearVentaRequest.getIdCliente() + "' no existe"));
        }
        EstadoVentaId estadoVentaId = new EstadoVentaId(crearVentaRequest.getIdEstadoVenta());
        EstadoVenta estadoVenta = estadoVentaRepository.obtenerPorId(estadoVentaId)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta con id '" + crearVentaRequest.getIdEstadoVenta() + "' no existe"));

        Venta venta = Venta.crear(
                crearVentaRequest.getFechaVenta(),
                usuarioId,
                clienteId,
                estadoVentaId);

        agregarDetalles(venta, crearVentaRequest.getDetalles());
        venta.validarRegistrable();

        Venta ventaCreada = ventaRepository.crear(venta);

        descontarInventario(ventaCreada);

        return VentaResponse.createFromAggregate(ventaCreada, estadoVenta);
    }

    /**
     * Las lineas se procesan en orden de id de producto para que dos ventas concurrentes tomen
     * los bloqueos en el mismo orden (inventario.md INV-53).
     */
    private void descontarInventario(Venta venta) {
        List<DetalleVenta> enOrden = venta.getDetallesActivos().stream()
                .sorted(Comparator.comparing(detalle -> detalle.getProductoId().getValue()))
                .toList();
        for (DetalleVenta detalle : enOrden) {
            registrarSalidaDeInventarioUseCase.execute(RegistrarSalidaDeInventarioRequest.crear(
                    detalle.getProductoId().getValue(),
                    detalle.getCantidad(),
                    TIPO_VENTA,
                    TIPO_CONSUMO_VENTA,
                    venta.getUsuarioId().getValue(),
                    null,
                    "Venta '" + venta.getId().getValue() + "'",
                    venta.getFechaVenta()));
        }
    }

    /**
     * Incorpora las lineas a la raiz. Cada producto se valida contra su propio agregado antes
     * de entregarselo a Venta, que a su vez comprueba las invariantes de importe.
     */
    private void agregarDetalles(Venta venta, List<DetalleVentaRequest> detalles) {
        for (DetalleVentaRequest detalle : detalles) {
            ProductoId productoId = new ProductoId(detalle.getIdProducto());
            productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + detalle.getIdProducto() + "' no existe"));

            venta.agregarDetalle(
                    productoId,
                    detalle.getPrecioUnitario(),
                    detalle.getCantidad(),
                    detalle.getDescuentoAplicado());
        }
    }
}
