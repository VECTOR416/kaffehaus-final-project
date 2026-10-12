package com.maestria.kaffehausv4.Shared.Application.Inicializacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Domain.Aggregates.Categoria;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories.ITipoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Repositories.ITipoPagoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;

/**
 * Pruebas unitarias de la siembra de primer arranque (POST api/v1/seed). Todos los
 * repositorios son dobles de Mockito con una "base" en memoria por catalogo, lo justo para
 * comprobar que datos se siembran y que repetir la llamada no duplica nada.
 */
@ExtendWith(MockitoExtension.class)
class SembrarSistemaUseCaseTest {

    @Mock private IRolRepository rolRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IPersonaRepository personaRepository;
    @Mock private IClienteRepository clienteRepository;
    @Mock private IHasheadorDeContrasena hasheadorDeContrasena;
    @Mock private ITipoMovimientoRepository tipoMovimientoRepository;
    @Mock private IEstadoExistenciaRepository estadoExistenciaRepository;
    @Mock private IEstadoVentaRepository estadoVentaRepository;
    @Mock private ITipoDescuentoRepository tipoDescuentoRepository;
    @Mock private IEstadoPedidoRepository estadoPedidoRepository;
    @Mock private ITipoPagoRepository tipoPagoRepository;
    @Mock private ITipoRepository tipoRepository;
    @Mock private ICategoriaRepository categoriaRepository;
    @Mock private IUnidadRepository unidadRepository;
    @Mock private IConversionRepository conversionRepository;

    private SembrarSistemaUseCase useCase;

    /** Lo sembrado, por nombre, para que una segunda llamada lo encuentre. */
    private final Map<String, Rol> roles = new HashMap<>();
    private final Map<String, Unidad> unidades = new HashMap<>();
    private final List<Conversion> conversiones = new java.util.ArrayList<>();
    private int secuencia;

