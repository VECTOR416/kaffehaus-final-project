package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas de dominio puro del stock proyectado y la jerarquia de un producto. */
class ProductoTest {

    private static final String ID_HARINA = "b0000000-0000-0000-0000-000000000001";

    private Producto producto(boolean esStockDirecto, ProductoId padre) {
        return Producto.restaurar(
                "b0000000-0000-0000-0000-000000000002",
                "producto",
                new BigDecimal("0.00"),
                new BigDecimal("10.00"),
                new BigDecimal("0.00"),
                true,
                esStockDirecto,
                new UnidadId("b0000000-0000-0000-0000-000000000003"),
                new TipoId("b0000000-0000-0000-0000-000000000004"),
                new CategoriaId("b0000000-0000-0000-0000-000000000005"),
                padre,
                false);
    }

    @Test
    void unProductoNuevoNaceConStockCero() {
        Producto nuevo = Producto.crear("pan", new BigDecimal("1.00"), BigDecimal.ZERO, true, true,
                new UnidadId("b0000000-0000-0000-0000-000000000003"),
                new TipoId("b0000000-0000-0000-0000-000000000004"),
                new CategoriaId("b0000000-0000-0000-0000-000000000005"),
                null);

        assertEquals(0, nuevo.getStock().signum());
    }

    @Test
    void proyectarStockFijaElContenidoAbierto() {
        Producto producto = producto(true, null);

        producto.proyectarStock(new BigDecimal("1234.5"));

        assertEquals(new BigDecimal("1234.50"), producto.getStock());
    }

    @Test
    void elStockNoPuedeSerNegativo() {
        DomainException error = assertThrows(DomainException.class, () -> producto(true, null).proyectarStock(new BigDecimal("-1")));

        assertEquals(409, error.getCodigo());
    }

    @Test
    void unProductoBajoDemandaNoTieneStockPropio() {
        DomainException error = assertThrows(DomainException.class, () -> producto(false, null).proyectarStock(BigDecimal.ONE));

        assertEquals(409, error.getCodigo());
    }

    @Test
    void elProductoBaseDeUnaPresentacionEsSuPadre() {
        Producto presentacion = producto(true, new ProductoId(ID_HARINA));
        Producto base = producto(true, null);

        assertTrue(presentacion.esPresentacion());
        assertEquals(ID_HARINA, presentacion.productoBaseId().getValue());
        assertFalse(base.esPresentacion());
        assertEquals(base.getId().getValue(), base.productoBaseId().getValue());
    }
}
