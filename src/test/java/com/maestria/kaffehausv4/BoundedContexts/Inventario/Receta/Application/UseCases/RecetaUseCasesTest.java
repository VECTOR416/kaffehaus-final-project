package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.Soporte.DatosDePrueba;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ActualizarRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.CrearRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ObtenerRecetasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;

/**
 * Pruebas unitarias de los casos de uso de Receta: el alta, la correccion (que concilia
 * ingredientes y pasos) y las consultas. Repositorios simulados.
 *
 * Escenario de correccion: el pan rinde 10 con harina (4) y levadura (2), en dos pasos.
 */
@ExtendWith(MockitoExtension.class)
class RecetaUseCasesTest {

    private static final String ID_RECETA = "0c000000-0000-0000-0000-000000000001";
    private static final String ID_PAN = "0c000000-0000-0000-0000-00000000000a";
    private static final String ID_HARINA = "0c000000-0000-0000-0000-00000000000b";
    private static final String ID_LEVADURA = "0c000000-0000-0000-0000-00000000000c";
    private static final String ID_SAL = "0c000000-0000-0000-0000-00000000000d";
    private static final String ID_BOLSA = "0c000000-0000-0000-0000-00000000000e";
    private static final String ID_ING_HARINA = "0c000000-0000-0000-0000-0000000000a1";
    private static final String ID_ING_LEVADURA = "0c000000-0000-0000-0000-0000000000a2";
    private static final String ID_PASO_MEZCLAR = "0c000000-0000-0000-0000-0000000000b1";
    private static final String ID_PASO_HORNEAR = "0c000000-0000-0000-0000-0000000000b2";
    private static final String ID_INEXISTENTE = "0c000000-0000-0000-0000-0000000000ee";

    @Mock private IRecetaRepository recetaRepository;
    @Mock private IProductoRepository productoRepository;

    @BeforeEach
    void prepararCatalogosYPersistencia() {
        hayProducto(productoDirecto(ID_PAN, "Pan"));
        hayProducto(productoDirecto(ID_HARINA, "Harina"));
        hayProducto(productoDirecto(ID_LEVADURA, "Levadura"));
        hayProducto(productoDirecto(ID_SAL, "Sal"));
        hayProducto(presentacion(ID_BOLSA, "Bolsa de harina", ID_HARINA));
        lenient().when(recetaRepository.crear(any())).thenAnswer(llamada -> conIds(llamada.getArgument(0)));
        lenient().when(recetaRepository.actualizar(any(), any())).thenAnswer(llamada -> conIds(llamada.getArgument(1)));
    }

    private void hayProducto(Producto producto) {
        lenient().when(productoRepository.obtenerPorId(argThat(id -> id != null && producto.getId().getValue().equals(id.getValue()))))
                .thenReturn(Optional.of(producto));
    }

    /** Lo que hace la persistencia al guardar: asignar id a la receta y a las hijas que no lo tienen. */
    private static Receta conIds(Receta receta) {
        List<IngredienteReceta> ingredientes = new ArrayList<>();
        for (IngredienteReceta ingrediente : receta.getIngredientes()) {
            String id = ingrediente.getId() != null ? ingrediente.getId().getValue() : "0c000000-0000-0000-0000-0000000000c" + ingredientes.size();
            ingredientes.add(IngredienteReceta.restaurar(id, ingrediente.getProductoId(), ingrediente.getCantidad(), ingrediente.isBorrado()));
        }
        List<PasoReceta> pasos = new ArrayList<>();
        for (PasoReceta paso : receta.getPasos()) {
            String id = paso.getId() != null ? paso.getId().getValue() : "0c000000-0000-0000-0000-0000000000d" + pasos.size();
            pasos.add(PasoReceta.restaurar(id, paso.getPasos(), paso.getDescripcion(), paso.isBorrado()));
        }
        return Receta.restaurar(ID_RECETA, receta.getCantidad(), receta.getTitulo(), receta.getProductoObtenidoId(), receta.isBorrado(), ingredientes, pasos);
    }

