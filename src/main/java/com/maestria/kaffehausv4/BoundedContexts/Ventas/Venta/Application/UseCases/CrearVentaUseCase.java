package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarSalidaDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.CrearVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.DetalleVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.PagoVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.VentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories.IVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Alta de una venta con sus detalles y el descuento de su inventario, en una sola transaccion
 * (ventas.md VEN-21):
 *
 * 1. Si trae una clave de idempotencia ya usada, devuelve esa venta sin crear otra (VEN-66),
 *    aunque su caja ya se haya cerrado.
 * 2. Exige una sesion de caja abierta y la toma con bloqueo compartido (FOR SHARE), antes que
 *    cualquier bloqueo de inventario: un cierre de caja espera a que la venta termine (CAJ-01).
 * 3. Valida referencias y cada linea: producto vivo, vendible y base, no presentacion (VEN-10,
 *    VEN-11). Sin cliente, la venta es del cliente 'Mostrador' (CAJ-02).
 * 4. Pone el precio de lista y el descuento vigente mas favorable de cada linea (VEN-13,
 *    VEN-14); la raiz calcula subtotales y total.
 * 5. Incorpora los pagos y exige que sumen el total; en efectivo calcula el cambio (CAJ-10..73).
 * 6. Guarda la venta 'Completada' con sus lineas y pagos (VEN-01).
 * 7. Descuenta el inventario de cada linea, en orden de id de producto (inventario.md INV-53):
 *    MTS de sus existencias, MTO por backflush. Los movimientos citan la venta en su motivo.
 * 8. Guarda el costo de cada linea que devuelve el inventario (VEN-23).
 *
 * Si falta stock responde 409 y no queda ni la venta, ni sus pagos, ni un solo movimiento
 * (VEN-22).
 */
