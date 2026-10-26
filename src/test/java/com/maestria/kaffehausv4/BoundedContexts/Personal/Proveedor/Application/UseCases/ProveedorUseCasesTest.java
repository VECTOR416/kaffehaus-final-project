package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ActualizarProveedorRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.CrearProveedorRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ObtenerProveedoresRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ProveedorResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;

/** Pruebas unitarias de los casos de uso de Proveedor: una persona, con o sin entidad detras. */
@ExtendWith(MockitoExtension.class)
class ProveedorUseCasesTest {

    private static final String ID_PROVEEDOR = "af000000-0000-0000-0000-000000000001";
    private static final String ID_ENTIDAD = "af000000-0000-0000-0000-000000000002";
    private static final String ID_OTRA_ENTIDAD = "af000000-0000-0000-0000-000000000003";

    @Mock private IProveedorRepository proveedorRepository;
    @Mock private IPersonaRepository personaRepository;
    @Mock private IEntidadRepository entidadRepository;

    private static final String ID_PERSONA = "ab000000-0000-0000-0000-000000000001";
    private static final String ID_PERSONA_NUEVA = "ab000000-0000-0000-0000-0000000000ff";

    private static Persona ana() {
        return Persona.restaurar(ID_PERSONA, "Ana", "Rojas", "70000000", "1234567", false);
    }

    /** La persistencia simulada devuelve la persona nueva con id y la actualizada tal cual. */
    private void prepararPersonas() {
        lenient().when(personaRepository.crear(any())).thenAnswer(llamada -> {
            Persona nueva = llamada.getArgument(0);
            return Persona.restaurar(ID_PERSONA_NUEVA, nueva.getNombres(), nueva.getApellidos(), nueva.getTelefono(), nueva.getCarnet(), false);
        });
        lenient().when(personaRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
    }

    private static Map<String, Object> datosDe(String nombres, String apellidos) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombres", nombres);
        datos.put("apellidos", apellidos);
        datos.put("telefono", "71111111");
        datos.put("carnet", "7654321");
        return datos;
    }

    @BeforeEach
    void prepararPersistencia() {
        prepararPersonas();
        lenient().when(proveedorRepository.crear(any())).thenAnswer(llamada -> {
            Proveedor nuevo = llamada.getArgument(0);
            return Proveedor.restaurar(ID_PROVEEDOR, nuevo.getPersonaId(), nuevo.getEntidadId(), null, null, false);
        });
        lenient().when(proveedorRepository.actualizar(any(), any())).thenAnswer(llamada -> llamada.getArgument(1));
        lenient().when(entidadRepository.obtenerPorId(argThat(id -> id != null && ID_ENTIDAD.equals(id.getValue()))))
                .thenReturn(Optional.of(Entidad.restaurar(ID_ENTIDAD, "Tostadora Andina", "102", null, null, false)));
        lenient().when(entidadRepository.obtenerPorId(argThat(id -> id != null && ID_OTRA_ENTIDAD.equals(id.getValue()))))
                .thenReturn(Optional.of(Entidad.restaurar(ID_OTRA_ENTIDAD, "Lacteos del Valle", "103", null, null, false)));
    }

    private static Proveedor proveedor(String idEntidad) {
        return Proveedor.restaurar(ID_PROVEEDOR, new PersonaId(ID_PERSONA), idEntidad != null ? new EntidadId(idEntidad) : null, null, null, false);
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearSinEntidadRegistraLaPersonaYElProveedor() {
        // Arrange
        CrearProveedorUseCase useCase = new CrearProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);

        // Act
        ProveedorResponse respuesta = useCase.execute(CrearProveedorRequest.createFromRequest(datosDe("Luis", "Mamani")));

        // Assert
        assertEquals(ID_PROVEEDOR, respuesta.getId());
        assertEquals("Luis", respuesta.getPersona().getNombres());
        assertNull(respuesta.getEntidad());
        verify(personaRepository).crear(any());
        verify(proveedorRepository).crear(argThat(nuevo ->
                ID_PERSONA_NUEVA.equals(nuevo.getPersonaId().getValue()) && nuevo.getEntidadId() == null));
    }

    @Test
    void crearConEntidadLaVinculaAlProveedor() {
        // Arrange
        CrearProveedorUseCase useCase = new CrearProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);
        Map<String, Object> datos = datosDe("Luis", "Mamani");
        datos.put("idEntidad", ID_ENTIDAD);

        // Act
        ProveedorResponse respuesta = useCase.execute(CrearProveedorRequest.createFromRequest(datos));

