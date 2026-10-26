package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias del recalculo de 'producto.stock' (logica.md PRODUCTO-11, PRODUCTO-16) con
 * repositorios simulados: contenido abierto para un producto base con presentaciones y unidades
 * disponibles en lotes para un producto por unidades.
 */
@ExtendWith(MockitoExtension.class)
class RecalcularStockUseCaseTest {

    private static final String ID_HARINA = "b0000000-0000-0000-0000-000000000001";
    private static final String ID_BOLSA = "b0000000-0000-0000-0000-000000000002";
    private static final String ID_DETALLE = "b2000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE_BOLSA = "b2000000-0000-0000-0000-000000000002";
    private static final String ID_DETALLE_AJENO = "b2000000-0000-0000-0000-000000000003";

    @Mock private IProductoRepository productoRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IExistenciaRepository existenciaRepository;

    private RecalcularStockUseCase useCase;

    @BeforeEach
    void crearCasoDeUso() {
        useCase = new RecalcularStockUseCase(productoRepository, loteRepository, existenciaRepository);
    }

    @Test
    void elStockEsLaSumaDelContenidoAbiertoDelProductoYDeSusPresentaciones() {
        // Arrange
        Producto harina = productoDirecto(ID_HARINA, "Harina");
        when(productoRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(harina));
        when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa", ID_HARINA)));
        when(loteRepository.obtenerPorProductos(argThat(fuentes -> fuentes.size() == 2))).thenReturn(List.of(loteDeCompra(
                "b1000000-0000-0000-0000-000000000001",
                detalle(ID_DETALLE, ID_HARINA, "10", "0", "5", null),
                detalle(ID_DETALLE_BOLSA, ID_BOLSA, "10", "5", "5", null))));
        when(existenciaRepository.obtenerDisponiblesPorDetallesLote(anyList())).thenReturn(List.of(
                existencia("b3000000-0000-0000-0000-000000000001", ID_DETALLE, "1000", "250.5", null),
                existencia("b3000000-0000-0000-0000-000000000002", ID_DETALLE_BOLSA, "1000", "1000", null)));

        // Act
        useCase.execute(ID_HARINA);

        // Assert
        assertEquals(new BigDecimal("1250.50"), harina.getStock());
        verify(productoRepository).actualizarStock(harina);
    }

    @Test
    void soloCuentanLasExistenciasEnLaUnidadDelProducto() {
        // Arrange
        Producto harina = productoDirecto(ID_HARINA, "Harina");
        when(productoRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(harina));
        when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa", ID_HARINA)));
        when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(loteDeCompra(
                "b1000000-0000-0000-0000-000000000001", detalle(ID_DETALLE, ID_HARINA, "10", "0", "5", null))));
        when(existenciaRepository.obtenerDisponiblesPorDetallesLote(anyList())).thenReturn(List.of(
                existencia("b3000000-0000-0000-0000-000000000001", ID_DETALLE, "1000", "300", null),
                existencia("b3000000-0000-0000-0000-000000000002", ID_DETALLE, "1000", "999", null, ID_OTRA_UNIDAD)));

        // Act
        useCase.execute(ID_HARINA);

        // Assert
        assertEquals(new BigDecimal("300.00"), harina.getStock());
    }

    @Test
    void noConsultaLasExistenciasDeLineasDeOtrosProductos() {
        // Arrange
        Producto harina = productoDirecto(ID_HARINA, "Harina");
        when(productoRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(harina));
        when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa", ID_HARINA)));
        when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(loteDeCompra(
                "b1000000-0000-0000-0000-000000000001",
                detalle(ID_DETALLE, ID_HARINA, "10", "0", "5", null),
                detalle(ID_DETALLE_AJENO, "b0000000-0000-0000-0000-000000000099", "10", "0", "5", null))));
        when(existenciaRepository.obtenerDisponiblesPorDetallesLote(anyList())).thenReturn(List.of());

        // Act
        useCase.execute(ID_HARINA);

        // Assert
        verify(existenciaRepository).obtenerDisponiblesPorDetallesLote(argThat(detalles ->
                detalles.size() == 1 && ID_DETALLE.equals(detalles.get(0).getValue())));
    }

    @Test
    void sinExistenciasAbiertasElStockQuedaEnCero() {
        // Arrange
        Producto harina = productoDirecto(ID_HARINA, "Harina");
        harina.proyectarStock(new BigDecimal("40"));
        when(productoRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(harina));
        when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa", ID_HARINA)));
        when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of());
        when(existenciaRepository.obtenerDisponiblesPorDetallesLote(anyList())).thenReturn(List.of());

        // Act
        useCase.execute(ID_HARINA);

        // Assert
        assertEquals(new BigDecimal("0.00"), harina.getStock());
        verify(productoRepository).actualizarStock(harina);
    }

    @Test
    void unProductoPorUnidadesSumaLasUnidadesDisponiblesDeSusLineas() {
        // Arrange: pan sin presentaciones, con 3 y 4.5 unidades disponibles en dos lineas, y una
        // linea de otro producto en el mismo lote que no cuenta.
        Producto pan = productoDirecto(ID_HARINA, "Pan");
        when(productoRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(pan));
        when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of());
        when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(
                loteDeCompra("b1000000-0000-0000-0000-000000000001",
                        detalle(ID_DETALLE, ID_HARINA, "10", "3", "1", null),
                        detalle(ID_DETALLE_AJENO, "b0000000-0000-0000-0000-000000000099", "10", "10", "1", null)),
                loteDeCompra("b1000000-0000-0000-0000-000000000002",
                        detalle(ID_DETALLE_BOLSA, ID_HARINA, "5", "4.5", "1", null))));

        // Act
        useCase.execute(ID_HARINA);

        // Assert
        assertEquals(new BigDecimal("7.50"), pan.getStock());
        verify(productoRepository).actualizarStock(pan);
        verifyNoInteractions(existenciaRepository);
    }

    @Test
    void unProductoBajoDemandaSeDejaComoEsta() {
        // Arrange
        when(productoRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(productoBajoDemanda(ID_HARINA, "Capuchino")));

        // Act
        useCase.execute(ID_HARINA);

        // Assert
        verify(productoRepository, never()).actualizarStock(any());
        verifyNoInteractions(loteRepository, existenciaRepository);
    }

    @Test
    void unProductoInexistenteRespondeNoEncontrado() {
        // Arrange
        when(productoRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_HARINA));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(productoRepository, never()).actualizarStock(any());
    }

    @Test
    void bloqueaElProductoAntesDeSumarSusExistencias() {
        // Arrange
        when(productoRepository.obtenerPorIdParaActualizar(any())).thenReturn(Optional.of(productoDirecto(ID_HARINA, "Harina")));
        when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa", ID_HARINA)));
        when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of());
        when(existenciaRepository.obtenerDisponiblesPorDetallesLote(anyList())).thenReturn(List.of());

        // Act
        useCase.execute(ID_HARINA);

        // Assert
        InOrder orden = inOrder(productoRepository, existenciaRepository);
        orden.verify(productoRepository).obtenerPorIdParaActualizar(any());
        orden.verify(existenciaRepository).obtenerDisponiblesPorDetallesLote(anyList());
        orden.verify(productoRepository).actualizarStock(any());
    }
}