@Service
@Transactional
public class CrearVentaUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_COMPLETADA = "Completada";

    private final IVentaRepository ventaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IClienteRepository clienteRepository;
    private final IEstadoVentaRepository estadoVentaRepository;
    private final IProductoRepository productoRepository;
    private final IDescuentoRepository descuentoRepository;
    private final ITipoDescuentoRepository tipoDescuentoRepository;
    private final ISesionCajaRepository sesionCajaRepository;
    private final ITipoPagoRepository tipoPagoRepository;
    private final RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase;

    public CrearVentaUseCase(
            IVentaRepository ventaRepository,
            IUsuarioRepository usuarioRepository,
            IClienteRepository clienteRepository,
            IEstadoVentaRepository estadoVentaRepository,
            IProductoRepository productoRepository,
            IDescuentoRepository descuentoRepository,
            ITipoDescuentoRepository tipoDescuentoRepository,
            ISesionCajaRepository sesionCajaRepository,
            ITipoPagoRepository tipoPagoRepository,
            RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.estadoVentaRepository = estadoVentaRepository;
        this.productoRepository = productoRepository;
        this.descuentoRepository = descuentoRepository;
        this.tipoDescuentoRepository = tipoDescuentoRepository;
        this.sesionCajaRepository = sesionCajaRepository;
        this.tipoPagoRepository = tipoPagoRepository;
        this.registrarSalidaDeInventarioUseCase = registrarSalidaDeInventarioUseCase;
    }

    public VentaResponse execute(CrearVentaRequest crearVentaRequest) {
        if (crearVentaRequest.getClaveIdempotencia() != null) {
            Optional<Venta> repetida = ventaRepository.obtenerPorClaveIdempotencia(crearVentaRequest.getClaveIdempotencia());
            if (repetida.isPresent()) {
                return toResponse(repetida.get());
            }
        }

        UsuarioId usuarioId = new UsuarioId(crearVentaRequest.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + crearVentaRequest.getIdUsuario() + "' no existe"));
        SesionCaja sesionCaja = sesionCajaRepository.obtenerAbiertaParaVenta()
                .orElseThrow(() -> DomainException.throwException(409,
                        "No hay una caja abierta; abre una sesion de caja (POST api/v1/sesion-caja/apertura) antes de vender"));
        ClienteId clienteId = resolverCliente(crearVentaRequest.getIdCliente());
        EstadoVenta completada = estadoVentaRepository.obtenerPorNombre(ESTADO_COMPLETADA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta '" + ESTADO_COMPLETADA + "' no esta registrado"));

        Venta venta = Venta.crear(
                crearVentaRequest.getFechaVenta(),
                usuarioId,
                clienteId,
                sesionCaja.getId(),
                completada.getId(),
                crearVentaRequest.getClaveIdempotencia());

        agregarDetalles(venta, crearVentaRequest.getDetalles());
        venta.validarRegistrable();
        agregarPagos(venta, crearVentaRequest.getPagos());
        venta.validarPagos();

        Venta ventaCreada = ventaRepository.crear(venta);

        descontarInventario(ventaCreada);

        return VentaResponse.createFromAggregate(ventaRepository.actualizar(ventaCreada.getId(), ventaCreada), completada);
    }

    /** El cliente indicado o, si no se indico ninguno, el cliente 'Mostrador' (CAJ-02). */
    private ClienteId resolverCliente(String idCliente) {
        if (idCliente == null) {
            ClienteId mostrador = new ClienteId(Cliente.ID_MOSTRADOR);
            clienteRepository.obtenerPorId(mostrador)
                    .orElseThrow(() -> DomainException.throwException(404,
                            "El cliente 'Mostrador' no esta registrado; inicializa el sistema con POST api/v1/seed"));
            return mostrador;
        }
        ClienteId clienteId = new ClienteId(idCliente);
        clienteRepository.obtenerPorId(clienteId)
                .orElseThrow(() -> DomainException.throwException(404, "El cliente con id '" + idCliente + "' no existe"));
        return clienteId;
    }

    /** Cada pago con su tipo resuelto: solo el efectivo admite cambio (CAJ-12, CAJ-13). */
    private void agregarPagos(Venta venta, List<PagoVentaRequest> pagos) {
        for (PagoVentaRequest pago : pagos) {
            TipoPago tipoPago = tipoPagoRepository.obtenerPorId(new TipoPagoId(pago.getIdTipoPago()))
                    .orElseThrow(() -> DomainException.throwException(404, "El tipo de pago con id '" + pago.getIdTipoPago() + "' no existe"));
            venta.agregarPago(tipoPago.getId(), tipoPago.esEfectivo(), pago.getMonto(), pago.getMontoRecibido(), pago.getReferencia());
        }
    }

    /** Cada linea con su precio de lista y su mejor descuento vigente (VEN-10..VEN-14). */
    private void agregarDetalles(Venta venta, List<DetalleVentaRequest> detalles) {
        for (DetalleVentaRequest detalle : detalles) {
            ProductoId productoId = new ProductoId(detalle.getIdProducto());
            Producto producto = productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + detalle.getIdProducto() + "' no existe"));
            if (!producto.isVendible()) {
                throw DomainException.throwException(400, "El producto '" + producto.getNombre() + "' no es vendible");
            }
            if (producto.esPresentacion()) {
                throw DomainException.throwException(400,
                        "El producto '" + producto.getNombre() + "' es una presentacion; se vende su producto base");
            }

            BigDecimal precio = producto.getPrecio();
            venta.agregarDetalle(
                    productoId,
                    precio,
                    detalle.getCantidad(),
                    mejorDescuento(productoId, precio, detalle.getCantidad(), venta.getFechaVenta()));
        }
    }

    /** Uno solo, el de mayor importe para el cliente; no se acumulan (VEN-14). */
    private BigDecimal mejorDescuento(ProductoId productoId, BigDecimal precio, BigDecimal cantidad, LocalDateTime fecha) {
        BigDecimal mejor = BigDecimal.ZERO;
        for (Descuento descuento : descuentoRepository.obtenerVigentes(productoId, fecha)) {
            TipoDescuento tipo = tipoDescuentoRepository.obtenerPorId(descuento.getTipoDescuentoId())
                    .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento asociado al descuento no existe"));
            mejor = mejor.max(descuento.importeSobre(precio, cantidad, tipo));
        }
        return mejor;
    }

    /**
     * Las lineas se procesan en orden de id de producto para que dos ventas concurrentes tomen
     * los bloqueos en el mismo orden (inventario.md INV-53). El costo de cada una queda en la
     * linea (VEN-23).
     */
    private void descontarInventario(Venta venta) {
        List<DetalleVenta> enOrden = venta.getDetallesActivos().stream()
                .sorted(Comparator.comparing(detalle -> detalle.getProductoId().getValue()))
                .toList();
        for (DetalleVenta detalle : enOrden) {
            BigDecimal costo = registrarSalidaDeInventarioUseCase.execute(RegistrarSalidaDeInventarioRequest.crear(
                    detalle.getProductoId().getValue(),
                    detalle.getCantidad(),
                    TipoMovimiento.AJUSTE,
                    TipoMovimiento.RECETA,
                    venta.getUsuarioId().getValue(),
                    null,
                    venta.motivoDeMovimientos(),
                    venta.getFechaVenta())).getCostoTotal();
            venta.registrarCosto(detalle.getId().getValue(), costo);
        }
    }

    private VentaResponse toResponse(Venta venta) {
        EstadoVenta estadoVenta = estadoVentaRepository.obtenerPorId(venta.getEstadoVentaId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta asociado a la venta no existe"));
        return VentaResponse.createFromAggregate(venta, estadoVenta);
    }
}