        // Assert
        assertEquals("Tostadora Andina", respuesta.getEntidad().getNombre());
        verify(proveedorRepository).crear(argThat(nuevo -> ID_ENTIDAD.equals(nuevo.getEntidadId().getValue())));
    }

    @Test
    void crearConUnaEntidadInexistenteNoRegistraNada() {
        // Arrange
        CrearProveedorUseCase useCase = new CrearProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);
        Map<String, Object> datos = datosDe("Luis", "Mamani");
        datos.put("idEntidad", "af000000-0000-0000-0000-0000000000ee");

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(CrearProveedorRequest.createFromRequest(datos)));

        // Assert
        assertEquals(404, error.getCodigo());
        verifyNoInteractions(personaRepository);
        verify(proveedorRepository, never()).crear(any());
    }

    @Test
    void crearConUnaPersonaExistenteLaReutilizaYActualizaSusDatos() {
        // Arrange
        CrearProveedorUseCase useCase = new CrearProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));
        Map<String, Object> datos = datosDe(null, "Rojas Paz");
        datos.put("idPersona", ID_PERSONA);

        // Act
        ProveedorResponse respuesta = useCase.execute(CrearProveedorRequest.createFromRequest(datos));

        // Assert
        assertEquals(ID_PERSONA, respuesta.getPersona().getIdPersona());
        assertEquals("Ana", respuesta.getPersona().getNombres());
        assertEquals("Rojas Paz", respuesta.getPersona().getApellidos());
        verify(personaRepository, never()).crear(any());
    }

    @Test
    void crearConUnIdDePersonaQueNoExisteRegistraUnaNueva() {
        // Arrange
        CrearProveedorUseCase useCase = new CrearProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        Map<String, Object> datos = datosDe("Luis", "Mamani");
        datos.put("idPersona", ID_PERSONA);

        // Act
        ProveedorResponse respuesta = useCase.execute(CrearProveedorRequest.createFromRequest(datos));

        // Assert
        assertEquals(ID_PERSONA_NUEVA, respuesta.getPersona().getIdPersona());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarCambiaLosDatosDeLaPersonaYConservaLaEntidadSiNoSeIndicaOtra() {
        // Arrange
        ActualizarProveedorUseCase useCase = new ActualizarProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(proveedorRepository.obtenerPorId(any())).thenReturn(Optional.of(proveedor(ID_ENTIDAD)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));

        // Act
        ProveedorResponse respuesta = useCase.execute(ID_PROVEEDOR, ActualizarProveedorRequest.createFromRequest(datosDe("Ana Maria", null)));

        // Assert
        assertEquals("Ana Maria", respuesta.getPersona().getNombres());
        assertEquals("Tostadora Andina", respuesta.getEntidad().getNombre());
        verify(proveedorRepository).actualizar(any(), argThat(guardado -> ID_ENTIDAD.equals(guardado.getEntidadId().getValue())));
    }

    @Test
    void actualizarPuedeCambiarLaEntidadDelProveedor() {
        // Arrange
        ActualizarProveedorUseCase useCase = new ActualizarProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(proveedorRepository.obtenerPorId(any())).thenReturn(Optional.of(proveedor(ID_ENTIDAD)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));
        Map<String, Object> datos = datosDe(null, null);
        datos.put("idEntidad", ID_OTRA_ENTIDAD);

        // Act
        ProveedorResponse respuesta = useCase.execute(ID_PROVEEDOR, ActualizarProveedorRequest.createFromRequest(datos));

        // Assert
        assertEquals("Lacteos del Valle", respuesta.getEntidad().getNombre());
        verify(proveedorRepository).actualizar(any(), argThat(guardado -> ID_OTRA_ENTIDAD.equals(guardado.getEntidadId().getValue())));
    }

    @Test
    void actualizarUnProveedorSinEntidadLoDejaSinEntidad() {
        // Arrange
        ActualizarProveedorUseCase useCase = new ActualizarProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(proveedorRepository.obtenerPorId(any())).thenReturn(Optional.of(proveedor(null)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));

        // Act
        ProveedorResponse respuesta = useCase.execute(ID_PROVEEDOR, ActualizarProveedorRequest.createFromRequest(datosDe("Ana", null)));

        // Assert
        assertNull(respuesta.getEntidad());
        verifyNoInteractions(entidadRepository);
    }

    @Test
    void actualizarConUnaEntidadInexistenteNoGuardaElProveedor() {
        // Arrange
        ActualizarProveedorUseCase useCase = new ActualizarProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(proveedorRepository.obtenerPorId(any())).thenReturn(Optional.of(proveedor(null)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));
        Map<String, Object> datos = datosDe(null, null);
        datos.put("idEntidad", "af000000-0000-0000-0000-0000000000ee");

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID_PROVEEDOR, ActualizarProveedorRequest.createFromRequest(datos)));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(proveedorRepository, never()).actualizar(any(), any());
    }

    @Test
    void actualizarRespondeNoEncontradoSiFaltaElProveedorOSuPersona() {
        // Arrange
        ActualizarProveedorUseCase useCase = new ActualizarProveedorUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(proveedorRepository.obtenerPorId(any())).thenReturn(Optional.empty()).thenReturn(Optional.of(proveedor(null)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        ActualizarProveedorRequest peticion = ActualizarProveedorRequest.createFromRequest(datosDe("x", "y"));

        // Act
        DomainException sinProveedor = assertThrows(DomainException.class, () -> useCase.execute(ID_PROVEEDOR, peticion));
        DomainException sinPersona = assertThrows(DomainException.class, () -> useCase.execute(ID_PROVEEDOR, peticion));

        // Assert
        assertEquals(404, sinProveedor.getCodigo());
        assertEquals(404, sinPersona.getCodigo());
        verify(proveedorRepository, never()).actualizar(any(), any());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminarDaDeBajaElProveedorExistente() {
        // Arrange
        EliminarProveedorUseCase useCase = new EliminarProveedorUseCase(proveedorRepository);
        when(proveedorRepository.obtenerPorId(any())).thenReturn(Optional.of(proveedor(null)));

        // Act
        useCase.execute(ID_PROVEEDOR);

        // Assert
        verify(proveedorRepository).eliminar(argThat(id -> ID_PROVEEDOR.equals(id.getValue())));
    }

    @Test
    void eliminarUnProveedorInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarProveedorUseCase useCase = new EliminarProveedorUseCase(proveedorRepository);
        when(proveedorRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_PROVEEDOR));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(proveedorRepository, never()).eliminar(any());
    }

    // ------------------------------------------------------------------ consultas

    @Test
    void obtenerPorIdDevuelveElProveedorConSuPersonaYSuEntidad() {
        // Arrange
        ObtenerProveedorPorIdUseCase useCase = new ObtenerProveedorPorIdUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(proveedorRepository.obtenerPorId(any())).thenReturn(Optional.of(proveedor(ID_ENTIDAD)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));

        // Act
        ProveedorResponse respuesta = useCase.execute(ID_PROVEEDOR);

        // Assert
        assertEquals("Ana", respuesta.getPersona().getNombres());
        assertEquals("Tostadora Andina", respuesta.getEntidad().getNombre());
    }

    @Test
    void obtenerPorIdRespondeNoEncontradoSiFaltaElProveedorSuPersonaOSuEntidad() {
        // Arrange
        ObtenerProveedorPorIdUseCase useCase = new ObtenerProveedorPorIdUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(proveedorRepository.obtenerPorId(any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(proveedor(null)))
                .thenReturn(Optional.of(proveedor("af000000-0000-0000-0000-0000000000ee")));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty()).thenReturn(Optional.of(ana()));

        // Act
        DomainException sinProveedor = assertThrows(DomainException.class, () -> useCase.execute(ID_PROVEEDOR));
        DomainException sinPersona = assertThrows(DomainException.class, () -> useCase.execute(ID_PROVEEDOR));
        DomainException sinEntidad = assertThrows(DomainException.class, () -> useCase.execute(ID_PROVEEDOR));

        // Assert
        assertEquals(404, sinProveedor.getCodigo());
        assertEquals(404, sinPersona.getCodigo());
        assertEquals(404, sinEntidad.getCodigo());
    }

    @Test
    void elListadoResuelvePersonaYEntidadDeCadaProveedor() {
        // Arrange
        ObtenerProveedoresUseCase useCase = new ObtenerProveedoresUseCase(proveedorRepository, personaRepository, entidadRepository);
        when(proveedorRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(
                List.of(proveedor(ID_ENTIDAD), proveedor(null)), 0, 10, 2, false));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));

        // Act
        PaginatedResponse<List<ProveedorResponse>> pagina = useCase.execute(ObtenerProveedoresRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals(2, pagina.getData().size());
        assertNotNull(pagina.getData().get(0).getEntidad());
        assertNull(pagina.getData().get(1).getEntidad());
        assertEquals(2, pagina.getTotal());
    }
}
