package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
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
 *
 * Un lote tiene uno de dos origenes, y solo uno (lo exige tambien 'chk_lote_origen'):
 * - Compra: tiene proveedor y no tiene produccion. Nace con crear(...).
 * - Produccion interna (MTS): tiene produccion y no tiene proveedor. Nace con
 *   registrarProduccion(...), con una sola linea: el producto terminado con sus unidades
 *   disponibles y sin costo (logica.md PRODUCCION-12).
 */
public class Lote {

    /** Escala de calculo del costo por unidad de contenido; se redondea al final, no antes. */
    private static final int ESCALA_COSTO_CONTENIDO = 6;

    private LoteId id;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private ProveedorId proveedorId;
    private UsuarioId usuarioId;
    private boolean esProduccion;
    private ProduccionId produccionId;
    private boolean borrado;
    private final List<DetalleLote> detalles;

    private Lote(
        LoteId id,
        LocalDateTime fechaCompra,
        BigDecimal total,
        ProveedorId proveedorId,
        UsuarioId usuarioId,
        boolean esProduccion,
        ProduccionId produccionId,
        boolean borrado,
        List<DetalleLote> detalles
    ){
        this.id = id;
        this.fechaCompra = fechaCompra;
        this.total = total;
        this.proveedorId = proveedorId;
        this.usuarioId = usuarioId;
        this.esProduccion = esProduccion;
        this.produccionId = produccionId;
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
        boolean esProduccion,
        ProduccionId produccionId,
        boolean borrado,
        List<DetalleLote> detalles
    ){
        return new Lote(new LoteId(id), fechaCompra, total, proveedorId, usuarioId, esProduccion, produccionId, borrado, detalles);
    }

    /**
     * Crea un lote de compra sin lineas y con total en cero. Las lineas se incorporan despues
     * con agregarDetalle(...), que es lo que mantiene el total en sincronia.
     */
    public static Lote crear(
        LocalDateTime fechaCompra,
        ProveedorId proveedorId,
        UsuarioId usuarioId
    ){
        if (proveedorId == null || proveedorId.getValue() == null) {
            throw DomainException.throwException(400, "Un lote de compra requiere un proveedor");
        }
        return new Lote(null, fechaCompra, aEscala(BigDecimal.ZERO), proveedorId, usuarioId, false, null, false, null);
    }

    /**
     * Crea el lote que deja una produccion interna (MTS): una sola linea con el producto
     * terminado y su caducidad. No tiene proveedor ni costo: producir no calcula dinero
     * (logica.md GENERAL-11).
     *
     * La linea nace con todas sus unidades disponibles (logica.md LOTE-19, PRODUCCION-12): el
     * terminado se maneja por unidades, y la venta o la receta que lo usen las descuentan de aqui.
     */
    public static Lote registrarProduccion(
        LocalDateTime fechaProduccion,
        ProduccionId produccionId,
        UsuarioId usuarioId,
        ProductoId productoId,
        BigDecimal cantidadProducida,
        LocalDate fechaVencimiento
    ){
        if (produccionId == null || produccionId.getValue() == null) {
            throw DomainException.throwException(400, "Un lote de produccion requiere la produccion que lo origina");
        }

        Lote lote = new Lote(null, fechaProduccion, aEscala(BigDecimal.ZERO), null, usuarioId, true, produccionId, false, null);

        lote.detalles.add(DetalleLote.crear(productoId, cantidadProducida, BigDecimal.ZERO, false, fechaVencimiento));
        lote.recalcularTotal();

        return lote;
    }

