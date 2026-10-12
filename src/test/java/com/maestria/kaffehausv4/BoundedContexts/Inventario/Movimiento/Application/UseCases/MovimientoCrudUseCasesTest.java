package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Soporte.DatosDePrueba;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ActualizarMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.CrearMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.MovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ObtenerMovimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories.IProduccionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

/**
 * Pruebas unitarias de los casos de uso CRUD de Movimiento. Un movimiento es auditoria: se
 * puede registrar y consultar, pero no modificar.
 */
@ExtendWith(MockitoExtension.class)
class MovimientoCrudUseCasesTest {

    private static final String ID = "c5000000-0000-0000-0000-000000000001";
    private static final String ID_EXISTENCIA = "c5000000-0000-0000-0000-000000000002";
    private static final String ID_PRODUCCION = "c5000000-0000-0000-0000-000000000003";

    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private IExistenciaRepository existenciaRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IProduccionRepository produccionRepository;

    @BeforeEach
    void prepararCatalogos() {
        lenient().when(tipoMovimientoRepository.obtenerPorId(argThat(id -> id != null && ID_TIPO_AJUSTE.equals(id.getValue()))))
                .thenReturn(Optional.of(AJUSTE));
        lenient().when(existenciaRepository.obtenerPorId(any()))
                .thenReturn(Optional.of(existencia(ID_EXISTENCIA, "c5000000-0000-0000-0000-000000000009", "10", "10", null)));
        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
        lenient().when(movimientoRepository.crear(any())).thenAnswer(llamada -> DatosDePrueba.guardado(llamada.getArgument(0), ID));
    }

    private static Movimiento registrado() {
        return Movimiento.restaurar(ID, new BigDecimal("3.0000"), AHORA, "Conteo", new ExistenciaId(ID_EXISTENCIA),
                new TipoMovimientoId(ID_TIPO_AJUSTE), new UsuarioId(ID_USUARIO), null, false);
    }

    private CrearMovimientoUseCase crear() {
        return new CrearMovimientoUseCase(movimientoRepository, existenciaRepository, tipoMovimientoRepository, usuarioRepository, produccionRepository);
    }

