package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Raiz del agregado Venta.
 *
 * Venta contiene sus lineas (DetalleVenta) y es la unica puerta de entrada para crearlas,
 * eliminarlas y calcular importes. La coleccion se expone como vista de solo lectura, de
 * modo que ninguna capa externa puede mutarla sin pasar por agregarDetalle/removerDetalle.
 *
 * El total NO se recibe desde fuera: es un valor derivado que la raiz recalcula en cada
 * mutacion como la suma de los subtotales de sus lineas. Asi no puede existir una venta
 * cuyo encabezado contradiga a su detalle.
 */
public class Venta {

    private VentaId id;
    private LocalDateTime fechaVenta;
    private BigDecimal total;
    private UsuarioId usuarioId;
    private ClienteId clienteId;
    private EstadoVentaId estadoVentaId;
    private final List<DetalleVenta> detalles;

    private Venta(
        VentaId id,
        LocalDateTime fechaVenta,
        BigDecimal total,
        UsuarioId usuarioId,
        ClienteId clienteId,
        EstadoVentaId estadoVentaId,
        List<DetalleVenta> detalles
    ){
        this.id = id;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.usuarioId = usuarioId;
        this.clienteId = clienteId;
        this.estadoVentaId = estadoVentaId;
        this.detalles = detalles != null ? new ArrayList<>(detalles) : new ArrayList<>();
    }

    /**
     * Reconstruye el agregado completo desde persistencia. El total llega tal como se guardo;
     * no se recalcula aqui para no reescribir historia con precios o reglas de hoy.
     */
    public static Venta restaurar(
        String id,
        LocalDateTime fechaVenta,
        BigDecimal total,
        UsuarioId usuarioId,
        ClienteId clienteId,
        EstadoVentaId estadoVentaId,
        List<DetalleVenta> detalles
    ){
        return new Venta(new VentaId(id), fechaVenta, total, usuarioId, clienteId, estadoVentaId, detalles);
    }

    /**
     * Crea una venta sin lineas y con total en cero. Las lineas se incorporan despues con
     * agregarDetalle(...), que es lo que mantiene el total en sincronia.
     */
    public static Venta crear(
        LocalDateTime fechaVenta,
        UsuarioId usuarioId,
        ClienteId clienteId,
        EstadoVentaId estadoVentaId
    ){
        return new Venta(null, fechaVenta, aEscalaMonetaria(BigDecimal.ZERO), usuarioId, clienteId, estadoVentaId, null);
    }

    /**
     * Actualiza los datos de encabezado. El total no es parametro: solo cambia al mutar las
     * lineas del agregado.
     */
    public void actualizar(
        LocalDateTime fechaVenta,
        UsuarioId usuarioId,
        ClienteId clienteId,
        EstadoVentaId estadoVentaId
    ){
        this.fechaVenta = fechaVenta != null ? fechaVenta : this.fechaVenta;
        this.usuarioId = usuarioId != null ? usuarioId : this.usuarioId;
        this.clienteId = clienteId != null ? clienteId : this.clienteId;
        this.estadoVentaId = estadoVentaId != null ? estadoVentaId : this.estadoVentaId;
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

    /** Unica via para retirar una linea. Recalcula el total y falla si la linea no pertenece a la venta. */
    public void removerDetalle(String detalleVentaId) {
        boolean removido = this.detalles.removeIf(
                detalle -> detalle.getId() != null && detalle.getId().getValue().equals(detalleVentaId));

        if (!removido) {
            throw DomainException.throwException(404,
                    "El detalle con id '" + detalleVentaId + "' no pertenece a esta venta");
        }

        recalcularTotal();
    }

    /**
     * Invariante de agregado: una venta registrada tiene al menos una linea. La comprueba el
     * caso de uso antes de persistir, tanto en el alta como tras modificar los detalles.
     */
    public void validarRegistrable() {
        if (this.detalles.isEmpty()) {
            throw DomainException.throwException(400, "La venta debe tener al menos un detalle");
        }
    }

    private void recalcularTotal() {
        BigDecimal suma = this.detalles.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.total = aEscalaMonetaria(suma);
    }

    private static BigDecimal aEscalaMonetaria(BigDecimal valor) {
        return valor.setScale(DetalleVenta.ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    public VentaId getId() {return id;}
    public LocalDateTime getFechaVenta() {return fechaVenta;}
    public BigDecimal getTotal() {return total;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public ClienteId getClienteId() {return clienteId;}
    public EstadoVentaId getEstadoVentaId() {return estadoVentaId;}
    public List<DetalleVenta> getDetalles() {return Collections.unmodifiableList(detalles);}
}
