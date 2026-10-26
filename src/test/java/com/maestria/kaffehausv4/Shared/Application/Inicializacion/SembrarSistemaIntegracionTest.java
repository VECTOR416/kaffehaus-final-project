package com.maestria.kaffehausv4.Shared.Application.Inicializacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Application.Repositories.ICategoriaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Soporte.PruebaConBaseDeDatos;

/** Siembra sobre una base vacia (logica.md SIEMBRA-01): crea todo una vez y despues no repite nada. */
class SembrarSistemaIntegracionTest extends PruebaConBaseDeDatos {

    @Autowired private SembrarSistemaUseCase sembrarSistemaUseCase;
    @Autowired private IUsuarioRepository usuarioRepository;
    @Autowired private IClienteRepository clienteRepository;
    @Autowired private ITipoMovimientoRepository tipoMovimientoRepository;
    @Autowired private ICategoriaRepository categoriaRepository;
    @Autowired private IConversionRepository conversionRepository;
    @Autowired private IHasheadorDeContrasena hasheadorDeContrasena;

    @Test
    void sobreUnaBaseVaciaSiembraTodoYUnaSegundaVezNoCreaNada() {
        // Act
        SembrarSistemaResponse primera = sembrarSistemaUseCase.execute("clave-inicial");
        SembrarSistemaResponse segunda = sembrarSistemaUseCase.execute("clave-inicial");

        // Assert
        assertEquals(38, primera.getCreados().size());
        assertTrue(primera.getExistentes().isEmpty());
        assertTrue(segunda.getCreados().isEmpty());
        assertEquals(38, segunda.getExistentes().size());
    }

    @Test
    void elAdministradorQuedaConSuContrasenaHasheadaYElMostradorConSuIdFijo() {
        // Act
        sembrarSistemaUseCase.execute("clave-inicial");

        // Assert
        Usuario admin = usuarioRepository.obtenerPorCorreo(SembrarSistemaUseCase.CORREO_ADMINISTRADOR).orElseThrow();
        assertTrue(hasheadorDeContrasena.coincide("clave-inicial", admin.getContrasena()));
        assertTrue(clienteRepository.obtenerPorId(new ClienteId(Cliente.ID_MOSTRADOR)).isPresent());
    }

    @Test
    void losCatalogosQueElFlujoResuelvePorNombreQuedanRegistrados() {
        // Act
        sembrarSistemaUseCase.execute("clave-inicial");

        // Assert
        for (String tipo : new String[] {TipoMovimiento.PRODUCCION, TipoMovimiento.AJUSTE, TipoMovimiento.MERMA, TipoMovimiento.APERTURA}) {
            assertTrue(tipoMovimientoRepository.obtenerPorNombre(tipo).isPresent(), tipo);
        }
        assertTrue(categoriaRepository.obtenerPorNombre("Insumos").map(categoria -> !categoria.isEsBorrable()).orElse(false));
        assertEquals(2, conversionRepository.obtenerVivas().size());
    }
}
