package com.maestria.kaffehausv4.Shared.Presentation.Security;

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

/**
 * Pruebas de la seguridad sobre HTTP real: el filtro que valida el token, el decorador
 * @RequiereRol y el formato de los errores.
 *
 * Verifican las dos respuestas que importan (401 sin token valido, 403 con token valido pero
 * rol insuficiente) y que las dos tengan el mismo ErrorResponse que devuelve el resto del
 * sistema, aunque una la escriba un filtro y la otra el manejador global.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadHttpTest {

    private static final String AUTENTICADO = "/api/v1/pruebas-seguridad/autenticado";
    private static final String INYECTADO = "/api/v1/pruebas-seguridad/inyectado";
    private static final String SOLO_ADMINISTRADOR = "/api/v1/pruebas-seguridad/solo-administrador";
    private static final String SOLO_OPERADOR = "/api/v1/pruebas-seguridad/solo-operador";
    private static final String CUALQUIERA = "/api/v1/pruebas-seguridad/cualquiera-de-los-dos";

    private static final String ID_USUARIO = "33333333-3333-3333-3333-333333333333";
    private static final String ID_ROL_OPERADOR = "11111111-1111-1111-1111-111111111111";
    private static final String ID_ROL_ADMINISTRADOR = "22222222-2222-2222-2222-222222222222";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    private String operador() {
        return "Bearer " + jwtService.emitirAccessToken(
                new UsuarioAutenticado(ID_USUARIO, "Ana Perez", ID_ROL_OPERADOR, "Operador"));
    }

    private String administrador() {
        return "Bearer " + jwtService.emitirAccessToken(
                new UsuarioAutenticado(ID_USUARIO, "Ana Perez", ID_ROL_ADMINISTRADOR, "Administrador"));
    }

    // -------------------------------------------------- el filtro valida el token

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
        String token = administrador();
        String alterado = token.substring(0, token.length() - 3) + "aaa";

        mockMvc.perform(get(AUTENTICADO).header("Authorization", alterado))
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
    void unEndpointNuevoNaceProtegido() throws Exception {
        // 'anyRequest().authenticated()': no hace falta anotar nada para que exija token.
        mockMvc.perform(get(AUTENTICADO)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(AUTENTICADO).header("Authorization", operador())).andExpect(status().isOk());
    }

    @Test
    void elContextoNoSobreviveAUnaPeticionSinToken() throws Exception {
        // Dos peticiones seguidas en el mismo hilo: la segunda no puede heredar al usuario de
        // la primera. Si el contexto no se limpiara, esta respuesta seria 200.
        mockMvc.perform(get(AUTENTICADO).header("Authorization", operador())).andExpect(status().isOk());

        mockMvc.perform(get(AUTENTICADO)).andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------- el usuario llega al controller

    @Test
    void elUsuarioSeInyectaComoParametroDelControlador() throws Exception {
        mockMvc.perform(get(INYECTADO).header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(ID_USUARIO))
                .andExpect(jsonPath("$.nombreCompleto").value("Ana Perez"))
                .andExpect(jsonPath("$.rolId").value(ID_ROL_OPERADOR))
                .andExpect(jsonPath("$.rolNombre").value("Operador"))
                .andExpect(jsonPath("$.autoridad").value("ROLE_OPERADOR"));
    }

    // -------------------------------------------------- el decorador @RequiereRol

    @Test
    void unOperadorNoEntraAUnRecursoDeAdministrador() throws Exception {
        mockMvc.perform(get(SOLO_ADMINISTRADOR).header("Authorization", operador()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403))
                .andExpect(jsonPath("$.tipo").value("Authorization Exception"))
                .andExpect(jsonPath("$.mensaje").value("El rol 'Operador' no tiene acceso a este recurso"));
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
    void conVariosRolesAdmitidosBastaConTenerUno() throws Exception {
        mockMvc.perform(get(CUALQUIERA).header("Authorization", operador())).andExpect(status().isOk());
        mockMvc.perform(get(CUALQUIERA).header("Authorization", administrador())).andExpect(status().isOk());
    }

    @Test
    void unEndpointConRolSinTokenEs401YNo403() throws Exception {
        // Primero se decide si sabemos quien llama; el rol solo se mira despues.
        mockMvc.perform(get(SOLO_ADMINISTRADOR))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------- rutas publicas y CORS

    @Test
    void losEndpointsDeAutenticacionNoExigenToken() throws Exception {
        // Sin cabecera: el login responde 401 por credenciales, no por falta de token, y eso
        // demuestra que la peticion llego al servicio en vez de frenarse en la cadena.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@test.local\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Las credenciales no son validas"));
    }

    @Test
    void elLoginValidaSuEntradaConElValidadorDelProyecto() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.tipo").value("Domain Exception"));
    }

    @Test
    void unTokenCaducadoNoImpidePedirUnoNuevo() throws Exception {
        // El filtro se salta /api/v1/auth: el token viejo que el cliente aun tenga guardado no
        // puede bloquear el login.
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Authorization", "Bearer token-basura")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@test.local\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Las credenciales no son validas"));
    }

    @Test
    void elPreflightDeCorsNoSeBloquea() throws Exception {
        // El preflight viaja sin Authorization: si la cadena lo rechazara, el navegador ni
        // siquiera llegaria a enviar la peticion real.
        mockMvc.perform(options(AUTENTICADO)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }
}