    private Receta hayRecetaDePan() {
        Receta receta = Receta.restaurar(ID_RECETA, new BigDecimal("10.00"), "Pan casero", new ProductoId(ID_PAN), false,
                List.of(IngredienteReceta.restaurar(ID_ING_HARINA, new ProductoId(ID_HARINA), new BigDecimal("4.0000"), false),
                        IngredienteReceta.restaurar(ID_ING_LEVADURA, new ProductoId(ID_LEVADURA), new BigDecimal("2.0000"), false)),
                List.of(PasoReceta.restaurar(ID_PASO_MEZCLAR, 1, "Mezclar", false),
                        PasoReceta.restaurar(ID_PASO_HORNEAR, 2, "Hornear", false)));
        lenient().when(recetaRepository.obtenerPorId(argThat(id -> id != null && ID_RECETA.equals(id.getValue())))).thenReturn(Optional.of(receta));
        return receta;
    }

    private Receta recetaGuardada() {
        ArgumentCaptor<Receta> captor = ArgumentCaptor.forClass(Receta.class);
        verify(recetaRepository).actualizar(any(), captor.capture());
        return captor.getValue();
    }

    private static Map<String, Object> alta(List<Map<String, Object>> ingredientes, List<Map<String, Object>> pasos) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("cantidad", "10");
        datos.put("titulo", "Pan casero");
        datos.put("idProductoObtenido", ID_PAN);
        datos.put("ingredientes", ingredientes);
        datos.put("pasos", pasos);
        return datos;
    }

    private static Map<String, Object> ingrediente(String idProducto, String cantidad) {
        return Map.of("idProducto", idProducto, "cantidad", cantidad);
    }

    private static Map<String, Object> paso(String descripcion) {
        return Map.of("descripcion", descripcion);
    }

    private ActualizarRecetaUseCase actualizar() {
        return new ActualizarRecetaUseCase(recetaRepository, productoRepository);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearRegistraLaRecetaConSusIngredientesYPasosNumerados() {
        // Arrange
        CrearRecetaUseCase useCase = new CrearRecetaUseCase(recetaRepository, productoRepository);

        // Act
        RecetaResponse respuesta = useCase.execute(CrearRecetaRequest.createFromRequest(alta(
                List.of(ingrediente(ID_HARINA, "4"), ingrediente(ID_LEVADURA, "2")), List.of(paso("Mezclar"), paso("Hornear")))));

        // Assert
        assertEquals(ID_RECETA, respuesta.getId());
        assertEquals("Pan casero", respuesta.getTitulo());
        assertEquals(ID_PAN, respuesta.getIdProductoObtenido());
        assertEquals(2, respuesta.getIngredientes().size());
        assertEquals(List.of(1, 2), respuesta.getPasos().stream().map(p -> p.getPasos()).toList());
        assertEquals(List.of("Mezclar", "Hornear"), respuesta.getPasos().stream().map(p -> p.getDescripcion()).toList());
    }

    @Test
    void crearValidaQueExistanElProductoObtenidoYLosIngredientes() {
        // Arrange
        CrearRecetaUseCase useCase = new CrearRecetaUseCase(recetaRepository, productoRepository);
        Map<String, Object> sinProductoObtenido = alta(List.of(ingrediente(ID_HARINA, "4")), List.of(paso("Mezclar")));
        sinProductoObtenido.put("idProductoObtenido", ID_INEXISTENTE);

        // Act
        DomainException porProducto = assertThrows(DomainException.class,
                () -> useCase.execute(CrearRecetaRequest.createFromRequest(sinProductoObtenido)));
        DomainException porIngrediente = assertThrows(DomainException.class, () -> useCase.execute(CrearRecetaRequest.createFromRequest(
                alta(List.of(ingrediente(ID_INEXISTENTE, "1")), List.of(paso("Mezclar"))))));

        // Assert
        assertEquals(404, porProducto.getCodigo());
        assertEquals(404, porIngrediente.getCodigo());
        verify(recetaRepository, never()).crear(any());
    }

    @Test
    void unaRecetaSeExpresaEnProductosBaseNoEnPresentaciones() {
        // Arrange
        CrearRecetaUseCase useCase = new CrearRecetaUseCase(recetaRepository, productoRepository);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(CrearRecetaRequest.createFromRequest(
                alta(List.of(ingrediente(ID_BOLSA, "1")), List.of(paso("Mezclar"))))));

        // Assert
        assertEquals(400, error.getCodigo());
        assertTrue(error.getMensaje().contains("es una presentacion"));
        verify(recetaRepository, never()).crear(any());
    }

    @Test
    void crearRechazaIngredientesRepetidosYCantidadesInvalidas() {
        // Arrange
        CrearRecetaUseCase useCase = new CrearRecetaUseCase(recetaRepository, productoRepository);

        // Act
        DomainException porRepetido = assertThrows(DomainException.class, () -> useCase.execute(CrearRecetaRequest.createFromRequest(
                alta(List.of(ingrediente(ID_HARINA, "4"), ingrediente(ID_HARINA, "1")), List.of(paso("Mezclar"))))));
        DomainException porCantidad = assertThrows(DomainException.class, () -> useCase.execute(CrearRecetaRequest.createFromRequest(
                alta(List.of(ingrediente(ID_HARINA, "0")), List.of(paso("Mezclar"))))));

        // Assert
        assertEquals(400, porRepetido.getCodigo());
        assertEquals(400, porCantidad.getCodigo());
        verify(recetaRepository, never()).crear(any());
    }

    @Test
    void laPeticionDeAltaExigeTituloRendimientoIngredientesYPasos() {
        // Arrange
        Map<String, Object> sinTitulo = alta(List.of(ingrediente(ID_HARINA, "4")), List.of(paso("Mezclar")));
        sinTitulo.remove("titulo");

        // Act
        DomainException porTitulo = assertThrows(DomainException.class, () -> CrearRecetaRequest.createFromRequest(sinTitulo));
        DomainException sinIngredientes = assertThrows(DomainException.class,
                () -> CrearRecetaRequest.createFromRequest(alta(List.of(), List.of(paso("Mezclar")))));
        DomainException sinPasos = assertThrows(DomainException.class,
                () -> CrearRecetaRequest.createFromRequest(alta(List.of(ingrediente(ID_HARINA, "4")), List.of())));
        DomainException pasoVacio = assertThrows(DomainException.class,
                () -> CrearRecetaRequest.createFromRequest(alta(List.of(ingrediente(ID_HARINA, "4")), List.of(Map.of("descripcion", " ")))));

        // Assert
        assertEquals(400, porTitulo.getCodigo());
        assertEquals(400, sinIngredientes.getCodigo());
        assertEquals(400, sinPasos.getCodigo());
        assertEquals(400, pasoVacio.getCodigo());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarSinColeccionesSoloCorrigeLaCabecera() {
        // Arrange
        hayRecetaDePan();

        // Act
        RecetaResponse respuesta = actualizar().execute(ID_RECETA, ActualizarRecetaRequest.createFromRequest(Map.of("titulo", "Pan de campo", "cantidad", "12")));

        // Assert
        assertEquals("Pan de campo", respuesta.getTitulo());
        assertEquals(0, new BigDecimal("12").compareTo(respuesta.getCantidad()));
        Receta guardada = recetaGuardada();
        assertEquals(2, guardada.getIngredientesActivos().size());
        assertEquals(2, guardada.getPasosActivos().size());
    }

    @Test
    void conciliarIngredientesDaDeBajaLosQueNoVienenCorrigeLosQueSiYAgregaLosNuevos() {
        // Arrange
        hayRecetaDePan();

        // Act: sale la levadura, la harina pasa a 5 y entra la sal.
        actualizar().execute(ID_RECETA, ActualizarRecetaRequest.createFromRequest(Map.of("ingredientes",
                List.of(Map.of("id", ID_ING_HARINA, "cantidad", "5"), ingrediente(ID_SAL, "0.2")))));

        // Assert
        Receta guardada = recetaGuardada();
        assertEquals(2, guardada.getIngredientesActivos().size());
        assertEquals(List.of(ID_HARINA, ID_SAL), guardada.getIngredientesActivos().stream().map(i -> i.getProductoId().getValue()).toList());
        assertEquals(0, new BigDecimal("5").compareTo(guardada.getIngredientesActivos().get(0).getCantidad()));
    }

    @Test
    void sePuedeDarDeBajaUnIngredienteYVolverAAgregarElMismoProductoEnLaMismaPeticion() {
        // Arrange
        hayRecetaDePan();

        // Act
        actualizar().execute(ID_RECETA, ActualizarRecetaRequest.createFromRequest(Map.of("ingredientes",
                List.of(Map.of("id", ID_ING_HARINA), ingrediente(ID_LEVADURA, "3")))));

        // Assert
        Receta guardada = recetaGuardada();
        assertEquals(2, guardada.getIngredientesActivos().size());
        assertEquals(0, new BigDecimal("3").compareTo(guardada.getIngredientesActivos().get(1).getCantidad()));
    }

    @Test
    void unIngredienteNuevoExigeProductoYCantidadYNoPuedeSerPresentacion() {
        // Arrange
        hayRecetaDePan();

        // Act
        DomainException sinCantidad = assertThrows(DomainException.class, () -> actualizar().execute(ID_RECETA,
                ActualizarRecetaRequest.createFromRequest(Map.of("ingredientes", List.of(Map.of("id", ID_ING_HARINA), Map.of("idProducto", ID_SAL))))));
        DomainException porPresentacion = assertThrows(DomainException.class, () -> actualizar().execute(ID_RECETA,
                ActualizarRecetaRequest.createFromRequest(Map.of("ingredientes", List.of(Map.of("id", ID_ING_HARINA), ingrediente(ID_BOLSA, "1"))))));
        DomainException porInexistente = assertThrows(DomainException.class, () -> actualizar().execute(ID_RECETA,
                ActualizarRecetaRequest.createFromRequest(Map.of("ingredientes", List.of(Map.of("id", ID_ING_HARINA), ingrediente(ID_INEXISTENTE, "1"))))));

        // Assert
        assertEquals(400, sinCantidad.getCodigo());
        assertEquals(400, porPresentacion.getCodigo());
        assertEquals(404, porInexistente.getCodigo());
        verify(recetaRepository, never()).actualizar(any(), any());
    }

    @Test
    void noSePuedeCambiarElProductoDeUnIngredienteExistente() {
        // Arrange
        hayRecetaDePan();

        // Act
        DomainException error = assertThrows(DomainException.class, () -> actualizar().execute(ID_RECETA,
                ActualizarRecetaRequest.createFromRequest(Map.of("ingredientes",
                        List.of(Map.of("id", ID_ING_HARINA, "idProducto", ID_SAL), Map.of("id", ID_ING_LEVADURA))))));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(recetaRepository, never()).actualizar(any(), any());
    }

    @Test
    void unIngredienteQueNoPerteneceALaRecetaSeRechaza() {
        // Arrange
        hayRecetaDePan();

        // Act
        DomainException error = assertThrows(DomainException.class, () -> actualizar().execute(ID_RECETA,
                ActualizarRecetaRequest.createFromRequest(Map.of("ingredientes",
                        List.of(Map.of("id", ID_ING_HARINA), Map.of("id", ID_INEXISTENTE, "cantidad", "1"))))));

        // Assert
        assertEquals(404, error.getCodigo());
    }

    @Test
    void losPasosQuedanEnElOrdenEnQueLlegan() {
        // Arrange
        hayRecetaDePan();

        // Act: se invierte el orden y se inserta un paso nuevo en medio.
        actualizar().execute(ID_RECETA, ActualizarRecetaRequest.createFromRequest(Map.of("pasos", List.of(
                Map.of("id", ID_PASO_HORNEAR), paso("Dejar reposar"), Map.of("id", ID_PASO_MEZCLAR, "descripcion", "Mezclar bien")))));

        // Assert
        Receta guardada = recetaGuardada();
        assertEquals(List.of("Hornear", "Dejar reposar", "Mezclar bien"), guardada.getPasosActivos().stream().map(PasoReceta::getDescripcion).toList());
        assertEquals(List.of(1, 2, 3), guardada.getPasosActivos().stream().map(PasoReceta::getPasos).toList());
    }

    @Test
    void unPasoQueNoVieneSeDaDeBajaYLosRestantesSeRenumeran() {
        // Arrange
        hayRecetaDePan();

        // Act
        actualizar().execute(ID_RECETA, ActualizarRecetaRequest.createFromRequest(Map.of("pasos", List.of(Map.of("id", ID_PASO_HORNEAR)))));

        // Assert
        Receta guardada = recetaGuardada();
        assertEquals(1, guardada.getPasosActivos().size());
        assertEquals("Hornear", guardada.getPasosActivos().get(0).getDescripcion());
        assertEquals(1, guardada.getPasosActivos().get(0).getPasos());
    }

    @Test
    void unPasoNuevoExigeDescripcion() {
        // Arrange
        hayRecetaDePan();

        // Act
        DomainException error = assertThrows(DomainException.class, () -> actualizar().execute(ID_RECETA,
                ActualizarRecetaRequest.createFromRequest(Map.of("pasos", List.of(Map.of("id", ID_PASO_MEZCLAR), new HashMap<String, Object>())))));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(recetaRepository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarValidaElNuevoProductoObtenidoYQueLaRecetaExista() {
        // Arrange
        hayRecetaDePan();

        // Act
        DomainException sinProducto = assertThrows(DomainException.class, () -> actualizar().execute(ID_RECETA,
                ActualizarRecetaRequest.createFromRequest(Map.of("idProductoObtenido", ID_INEXISTENTE))));
        DomainException sinReceta = assertThrows(DomainException.class, () -> actualizar().execute(ID_INEXISTENTE,
                ActualizarRecetaRequest.createFromRequest(Map.of("titulo", "x"))));

        // Assert
        assertEquals(404, sinProducto.getCodigo());
        assertEquals(404, sinReceta.getCodigo());
        verify(recetaRepository, never()).actualizar(any(), any());
    }

    @Test
    void unRendimientoNoPositivoSeRechaza() {
        // Arrange
        hayRecetaDePan();

        // Act
        DomainException error = assertThrows(DomainException.class, () -> actualizar().execute(ID_RECETA,
                ActualizarRecetaRequest.createFromRequest(Map.of("cantidad", "0"))));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(recetaRepository, never()).actualizar(any(), any());
    }

    // ------------------------------------------------------------------ eliminar y consultas

    @Test
    void elProductoObtenidoNoPuedeSerIngredienteDeSuPropiaReceta() {
        // Arrange (logica.md RECETA-07)
        CrearRecetaUseCase useCase = new CrearRecetaUseCase(recetaRepository, productoRepository);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(CrearRecetaRequest.createFromRequest(
                alta(List.of(ingrediente(ID_HARINA, "4"), ingrediente(ID_PAN, "1")), List.of(paso("Mezclar"))))));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(recetaRepository, never()).crear(any());
    }

    @Test
    void unaRecetaNoPuedeFormarUnCicloConLasRecetasDeSusInsumosMto() {
        // Arrange (logica.md RECETA-07): el mocha usa base de chocolate, cuya receta vigente usa mocha.
        String idMocha = "0c000000-0000-0000-0000-0000000000f1";
        String idBase = "0c000000-0000-0000-0000-0000000000f2";
        hayProducto(productoBajoDemanda(idMocha, "Mocha"));
        hayProducto(productoBajoDemanda(idBase, "Base de chocolate"));
        when(recetaRepository.obtenerPorProductoObtenido(argThat(id -> id != null && idBase.equals(id.getValue()))))
                .thenReturn(Optional.of(receta("0c000000-0000-0000-0000-0000000000f3", idBase, "1",
                        DatosDePrueba.ingrediente("0c000000-0000-0000-0000-0000000000f4", idMocha, "0.5"))));
        Map<String, Object> datos = alta(List.of(ingrediente(idBase, "1")), List.of(paso("Servir")));
        datos.put("idProductoObtenido", idMocha);
        CrearRecetaUseCase useCase = new CrearRecetaUseCase(recetaRepository, productoRepository);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(CrearRecetaRequest.createFromRequest(datos)));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("Mocha -> Base de chocolate -> Mocha"));
        verify(recetaRepository, never()).crear(any());
    }

    @Test
    void lasRecetasDeLosInsumosMtsNoSeRecorren() {
        // Arrange: el pan (MTS) tiene su propia receta de produccion, pero se descuenta de su stock.
        String idSandwich = "0c000000-0000-0000-0000-0000000000f5";
        hayProducto(productoBajoDemanda(idSandwich, "Sandwich"));
        Map<String, Object> datos = alta(List.of(ingrediente(ID_PAN, "1")), List.of(paso("Armar")));
        datos.put("idProductoObtenido", idSandwich);
        CrearRecetaUseCase useCase = new CrearRecetaUseCase(recetaRepository, productoRepository);

        // Act
        useCase.execute(CrearRecetaRequest.createFromRequest(datos));

        // Assert
        verify(recetaRepository).crear(any());
        verify(recetaRepository, never()).obtenerPorProductoObtenido(any());
    }

    @Test
    void laUltimaRecetaDeUnMtoVendibleNoSeDaDeBaja() {
        // Arrange (logica.md RECETA-08, RECETA-09)
        hayProducto(productoBajoDemanda(ID_PAN, "Pan"));
        Receta receta = hayRecetaDePan();
        when(recetaRepository.obtenerVivasPorProductoObtenido(any())).thenReturn(List.of(receta));
        EliminarRecetaUseCase useCase = new EliminarRecetaUseCase(recetaRepository, productoRepository);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_RECETA));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("ultima"));
        verify(recetaRepository, never()).eliminar(any());
    }

    @Test
    void unMtoConOtraRecetaVivaSiPierdeUna() {
        // Arrange
        hayProducto(productoBajoDemanda(ID_PAN, "Pan"));
        Receta receta = hayRecetaDePan();
        when(recetaRepository.obtenerVivasPorProductoObtenido(any())).thenReturn(List.of(
                receta("0c000000-0000-0000-0000-0000000000f6", ID_PAN, "10", DatosDePrueba.ingrediente("0c000000-0000-0000-0000-0000000000f7", ID_HARINA, "4")),
                receta));
        EliminarRecetaUseCase useCase = new EliminarRecetaUseCase(recetaRepository, productoRepository);

        // Act
        useCase.execute(ID_RECETA);

        // Assert
        verify(recetaRepository).eliminar(argThat(id -> ID_RECETA.equals(id.getValue())));
    }

    @Test
    void eliminarDaDeBajaLaExistenteYRechazaLaInexistente() {
        // Arrange: el pan de esta prueba es MTS, su receta es de produccion y se puede dar de baja.
        EliminarRecetaUseCase useCase = new EliminarRecetaUseCase(recetaRepository, productoRepository);
        hayRecetaDePan();

        // Act
        useCase.execute(ID_RECETA);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

        // Assert
        verify(recetaRepository, times(1)).eliminar(argThat(id -> ID_RECETA.equals(id.getValue())));
        assertEquals(404, error.getCodigo());
    }

    @Test
    void obtenerPorIdDevuelveLaRecetaOResponde404() {
        // Arrange
        ObtenerRecetaPorIdUseCase useCase = new ObtenerRecetaPorIdUseCase(recetaRepository);
        hayRecetaDePan();

        // Act
        RecetaResponse encontrada = useCase.execute(ID_RECETA);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_INEXISTENTE));

        // Assert
        assertEquals("Pan casero", encontrada.getTitulo());
        assertEquals(2, encontrada.getIngredientes().size());
        assertEquals(404, error.getCodigo());
    }

    @Test
    void elListadoConvierteCadaRecetaYConservaLaPaginacion() {
        // Arrange
        ObtenerRecetasUseCase useCase = new ObtenerRecetasUseCase(recetaRepository);
        Receta pan = hayRecetaDePan();
        when(recetaRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(pan), 0, 10, 1, false));

        // Act
        PaginatedResponse<List<RecetaResponse>> pagina = useCase.execute(ObtenerRecetasRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals(ID_RECETA, pagina.getData().get(0).getId());
        assertEquals(1, pagina.getTotal());
        assertFalse(pagina.isHasMore());
    }
}
