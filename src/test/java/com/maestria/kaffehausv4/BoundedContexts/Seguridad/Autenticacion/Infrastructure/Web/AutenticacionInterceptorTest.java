package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Infrastructure.Web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Tokens.IGeneradorDeTokens;
import com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Domain.ValueObjects.UsuarioAutenticado;

/**
 * Pruebas del guard sobre HTTP real, contra el controller de prueba
 * RecursoProtegidoDePruebaControllerV1.
 *
 * Verifican las dos respuestas que pide la especificacion: 401 cuando no hay token valido y
 * 403 cuando lo hay pero el rol no alcanza. Tambien que el formato del error sea el mismo
 * ErrorResponse que devuelve el resto del sistema.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutenticacionInterceptorTest {

    private static final String OPERADOR = "/api/v1/pruebas-seguridad/solo-operador";
    private static final String ADMINISTRADOR = "/api/v1/pruebas-seguridad/solo-administrador";
    private static final String AUTENTICADO = "/api/v1/pruebas-seguridad/autenticado";
    private static final String ABIERTO = "/api/v1/pruebas-seguridad/abierto";

    private static final String ID_USUARIO = "33333333-3333-3333-3333-333333333333";

    @Autowired private MockMvc mockMvc;
    @Autowired private IGeneradorDeTokens generadorDeTokens;

    private String tokenDe(int rolId, String rolNombre) {
        return generadorDeTokens.emitirAccessToken(
                new UsuarioAutenticado(ID_USUARIO, "Ana Perez", rolId, rolNombre));
    }

    @Test
    void unEndpointSinAnotarSigueAbierto() throws Exception {
        mockMvc.perform(get(ABIERTO)).andExpect(status().isOk());
    }

    @Test
    void respondeA401SiNoHayCabeceraAuthorization() throws Exception {
        mockMvc.perform(get(AUTENTICADO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.tipo").value("Authorization Exception"));
    }

    @Test
    void respondeA401SiLaCabeceraNoEsBearer() throws Exception {
        mockMvc.perform(get(AUTENTICADO).header("Authorization", "Basic dXN1YXJpbzpjbGF2ZQ=="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void respondeA401SiElTokenEstaAlterado() throws Exception {
        String token = tokenDe(2, "Administrador");
        String alterado = token.substring(0, token.length() - 3) + "aaa";

        mockMvc.perform(get(ADMINISTRADOR).header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dejaPasarACualquierUsuarioAutenticado() throws Exception {
        mockMvc.perform(get(AUTENTICADO).header("Authorization", "Bearer " + tokenDe(1, "Operador")))
                .andExpect(status().isOk());
    }

    @Test
    void unOperadorNoEntraAUnRecursoDeAdministrador() throws Exception {
        mockMvc.perform(get(ADMINISTRADOR).header("Authorization", "Bearer " + tokenDe(1, "Operador")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    void unAdministradorSiEntraASuRecurso() throws Exception {
        mockMvc.perform(get(ADMINISTRADOR).header("Authorization", "Bearer " + tokenDe(2, "Administrador")))
                .andExpect(status().isOk());
    }

    @Test
    void unAdministradorNoEntraAUnRecursoExclusivoDeOperador() throws Exception {
        mockMvc.perform(get(OPERADOR).header("Authorization", "Bearer " + tokenDe(2, "Administrador")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elGuardDejaAlUsuarioDisponibleParaElEndpoint() throws Exception {
        mockMvc.perform(get(AUTENTICADO).header("Authorization", "Bearer " + tokenDe(1, "Operador")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(ID_USUARIO));
    }

    @Test
    void losEndpointsDeAutenticacionNoExigenToken() throws Exception {
        // Sin cabecera: el login responde 401 por credenciales, no por falta de token, y eso
        // demuestra que la peticion llego al caso de uso en vez de frenarse en el guard.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@test.local\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Las credenciales no son validas"));
    }
}
