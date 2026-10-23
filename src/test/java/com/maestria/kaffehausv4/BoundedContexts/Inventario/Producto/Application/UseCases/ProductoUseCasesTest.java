package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_CATEGORIA;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_OTRA_UNIDAD;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_TIPO;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ID_UNIDAD;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.UNIDAD;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.detalle;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.existencia;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.ingrediente;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.loteDeCompra;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.presentacion;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.productoBajoDemanda;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.productoDirecto;
import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.receta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.ValueObjects.CategoriaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ActualizarProductoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.CrearProductoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.InventarioProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ObtenerProductosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Dtos.ProductoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Pruebas unitarias de los casos de uso de Producto: el CRUD con su jerarquia de un solo nivel
 * (presentacion -> producto base, INV-07) y la consulta de inventario. Repositorios simulados.
 */
@ExtendWith(MockitoExtension.class)
class ProductoUseCasesTest {

    private static final String ID_HARINA = "d7000000-0000-0000-0000-000000000001";
    private static final String ID_BOLSA = "d7000000-0000-0000-0000-000000000002";
    private static final String ID_PAN = "d7000000-0000-0000-0000-000000000003";
    private static final String ID_MASA = "d7000000-0000-0000-0000-000000000004";
    private static final String ID_INEXISTENTE = "d7000000-0000-0000-0000-0000000000ee";

    @Mock private IProductoRepository productoRepository;
    @Mock private IUnidadRepository unidadRepository;
    @Mock private ITipoRepository tipoRepository;
    @Mock private ICategoriaRepository categoriaRepository;
    @Mock private ILoteRepository loteRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private IConversionRepository conversionRepository;
    @Mock private IRecetaRepository recetaRepository;

