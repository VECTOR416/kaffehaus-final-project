package com.maestria.kaffehausv4.Shared.Application.Inicializacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
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
 * Inicializa una base recien creada con databasev4.sql (POST api/v1/seed). Es idempotente:
 * cada fila se busca primero (por nombre, por correo o por su id fijo) y solo se crea si falta,
 * asi que llamarla dos veces no duplica nada ni falla.
 *
 * Siembra, en este orden:
 *
 * 1. Roles 'Administrador' y 'Operador', y el administrador inicial (admin@gmail.com) con su
 *    persona. Si el correo ya existe no se toca: nunca se reescribe una contrasena.
 * 2. Los catalogos que el negocio resuelve por nombre: tipos de movimiento, estados de
 *    existencia, de venta y de pedido, tipos de descuento y tipos de pago.
 * 3. Tipos de producto y categorias por defecto.
 * 4. Unidades estandar y sus conversiones. 'Unidad -> Unidad' no se siembra: la base prohibe
 *    una conversion entre la misma unidad (chk_distintas_unidades) y Conversion.factorEntre
 *    ya devuelve 1 en ese caso.
 * 5. El cliente 'Mostrador' (Cliente.ID_MOSTRADOR), con una persona del mismo id fijo.
 *
 * No es un agregado: compone los repositorios de varios contextos, igual que la autenticacion
 * (arquitectura.md 6.5).
 */
@Service
@Transactional
public class SembrarSistemaUseCase {
    public static final String CORREO_ADMINISTRADOR = "admin@gmail.com";
    // Contrasena de primer arranque. Hay que cambiarla en cuanto se entra por primera vez.
    static final String CONTRASENA_ADMINISTRADOR = "admin";

    private static final List<String> ROLES = List.of("Administrador", "Operador");
    private static final List<String> TIPOS_MOVIMIENTO = List.of(
            "Apertura", "Consumo produccion", "Ingreso produccion", "Venta", "Consumo venta",
            "Merma", "Vencimiento", "Ajuste positivo", "Ajuste negativo", "Devolucion venta");
    private static final List<String> ESTADOS_EXISTENCIA = List.of("Abierta", "Agotada");
    private static final List<String> ESTADOS_VENTA = List.of("Completada", "Anulada");
    private static final List<String> TIPOS_DESCUENTO = List.of("Porcentaje", "Monto fijo");
    private static final List<String> ESTADOS_PEDIDO = List.of("Recibido", "En preparacion", "En camino", "Entregado", "Cancelado");
    private static final List<String> TIPOS_PAGO = List.of(TipoPago.EFECTIVO, "Tarjeta", "QR");
    private static final List<String> TIPOS_PRODUCTO = List.of("Insumo", "Producto elaborado", "Reventa");
    private static final List<String> CATEGORIAS = List.of("Cafes", "Bebidas", "Pasteleria", "Insumos");
    private static final List<String[]> UNIDADES = List.of(
            new String[] {"Kilogramo", "kg"},
            new String[] {"Gramo", "g"},
            new String[] {"Litro", "l"},
            new String[] {"Mililitro", "ml"},
            new String[] {"Unidad", "und"});

    private final IRolRepository rolRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;
    private final IClienteRepository clienteRepository;
    private final IHasheadorDeContrasena hasheadorDeContrasena;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IEstadoVentaRepository estadoVentaRepository;
    private final ITipoDescuentoRepository tipoDescuentoRepository;
    private final IEstadoPedidoRepository estadoPedidoRepository;
    private final ITipoPagoRepository tipoPagoRepository;
    private final ITipoRepository tipoRepository;
    private final ICategoriaRepository categoriaRepository;
    private final IUnidadRepository unidadRepository;
    private final IConversionRepository conversionRepository;

