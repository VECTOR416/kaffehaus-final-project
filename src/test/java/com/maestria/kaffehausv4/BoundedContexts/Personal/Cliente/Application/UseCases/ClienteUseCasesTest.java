package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ActualizarClienteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ClienteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.CrearClienteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ObtenerClientesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;

/** Pruebas unitarias de los casos de uso de Cliente: un cliente es una persona con ese papel. */
@ExtendWith(MockitoExtension.class)
class ClienteUseCasesTest {

    private static final String ID_CLIENTE = "ac000000-0000-0000-0000-000000000001";

    @Mock private IClienteRepository clienteRepository;
    @Mock private IPersonaRepository personaRepository;

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
        lenient().when(clienteRepository.crear(any())).thenAnswer(llamada ->
                Cliente.restaurar(ID_CLIENTE, ((Cliente) llamada.getArgument(0)).getPersona().getValue(), false));
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crearSinPersonaRegistraUnaPersonaNuevaYElCliente() {
        // Arrange
        CrearClienteUseCase useCase = new CrearClienteUseCase(clienteRepository, personaRepository);

        // Act
        ClienteResponse respuesta = useCase.execute(CrearClienteRequest.createFromRequest(datosDe("Luis", "Mamani")));

        // Assert
        assertEquals(ID_CLIENTE, respuesta.getId());
        assertEquals(ID_PERSONA_NUEVA, respuesta.getPersona().getIdPersona());
        assertEquals("Luis", respuesta.getPersona().getNombres());
        assertEquals("Mamani", respuesta.getPersona().getApellidos());
        verify(personaRepository).crear(any());
        verify(personaRepository, never()).actualizar(any(), any());
        verify(clienteRepository).crear(argThat(cliente -> ID_PERSONA_NUEVA.equals(cliente.getPersona().getValue())));
    }

    @Test
    void crearConUnaPersonaExistenteLaReutilizaYActualizaSusDatos() {
        // Arrange
        CrearClienteUseCase useCase = new CrearClienteUseCase(clienteRepository, personaRepository);
        when(personaRepository.obtenerPorId(argThat(id -> ID_PERSONA.equals(id.getValue())))).thenReturn(Optional.of(ana()));
        Map<String, Object> datos = datosDe("Ana Maria", null);
        datos.put("idPersona", ID_PERSONA);

        // Act
        ClienteResponse respuesta = useCase.execute(CrearClienteRequest.createFromRequest(datos));

        // Assert
        assertEquals(ID_PERSONA, respuesta.getPersona().getIdPersona());
        assertEquals("Ana Maria", respuesta.getPersona().getNombres());
        assertEquals("Rojas", respuesta.getPersona().getApellidos());
        verify(personaRepository, never()).crear(any());
        verify(personaRepository).actualizar(any(), any());
    }

    @Test
    void crearConUnIdDePersonaQueNoExisteRegistraUnaNueva() {
        // Arrange
        CrearClienteUseCase useCase = new CrearClienteUseCase(clienteRepository, personaRepository);
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        Map<String, Object> datos = datosDe("Luis", "Mamani");
        datos.put("idPersona", ID_PERSONA);

        // Act
        ClienteResponse respuesta = useCase.execute(CrearClienteRequest.createFromRequest(datos));

        // Assert
        assertEquals(ID_PERSONA_NUEVA, respuesta.getPersona().getIdPersona());
        verify(personaRepository).crear(any());
    }

    // ------------------------------------------------------------------ actualizar

    @Test
    void actualizarCambiaLosDatosDeLaPersonaDelCliente() {
        // Arrange
        ActualizarClienteUseCase useCase = new ActualizarClienteUseCase(clienteRepository, personaRepository);
        when(clienteRepository.obtenerPorId(any())).thenReturn(Optional.of(Cliente.restaurar(ID_CLIENTE, ID_PERSONA, false)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));

        // Act
        ClienteResponse respuesta = useCase.execute(ID_CLIENTE, ActualizarClienteRequest.createFromRequest(datosDe(null, "Rojas Vda. de Paz")));

        // Assert
        assertEquals("Ana", respuesta.getPersona().getNombres());
        assertEquals("Rojas Vda. de Paz", respuesta.getPersona().getApellidos());
        assertEquals("71111111", respuesta.getPersona().getTelefono());
        verify(personaRepository).actualizar(argThat(id -> ID_PERSONA.equals(id.getValue())), any());
    }

    @Test
    void actualizarUnClienteInexistenteRespondeNoEncontrado() {
        // Arrange
        ActualizarClienteUseCase useCase = new ActualizarClienteUseCase(clienteRepository, personaRepository);
        when(clienteRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID_CLIENTE, ActualizarClienteRequest.createFromRequest(datosDe("x", "y"))));

