package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Raiz del agregado Lote.
 *
 * Lote contiene sus lineas (DetalleLote) y es la unica puerta para crearlas, corregirlas,
 * ajustar su stock y darlas de baja. La coleccion se expone como vista de solo lectura, de
 * modo que ninguna capa externa puede mutarla sin pasar por los metodos de la raiz.
 *
 * El total NO se recibe desde fuera: es el costo de la compra, la suma de
 * cantidadInicial * precioUnitario de las lineas vivas, y la raiz lo recalcula en cada
 * mutacion. Asi el encabezado nunca contradice al detalle.
 *
 * Las lineas dadas de baja siguen en la coleccion marcadas como borradas, porque el borrado
 * es logico y la baja debe llegar a la base de datos como un UPDATE.
 */
public class Lote {

    private LoteId id;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private ProveedorId proveedorId;
    private UsuarioId usuarioId;
    private boolean borrado;
    private final List<DetalleLote> detalles;

    private Lote(
        LoteId id,
        LocalDateTime fechaCompra,
        BigDecimal total,
        ProveedorId proveedorId,
        UsuarioId usuarioId,
        boolean borrado,
        List<DetalleLote> detalles
    ){
        this.id = id;
        this.fechaCompra = fechaCompra;
        this.total = total;
        this.proveedorId = proveedorId;
        this.usuarioId = usuarioId;
        this.borrado = borrado;
        this.detalles = detalles != null ? new ArrayList<>(detalles) : new ArrayList<>();
    }

    /**
     * Reconstruye el agregado completo desde persistencia. El total llega tal como se guardo;
     * no se recalcula aqui para no reescribir el costo historico de la compra.
     */
    public static Lote restaurar(
        String id,
        LocalDateTime fechaCompra,
        BigDecimal total,
        ProveedorId proveedorId,
        UsuarioId usuarioId,
        boolean borrado,
        List<DetalleLote> detalles
    ){
        return new Lote(new LoteId(id), fechaCompra, total, proveedorId, usuarioId, borrado, detalles);
    }

    /**
     * Crea un lote sin lineas y con total en cero. Las lineas se incorporan despues con
     * agregarDetalle(...), que es lo que mantiene el total en sincronia.
     */
    public static Lote crear(
        LocalDateTime fechaCompra,
        ProveedorId proveedorId,
        UsuarioId usuarioId
    ){
        return new Lote(null, fechaCompra, aEscala(BigDecimal.ZERO), proveedorId, usuarioId, false, null);
    }

    /**
     * Actualiza los datos de encabezado. El total no es parametro: solo cambia al mutar las
     * lineas del agregado.
     */
    public void actualizar(
        LocalDateTime fechaCompra,
        ProveedorId proveedorId,
        UsuarioId usuarioId
    ){
        this.fechaCompra = fechaCompra != null ? fechaCompra : this.fechaCompra;
        this.proveedorId = proveedorId != null ? proveedorId : this.proveedorId;
        this.usuarioId = usuarioId != null ? usuarioId : this.usuarioId;
    }

    /** Unica via para incorporar una linea al lote. */
    public void agregarDetalle(
        ProductoId productoId,
        BigDecimal cantidadInicial,
        BigDecimal precioUnitario,
        Boolean fraccionable,
        LocalDate fechaVencimiento
    ){
        this.detalles.add(DetalleLote.crear(productoId, cantidadInicial, precioUnitario, fraccionable, fechaVencimiento));
        recalcularTotal();
    }

    /** Corrige los datos de compra de una linea viva y reajusta el costo del lote. */
    public void actualizarDetalle(
        String detalleLoteId,
        BigDecimal cantidadInicial,
        BigDecimal precioUnitario,
        Boolean fraccionable,
        LocalDate fechaVencimiento
    ){
        detalleVivo(detalleLoteId).actualizarDatosDeCompra(cantidadInicial, precioUnitario, fraccionable, fechaVencimiento);
        recalcularTotal();
    }

    /**
     * Ajusta el stock restante de una linea (consumo, merma o correccion de inventario). No
     * toca el total: el costo de compra no cambia porque se haya consumido mercaderia.
     */
    public void ajustarCantidadRestante(String detalleLoteId, BigDecimal nuevaCantidad) {
        detalleVivo(detalleLoteId).ajustarCantidadRestante(nuevaCantidad);
    }

    /** Da de baja logica una linea y descuenta su costo del total. */
    public void removerDetalle(String detalleLoteId) {
        detalleVivo(detalleLoteId).eliminar();
        recalcularTotal();
    }

    /** Baja logica del agregado entero: el encabezado y todas sus lineas. */
    public void eliminar(){
        this.borrado = true;
        this.detalles.forEach(DetalleLote::eliminar);
        recalcularTotal();
    }

    /**
     * Invariante de agregado: un lote registrado tiene al menos una linea viva. La comprueba
     * el caso de uso antes de persistir, en el alta y tras modificar los detalles.
     */
    public void validarRegistrable() {
        if (getDetallesActivos().isEmpty()) {
            throw DomainException.throwException(400, "El lote debe tener al menos un detalle");
        }
    }

    private DetalleLote detalleVivo(String detalleLoteId) {
        return this.detalles.stream()
                .filter(detalle -> !detalle.isBorrado())
                .filter(detalle -> detalle.getId() != null && detalle.getId().getValue().equals(detalleLoteId))
                .findFirst()
                .orElseThrow(() -> DomainException.throwException(404,
                        "El detalle con id '" + detalleLoteId + "' no pertenece a este lote"));
    }

    private void recalcularTotal() {
        BigDecimal suma = getDetallesActivos().stream()
                .map(DetalleLote::costoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.total = aEscala(suma);
    }

    private static BigDecimal aEscala(BigDecimal valor) {
        return valor.setScale(DetalleLote.ESCALA, RoundingMode.HALF_UP);
    }

    public LoteId getId() {return id;}
    public LocalDateTime getFechaCompra() {return fechaCompra;}
    public BigDecimal getTotal() {return total;}
    public ProveedorId getProveedorId() {return proveedorId;}
    public UsuarioId getUsuarioId() {return usuarioId;}
    public boolean isBorrado() {return borrado;}

    /** Todas las lineas, incluidas las dadas de baja: es lo que la persistencia debe escribir. */
    public List<DetalleLote> getDetalles() {return Collections.unmodifiableList(detalles);}

    /** Solo las lineas vivas: es lo que ve el exterior y lo que suma el total. */
    public List<DetalleLote> getDetallesActivos() {
        return detalles.stream()
                .filter(detalle -> !detalle.isBorrado())
                .collect(Collectors.toList());
    }
}