    public SembrarSistemaUseCase(
            IRolRepository rolRepository,
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository,
            IClienteRepository clienteRepository,
            IHasheadorDeContrasena hasheadorDeContrasena,
            ITipoMovimientoRepository tipoMovimientoRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IEstadoVentaRepository estadoVentaRepository,
            ITipoDescuentoRepository tipoDescuentoRepository,
            IEstadoPedidoRepository estadoPedidoRepository,
            ITipoPagoRepository tipoPagoRepository,
            ITipoRepository tipoRepository,
            ICategoriaRepository categoriaRepository,
            IUnidadRepository unidadRepository,
            IConversionRepository conversionRepository) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.clienteRepository = clienteRepository;
        this.hasheadorDeContrasena = hasheadorDeContrasena;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.estadoVentaRepository = estadoVentaRepository;
        this.tipoDescuentoRepository = tipoDescuentoRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
        this.tipoPagoRepository = tipoPagoRepository;
        this.tipoRepository = tipoRepository;
        this.categoriaRepository = categoriaRepository;
        this.unidadRepository = unidadRepository;
        this.conversionRepository = conversionRepository;
    }

    public SembrarSistemaResponse execute() {
        Bitacora bitacora = new Bitacora();

        for (String nombre : ROLES) {
            asegurar(bitacora, "Rol '" + nombre + "'", () -> rolRepository.obtenerPorNombre(nombre),
                    () -> rolRepository.crear(Rol.crear(nombre)));
        }
        sembrarAdministrador(bitacora);

        sembrarCatalogo(bitacora, "Tipo de movimiento", TIPOS_MOVIMIENTO, tipoMovimientoRepository::obtenerPorNombre,
                nombre -> tipoMovimientoRepository.crear(TipoMovimiento.crear(nombre)));
        sembrarCatalogo(bitacora, "Estado de existencia", ESTADOS_EXISTENCIA, estadoExistenciaRepository::obtenerPorNombre,
                nombre -> estadoExistenciaRepository.crear(EstadoExistencia.crear(nombre)));
        sembrarCatalogo(bitacora, "Estado de venta", ESTADOS_VENTA, estadoVentaRepository::obtenerPorNombre,
                nombre -> estadoVentaRepository.crear(EstadoVenta.crear(nombre)));
        sembrarCatalogo(bitacora, "Tipo de descuento", TIPOS_DESCUENTO, tipoDescuentoRepository::obtenerPorNombre,
                nombre -> tipoDescuentoRepository.crear(TipoDescuento.crear(nombre)));
        sembrarCatalogo(bitacora, "Estado de pedido", ESTADOS_PEDIDO, estadoPedidoRepository::obtenerPorNombre,
                nombre -> estadoPedidoRepository.crear(EstadoPedido.crear(nombre)));
        sembrarCatalogo(bitacora, "Tipo de pago", TIPOS_PAGO, tipoPagoRepository::obtenerPorNombre,
                nombre -> tipoPagoRepository.crear(TipoPago.crear(nombre)));
        sembrarCatalogo(bitacora, "Tipo de producto", TIPOS_PRODUCTO, tipoRepository::obtenerPorNombre,
                nombre -> tipoRepository.crear(Tipo.crear(nombre)));
        sembrarCatalogo(bitacora, "Categoria", CATEGORIAS, categoriaRepository::obtenerPorNombre,
                nombre -> categoriaRepository.crear(Categoria.crear(nombre, true)));

        for (String[] unidad : UNIDADES) {
            asegurar(bitacora, "Unidad '" + unidad[0] + "'", () -> unidadRepository.obtenerPorNombre(unidad[0]),
                    () -> unidadRepository.crear(Unidad.crear(unidad[0], unidad[1], false)));
        }
        sembrarConversion(bitacora, "Kilogramo", "Gramo", new BigDecimal("1000.0000"));
        sembrarConversion(bitacora, "Litro", "Mililitro", new BigDecimal("1000.0000"));

        sembrarClienteMostrador(bitacora);

        return SembrarSistemaResponse.crear(bitacora.creados, bitacora.existentes);
    }

    private void sembrarAdministrador(Bitacora bitacora) {
        String descripcion = "Usuario '" + CORREO_ADMINISTRADOR + "'";
        if (usuarioRepository.obtenerPorCorreo(CORREO_ADMINISTRADOR).isPresent()) {
            bitacora.existentes.add(descripcion);
            return;
        }
        Rol administrador = rolRepository.obtenerPorNombre(ROLES.get(0)).orElseThrow();
        Persona persona = personaRepository.crear(Persona.crear("Administrador", "del Sistema", null, "ADMIN"));
        usuarioRepository.crear(Usuario.crear(
                persona.getId(),
                CORREO_ADMINISTRADOR,
                hasheadorDeContrasena.hashear(CONTRASENA_ADMINISTRADOR),
                administrador.getId()));
        bitacora.creados.add(descripcion);
    }

    private void sembrarConversion(Bitacora bitacora, String origen, String destino, BigDecimal factor) {
        Unidad unidadOrigen = unidadRepository.obtenerPorNombre(origen).orElseThrow();
        Unidad unidadDestino = unidadRepository.obtenerPorNombre(destino).orElseThrow();
        String descripcion = "Conversion '" + origen + " -> " + destino + "'";

        boolean existe = conversionRepository.obtenerVivas().stream()
                .anyMatch(conversion -> conversion.getUnidadOrigenId().getValue().equals(unidadOrigen.getId().getValue())
                        && conversion.getUnidadDestinoId().getValue().equals(unidadDestino.getId().getValue()));
        if (existe) {
            bitacora.existentes.add(descripcion);
            return;
        }
        conversionRepository.crear(Conversion.crear(factor, false, unidadOrigen.getId(), unidadDestino.getId()));
        bitacora.creados.add(descripcion);
    }

    /** La persona y el cliente comparten el id fijo; son tablas distintas. */
    private void sembrarClienteMostrador(Bitacora bitacora) {
        String descripcion = "Cliente 'Mostrador'";
        if (clienteRepository.obtenerPorId(new ClienteId(Cliente.ID_MOSTRADOR)).isPresent()) {
            bitacora.existentes.add(descripcion);
            return;
        }
        if (personaRepository.obtenerPorId(new PersonaId(Cliente.ID_MOSTRADOR)).isEmpty()) {
            personaRepository.crear(Persona.restaurar(Cliente.ID_MOSTRADOR, "Cliente", "Casual / Mostrador", null, "0", false));
        }
        clienteRepository.crear(Cliente.restaurar(Cliente.ID_MOSTRADOR, Cliente.ID_MOSTRADOR, false));
        bitacora.creados.add(descripcion);
    }

    private <T> void sembrarCatalogo(
            Bitacora bitacora,
            String catalogo,
            List<String> nombres,
            Function<String, Optional<T>> buscar,
            Function<String, T> crear) {
        for (String nombre : nombres) {
            asegurar(bitacora, catalogo + " '" + nombre + "'", () -> buscar.apply(nombre), () -> crear.apply(nombre));
        }
    }

    private <T> void asegurar(Bitacora bitacora, String descripcion, Supplier<Optional<T>> buscar, Supplier<T> crear) {
        if (buscar.get().isPresent()) {
            bitacora.existentes.add(descripcion);
            return;
        }
        crear.get();
        bitacora.creados.add(descripcion);
    }

    private static final class Bitacora {
        private final List<String> creados = new ArrayList<>();
        private final List<String> existentes = new ArrayList<>();
    }
}
