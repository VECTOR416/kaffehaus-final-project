package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.ValueObjects.SesionCajaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Raiz del agregado Venta (ventas.md).
 *
 * Venta contiene sus lineas (DetalleVenta) y sus pagos (PagoVenta), y es la unica puerta de
 * entrada para crear unas y otros. Ambas colecciones se exponen como vistas de solo lectura.
 *
 * El total NO se recibe desde fuera: es la suma de los subtotales de sus lineas vivas. Los
 * pagos deben sumar exactamente ese total (CAJ-11).
 *
 * Toda venta pertenece a la sesion de caja abierta en la que se cobro (CAJ-01) y a un cliente,
 * que es el cliente 'Mostrador' cuando no se identifica a nadie (CAJ-02). 'numeroTicket' lo
 * asigna la base (seq_numero_ticket) y es nulo hasta guardarse.
 *
 * Una venta nace 'Completada' con su inventario ya descontado (VEN-01) y es inmutable en todo
 * lo que movio inventario (VEN-30): no se corrigen lineas, importes, fecha ni usuario, y no se
 * da de baja (VEN-33). Solo admite asociarle el cliente (VEN-31) y anularse (VEN-40). Como el
 * dominio no conoce los ids del catalogo 'estado_venta', quien la usa le pasa los de
 * 'Completada' y 'Anulada' resueltos por nombre.
 */
public class Venta {

    private VentaId id;
    private Long numeroTicket;
    private LocalDateTime fechaVenta;
    private BigDecimal total;
    private UsuarioId usuarioId;
    private ClienteId clienteId;
    private SesionCajaId sesionCajaId;
    private EstadoVentaId estadoVentaId;
    private String claveIdempotencia;
    private String motivoAnulacion;
    private UsuarioId usuarioAnulacionId;
    private boolean borrado;
    private final List<DetalleVenta> detalles;
    private final List<PagoVenta> pagos;

    private Venta(
        VentaId id,
        Long numeroTicket,
        LocalDateTime fechaVenta,
        BigDecimal total,
        UsuarioId usuarioId,
        ClienteId clienteId,
        SesionCajaId sesionCajaId,
        EstadoVentaId estadoVentaId,
        String claveIdempotencia,
        String motivoAnulacion,
        UsuarioId usuarioAnulacionId,
        boolean borrado,
        List<DetalleVenta> detalles,
        List<PagoVenta> pagos
    ){
        this.id = id;
        this.numeroTicket = numeroTicket;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.usuarioId = usuarioId;
        this.clienteId = clienteId;
        this.sesionCajaId = sesionCajaId;
        this.estadoVentaId = estadoVentaId;
        this.claveIdempotencia = claveIdempotencia;
        this.motivoAnulacion = motivoAnulacion;
        this.usuarioAnulacionId = usuarioAnulacionId;
        this.borrado = borrado;
        this.detalles = detalles != null ? new ArrayList<>(detalles) : new ArrayList<>();
        this.pagos = pagos != null ? new ArrayList<>(pagos) : new ArrayList<>();
    }

    /**
     * Reconstruye el agregado completo desde persistencia. El total llega tal como se guardo;
     * no se recalcula aqui para no reescribir historia con precios o reglas de hoy.
     */
    public static Venta restaurar(
        String id,
        Long numeroTicket,
        LocalDateTime fechaVenta,
        BigDecimal total,
        UsuarioId usuarioId,
        ClienteId clienteId,
        SesionCajaId sesionCajaId,
        EstadoVentaId estadoVentaId,
        String claveIdempotencia,
        String motivoAnulacion,
        UsuarioId usuarioAnulacionId,
        boolean borrado,
        List<DetalleVenta> detalles,
        List<PagoVenta> pagos
    ){
        return new Venta(new VentaId(id), numeroTicket, fechaVenta, total, usuarioId, clienteId, sesionCajaId, estadoVentaId,
                claveIdempotencia, motivoAnulacion, usuarioAnulacionId, borrado, detalles, pagos);
    }

    /**
     * Crea una venta 'Completada' sin lineas ni pagos y con total en cero (VEN-01, VEN-02). Las
     * lineas se incorporan despues con agregarDetalle(...), que mantiene el total en sincronia,
     * y los pagos con agregarPago(...). 'usuarioId' es el empleado que cobra; la sesion de caja
     * y el cliente son obligatorios (CAJ-01, CAJ-02).
     */
    public static Venta crear(
        LocalDateTime fechaVenta,
        UsuarioId usuarioId,
        ClienteId clienteId,
        SesionCajaId sesionCajaId,
        EstadoVentaId completada,
        String claveIdempotencia
    ){
        if (clienteId == null) {
            throw DomainException.throwException(400, "Una venta requiere un cliente");
        }
        if (sesionCajaId == null) {
            throw DomainException.throwException(409, "Una venta requiere una sesion de caja abierta");
        }
        return new Venta(null, null, fechaVenta, aEscalaMonetaria(BigDecimal.ZERO), usuarioId, clienteId, sesionCajaId, completada,
                claveIdempotencia, null, null, false, null, null);
    }

    /** Unica via para incorporar una linea. Valida la linea y recalcula el total. */
    public void agregarDetalle(
        ProductoId productoId,
        BigDecimal precioUnitario,
        BigDecimal cantidad,
        BigDecimal descuentoAplicado
    ){
        this.detalles.add(DetalleVenta.crear(productoId, precioUnitario, cantidad, descuentoAplicado));
        recalcularTotal();
    }