    @BeforeEach
    void prepararBaseVacia() {
        useCase = new SembrarSistemaUseCase(rolRepository, usuarioRepository, personaRepository, clienteRepository,
                hasheadorDeContrasena, tipoMovimientoRepository, estadoExistenciaRepository, estadoVentaRepository,
                tipoDescuentoRepository, estadoPedidoRepository, tipoPagoRepository, tipoRepository, categoriaRepository,
                unidadRepository, conversionRepository);

        // Roles, unidades y conversiones se recuerdan porque el propio caso de uso los vuelve a leer.
        lenient().when(rolRepository.obtenerPorNombre(anyString())).thenAnswer(llamada -> Optional.ofNullable(roles.get(llamada.<String>getArgument(0))));
        lenient().when(rolRepository.crear(any())).thenAnswer(llamada -> {
            Rol creado = Rol.restaurar(id(), ((Rol) llamada.getArgument(0)).getNombre(), false);
            roles.put(creado.getNombre(), creado);
            return creado;
        });
        lenient().when(unidadRepository.obtenerPorNombre(anyString())).thenAnswer(llamada -> Optional.ofNullable(unidades.get(llamada.<String>getArgument(0))));
        lenient().when(unidadRepository.crear(any())).thenAnswer(llamada -> {
            Unidad nueva = llamada.getArgument(0);
            Unidad creada = Unidad.restaurar(id(), nueva.getUnidad(), nueva.getAbreviatura(), nueva.isEsBorrable(), false);
            unidades.put(creada.getUnidad(), creada);
            return creada;
        });
        lenient().when(conversionRepository.obtenerVivas()).thenAnswer(llamada -> List.copyOf(conversiones));
        lenient().when(conversionRepository.crear(any())).thenAnswer(llamada -> {
            conversiones.add(llamada.getArgument(0));
            return llamada.getArgument(0);
        });

        // El resto de catalogos arranca vacio.
        lenient().when(tipoMovimientoRepository.obtenerPorNombre(anyString())).thenReturn(Optional.empty());
        lenient().when(estadoExistenciaRepository.obtenerPorNombre(anyString())).thenReturn(Optional.empty());
        lenient().when(estadoVentaRepository.obtenerPorNombre(anyString())).thenReturn(Optional.empty());
        lenient().when(tipoDescuentoRepository.obtenerPorNombre(anyString())).thenReturn(Optional.empty());
        lenient().when(estadoPedidoRepository.obtenerPorNombre(anyString())).thenReturn(Optional.empty());
        lenient().when(tipoPagoRepository.obtenerPorNombre(anyString())).thenReturn(Optional.empty());
        lenient().when(tipoRepository.obtenerPorNombre(anyString())).thenReturn(Optional.empty());
        lenient().when(categoriaRepository.obtenerPorNombre(anyString())).thenReturn(Optional.empty());
        lenient().when(usuarioRepository.obtenerPorCorreo(anyString())).thenReturn(Optional.empty());
        lenient().when(clienteRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        lenient().when(personaRepository.obtenerPorId(any())).thenReturn(Optional.empty());
        lenient().when(personaRepository.crear(any())).thenAnswer(llamada -> {
            Persona nueva = llamada.getArgument(0);
            String idPersona = nueva.getId() != null ? nueva.getId().getValue() : id();
            return Persona.restaurar(idPersona, nueva.getNombres(), nueva.getApellidos(), nueva.getTelefono(), nueva.getCarnet(), false);
        });
        lenient().when(hasheadorDeContrasena.hashear(anyString())).thenAnswer(llamada -> "hash(" + llamada.getArgument(0) + ")");
    }

    private String id() {
        return String.format("5e000000-0000-0000-0000-%012d", ++secuencia);
    }

    private static <T> List<T> capturados(ArgumentCaptor<T> captor) {
        return captor.getAllValues();
    }

    // ------------------------------------------------------------------ que se siembra

    @Test
    void losTiposDeMovimientoSonSoloRecetaAjusteYMerma() {
        // Arrange
        ArgumentCaptor<TipoMovimiento> sembrados = ArgumentCaptor.forClass(TipoMovimiento.class);

        // Act
        useCase.execute();

        // Assert
        verify(tipoMovimientoRepository, times(3)).crear(sembrados.capture());
        assertEquals(List.of("Receta", "Ajuste", "Merma"), capturados(sembrados).stream().map(TipoMovimiento::getNombre).toList());
    }

    @Test
    void losTiposDeProductoSonSoloProductoEInsumo() {
        // Arrange
        ArgumentCaptor<Tipo> sembrados = ArgumentCaptor.forClass(Tipo.class);

        // Act
        useCase.execute();

        // Assert
        verify(tipoRepository, times(2)).crear(sembrados.capture());
        assertEquals(List.of("Producto", "Insumo"), capturados(sembrados).stream().map(Tipo::getNombre).toList());
    }

    @Test
    void siembraLosCatalogosQueElNegocioResuelvePorNombre() {
        // Arrange
        ArgumentCaptor<EstadoExistencia> estadosExistencia = ArgumentCaptor.forClass(EstadoExistencia.class);
        ArgumentCaptor<EstadoVenta> estadosVenta = ArgumentCaptor.forClass(EstadoVenta.class);
        ArgumentCaptor<EstadoPedido> estadosPedido = ArgumentCaptor.forClass(EstadoPedido.class);
        ArgumentCaptor<TipoDescuento> tiposDescuento = ArgumentCaptor.forClass(TipoDescuento.class);
        ArgumentCaptor<TipoPago> tiposPago = ArgumentCaptor.forClass(TipoPago.class);

        // Act
        useCase.execute();

        // Assert
        verify(estadoExistenciaRepository, times(2)).crear(estadosExistencia.capture());
        verify(estadoVentaRepository, times(2)).crear(estadosVenta.capture());
        verify(estadoPedidoRepository, times(5)).crear(estadosPedido.capture());
        verify(tipoDescuentoRepository, times(2)).crear(tiposDescuento.capture());
        verify(tipoPagoRepository, times(3)).crear(tiposPago.capture());
        assertEquals(List.of("Abierta", "Agotada"), capturados(estadosExistencia).stream().map(EstadoExistencia::getNombre).toList());
        assertEquals(List.of("Completada", "Anulada"), capturados(estadosVenta).stream().map(EstadoVenta::getNombre).toList());
        assertEquals(List.of("Recibido", "En preparacion", "En camino", "Entregado", "Cancelado"),
                capturados(estadosPedido).stream().map(EstadoPedido::getNombre).toList());
        assertEquals(List.of("Porcentaje", "Monto fijo"), capturados(tiposDescuento).stream().map(TipoDescuento::getNombre).toList());
        assertEquals(List.of("Efectivo", "Tarjeta", "QR"), capturados(tiposPago).stream().map(TipoPago::getNombre).toList());
    }

    @Test
    void siembraLasCategoriasPorDefectoComoBorrables() {
        // Arrange
        ArgumentCaptor<Categoria> sembradas = ArgumentCaptor.forClass(Categoria.class);

        // Act
        useCase.execute();

        // Assert
        verify(categoriaRepository, times(4)).crear(sembradas.capture());
        assertEquals(List.of("Cafes", "Bebidas", "Pasteleria", "Insumos"), capturados(sembradas).stream().map(Categoria::getNombre).toList());
        assertTrue(capturados(sembradas).stream().allMatch(Categoria::isEsBorrable));
    }

    @Test
    void siembraLasUnidadesEstandarYSusDosConversiones() {
        // Act
        useCase.execute();

        // Assert
        assertEquals(java.util.Set.of("Kilogramo", "Gramo", "Litro", "Mililitro", "Unidad"), unidades.keySet());
        assertEquals("kg", unidades.get("Kilogramo").getAbreviatura());
        assertEquals("und", unidades.get("Unidad").getAbreviatura());
        assertTrue(unidades.values().stream().noneMatch(Unidad::isEsBorrable));

        assertEquals(2, conversiones.size());
        Conversion kiloAGramo = conversiones.get(0);
        assertEquals(unidades.get("Kilogramo").getId().getValue(), kiloAGramo.getUnidadOrigenId().getValue());
        assertEquals(unidades.get("Gramo").getId().getValue(), kiloAGramo.getUnidadDestinoId().getValue());
        assertEquals(0, new BigDecimal("1000").compareTo(kiloAGramo.getFactorConversion()));
        assertFalse(kiloAGramo.isEsBorrable());
        Conversion litroAMililitro = conversiones.get(1);
        assertEquals(unidades.get("Litro").getId().getValue(), litroAMililitro.getUnidadOrigenId().getValue());
        assertEquals(unidades.get("Mililitro").getId().getValue(), litroAMililitro.getUnidadDestinoId().getValue());
    }

    @Test
    void siembraLosDosRolesYElAdministradorInicialConLaContrasenaHasheada() {
        // Arrange
        ArgumentCaptor<Usuario> administrador = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<Persona> personas = ArgumentCaptor.forClass(Persona.class);

        // Act
        useCase.execute();

        // Assert
        assertEquals(java.util.Set.of("Administrador", "Operador"), roles.keySet());
        verify(usuarioRepository).crear(administrador.capture());
        assertEquals("admin@gmail.com", administrador.getValue().getCorreo());
        assertEquals("hash(admin)", administrador.getValue().getContrasena());
        assertEquals(roles.get("Administrador").getId().getValue(), administrador.getValue().getRolId().getValue());

        verify(personaRepository, times(2)).crear(personas.capture());
        Persona personaAdministrador = capturados(personas).get(0);
        assertEquals("Administrador", personaAdministrador.getNombres());
        assertEquals("del Sistema", personaAdministrador.getApellidos());
        assertEquals("ADMIN", personaAdministrador.getCarnet());
    }

    @Test
    void siembraElClienteMostradorConSuIdFijoYUnaPersonaDelMismoId() {
        // Arrange
        ArgumentCaptor<Cliente> mostrador = ArgumentCaptor.forClass(Cliente.class);
        ArgumentCaptor<Persona> personas = ArgumentCaptor.forClass(Persona.class);

        // Act
        useCase.execute();

        // Assert
        verify(clienteRepository).crear(mostrador.capture());
        assertEquals(Cliente.ID_MOSTRADOR, mostrador.getValue().getId().getValue());
        assertEquals(Cliente.ID_MOSTRADOR, mostrador.getValue().getPersona().getValue());
        assertTrue(mostrador.getValue().esMostrador());

        verify(personaRepository, times(2)).crear(personas.capture());
        Persona personaMostrador = capturados(personas).get(1);
        assertEquals(Cliente.ID_MOSTRADOR, personaMostrador.getId().getValue());
        assertEquals("Cliente", personaMostrador.getNombres());
        assertEquals("Casual / Mostrador", personaMostrador.getApellidos());
        assertNull(personaMostrador.getTelefono());
    }

    @Test
    void laRespuestaListaTodoLoCreadoEnUnaBaseVacia() {
        // Act
        SembrarSistemaResponse respuesta = useCase.execute();

        // Assert: 2 roles + admin + 3 + 2 + 2 + 2 + 5 + 3 + 2 + 4 catalogos + 5 unidades + 2 conversiones + mostrador.
        assertEquals(34, respuesta.getCreados().size());
        assertTrue(respuesta.getExistentes().isEmpty());
        assertTrue(respuesta.getCreados().contains("Tipo de movimiento 'Receta'"));
        assertTrue(respuesta.getCreados().contains("Tipo de producto 'Insumo'"));
        assertTrue(respuesta.getCreados().contains("Usuario 'admin@gmail.com'"));
        assertTrue(respuesta.getCreados().contains("Conversion 'Kilogramo -> Gramo'"));
        assertTrue(respuesta.getCreados().contains("Cliente 'Mostrador'"));
    }

    // ------------------------------------------------------------------ idempotencia

    @Test
    void siElAdministradorYaExisteNoSeTocaNiSeReescribeSuContrasena() {
        // Arrange
        when(usuarioRepository.obtenerPorCorreo("admin@gmail.com")).thenReturn(Optional.of(
                Usuario.restaurar(id(), id(), "admin@gmail.com", "hash-cambiado-por-el-usuario", false, null)));

        // Act
        SembrarSistemaResponse respuesta = useCase.execute();

        // Assert
        verify(usuarioRepository, never()).crear(any());
        verify(hasheadorDeContrasena, never()).hashear(anyString());
        assertTrue(respuesta.getExistentes().contains("Usuario 'admin@gmail.com'"));
        assertFalse(respuesta.getCreados().contains("Usuario 'admin@gmail.com'"));
    }

    @Test
    void unCatalogoQueYaExisteNoSeVuelveACrear() {
        // Arrange
        when(tipoMovimientoRepository.obtenerPorNombre("Merma")).thenReturn(Optional.of(TipoMovimiento.restaurar(id(), "Merma", false)));
        ArgumentCaptor<TipoMovimiento> sembrados = ArgumentCaptor.forClass(TipoMovimiento.class);

        // Act
        SembrarSistemaResponse respuesta = useCase.execute();

        // Assert
        verify(tipoMovimientoRepository, times(2)).crear(sembrados.capture());
        assertEquals(List.of("Receta", "Ajuste"), capturados(sembrados).stream().map(TipoMovimiento::getNombre).toList());
        assertTrue(respuesta.getExistentes().contains("Tipo de movimiento 'Merma'"));
    }

    @Test
    void siElMostradorYaExisteNoSeCreaOtroNiOtraPersona() {
        // Arrange
        when(clienteRepository.obtenerPorId(any())).thenReturn(Optional.of(Cliente.restaurar(Cliente.ID_MOSTRADOR, Cliente.ID_MOSTRADOR, false)));

        // Act
        SembrarSistemaResponse respuesta = useCase.execute();

        // Assert: solo se crea la persona del administrador.
        verify(clienteRepository, never()).crear(any());
        verify(personaRepository, times(1)).crear(any());
        assertTrue(respuesta.getExistentes().contains("Cliente 'Mostrador'"));
    }

    @Test
    void siLaPersonaDelMostradorYaExisteSoloSeCreaElCliente() {
        // Arrange
        when(personaRepository.obtenerPorId(any())).thenReturn(Optional.of(
                Persona.restaurar(Cliente.ID_MOSTRADOR, "Cliente", "Casual / Mostrador", null, "0", false)));

        // Act
        useCase.execute();

        // Assert
        verify(clienteRepository).crear(any());
        verify(personaRepository, times(1)).crear(any());
    }

    @Test
    void sembrarDosVecesNoDuplicaRolesUnidadesNiConversiones() {
        // Act
        useCase.execute();
        SembrarSistemaResponse segunda = useCase.execute();

        // Assert
        verify(rolRepository, times(2)).crear(any());
        verify(unidadRepository, times(5)).crear(any());
        verify(conversionRepository, times(2)).crear(any());
        assertTrue(segunda.getExistentes().contains("Rol 'Administrador'"));
        assertTrue(segunda.getExistentes().contains("Unidad 'Gramo'"));
        assertTrue(segunda.getExistentes().contains("Conversion 'Litro -> Mililitro'"));
    }
}
