package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ActualizarMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.CrearMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.MovimientoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ObtenerMovimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

/**
 * Pruebas unitarias de los casos de uso CRUD de Movimiento. Un movimiento es auditoria: solo se
 * consulta; no se crea a mano, no se modifica ni se da de baja.
 */
@ExtendWith(MockitoExtension.class)
class MovimientoCrudUseCasesTest {

    private static final String ID = "c5000000-0000-0000-0000-000000000001";
    private static final String ID_EXISTENCIA = "c5000000-0000-0000-0000-000000000002";

    @Mock private IMovimientoRepository movimientoRepository;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;

    @BeforeEach
    void prepararCatalogos() {
        lenient().when(tipoMovimientoRepository.obtenerPorId(argThat(id -> id != null && ID_TIPO_AJUSTE.equals(id.getValue()))))
                .thenReturn(Optional.of(AJUSTE));
    }

    private static Movimiento registrado() {
        return Movimiento.restaurar(ID, new BigDecimal("3.0000"), AHORA, "Conteo", new ExistenciaId(ID_EXISTENCIA),
                new TipoMovimientoId(ID_TIPO_AJUSTE), new UsuarioId(ID_USUARIO), null, false);
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
    void unMovimientoNoSeCreaAMano() {
        // Arrange
        CrearMovimientoRequest peticion = CrearMovimientoRequest.createFromRequest(alta(), ID_USUARIO);

        // Act
        DomainException error = assertThrows(DomainException.class, () -> new CrearMovimientoUseCase().execute(peticion));

        // Assert
        assertEquals(409, error.getCodigo());
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