    /**
     * Unica via para incorporar un pago (CAJ-10). Como el dominio no conoce el catalogo
     * 'tipo_pago', quien la usa le indica si el tipo es efectivo: solo el efectivo da cambio.
     */
    public void agregarPago(TipoPagoId tipoPagoId, boolean esEfectivo, BigDecimal monto, BigDecimal montoRecibido, String referencia) {
        this.pagos.add(PagoVenta.crear(tipoPagoId, esEfectivo, monto, montoRecibido, referencia));
    }

    /**
     * Invariante de cobro: los pagos vivos suman exactamente el total (CAJ-11). Se comprueba
     * despues de incorporar lineas y pagos, antes de persistir.
     */
    public void validarPagos() {
        BigDecimal pagado = getTotalPagado();
        if (pagado.compareTo(this.total) != 0) {
            throw DomainException.throwException(400,
                    "La suma de los pagos (" + pagado + ") no coincide con el total de la venta (" + this.total + ")");
        }
    }

    /** Asocia el cliente a una venta ya cobrada (VEN-31). Una venta anulada no cambia (VEN-32). */
    public void asignarCliente(ClienteId clienteId, EstadoVentaId anulada) {
        exigirNoAnulada(anulada);
        this.clienteId = clienteId != null ? clienteId : this.clienteId;
    }

    /**
     * Anula la venta (VEN-40). Solo una venta 'Completada' se anula, y siempre con motivo. Las
     * lineas, importes y pagos se conservan: la anulacion no borra nada.
     */
    public void anular(EstadoVentaId completada, EstadoVentaId anulada, UsuarioId usuarioId, String motivo) {
        exigirNoAnulada(anulada);
        if (!this.estadoVentaId.getValue().equals(completada.getValue())) {
            throw DomainException.throwException(409, "Solo se puede anular una venta completada");
        }
        if (motivo == null || motivo.isBlank()) {
            throw DomainException.throwException(400, "La anulacion de una venta requiere un motivo");
        }
        this.estadoVentaId = anulada;
        this.motivoAnulacion = motivo;
        this.usuarioAnulacionId = usuarioId;
    }

    public boolean estaEn(EstadoVentaId estado) {
        return this.estadoVentaId.getValue().equals(estado.getValue());
    }

    /**
     * Una venta no se da de baja (VEN-33): esconderla dejaria sin explicacion el inventario que
     * ya descontaron sus movimientos. Se anula.
     */
    public void eliminar(){
        throw DomainException.throwException(409,
                "Una venta no se da de baja; anulala con POST api/v1/venta/{id}/anulacion");
    }

    /**
     * Invariante de agregado: una venta registrada tiene al menos una linea viva. La comprueba
     * el caso de uso antes de persistir.
     */
    public void validarRegistrable() {
        if (getDetallesActivos().isEmpty()) {
            throw DomainException.throwException(400, "La venta debe tener al menos un detalle");
        }
    }

    private void exigirNoAnulada(EstadoVentaId anulada) {
        if (estaEn(anulada)) {
            throw DomainException.throwException(409, "La venta con id '" + this.id.getValue() + "' esta anulada y no admite cambios");
        }
    }

    private void recalcularTotal() {
        BigDecimal suma = getDetallesActivos().stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.total = aEscalaMonetaria(suma);
    }

    private static BigDecimal aEscalaMonetaria(BigDecimal valor) {
        return valor.setScale(DetalleVenta.ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    public VentaId getId() {return id;}
    public Long getNumeroTicket() {return numeroTicket;}
    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public BigDecimal getTotal() {return total;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public ClienteId getClienteId() {return clienteId;}
    public SesionCajaId getSesionCajaId() {return sesionCajaId;}
    public EstadoVentaId getEstadoVentaId() {return estadoVentaId;}
    public String getClaveIdempotencia() {return claveIdempotencia;}
    public String getMotivoAnulacion() {return motivoAnulacion;}
    public UsuarioId getUsuarioAnulacionId() {return usuarioAnulacionId;}
    public boolean isBorrado() {return borrado;}

    /** Todas las lineas, incluidas las dadas de baja: es lo que la persistencia debe escribir. */
    public List<DetalleVenta> getDetalles() {return Collections.unmodifiableList(detalles);}

    /** Solo las lineas vivas: es lo que ve el exterior y lo que suma el total. */
    public List<DetalleVenta> getDetallesActivos() {
        return detalles.stream()
                .filter(detalle -> !detalle.isBorrado())
                .collect(Collectors.toList());
    }

    /** Todos los pagos, incluidos los dados de baja: es lo que la persistencia debe escribir. */
    public List<PagoVenta> getPagos() {return Collections.unmodifiableList(pagos);}

    public List<PagoVenta> getPagosActivos() {
        return pagos.stream()
                .filter(pago -> !pago.isBorrado())
                .collect(Collectors.toList());
    }

    public BigDecimal getTotalPagado() {
        return aEscalaMonetaria(getPagosActivos().stream()
                .map(PagoVenta::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Cambio total entregado al cliente: solo lo genera el efectivo. */
    public BigDecimal getCambio() {
        return aEscalaMonetaria(getPagosActivos().stream()
                .map(PagoVenta::getCambio)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