        // Assert
        assertEquals(404, error.getCodigo());
        verifyNoInteractions(personaRepository);
    }

    @Test
    void actualizarUnClienteCuyaPersonaNoExisteRespondeNoEncontrado() {
        // Arrange
        ActualizarClienteUseCase useCase = new ActualizarClienteUseCase(clienteRepository, personaRepository);
        when(clienteRepository.obtenerPorId(any())).thenReturn(Optional.of(Cliente.restaurar(ID_CLIENTE, ID_PERSONA, false)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ID_CLIENTE, ActualizarClienteRequest.createFromRequest(datosDe("x", "y"))));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(personaRepository, never()).actualizar(any(), any());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    void eliminarDaDeBajaElClienteExistente() {
        // Arrange
        EliminarClienteUseCase useCase = new EliminarClienteUseCase(clienteRepository);
        when(clienteRepository.obtenerPorId(any())).thenReturn(Optional.of(Cliente.restaurar(ID_CLIENTE, ID_PERSONA, false)));

        // Act
        useCase.execute(ID_CLIENTE);

        // Assert
        verify(clienteRepository).eliminar(argThat(id -> ID_CLIENTE.equals(id.getValue())));
    }

    @Test
    void eliminarUnClienteInexistenteRespondeNoEncontradoYNoBorra() {
        // Arrange
        EliminarClienteUseCase useCase = new EliminarClienteUseCase(clienteRepository);
        when(clienteRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class, () -> useCase.execute(ID_CLIENTE));

        // Assert
        assertEquals(404, error.getCodigo());
        verify(clienteRepository, never()).eliminar(any());
    }

    // ------------------------------------------------------------------ consultas

    @Test
    void obtenerPorIdDevuelveElClienteConSuPersona() {
        // Arrange
        ObtenerClientePorIdUseCase useCase = new ObtenerClientePorIdUseCase(clienteRepository, personaRepository);
        when(clienteRepository.obtenerPorId(any())).thenReturn(Optional.of(Cliente.restaurar(ID_CLIENTE, ID_PERSONA, false)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));

        // Act
        ClienteResponse respuesta = useCase.execute(ID_CLIENTE);

        // Assert
        assertEquals(ID_CLIENTE, respuesta.getId());
        assertEquals("Ana", respuesta.getPersona().getNombres());
        assertEquals("1234567", respuesta.getPersona().getCarnet());
    }

    @Test
    void obtenerPorIdRespondeNoEncontradoSiFaltaElClienteOSuPersona() {
        // Arrange
        ObtenerClientePorIdUseCase useCase = new ObtenerClientePorIdUseCase(clienteRepository, personaRepository);
        when(clienteRepository.obtenerPorId(any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(Cliente.restaurar(ID_CLIENTE, ID_PERSONA, false)));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException sinCliente = assertThrows(DomainException.class, () -> useCase.execute(ID_CLIENTE));
        DomainException sinPersona = assertThrows(DomainException.class, () -> useCase.execute(ID_CLIENTE));

        // Assert
        assertEquals(404, sinCliente.getCodigo());
        assertEquals(404, sinPersona.getCodigo());
    }

    @Test
    void elListadoResuelveLaPersonaDeCadaClienteYConservaLaPaginacion() {
        // Arrange
        ObtenerClientesUseCase useCase = new ObtenerClientesUseCase(clienteRepository, personaRepository);
        when(clienteRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(
                List.of(Cliente.restaurar(ID_CLIENTE, ID_PERSONA, false)), 0, 10, 1, false));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(ana()));

        // Act
        PaginatedResponse<List<ClienteResponse>> pagina = useCase.execute(ObtenerClientesRequest.createFromRequest(Map.of()));

        // Assert
        assertEquals(1, pagina.getData().size());
        assertEquals("Rojas", pagina.getData().get(0).getPersona().getApellidos());
        assertEquals(1, pagina.getTotal());
        assertFalse(pagina.isHasMore());
    }

    @Test
    void elListadoFallaSiUnClienteNoTienePersona() {
        // Arrange
        ObtenerClientesUseCase useCase = new ObtenerClientesUseCase(clienteRepository, personaRepository);
        when(clienteRepository.obtenerTodos(any())).thenReturn(new PaginatedResponse<>(
                List.of(Cliente.restaurar(ID_CLIENTE, ID_PERSONA, false)), 0, 10, 1, false));
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());

        // Act
        DomainException error = assertThrows(DomainException.class,
                () -> useCase.execute(ObtenerClientesRequest.createFromRequest(Map.of())));

        // Assert
        assertEquals(404, error.getCodigo());
    }
}
