package com.maestria.kaffehausv4.Shared.Application.Inicializacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Persistence.Jpa.ConversionJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa.ClienteJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Infrastructure.Persistence.Jpa.TipoPagoJpaRepository;
import com.maestria.kaffehausv4.Shared.Presentation.Security.AutenticacionService;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.AutenticacionResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.IniciarSesionRequest;

/**
 * La siembra de primer arranque contra la base real. No limpia lo que siembra: son los datos
 * de sistema que el resto de la aplicacion y de los tests necesitan.
 */
@SpringBootTest
class SembrarSistemaIntegracionTest {

    @Autowired private SembrarSistemaUseCase sembrarSistemaUseCase;
    @Autowired private AutenticacionService autenticacionService;
    @Autowired private TipoPagoJpaRepository tipoPagoJpaRepository;
    @Autowired private ClienteJpaRepository clienteJpaRepository;
    @Autowired private ConversionJpaRepository conversionJpaRepository;

    @Test
    void esIdempotente() {
        sembrarSistemaUseCase.execute();
        long tiposDePago = tipoPagoJpaRepository.count();
        long conversiones = conversionJpaRepository.count();

        SembrarSistemaResponse segunda = sembrarSistemaUseCase.execute();

        assertTrue(segunda.getCreados().isEmpty(), "La segunda siembra no debe crear nada: " + segunda.getCreados());
        assertTrue(segunda.getExistentes().contains("Usuario '" + SembrarSistemaUseCase.CORREO_ADMINISTRADOR + "'"));
        assertEquals(tiposDePago, tipoPagoJpaRepository.count());
        assertEquals(conversiones, conversionJpaRepository.count());
    }

    @Test
    void siembraLosCatalogosDePagoYElClienteMostrador() {
        sembrarSistemaUseCase.execute();

        for (String nombre : List.of("Efectivo", "Tarjeta", "QR")) {
            assertTrue(tipoPagoJpaRepository.findFirstByNombreIgnoreCaseAndBorradoFalse(nombre).isPresent(), nombre);
        }
        assertTrue(clienteJpaRepository.findById(UUID.fromString(Cliente.ID_MOSTRADOR)).isPresent());
    }

    @Test
    void elAdministradorInicialPuedeIniciarSesion() {
        sembrarSistemaUseCase.execute();

        AutenticacionResponse sesion = autenticacionService.iniciarSesion(
                new IniciarSesionRequest(SembrarSistemaUseCase.CORREO_ADMINISTRADOR, SembrarSistemaUseCase.CONTRASENA_ADMINISTRADOR));

        assertEquals("Administrador", sesion.usuario().rolNombre());
    }
}
