package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.HOY;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.detalle;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.loteDeCompra;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.productoBajoDemanda;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.productoDirecto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarReintegroDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias del reintegro de una venta anulada (logica.md VENTA-22, LOTE-18): las
 * unidades vuelven a las lineas de lote del producto con espacio, primero la que vence antes.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarReintegroDeInventarioUseCaseTest {

    private static final String ID_PAN = "c0000000-0000-0000-0000-000000000001";
    private static final String ID_GASEOSA = "c0000000-0000-0000-0000-000000000002";
    private static final String ID_CAFE_CON_LECHE = "c0000000-0000-0000-0000-000000000003";
    private static final String ID_LOTE_1 = "c1000000-0000-0000-0000-000000000001";
    private static final String ID_LOTE_2 = "c1000000-0000-0000-0000-000000000002";
    private static final String ID_DETALLE_1 = "c2000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE_2 = "c2000000-0000-0000-0000-000000000002";

    @Mock private ILoteRepository loteRepository;
    @Mock private IProductoRepository productoRepository;
    @Mock private RecalcularStockUseCase recalcularStockUseCase;

    private RegistrarReintegroDeInventarioUseCase useCase;

    @BeforeEach
    void prepararPersistencia() {
        useCase = new RegistrarReintegroDeInventarioUseCase(loteRepository, productoRepository, recalcularStockUseCase);
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_PAN.equals(id.getValue()))))
                .thenReturn(Optional.of(productoDirecto(ID_PAN, "Pan")));
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_GASEOSA.equals(id.getValue()))))
                .thenReturn(Optional.of(productoDirecto(ID_GASEOSA, "Gaseosa")));
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && ID_CAFE_CON_LECHE.equals(id.getValue()))))
                .thenReturn(Optional.of(productoBajoDemanda(ID_CAFE_CON_LECHE, "Cafe con leche")));
        lenient().when(loteRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    /** Lotes del producto; cada linea se relee bloqueada desde su propio lote. */
    private void hayLotes(Lote... lotes) {
        lenient().when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(lotes));
        lenient().when(loteRepository.obtenerPorDetalleIdParaActualizar(any())).thenAnswer(llamada -> {
            String idDetalle = llamada.getArgument(0, DetalleLoteId.class).getValue();
            for (Lote lote : lotes) {
                if (lote.getDetallesActivos().stream().anyMatch(detalle -> detalle.getId().getValue().equals(idDetalle))) {
                    return Optional.of(lote);
                }
            }
            return Optional.empty();
        });
    }

    @Test
    void lasUnidadesVuelvenALaLineaConEspacioQueVenceAntes() {
        // Arrange: la linea que vence antes tiene 2 de espacio; la otra, que no vence, 3.
        Lote venceAntes = loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_1, ID_PAN, "5", "3", "1.00", HOY.plusDays(2)));
        Lote sinVencimiento = loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_2, ID_PAN, "5", "2", "1.00", null));
        hayLotes(sinVencimiento, venceAntes);

        // Act
        useCase.execute(RegistrarReintegroDeInventarioRequest.crear().con(ID_PAN, new BigDecimal("3")));

        // Assert
        assertEquals(0, new BigDecimal("5").compareTo(venceAntes.obtenerDetalle(ID_DETALLE_1).getCantidadRestante()));
        assertEquals(0, new BigDecimal("3").compareTo(sinVencimiento.obtenerDetalle(ID_DETALLE_2).getCantidadRestante()));
        verify(recalcularStockUseCase).execute(ID_PAN);
    }

    @Test
    void unaLineaVencidaTambienRecibeSusUnidades() {
        // Arrange
        Lote vencido = loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_1, ID_PAN, "5", "4", "1.00", HOY.minusDays(1)));
        hayLotes(vencido);

        // Act
        useCase.execute(RegistrarReintegroDeInventarioRequest.crear().con(ID_PAN, BigDecimal.ONE));

        // Assert
        assertEquals(0, new BigDecimal("5").compareTo(vencido.obtenerDetalle(ID_DETALLE_1).getCantidadRestante()));
    }

    @Test
    void siNoCabenTodasLasUnidadesFallaSinDevolverNada() {
        // Arrange: solo cabe 1.
        Lote lote = loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_1, ID_PAN, "5", "4", "1.00", null));
        hayLotes(lote);

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(RegistrarReintegroDeInventarioRequest.crear().con(ID_PAN, new BigDecimal("2"))));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("solo admiten 1"));
        assertEquals(0, new BigDecimal("4").compareTo(lote.obtenerDetalle(ID_DETALLE_1).getCantidadRestante()));
        verify(loteRepository, never()).actualizar(any(), any());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void unProductoBajoDemandaNoDevuelveNada() {
        // Act
        useCase.execute(RegistrarReintegroDeInventarioRequest.crear().con(ID_CAFE_CON_LECHE, new BigDecimal("2")));

        // Assert
        verify(loteRepository, never()).obtenerPorProductos(anyList());
        verify(recalcularStockUseCase, never()).execute(anyString());
    }

    @Test
    void losProductosSeProcesanEnOrdenDeId() {
        // Arrange
        hayLotes(
                loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_1, ID_PAN, "5", "0", "1.00", null)),
                loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_2, ID_GASEOSA, "5", "0", "1.00", null)));
        when(loteRepository.obtenerPorProductos(argThat(ids -> ids != null && ID_PAN.equals(ids.get(0).getValue()))))
                .thenReturn(List.of(loteDeCompra(ID_LOTE_1, detalle(ID_DETALLE_1, ID_PAN, "5", "0", "1.00", null))));
        when(loteRepository.obtenerPorProductos(argThat(ids -> ids != null && ID_GASEOSA.equals(ids.get(0).getValue()))))
                .thenReturn(List.of(loteDeCompra(ID_LOTE_2, detalle(ID_DETALLE_2, ID_GASEOSA, "5", "0", "1.00", null))));

        // Act: la gaseosa llega primero, pero su id es mayor.
        useCase.execute(RegistrarReintegroDeInventarioRequest.crear()
                .con(ID_GASEOSA, BigDecimal.ONE)
                .con(ID_PAN, BigDecimal.ONE));

        // Assert
        InOrder orden = inOrder(recalcularStockUseCase);
        orden.verify(recalcularStockUseCase).execute(ID_PAN);
        orden.verify(recalcularStockUseCase).execute(ID_GASEOSA);
    }

    @Test
    void unProductoInexistenteRespondeNoEncontrado() {
        // Arrange
        String inexistente = "c0000000-0000-0000-0000-0000000000ee";
        when(productoRepository.obtenerPorId(argThat(id -> id != null && inexistente.equals(id.getValue())))).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(RegistrarReintegroDeInventarioRequest.crear().con(inexistente, BigDecimal.ONE)));

        // Assert
        assertEquals(404, error.getCodigo());
    }
}
