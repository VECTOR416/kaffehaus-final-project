package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import static com.maestria.kaffehausv4.Soporte.DatosDePrueba.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarAperturaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases.RegistrarAperturaUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.AbrirDetalleLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/** Pruebas unitarias de la apertura manual de una linea de lote, que exige respetar el orden FEFO. */
@ExtendWith(MockitoExtension.class)
class AbrirDetalleLoteUseCaseTest {

    private static final String ID_BOLSA = "f0000000-0000-0000-0000-000000000001";
    private static final String ID_DETALLE = "f2000000-0000-0000-0000-000000000002";
    private static final String ID_DETALLE_ANTERIOR = "f2000000-0000-0000-0000-000000000001";
    private static final LocalDate EN_UN_MES = LocalDate.now().plusDays(30);

    @Mock private ILoteRepository loteRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private RegistrarAperturaUseCase registrarAperturaUseCase;

    private AbrirDetalleLoteUseCase useCase;

    @BeforeEach
    void crearCasoDeUso() {
        useCase = new AbrirDetalleLoteUseCase(loteRepository, usuarioRepository, registrarAperturaUseCase);
        lenient().when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.of(usuario()));
    }

    private static AbrirDetalleLoteRequest peticion() {
        return AbrirDetalleLoteRequest.createFromRequest(Map.of(), ID_USUARIO);
    }

    private void hayLotes(Lote delDetalle, Lote... otros) {
        List<Lote> todos = new ArrayList<>(List.of(otros));
        todos.add(delDetalle);
        lenient().when(loteRepository.obtenerPorDetalleIdParaActualizar(any())).thenReturn(Optional.of(delDetalle));
        lenient().when(loteRepository.obtenerPorProductos(anyList())).thenReturn(todos);
    }

    @Test
    void abreElDetalleCuandoEsElQueTocaYDelegaEnLaApertura() {
        // Arrange
        hayLotes(loteDeCompra("f1000000-0000-0000-0000-000000000001", detalle(ID_DETALLE, ID_BOLSA, "5", "5", "50", EN_UN_MES)));
        ArgumentCaptor<RegistrarAperturaRequest> apertura = ArgumentCaptor.forClass(RegistrarAperturaRequest.class);

        // Act
        useCase.execute(ID_DETALLE, peticion());

        // Assert
        verify(registrarAperturaUseCase).execute(apertura.capture());
        assertEquals(ID_DETALLE, apertura.getValue().getIdDetalleLote());
        assertEquals(ID_USUARIO, apertura.getValue().getIdUsuario());
        assertFalse(apertura.getValue().isPermitirVencido());
    }

    @Test
    void siOtraLineaVenceAntesHayQueAbrirEsaPrimero() {
        // Arrange
        LocalDate antes = EN_UN_MES.minusDays(10);
        hayLotes(
                loteDeCompra("f1000000-0000-0000-0000-000000000001", detalle(ID_DETALLE, ID_BOLSA, "5", "5", "50", EN_UN_MES)),
                loteDeCompra("f1000000-0000-0000-0000-000000000002", detalle(ID_DETALLE_ANTERIOR, ID_BOLSA, "5", "5", "50", antes)));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion()));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains(ID_DETALLE_ANTERIOR));
        assertTrue(error.getMensaje().contains(antes.toString()));
        verifyNoInteractions(registrarAperturaUseCase);
    }

    @Test
    void lasLineasVencidasOSinUnidadesNoBloqueanElOrden() {
        // Arrange: la otra linea vence antes pero ya no tiene unidades cerradas.
        hayLotes(
                loteDeCompra("f1000000-0000-0000-0000-000000000001", detalle(ID_DETALLE, ID_BOLSA, "5", "5", "50", EN_UN_MES)),
                loteDeCompra("f1000000-0000-0000-0000-000000000002", detalle(ID_DETALLE_ANTERIOR, ID_BOLSA, "5", "0", "50", EN_UN_MES.minusDays(10))));

        // Act
        useCase.execute(ID_DETALLE, peticion());

        // Assert
        verify(registrarAperturaUseCase).execute(any());
    }

    @Test
    void unDetalleVencidoNoSeAbre() {
        // Arrange
        hayLotes(loteDeCompra("f1000000-0000-0000-0000-000000000001", detalle(ID_DETALLE, ID_BOLSA, "5", "5", "50", LocalDate.now().minusDays(1))));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion()));

        // Assert
        assertEquals(409, error.getCodigo());
        assertTrue(error.getMensaje().contains("esta vencido"));
        verifyNoInteractions(registrarAperturaUseCase);
    }

    @Test
    void unDetalleSinUnidadesCerradasNoSeAbre() {
        // Arrange
        hayLotes(loteDeCompra("f1000000-0000-0000-0000-000000000001", detalle(ID_DETALLE, ID_BOLSA, "5", "0", "50", EN_UN_MES)));

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion()));

        // Assert
        assertEquals(409, error.getCodigo());
        verifyNoInteractions(registrarAperturaUseCase);
    }

    @Test
    void unUsuarioInexistenteNoPuedeAbrir() {
        // Arrange
        when(usuarioRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion()));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(loteRepository, never()).obtenerPorDetalleIdParaActualizar(any());
    }

    @Test
    void unDetalleInexistenteRespondeNoEncontrado() {
        // Arrange
        when(loteRepository.obtenerPorDetalleIdParaActualizar(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_DETALLE, peticion()));

        // Assert
        assertEquals(404, error.getCodigo());
        verifyNoInteractions(registrarAperturaUseCase);
    }
}