    private static Map<String, Object> alta() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("cantidad", "3");
        datos.put("motivo", "Conteo");
        datos.put("idExistencia", ID_EXISTENCIA);
        datos.put("idTipoMovimiento", ID_TIPO_AJUSTE);
        return datos;
    }

    @Test
    void crearRegistraElMovimientoANombreDeQuienFirma() {
        // Arrange
        ArgumentCaptor<Movimiento> guardado = ArgumentCaptor.forClass(Movimiento.class);

        // Act
        MovimientoResponse respuesta = crear().execute(CrearMovimientoRequest.createFromRequest(alta(), ID_USUARIO));

        // Assert
        verify(movimientoRepository).crear(guardado.capture());
        assertEquals(0, new BigDecimal("3").compareTo(guardado.getValue().getCantidad()));
        assertEquals(ID_USUARIO, guardado.getValue().getUsuarioId().getValue());
        assertNull(guardado.getValue().getProduccionId());
        assertNotNull(guardado.getValue().getFechaMovimiento());
        assertEquals(ID, respuesta.getId());
        assertEquals("Ajuste", respuesta.getTipoMovimiento().getNombre());
        assertEquals(ID_EXISTENCIA, respuesta.getIdExistencia());
    }

    @Test
    void crearIgnoraUnUsuarioEnviadoEnElCuerpo() {
        // Arrange
        Map<String, Object> datos = alta();
        datos.put("idUsuario", "c5000000-0000-0000-0000-0000000000aa");

        // Act
        MovimientoResponse respuesta = crear().execute(CrearMovimientoRequest.createFromRequest(datos, ID_USUARIO));

        // Assert
        assertEquals(ID_USUARIO, respuesta.getIdUsuario());
    }

    @Test
    void crearPuedeVincularElMovimientoAUnaProduccionExistente() {
        // Arrange
        Map<String, Object> datos = alta();
        datos.put("idProduccion", ID_PRODUCCION);
        when(produccionRepository.obtenerPorId(any())).thenReturn(Optional.of(Produccion.restaurar(ID_PRODUCCION,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, null, AHORA, new RecetaId("c5000000-0000-0000-0000-000000000004"), false)));

        // Act
        MovimientoResponse respuesta = crear().execute(CrearMovimientoRequest.createFromRequest(datos, ID_USUARIO));

        // Assert
        assertEquals(ID_PRODUCCION, respuesta.getIdProduccion());
    }

    @Test
    void crearValidaQueExistanSusReferencias() {
        // Arrange
        Map<String, Object> conProduccion = alta();
        conProduccion.put("idProduccion", ID_PRODUCCION);
        when(produccionRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        CrearMovimientoRequest peticion = CrearMovimientoRequest.createFromRequest(alta(), ID_USUARIO);

        // Act
        DomainException sinProduccion = assertThrows(DomainException.class,
                () -> crear().execute(CrearMovimientoRequest.createFromRequest(conProduccion, ID_USUARIO)));
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        DomainException sinUsuario = assertThrows(DomainException.class, () -> crear().execute(peticion));
        when(tipoMovimientoRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        DomainException sinTipo = assertThrows(DomainException.class, () -> crear().execute(peticion));
        when(existenciaRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        DomainException sinExistencia = assertThrows(DomainException.class, () -> crear().execute(peticion));

        // Assert
        assertEquals(404, sinProduccion.getCodigo());
        assertEquals(404, sinUsuario.getCodigo());
        assertEquals(404, sinTipo.getCodigo());
        assertEquals(404, sinExistencia.getCodigo());
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void crearRechazaUnaCantidadNoPositiva() {
        // Arrange
        Map<String, Object> datos = alta();
        datos.put("cantidad", "0");

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> crear().execute(CrearMovimientoRequest.createFromRequest(datos, ID_USUARIO)));

        // Assert
        assertEquals(400, error.getCodigo());
        verify(movimientoRepository, never()).crear(any());
    }

    @Test
    void laPeticionDeAltaExigeCantidadExistenciaTipoYUsuario() {
        // Arrange
        Map<String, Object> sinCantidad = alta();
        sinCantidad.remove("cantidad");
        Map<String, Object> sinExistencia = alta();
        sinExistencia.remove("idExistencia");

        // Act
        DomainException porCantidad = assertThrows(DomainException.class, () -> CrearMovimientoRequest.createFromRequest(sinCantidad, ID_USUARIO));
        DomainException porExistencia = assertThrows(DomainException.class, () -> CrearMovimientoRequest.createFromRequest(sinExistencia, ID_USUARIO));
        DomainException porUsuario = assertThrows(DomainException.class, () -> CrearMovimientoRequest.createFromRequest(alta(), null));

        // Assert
        assertEquals(400, porCantidad.getCodigo());
        assertEquals(400, porExistencia.getCodigo());
        assertEquals(400, porUsuario.getCodigo());
    }

    @Test
    void unMovimientoRegistradoNoSePuedeModificar() {
        // Arrange
        ActualizarMovimientoUseCase useCase = new ActualizarMovimientoUseCase(movimientoRepository, tipoMovimientoRepository);
        when(movimientoRepository.obtenerPorId(any())).thenReturn(Optional.of(registrado()));

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarMovimientoRequest.createFromRequest(Map.of("motivo", "Otro"))));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("inmutables"));
        verify(movimientoRepository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarUnoInexistenteRespondeNoEncontrado() {
        // Arrange
        ActualizarMovimientoUseCase useCase = new ActualizarMovimientoUseCase(movimientoRepository, tipoMovimientoRepository);
        when(movimientoRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID, ActualizarMovimientoRequest.createFromRequest(Map.of())));

        // Assert
        assertEquals(404, error.getCodigo());
    }

    @Test
    void eliminarDelegaLaBajaEnElRepositorioYRechazaElInexistente() {
        // Arrange
        EliminarMovimientoUseCase useCase = new EliminarMovimientoUseCase(movimientoRepository);
        when(movimientoRepository.obtenerPorId(any())).thenReturn(Optional.of(registrado())).thenReturn(Optional.empty());

        // Act
        useCase.execute(ID);
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        verify(movimientoRepository, times(1)).eliminar(argThat(id -> ID.equals(id.getValue())));
        assertEquals(404, error.getCodigo());
    }

    @Test
    void obtenerPorIdDevuelveElMovimientoConSuTipo() {
        // Arrange
        ObtenerMovimientoPorIdUseCase useCase = new ObtenerMovimientoPorIdUseCase(movimientoRepository, tipoMovimientoRepository);
        when(movimientoRepository.obtenerPorId(any())).thenReturn(Optional.of(registrado()));

        // Act
        MovimientoResponse respuesta = useCase.execute(ID);

        // Assert
        assertEquals(ID, respuesta.getId());
        assertEquals("Conteo", respuesta.getMotivo());
        assertEquals("Ajuste", respuesta.getTipoMovimiento().getNombre());
    }

    @Test
    void obtenerPorIdResponde404SiFaltaElMovimientoOSuTipo() {
        // Arrange
        ObtenerMovimientoPorIdUseCase useCase = new ObtenerMovimientoPorIdUseCase(movimientoRepository, tipoMovimientoRepository);
        Movimiento conTipoBorrado = Movimiento.restaurar(ID, BigDecimal.ONE, AHORA, "x", new ExistenciaId(ID_EXISTENCIA),
                new TipoMovimientoId("c5000000-0000-0000-0000-0000000000ee"), new UsuarioId(ID_USUARIO), null, false);
        when(movimientoRepository.obtenerPorId(any())).thenReturn(Optional.empty()).thenReturn(Optional.of(conTipoBorrado));

        // Act
        DomainException sinMovimiento = assertThrows(DomainException.class, () -> useCase.execute(ID));
        DomainException sinTipo = assertThrows(DomainException.class, () -> useCase.execute(ID));

        // Assert
        assertEquals(404, sinMovimiento.getCodigo());
        assertEquals(404, sinTipo.getCodigo());
    }

    @Test
    void elListadoResuelveElTipoDeCadaMovimientoYConservaLaPaginacion() {
        // Arrange
        ObtenerMovimientosUseCase useCase = new ObtenerMovimientosUseCase(movimientoRepository, tipoMovimientoRepository);
        when(movimientoRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(List.of(registrado()), 30, 10, 31, false));

        // Act
        PaginatedResponse<List<MovimientoResponse>> pagina = useCase.execute(ObtenerMovimientosRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals("Ajuste", pagina.getData().get(0).getTipoMovimiento().getNombre());
        assertEquals(30, pagina.getSkip());
        assertEquals(31, pagina.getTotal());
        assertFalse(pagina.isHasMore());
    }
}
