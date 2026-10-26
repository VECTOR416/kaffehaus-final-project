package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.UseCases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarSalidaDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
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
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Alta de una venta con sus detalles y el descuento de su inventario, en una sola transaccion
 * (ventas.md VEN-21):
 *
 * 1. Si trae una clave de idempotencia ya usada, devuelve esa venta sin crear otra (logica.md
 *    VENTA-08), aunque su caja ya se haya cerrado.
 * 2. Exige una sesion de caja abierta y la toma con bloqueo compartido (FOR SHARE), antes que
 *    cualquier bloqueo de inventario: un cierre de caja espera a que la venta termine (VENTA-02).
 * 3. Fija la fecha: la del servidor, o la de contingencia que envia un Administrador dentro del
 *    turno de esa caja (VENTA-28).
 * 4. Valida referencias y cada linea: producto vivo y vendible, y que no sea un producto base
 *    con presentaciones (VENTA-21). Sin cliente, la venta es del cliente 'Mostrador' (VENTA-03).
 * 5. Pone el precio de lista y el descuento vigente a la fecha de la venta mas favorable de cada
 *    linea (VENTA-06); la raiz calcula subtotales y total.
 * 6. Incorpora los pagos y exige que sumen el total; en efectivo calcula el cambio (VENTA-14).
 * 7. Guarda la venta 'Completada' con sus lineas y pagos (VENTA-01).
 * 8. Entrega todas sus lineas a la salida de inventario, que arma un solo plan de consumo: las
 *    unidades de los productos por unidades y los insumos de los MTO, con un movimiento 'Venta'
 *    por cada existencia o linea tocada (VENTA-24). La venta no guarda costo (VENTA-20).
 *
 * Si algo no alcanza responde el 409 estructurado de la salida ('APERTURA_REQUERIDA' o
 * 'STOCK_INSUFICIENTE', VENTA-30) sin interpretarlo, y no queda ni la venta ni sus pagos: la
 * clave de idempotencia sigue libre para reintentar.
 */
@Service
@Transactional
public class CrearVentaUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (logica.md GENERAL-04).
    private static final String ESTADO_COMPLETADA = "Completada";
    private static final String ROL_ADMINISTRADOR = "Administrador";

    private final IVentaRepository ventaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IRolRepository rolRepository;
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
            IRolRepository rolRepository,
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
        this.rolRepository = rolRepository;
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
        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + crearVentaRequest.getIdUsuario() + "' no existe"));
        SesionCaja sesionCaja = sesionCajaRepository.obtenerAbiertaParaVenta()
                .orElseThrow(() -> DomainException.throwException(409,
                        "No hay una caja abierta; abre una sesion de caja (POST api/v1/sesion-caja/apertura) antes de vender"));
        LocalDateTime fechaVenta = Venta.fechaDeVenta(
                crearVentaRequest.getFechaVenta(),
                crearVentaRequest.getFechaVenta() != null && esAdministrador(usuario),
                sesionCaja.getFechaApertura(),
                LocalDateTime.now());
        ClienteId clienteId = resolverCliente(crearVentaRequest.getIdCliente());
        EstadoVenta completada = estadoVentaRepository.obtenerPorNombre(ESTADO_COMPLETADA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta '" + ESTADO_COMPLETADA + "' no esta registrado"));

        Venta venta = Venta.crear(
                fechaVenta,
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

        return VentaResponse.createFromAggregate(ventaCreada, completada);
    }

    /**
     * El permiso de fijar la fecha depende del cuerpo y no del endpoint, por eso no es un
     * @RequiereRol: se resuelve el rol por nombre, como al cerrar la caja (logica.md VENTA-28).
     */
    private boolean esAdministrador(Usuario usuario) {
        return rolRepository.obtenerPorId(usuario.getRolId())
                .map(rol -> ROL_ADMINISTRADOR.equalsIgnoreCase(rol.getNombre()))
                .orElse(false);
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

    /** Cada linea con su precio de lista y su mejor descuento vigente (VEN-10..VEN-14, logica.md VENTA-21). */
    private void agregarDetalles(Venta venta, List<DetalleVentaRequest> detalles) {
        for (DetalleVentaRequest detalle : detalles) {
            ProductoId productoId = new ProductoId(detalle.getIdProducto());
            Producto producto = productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + detalle.getIdProducto() + "' no existe"));
            if (!producto.isVendible()) {
                throw DomainException.throwException(400, "El producto '" + producto.getNombre() + "' no es vendible");
            }
            if (producto.isEsStockDirecto() && !productoRepository.obtenerPresentaciones(productoId).isEmpty()) {
                throw DomainException.throwException(400,
                        "El producto '" + producto.getNombre() + "' tiene presentaciones y se maneja por existencias; vende una de sus presentaciones");
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
     * Todas las lineas de una vez: la salida las agrega en un solo plan de consumo y bloquea en
     * orden de id de producto, para que dos ventas concurrentes tomen los bloqueos en el mismo
     * orden (logica.md GENERAL-14, GENERAL-15).
     */
    private void descontarInventario(Venta venta) {
        RegistrarSalidaDeInventarioRequest salida = RegistrarSalidaDeInventarioRequest.deVenta(
                venta.getUsuarioId().getValue(),
                venta.getId().getValue(),
                "Venta del ticket " + venta.getNumeroTicket(),
                venta.getFechaVenta());
        venta.getDetallesActivos().forEach(detalle ->
                salida.con(detalle.getProductoId().getValue(), detalle.getCantidad()));
        registrarSalidaDeInventarioUseCase.execute(salida);
    }

    private VentaResponse toResponse(Venta venta) {
        EstadoVenta estadoVenta = estadoVentaRepository.obtenerPorId(venta.getEstadoVentaId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de venta asociado a la venta no existe"));
        return VentaResponse.createFromAggregate(venta, estadoVenta);
    }
}
