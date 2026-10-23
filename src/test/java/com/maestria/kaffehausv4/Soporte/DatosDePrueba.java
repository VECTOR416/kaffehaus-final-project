package com.maestria.kaffehausv4.Soporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.ValueObjects.RolId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

/**
 * Fabrica de objetos de dominio para las pruebas unitarias (patron Object Mother). Todo se
 * construye en memoria con las factorias 'restaurar' de cada agregado: ninguna prueba unitaria
 * toca la base de datos.
 */
public final class DatosDePrueba {
    private DatosDePrueba() {}

    public static final String ID_USUARIO = "22222222-2222-2222-2222-222222222222";
    public static final String ID_PERSONA = "22222222-2222-2222-2222-222222222223";
    public static final String ID_ROL = "22222222-2222-2222-2222-222222222224";
    public static final String ID_ROL_OPERADOR = "22222222-2222-2222-2222-222222222225";
    public static final String ID_PROVEEDOR = "11111111-1111-1111-1111-111111111111";
    public static final String ID_UNIDAD = "0a000000-0000-0000-0000-000000000001";
    public static final String ID_OTRA_UNIDAD = "0a000000-0000-0000-0000-000000000002";
    public static final String ID_TIPO = "0a000000-0000-0000-0000-000000000003";
    public static final String ID_CATEGORIA = "0a000000-0000-0000-0000-000000000004";
    public static final String ID_ESTADO_ABIERTA = "0e000000-0000-0000-0000-000000000001";
    public static final String ID_ESTADO_AGOTADA = "0e000000-0000-0000-0000-000000000002";
    public static final String ID_TIPO_PRODUCCION = "07000000-0000-0000-0000-000000000001";
    public static final String ID_TIPO_AJUSTE = "07000000-0000-0000-0000-000000000002";
    public static final String ID_TIPO_MERMA = "07000000-0000-0000-0000-000000000003";
    public static final String ID_TIPO_APERTURA = "07000000-0000-0000-0000-000000000004";
    public static final String ID_TIPO_VENTA = "07000000-0000-0000-0000-000000000005";
    public static final String ID_TIPO_REINTEGRO = "07000000-0000-0000-0000-000000000006";
    public static final String ID_TIPO_VENCIMIENTO = "07000000-0000-0000-0000-000000000007";

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 12, 9, 30);
    public static final LocalDate HOY = AHORA.toLocalDate();

    public static final EstadoExistencia ABIERTA = EstadoExistencia.restaurar(ID_ESTADO_ABIERTA, "Abierta", false);
    public static final EstadoExistencia AGOTADA = EstadoExistencia.restaurar(ID_ESTADO_AGOTADA, "Agotada", false);
    public static final TipoMovimiento PRODUCCION = TipoMovimiento.restaurar(ID_TIPO_PRODUCCION, TipoMovimiento.PRODUCCION, false);
    public static final TipoMovimiento AJUSTE = TipoMovimiento.restaurar(ID_TIPO_AJUSTE, TipoMovimiento.AJUSTE, false);
    public static final TipoMovimiento MERMA = TipoMovimiento.restaurar(ID_TIPO_MERMA, TipoMovimiento.MERMA, false);
    public static final TipoMovimiento APERTURA = TipoMovimiento.restaurar(ID_TIPO_APERTURA, TipoMovimiento.APERTURA, false);
    public static final TipoMovimiento VENTA = TipoMovimiento.restaurar(ID_TIPO_VENTA, TipoMovimiento.VENTA, false);
    public static final TipoMovimiento REINTEGRO = TipoMovimiento.restaurar(ID_TIPO_REINTEGRO, TipoMovimiento.REINTEGRO, false);
    public static final TipoMovimiento VENCIMIENTO = TipoMovimiento.restaurar(ID_TIPO_VENCIMIENTO, TipoMovimiento.VENCIMIENTO, false);
    public static final Unidad UNIDAD = Unidad.restaurar(ID_UNIDAD, "Gramo", "g", false, false);
    /** El rol del usuario de prueba (ID_ROL) y el otro rol sembrado, para los permisos por rol. */
    public static final Rol ROL_ADMINISTRADOR = Rol.restaurar(ID_ROL, Rol.ADMINISTRADOR, false);
    public static final Rol ROL_OPERADOR = Rol.restaurar(ID_ROL_OPERADOR, Rol.OPERADOR, false);

    // ------------------------------------------------------------------ personal

    public static Usuario usuario() {
        return Usuario.restaurar(ID_USUARIO, ID_PERSONA, "ana@kaffe.com", "hash", false, new RolId(ID_ROL));
    }

    /** Otro usuario, con el rol que se indique. */
    public static Usuario usuario(String id, String correo, String idRol) {
        return Usuario.restaurar(id, ID_PERSONA, correo, "hash", false, new RolId(idRol));
    }

    // ------------------------------------------------------------------ productos

    /** Producto con stock fisico propio (MTS, reventa, insumo). */
    public static Producto productoDirecto(String id, String nombre) {
        return producto(id, nombre, true, null, ID_UNIDAD);
    }

    /** Producto que se prepara al venderse: no tiene existencias, descuenta su receta. */
    public static Producto productoBajoDemanda(String id, String nombre) {
        return producto(id, nombre, false, null, ID_UNIDAD);
    }

    /** Forma de compra de un producto base (una bolsa de 1 kg de harina, por ejemplo). */
    /** Conversion viva de la unidad de las presentaciones (ID_OTRA_UNIDAD) a la del producto base. */
    public static Conversion conversionDePresentacionABase(String factor) {
        return Conversion.restaurar("0c000000-0000-0000-0000-000000000001", new BigDecimal(factor), true,
                new UnidadId(ID_OTRA_UNIDAD), new UnidadId(ID_UNIDAD), false);
    }

    public static Producto presentacion(String id, String nombre, String idProductoBase) {
        return producto(id, nombre, true, new ProductoId(idProductoBase), ID_OTRA_UNIDAD);
    }

    public static Producto producto(String id, String nombre, boolean esStockDirecto, ProductoId padre, String idUnidad) {
        return Producto.restaurar(id, nombre, new BigDecimal("0.00"), new BigDecimal("10.00"), new BigDecimal("0.00"),
                true, esStockDirecto, new UnidadId(idUnidad), new TipoId(ID_TIPO), new CategoriaId(ID_CATEGORIA), padre, false);
    }

    // ------------------------------------------------------------------ lotes y existencias

    public static DetalleLote detalle(String id, String idProducto, String inicial, String restante, String precio, LocalDate vence) {
        return DetalleLote.restaurar(id, new ProductoId(idProducto), new BigDecimal(inicial), new BigDecimal(restante),
                new BigDecimal(precio), true, vence, false);
    }

    public static Lote loteDeCompra(String id, DetalleLote... detalles) {
        return Lote.restaurar(id, AHORA.minusDays(5), new BigDecimal("0.00"), new ProveedorId(ID_PROVEEDOR),
                new UsuarioId(ID_USUARIO), false, null, false, List.of(detalles));
    }

    public static Lote loteDeProduccion(String id, String idProduccion, DetalleLote... detalles) {
        return Lote.restaurar(id, AHORA.minusDays(1), new BigDecimal("0.00"), null,
                new UsuarioId(ID_USUARIO), true, new ProduccionId(idProduccion), false, List.of(detalles));
    }

    /** Existencia abierta en la unidad por defecto de los productos de prueba. */
    public static Existencia existencia(String id, String idDetalle, String inicial, String actual, LocalDate vence) {
        return existencia(id, idDetalle, inicial, actual, vence, ID_UNIDAD);
    }

    public static Existencia existencia(String id, String idDetalle, String inicial, String actual, LocalDate vence, String idUnidad) {
        boolean agotada = new BigDecimal(actual).signum() == 0;
        return Existencia.restaurar(id, new BigDecimal(inicial), new BigDecimal(actual), agotada ? AHORA.minusDays(1) : null, vence,
                AHORA.minusDays(3), new DetalleLoteId(idDetalle),
                new EstadoExistenciaId(agotada ? ID_ESTADO_AGOTADA : ID_ESTADO_ABIERTA), new UnidadId(idUnidad),
                new UsuarioId(ID_USUARIO), false);
    }

    /** Simula lo que hace la persistencia al guardar una existencia nueva: devolverla con id. */
    public static Existencia guardada(Existencia existencia, String id) {
        return Existencia.restaurar(id, existencia.getContenidoInicial(), existencia.getContenidoActual(), existencia.getFechaAcabado(),
                existencia.getFechaVencimiento(), existencia.getFechaApertura(), existencia.getDetalleLoteId(),
                existencia.getEstadoExistenciaId(), existencia.getUnidadId(), existencia.getUsuarioId(), existencia.isBorrado());
    }

    // ------------------------------------------------------------------ recetas

    /** Receta que rinde 'rendimiento' unidades de 'idProductoObtenido' con los ingredientes dados. */
    public static Receta receta(String id, String idProductoObtenido, String rendimiento, IngredienteReceta... ingredientes) {
        return Receta.restaurar(id, new BigDecimal(rendimiento), "Receta de prueba", new ProductoId(idProductoObtenido), false,
                List.of(ingredientes), List.of(PasoReceta.restaurar("9a000000-0000-0000-0000-000000000001", 1, "Preparar", false)));
    }

    public static IngredienteReceta ingrediente(String id, String idProducto, String cantidad) {
        return IngredienteReceta.restaurar(id, new ProductoId(idProducto), new BigDecimal(cantidad), false);
    }

    // ------------------------------------------------------------------ movimientos

    /** Simula lo que hace la persistencia al guardar un movimiento: devolverlo con id. */
    public static Movimiento guardado(Movimiento movimiento, String id) {
        return Movimiento.restaurar(id, movimiento.getCantidad(), movimiento.getFechaMovimiento(), movimiento.getMotivo(),
                movimiento.getExistenciaId(), movimiento.getDetalleLoteId(), movimiento.getTipoMovimientoId(),
                movimiento.getUsuarioId(), movimiento.getProduccionId(), movimiento.getVentaId(), movimiento.isBorrado());
    }
}