    /**
     * Actualiza los datos de encabezado. El total no es parametro: solo cambia al mutar las
     * lineas del agregado. El origen del lote no cambia nunca: un lote de produccion no puede
     * recibir proveedor, porque dejaria de explicar de donde salio su stock.
     */
    public void actualizar(
        LocalDateTime fechaCompra,
        ProveedorId proveedorId,
        UsuarioId usuarioId
    ){
        if (this.esProduccion && proveedorId != null) {
            throw DomainException.throwException(400, "Un lote de produccion no tiene proveedor");
        }
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

    /**
     * Abre una unidad cerrada de una linea viva (inventario.md INV-10). No toca el total: el
     * costo de compra no cambia porque se abra un empaque.
     */
    public void abrirUnidad(String detalleLoteId) {
        if (this.borrado) {
            throw DomainException.throwException(409, "No se puede abrir una unidad de un lote dado de baja");
        }
        detalleVivo(detalleLoteId).abrirUnidad();
    }

    /**
     * Lineas de un producto que se pueden abrir, en el orden en que deben abrirse
     * (logica.md LOTE-08, LOTE-09): de lotes vivos, con unidades cerradas y no vencidas a la fecha.
     */
    public static List<DetalleLote> aperturablesEnOrden(List<Lote> lotes, ProductoId productoId, LocalDate fecha) {
        return lineasEnOrden(lotes, productoId).stream()
                .filter(DetalleLote::tieneUnidadesCerradas)
                .filter(detalle -> !detalle.estaVencido(fecha))
                .collect(Collectors.toList());
    }

    /**
     * Lineas de las que una venta o una receta descuentan un producto por unidades, en orden
     * (logica.md LOTE-17): de lotes vivos, con unidades disponibles (aunque sean una fraccion) y
     * no vencidas a la fecha.
     */
    public static List<DetalleLote> conUnidadesEnOrden(List<Lote> lotes, ProductoId productoId, LocalDate fecha) {
        return lineasEnOrden(lotes, productoId).stream()
                .filter(DetalleLote::tieneUnidadesDisponibles)
                .filter(detalle -> !detalle.estaVencido(fecha))
                .collect(Collectors.toList());
    }

    /**
     * Lineas vivas de un producto en lotes vivos, en el orden de logica.md LOTE-09: primero la que
     * vence antes (sin vencimiento, al final), despues la del lote mas antiguo y, como desempate
     * estable, la de menor id.
     */
    private static List<DetalleLote> lineasEnOrden(List<Lote> lotes, ProductoId productoId) {
        List<Lote> vivos = lotes.stream().filter(lote -> !lote.isBorrado()).collect(Collectors.toList());

        Comparator<DetalleLote> porVencimiento = Comparator.comparing(
                DetalleLote::getFechaVencimiento, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<DetalleLote> porCompra = Comparator.comparing(
                detalle -> dueno(vivos, detalle).getFechaCompra(), Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<DetalleLote> porId = Comparator.comparing(detalle -> detalle.getId().getValue());

        return vivos.stream()
                .flatMap(lote -> lote.getDetallesActivos().stream())
                .filter(detalle -> detalle.getProductoId().getValue().equals(productoId.getValue()))
                .sorted(porVencimiento.thenComparing(porCompra).thenComparing(porId))
                .collect(Collectors.toList());
    }

    /**
     * Descuenta unidades disponibles de una linea viva por una venta o una receta (logica.md
     * LOTE-17). No toca el total: el costo de compra no cambia porque se venda.
     */
    public void descontarUnidades(String detalleLoteId, BigDecimal cantidad) {
        if (this.borrado) {
            throw DomainException.throwException(409, "No se pueden descontar unidades de un lote dado de baja");
        }
        detalleVivo(detalleLoteId).descontar(cantidad);
    }

    /**
     * Devuelve a una linea viva las unidades que una venta anulada desconto de ella (logica.md
     * LOTE-24), aunque este vencida.
     */
    public void devolverUnidades(String detalleLoteId, BigDecimal cantidad) {
        if (this.borrado) {
            throw DomainException.throwException(409, "No se pueden devolver unidades a un lote dado de baja");
        }
        detalleVivo(detalleLoteId).devolver(cantidad);
    }

    /** Lote que contiene una linea, entre varios lotes. */
    public static Lote dueno(List<Lote> lotes, DetalleLote detalle) {
        return lotes.stream()
                .filter(lote -> lote.getDetalles().contains(detalle))
                .findFirst()
                .orElseThrow(() -> DomainException.throwException(404,
                        "El detalle con id '" + detalle.getId().getValue() + "' no pertenece a ninguno de los lotes"));
    }

    /** Linea viva por id, o 404 si no pertenece al lote. */
    public DetalleLote obtenerDetalle(String detalleLoteId) {
        return detalleVivo(detalleLoteId);
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

    /**
     * Costo de una unidad del contenido de una existencia abierta desde una linea de este lote.
     *
     * En una compra el precio es por unidad comprada (una bolsa) y la existencia guarda el
     * contenido de esa unidad (1000 g): el costo por gramo es precio / contenido inicial. En
     * una produccion la existencia se abre con la cantidad producida en la misma unidad de la
     * linea, asi que el costo por unidad de contenido es el propio costo unitario.
     */
    public BigDecimal costoPorUnidadDeContenido(DetalleLoteId detalleLoteId, BigDecimal contenidoInicial) {
        DetalleLote detalle = detalleVivo(detalleLoteId.getValue());

        if (this.esProduccion) {
            return detalle.getPrecioUnitario();
        }
        if (contenidoInicial == null || contenidoInicial.signum() <= 0) {
            throw DomainException.throwException(400, "El contenido inicial de la existencia debe ser mayor a cero");
        }
        return detalle.getPrecioUnitario().divide(contenidoInicial, ESCALA_COSTO_CONTENIDO, RoundingMode.HALF_UP);
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
    public boolean isEsProduccion() {return esProduccion;}
    public ProduccionId getProduccionId() {return produccionId;}
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
