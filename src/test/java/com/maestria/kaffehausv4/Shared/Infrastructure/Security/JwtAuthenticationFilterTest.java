package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.maestria.kaffehausv4.Shared.Application.Security.IGeneradorDeTokens;
import com.maestria.kaffehausv4.Shared.Domain.Security.UsuarioAutenticado;

/**
 * Pruebas de la cadena de seguridad sobre HTTP real: el filtro de JWT, las reglas de
 * SecurityConfig y los dos manejadores de error.
 *
 * Verifican las dos respuestas que pide la especificacion (401 sin token valido, 403 con token
 * valido pero rol insuficiente) y que el formato del error sea el mismo ErrorResponse que
 * devuelve el resto del sistema.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFilterTest {

    private static final String AUTENTICADO = "/api/v1/pruebas-seguridad/autenticado";
    private static final String INYECTADO = "/api/v1/pruebas-seguridad/inyectado";
    private static final String AUTORIDADES = "/api/v1/pruebas-seguridad/autoridades";
    private static final String SOLO_ADMINISTRADOR = "/api/v1/pruebas-seguridad/solo-administrador";
    private static final String SOLO_OPERADOR = "/api/v1/pruebas-seguridad/solo-operador";

    /** Ruta real protegida por rol en SecurityConfig, no por anotacion. */
    private static final String RUTA_DE_ADMINISTRACION = "/api/v1/usuario";

    private static final String ID_USUARIO = "33333333-3333-3333-3333-333333333333";

    @Autowired private MockMvc mockMvc;
    @Autowired private IGeneradorDeTokens generadorDeTokens;

    private String tokenDe(int rolId, String rolNombre) {
        return generadorDeTokens.emitirAccessToken(
                new UsuarioAutenticado(ID_USUARIO, "Ana Perez", rolId, rolNombre));
    }

    private String operador() {
        return "Bearer " + tokenDe(1, "Operador");
    }

    private String administrador() {
        return "Bearer " + tokenDe(2, "Administrador");
    }

    // -------------------------------------------------- autenticacion

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

        mockMvc.perform(get(AUTENTICADO).header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    void dejaPasarACualquierUsuarioAutenticado() throws Exception {
        mockMvc.perform(get(AUTENTICADO).header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(ID_USUARIO));
    }

    @Test
    void unaRutaNoDeclaradaExigeTokenPeroNoUnRolConcreto() throws Exception {
        // 'anyRequest().authenticated()': un controller nuevo nace protegido, no abierto.
        mockMvc.perform(get(AUTENTICADO)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(AUTENTICADO).header("Authorization", operador())).andExpect(status().isOk());
    }

    // -------------------------------------------------- el usuario en el controller

    @Test
    void elUsuarioSeInyectaComoParametroDelControlador() throws Exception {
        mockMvc.perform(get(INYECTADO).header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(ID_USUARIO))
                .andExpect(jsonPath("$.rolId").value(1))
                .andExpect(jsonPath("$.rolNombre").value("Operador"))
                .andExpect(jsonPath("$.nombreCompleto").value("Ana Perez"));
    }

    @Test
    void laAutoridadSeDerivaDelNombreDelRolQueTraeElToken() throws Exception {
        mockMvc.perform(get(AUTORIDADES).header("Authorization", administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("ROLE_ADMINISTRADOR"));
    }

    @Test
    void elContextoNoSobreviveAUnaPeticionSinToken() throws Exception {
        // Dos peticiones seguidas en el mismo hilo: la segunda no puede heredar al usuario de
        // la primera. Si el contexto no se limpiara, esta respuesta seria 200.
        mockMvc.perform(get(AUTENTICADO).header("Authorization", operador())).andExpect(status().isOk());

        mockMvc.perform(get(AUTENTICADO)).andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------- autorizacion por rol

    @Test
    void unOperadorNoEntraAUnRecursoDeAdministrador() throws Exception {
        mockMvc.perform(get(SOLO_ADMINISTRADOR).header("Authorization", operador()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403))
                .andExpect(jsonPath("$.tipo").value("Authorization Exception"));
    }

    @Test
    void unAdministradorSiEntraASuRecurso() throws Exception {
        mockMvc.perform(get(SOLO_ADMINISTRADOR).header("Authorization", administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string("Administrador"));
    }

    @Test
    void unAdministradorNoEntraAUnRecursoExclusivoDeOperador() throws Exception {
        mockMvc.perform(get(SOLO_OPERADOR).header("Authorization", administrador()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unOperadorNoEntraALasRutasDeAdministracion() throws Exception {
        // Regla por URL de SecurityConfig: se rechaza antes de llegar al controller.
        mockMvc.perform(get(RUTA_DE_ADMINISTRACION).header("Authorization", operador()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasRutasDeAdministracionTampocoSeAbrenSinToken() throws Exception {
        mockMvc.perform(get(RUTA_DE_ADMINISTRACION))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------- rutas publicas y CORS

    @Test
    void losEndpointsDeAutenticacionNoExigenToken() throws Exception {
        // Sin cabecera: el login responde 401 por credenciales, no por falta de token, y eso
        // demuestra que la peticion llego al caso de uso en vez de frenarse en la cadena.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@test.local\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Las credenciales no son validas"));
    }

    @Test
    void elPreflightDeCorsNoSeBloquea() throws Exception {
        // El preflight viaja sin Authorization: si la cadena lo rechazara, el navegador ni
        // siquiera llegaria a enviar la peticion real.
        mockMvc.perform(options(RUTA_DE_ADMINISTRACION)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }
}