    @BeforeEach
    void prepararCatalogos() {
        lenient().when(unidadRepository.obtenerPorId(argThat(id -> id != null && ID_UNIDAD.equals(id.getValue())))).thenReturn(Optional.of(UNIDAD));
        lenient().when(tipoRepository.obtenerPorId(argThat(id -> id != null && ID_TIPO.equals(id.getValue()))))
                .thenReturn(Optional.of(Tipo.restaurar(ID_TIPO, "Insumo", false)));
        lenient().when(categoriaRepository.obtenerPorId(argThat(id -> id != null && ID_CATEGORIA.equals(id.getValue()))))
                .thenReturn(Optional.of(Categoria.restaurar(ID_CATEGORIA, "Insumos", true, false)));
        lenient().when(productoRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(productoRepository.obtenerPresentaciones(any())).thenReturn(List.of());
    }

    private void hayProducto(Producto producto) {
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && producto.getId().getValue().equals(id.getValue()))))
                .thenReturn(Optional.of(producto));
    }

    private static Map<String, Object> alta() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Harina");
        datos.put("precio", "12.5");
        datos.put("stockMinimo", "100");
        datos.put("idUnidadActual", ID_UNIDAD);
        datos.put("idTipo", ID_TIPO);
        datos.put("idCategoria", ID_CATEGORIA);
        return datos;
    }

    // ================================================================== crear

    @Nested
    class Crear {

        private CrearProductoUseCase useCase;

        @BeforeEach
        void crearCasoDeUso() {
            useCase = new CrearProductoUseCase(productoRepository, unidadRepository, tipoRepository, categoriaRepository);
            lenient().when(productoRepository.crear(any())).thenAnswer(llamada -> {
                Producto nuevo = llamada.getArgument(0);
                return Producto.restaurar(ID_HARINA, nuevo.getNombre(), nuevo.getStock(), nuevo.getPrecio(), nuevo.getStockMinimo(),
                        nuevo.isVendible(), nuevo.isEsStockDirecto(), nuevo.getUnidadActualId(), nuevo.getTipoId(), nuevo.getCategoriaId(),
                        nuevo.getProductoPadreId(), false);
            });
        }

        @Test
        void unProductoNuevoNaceConStockCeroYSusCatalogosResueltos() {
            // Arrange
            ArgumentCaptor<Producto> guardado = ArgumentCaptor.forClass(Producto.class);

            // Act
            ProductoResponse respuesta = useCase.execute(CrearProductoRequest.createFromRequest(alta()));

            // Assert
            verify(productoRepository).crear(guardado.capture());
            assertNull(guardado.getValue().getId());
            assertEquals(0, guardado.getValue().getStock().signum());
            assertEquals(ID_HARINA, respuesta.getId());
            assertEquals("Harina", respuesta.getNombre());
            assertEquals(0, new BigDecimal("12.5").compareTo(respuesta.getPrecio()));
            assertEquals("Gramo", respuesta.getUnidadActual().getUnidad());
            assertEquals("Insumo", respuesta.getTipo().getNombre());
            assertEquals("Insumos", respuesta.getCategoria().getNombre());
            assertNull(respuesta.getIdProductoPadre());
        }

        @Test
        void unaPresentacionSeCreaApuntandoASuProductoBase() {
            // Arrange
            hayProducto(productoDirecto(ID_HARINA, "Harina"));
            Map<String, Object> datos = alta();
            datos.put("nombre", "Bolsa de harina");
            datos.put("idProductoPadre", ID_HARINA);

            // Act
            ProductoResponse respuesta = useCase.execute(CrearProductoRequest.createFromRequest(datos));

            // Assert
            assertEquals(ID_HARINA, respuesta.getIdProductoPadre());
        }

        @Test
        void unaPresentacionNoPuedeSerProductoBaseDeOtra() {
            // Arrange
            hayProducto(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA));
            Map<String, Object> datos = alta();
            datos.put("idProductoPadre", ID_BOLSA);

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(CrearProductoRequest.createFromRequest(datos)));

            // Assert
            assertEquals(409, error.getCodigo());
            assertTrue(error.getMensaje().contains("es una presentacion"));
            verify(productoRepository, never()).crear(any());
        }

        @Test
        void validaQueExistanUnidadTipoCategoriaYProductoPadre() {
            // Arrange
            Map<String, Object> sinUnidad = alta();
            sinUnidad.put("idUnidadActual", ID_INEXISTENTE);
            Map<String, Object> sinTipo = alta();
            sinTipo.put("idTipo", ID_INEXISTENTE);
            Map<String, Object> sinCategoria = alta();
            sinCategoria.put("idCategoria", ID_INEXISTENTE);
            Map<String, Object> sinPadre = alta();
            sinPadre.put("idProductoPadre", ID_INEXISTENTE);

            // Act
            DomainException porUnidad = assertThrows(DomainException.class, () -> useCase.execute(CrearProductoRequest.createFromRequest(sinUnidad)));
            DomainException porTipo = assertThrows(DomainException.class, () -> useCase.execute(CrearProductoRequest.createFromRequest(sinTipo)));
            DomainException porCategoria = assertThrows(DomainException.class, () -> useCase.execute(CrearProductoRequest.createFromRequest(sinCategoria)));
            DomainException porPadre = assertThrows(DomainException.class, () -> useCase.execute(CrearProductoRequest.createFromRequest(sinPadre)));

            // Assert
            assertEquals(404, porUnidad.getCodigo());
            assertEquals(404, porTipo.getCodigo());
            assertEquals(404, porCategoria.getCodigo());
            assertEquals(404, porPadre.getCodigo());
            verify(productoRepository, never()).crear(any());
        }

        @Test
        void laPeticionExigeNombrePrecioStockMinimoYCatalogos() {
            // Arrange
            Map<String, Object> sinNombre = alta();
            sinNombre.remove("nombre");
            Map<String, Object> sinPrecio = alta();
            sinPrecio.remove("precio");
            Map<String, Object> sinCategoria = alta();
            sinCategoria.remove("idCategoria");

            // Act
            DomainException porNombre = assertThrows(DomainException.class, () -> CrearProductoRequest.createFromRequest(sinNombre));
            DomainException porPrecio = assertThrows(DomainException.class, () -> CrearProductoRequest.createFromRequest(sinPrecio));
            DomainException porCategoria = assertThrows(DomainException.class, () -> CrearProductoRequest.createFromRequest(sinCategoria));

            // Assert
            assertEquals(400, porNombre.getCodigo());
            assertEquals(400, porPrecio.getCodigo());
            assertEquals(400, porCategoria.getCodigo());
        }
    }

    // ================================================================== actualizar

    @Nested
    class Actualizar {

        private ActualizarProductoUseCase useCase;

        @BeforeEach
        void crearCasoDeUso() {
            useCase = new ActualizarProductoUseCase(productoRepository, unidadRepository, tipoRepository, categoriaRepository, loteRepository);
            lenient().when(unidadRepository.obtenerPorId(argThat(id -> id != null && ID_OTRA_UNIDAD.equals(id.getValue()))))
                    .thenReturn(Optional.of(Unidad.restaurar(ID_OTRA_UNIDAD, "Kilogramo", "kg", false, false)));
        }

        /** Masa con una linea de lote viva: ya tiene inventario. */
        private void masaConLote() {
            hayProducto(productoDirecto(ID_MASA, "Masa"));
            hayProducto(productoDirecto(ID_HARINA, "Harina"));
            lenient().when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(loteDeCompra("d7000000-0000-0000-0000-0000000000a1",
                    detalle("d7000000-0000-0000-0000-0000000000a2", ID_MASA, "5", "5", "10", null))));
        }

        @Test
        void conInventarioNoCambiaSuNaturalezaSuUnidadNiSuPadre() {
            // Arrange
            masaConLote();

            // Act
            DomainException porNaturaleza = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_MASA, ActualizarProductoRequest.createFromRequest(Map.of("esStockDirecto", false))));
            DomainException porUnidad = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_MASA, ActualizarProductoRequest.createFromRequest(Map.of("idUnidadActual", ID_OTRA_UNIDAD))));
            DomainException porPadre = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_MASA, ActualizarProductoRequest.createFromRequest(Map.of("idProductoPadre", ID_HARINA))));

            // Assert
            assertEquals(409, porNaturaleza.getCodigo());
            assertEquals(409, porUnidad.getCodigo());
            assertEquals(409, porPadre.getCodigo());
            assertTrue(porUnidad.getMensaje().contains("ya tiene inventario"));
            verify(productoRepository, never()).actualizar(any(), any());
        }

        @Test
        void conInventarioSiguenCambiandoSusOtrosDatosYLosMismosValores() {
            // Arrange
            masaConLote();

            // Act
            ProductoResponse respuesta = useCase.execute(ID_MASA, ActualizarProductoRequest.createFromRequest(
                    Map.of("nombre", "Masa madre", "precio", "20", "esStockDirecto", true, "idUnidadActual", ID_UNIDAD)));

            // Assert
            assertEquals("Masa madre", respuesta.getNombre());
            verify(productoRepository).actualizar(any(), any());
        }

        @Test
        void unProductoConStockOConPresentacionesTampocoCambiaSuUnidad() {
            // Arrange
            hayProducto(Producto.restaurar(ID_MASA, "Pan", new BigDecimal("20.00"), new BigDecimal("1.00"), BigDecimal.ZERO,
                    true, true, new UnidadId(ID_UNIDAD), new TipoId(ID_TIPO), new CategoriaId(ID_CATEGORIA), null, false));
            hayProducto(productoDirecto(ID_HARINA, "Harina"));
            when(productoRepository.obtenerPresentaciones(argThat(id -> ID_HARINA.equals(id.getValue()))))
                    .thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA)));

            // Act
            DomainException porStock = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_MASA, ActualizarProductoRequest.createFromRequest(Map.of("idUnidadActual", ID_OTRA_UNIDAD))));
            DomainException porPresentaciones = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_HARINA, ActualizarProductoRequest.createFromRequest(Map.of("idUnidadActual", ID_OTRA_UNIDAD))));

            // Assert
            assertEquals(409, porStock.getCodigo());
            assertEquals(409, porPresentaciones.getCodigo());
        }

        @Test
        void cambiaSoloLoInformado() {
            // Arrange
            hayProducto(productoDirecto(ID_HARINA, "Harina"));

            // Act
            ProductoResponse respuesta = useCase.execute(ID_HARINA, ActualizarProductoRequest.createFromRequest(Map.of("precio", "15", "vendible", false)));

            // Assert
            assertEquals("Harina", respuesta.getNombre());
            assertEquals(0, new BigDecimal("15").compareTo(respuesta.getPrecio()));
            assertFalse(respuesta.isVendible());
            assertTrue(respuesta.isEsStockDirecto());
            verify(productoRepository).actualizar(argThat(id -> ID_HARINA.equals(id.getValue())), any());
        }

        @Test
        void unProductoNoPuedeSerSuPropioProductoBase() {
            // Arrange
            hayProducto(productoDirecto(ID_HARINA, "Harina"));

            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_HARINA, ActualizarProductoRequest.createFromRequest(Map.of("idProductoPadre", ID_HARINA))));

            // Assert
            assertEquals(409, error.getCodigo());
            verify(productoRepository, never()).actualizar(any(), any());
        }

        @Test
        void elNuevoPadreNoPuedeSerUnaPresentacion() {
            // Arrange
            hayProducto(productoDirecto(ID_MASA, "Masa"));
            hayProducto(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA));

            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_MASA, ActualizarProductoRequest.createFromRequest(Map.of("idProductoPadre", ID_BOLSA))));

            // Assert
            assertEquals(409, error.getCodigo());
            verify(productoRepository, never()).actualizar(any(), any());
        }

        @Test
        void unProductoQueYaEsBaseDeOtrasNoPuedeTenerPadre() {
            // Arrange
            hayProducto(productoDirecto(ID_HARINA, "Harina"));
            hayProducto(productoDirecto(ID_MASA, "Masa"));
            when(productoRepository.obtenerPresentaciones(argThat(id -> ID_HARINA.equals(id.getValue()))))
                    .thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA)));

            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_HARINA, ActualizarProductoRequest.createFromRequest(Map.of("idProductoPadre", ID_MASA))));

            // Assert
            assertEquals(409, error.getCodigo());
            assertTrue(error.getMensaje().contains("es producto base de otras presentaciones"));
        }

        @Test
        void unProductoSinPresentacionesPuedePasarASerPresentacionDeOtro() {
            // Arrange
            hayProducto(productoDirecto(ID_MASA, "Masa"));
            hayProducto(productoDirecto(ID_HARINA, "Harina"));

            // Act
            ProductoResponse respuesta = useCase.execute(ID_MASA, ActualizarProductoRequest.createFromRequest(Map.of("idProductoPadre", ID_HARINA)));

            // Assert
            assertEquals(ID_HARINA, respuesta.getIdProductoPadre());
        }

        @Test
        void validaQueExistanLasReferenciasNuevas() {
            // Arrange
            hayProducto(productoDirecto(ID_HARINA, "Harina"));

            // Act
            DomainException porUnidad = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_HARINA, ActualizarProductoRequest.createFromRequest(Map.of("idUnidadActual", ID_INEXISTENTE))));
            DomainException porTipo = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_HARINA, ActualizarProductoRequest.createFromRequest(Map.of("idTipo", ID_INEXISTENTE))));
            DomainException porCategoria = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_HARINA, ActualizarProductoRequest.createFromRequest(Map.of("idCategoria", ID_INEXISTENTE))));
            DomainException porPadre = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_HARINA, ActualizarProductoRequest.createFromRequest(Map.of("idProductoPadre", ID_INEXISTENTE))));

            // Assert
            assertEquals(404, porUnidad.getCodigo());
            assertEquals(404, porTipo.getCodigo());
            assertEquals(404, porCategoria.getCodigo());
            assertEquals(404, porPadre.getCodigo());
            verify(productoRepository, never()).actualizar(any(), any());
        }

        @Test
        void unProductoInexistenteRespondeNoEncontrado() {
            // Act
            DomainException error = assertThrows(DomainException.class,
                    () -> useCase.execute(ID_INEXISTENTE, ActualizarProductoRequest.createFromRequest(Map.of("nombre", "x"))));

            // Assert
            assertEquals(404, error.getCodigo());
            assertTrue(error.getMensaje().contains(ID_INEXISTENTE));
        }
    }

    // ================================================================== eliminar y consultas

    @Nested
    class EliminarYConsultar {

        @Test
        void eliminarDaDeBajaElExistenteYRechazaElInexistente() {
            // Arrange
            EliminarProductoUseCase useCase = new EliminarProductoUseCase(productoRepository);
            hayProducto(productoDirecto(ID_HARINA, "Harina"));

            // Act
            useCase.execute(ID_HARINA);
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

            // Assert
            verify(productoRepository, times(1)).eliminar(argThat(id -> ID_HARINA.equals(id.getValue())));
            assertEquals(404, error.getCodigo());
        }

        @Test
        void obtenerPorIdDevuelveElProductoConSusCatalogos() {
            // Arrange
            ObtenerProductoPorIdUseCase useCase = new ObtenerProductoPorIdUseCase(productoRepository, unidadRepository, tipoRepository, categoriaRepository);
            hayProducto(productoDirecto(ID_HARINA, "Harina"));

            // Act
            ProductoResponse respuesta = useCase.execute(ID_HARINA);

            // Assert
            assertEquals("Harina", respuesta.getNombre());
            assertEquals("Gramo", respuesta.getUnidadActual().getUnidad());
            assertEquals("Insumo", respuesta.getTipo().getNombre());
        }

        @Test
        void obtenerPorIdResponde404SiFaltaElProductoOAlgunoDeSusCatalogos() {
            // Arrange
            ObtenerProductoPorIdUseCase useCase = new ObtenerProductoPorIdUseCase(productoRepository, unidadRepository, tipoRepository, categoriaRepository);
            hayProducto(Producto.restaurar(ID_MASA, "Masa", BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, true, true,
                    new UnidadId(ID_UNIDAD), new TipoId(ID_INEXISTENTE), new CategoriaId(ID_CATEGORIA), null, false));
            hayProducto(Producto.restaurar(ID_PAN, "Pan", BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, true, true,
                    new UnidadId(ID_UNIDAD), new TipoId(ID_TIPO), new CategoriaId(ID_INEXISTENTE), null, false));
            hayProducto(presentacion(ID_BOLSA, "Bolsa", ID_HARINA));

            // Act
            DomainException sinProducto = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));
            DomainException sinTipo = assertThrows(DomainException.class, () -> useCase.execute(ID_MASA));
            DomainException sinCategoria = assertThrows(DomainException.class, () -> useCase.execute(ID_PAN));
            DomainException sinUnidad = assertThrows(DomainException.class, () -> useCase.execute(ID_BOLSA));

            // Assert
            assertEquals(404, sinProducto.getCodigo());
            assertEquals(404, sinTipo.getCodigo());
            assertEquals(404, sinCategoria.getCodigo());
            assertEquals(404, sinUnidad.getCodigo());
        }

        @Test
        void elListadoResuelveLosCatalogosDeCadaProductoYConservaLaPaginacion() {
            // Arrange
            ObtenerProductosUseCase useCase = new ObtenerProductosUseCase(productoRepository, unidadRepository, tipoRepository, categoriaRepository);
            when(productoRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(
                    List.of(productoDirecto(ID_HARINA, "Harina"), productoBajoDemanda(ID_PAN, "Pan")), 0, 2, 5, true));

            // Act
            PaginatedResponse<List<ProductoResponse>> pagina = useCase.execute(ObtenerProductosRequest.createFromRequest(Map.of()));

            // Assert
            assertEquals(List.of("Harina", "Pan"), pagina.getData().stream().map(ProductoResponse::getNombre).toList());
            assertFalse(pagina.getData().get(1).isEsStockDirecto());
            assertEquals(5, pagina.getTotal());
            assertTrue(pagina.isHasMore());
        }
    }

    // ================================================================== consulta de inventario

    /**
     * Escenario: la harina (en gramos) se compra en bolsas de 1 kg a 50.00. Quedan 2 bolsas
     * cerradas vigentes y 1 vencida; hay 400 g abiertos vigentes y 1000 g abiertos vencidos.
     */
    @Nested
    class Inventario {

        private static final String ID_DETALLE_VIGENTE = "d7000000-0000-0000-0000-0000000000b1";
        private static final String ID_DETALLE_VENCIDO = "d7000000-0000-0000-0000-0000000000b2";

        private ObtenerInventarioProductoUseCase useCase;

        @BeforeEach
        void prepararEscenario() {
            LocalDate ayer = LocalDate.now().minusDays(1);
            useCase = new ObtenerInventarioProductoUseCase(productoRepository, loteRepository, existenciaRepository, conversionRepository, recetaRepository);

            hayProducto(productoDirecto(ID_HARINA, "Harina"));
            lenient().when(productoRepository.obtenerPresentaciones(argThat(id -> id != null && ID_HARINA.equals(id.getValue()))))
                    .thenReturn(List.of(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA)));
            lenient().when(conversionRepository.obtenerVivas()).thenReturn(List.of(
                    Conversion.crear(new BigDecimal("1000"), false, new UnidadId(ID_OTRA_UNIDAD), new UnidadId(ID_UNIDAD))));
            lenient().when(loteRepository.obtenerPorProductos(anyList())).thenReturn(List.of(loteDeCompra("d7000000-0000-0000-0000-0000000000a1",
                    detalle(ID_DETALLE_VIGENTE, ID_BOLSA, "5", "2", "50", null),
                    detalle(ID_DETALLE_VENCIDO, ID_BOLSA, "5", "1", "50", ayer))));
            lenient().when(existenciaRepository.obtenerDisponiblesPorDetallesLote(anyList())).thenReturn(List.of(
                    existencia("d7000000-0000-0000-0000-0000000000c1", ID_DETALLE_VIGENTE, "1000", "400", null),
                    existencia("d7000000-0000-0000-0000-0000000000c2", ID_DETALLE_VENCIDO, "1000", "1000", ayer)));
        }

        @Test
        void unProductoDeStockDirectoInformaAbiertoCerradoValorYDisponible() {
            // Act
            InventarioProductoResponse inventario = useCase.execute(ID_HARINA);

            // Assert
            assertTrue(inventario.isEsStockDirecto());
            assertEquals(0, new BigDecimal("1400").compareTo(inventario.getStockAbierto()));
            assertEquals(0, new BigDecimal("3000").compareTo(inventario.getStockCerrado()));
            assertEquals(0, new BigDecimal("4400").compareTo(inventario.getStockTotal()));
            assertEquals(new BigDecimal("70.00"), inventario.getValorInventario());
            assertEquals(0, new BigDecimal("400").compareTo(inventario.getDisponible()));
            assertEquals(0, new BigDecimal("2400").compareTo(inventario.getDisponibleTrasAbrir()));
            assertEquals(ID_UNIDAD, inventario.getIdUnidad());
        }

        @Test
        void avisaCuandoElStockEstaPorDebajoDelMinimo() {
            // Arrange
            Producto conMinimo = Producto.restaurar(ID_HARINA, "Harina", new BigDecimal("3.00"), BigDecimal.ONE, new BigDecimal("5.00"),
                    true, true, new UnidadId(ID_UNIDAD), new TipoId(ID_TIPO), new CategoriaId(ID_CATEGORIA), null, false);
            hayProducto(conMinimo);

            // Act
            InventarioProductoResponse inventario = useCase.execute(ID_HARINA);

            // Assert
            assertTrue(inventario.isBajoMinimo());
            assertEquals(new BigDecimal("3.00"), inventario.getStock());
        }

        @Test
        void sinConversionLasUnidadesCerradasNoSeCuentan() {
            // Arrange
            when(conversionRepository.obtenerVivas()).thenReturn(List.of());

            // Act
            InventarioProductoResponse inventario = useCase.execute(ID_HARINA);

            // Assert
            assertEquals(0, inventario.getStockCerrado().signum());
            assertEquals(0, new BigDecimal("1400").compareTo(inventario.getStockAbierto()));
            assertEquals(0, new BigDecimal("400").compareTo(inventario.getDisponibleTrasAbrir()));
        }

        @Test
        void unProductoBajoDemandaInformaCuantasUnidadesAlcanzanSusInsumos() {
            // Arrange: la receta rinde 10 panes con 4000 g de harina => 400 g por pan.
            hayProducto(productoBajoDemanda(ID_PAN, "Pan"));
            when(recetaRepository.obtenerPorProductoObtenido(any())).thenReturn(Optional.of(receta("d7000000-0000-0000-0000-0000000000d1", ID_PAN, "10",
                    ingrediente("d7000000-0000-0000-0000-0000000000e1", ID_HARINA, "4000"))));

            // Act
            InventarioProductoResponse inventario = useCase.execute(ID_PAN);

            // Assert
            assertFalse(inventario.isEsStockDirecto());
            assertEquals(0, BigDecimal.ONE.compareTo(inventario.getDisponible()));
            assertEquals(0, new BigDecimal("6").compareTo(inventario.getDisponibleTrasAbrir()));
            assertNull(inventario.getStock());
            assertNull(inventario.getStockAbierto());
            assertFalse(inventario.isBajoMinimo());
        }

        @Test
        void unProductoBajoDemandaSinRecetaNoTieneDisponible() {
            // Arrange
            hayProducto(productoBajoDemanda(ID_PAN, "Pan"));
            when(recetaRepository.obtenerPorProductoObtenido(any())).thenReturn(Optional.empty());

            // Act
            InventarioProductoResponse inventario = useCase.execute(ID_PAN);

            // Assert
            assertEquals(0, inventario.getDisponible().signum());
            assertEquals(0, inventario.getDisponibleTrasAbrir().signum());
        }

        @Test
        void unaRecetaQueSeContieneASiMismaSeRechaza() {
            // Arrange
            hayProducto(productoBajoDemanda(ID_PAN, "Pan"));
            hayProducto(productoBajoDemanda(ID_MASA, "Masa"));
            when(recetaRepository.obtenerPorProductoObtenido(argThat(id -> id != null && ID_PAN.equals(id.getValue()))))
                    .thenReturn(Optional.of(receta("d7000000-0000-0000-0000-0000000000d1", ID_PAN, "1",
                            ingrediente("d7000000-0000-0000-0000-0000000000e1", ID_MASA, "1"))));
            when(recetaRepository.obtenerPorProductoObtenido(argThat(id -> id != null && ID_MASA.equals(id.getValue()))))
                    .thenReturn(Optional.of(receta("d7000000-0000-0000-0000-0000000000d2", ID_MASA, "1",
                            ingrediente("d7000000-0000-0000-0000-0000000000e2", ID_PAN, "1"))));

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_PAN));

            // Assert
            assertEquals(409, error.getCodigo());
        }

        @Test
        void unIngredienteQueYaNoExisteImpideCalcularElDisponible() {
            // Arrange
            hayProducto(productoBajoDemanda(ID_PAN, "Pan"));
            when(recetaRepository.obtenerPorProductoObtenido(any())).thenReturn(Optional.of(receta("d7000000-0000-0000-0000-0000000000d1", ID_PAN, "1",
                    ingrediente("d7000000-0000-0000-0000-0000000000e1", ID_INEXISTENTE, "1"))));

            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_PAN));

            // Assert
            assertEquals(404, error.getCodigo());
        }

        @Test
        void unProductoInexistenteRespondeNoEncontrado() {
            // Act
            DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

            // Assert
            assertEquals(404, error.getCodigo());
        }
    }
}
