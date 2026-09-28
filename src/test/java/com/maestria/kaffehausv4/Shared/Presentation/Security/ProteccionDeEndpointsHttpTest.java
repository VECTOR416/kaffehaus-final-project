package com.maestria.kaffehausv4.Shared.Presentation.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proteccion de los endpoints de negocio: solo el login y los tokens (/api/v1/auth) quedan
 * abiertos; todo controller de un agregado exige sesion con rol Operador o Administrador, y el
 * usuario que opera sale siempre del token (@UsuarioEnSesion), nunca del cuerpo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProteccionDeEndpointsHttpTest {

    private static final String RECURSO_DE_NEGOCIO = "/api/v1/tipo";
    private static final String SESION = "/api/v1/pruebas-seguridad/sesion";
    private static final String ID_USUARIO = "44444444-4444-4444-4444-444444444444";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private ApplicationContext contexto;

    private String conRol(String rol) {
        return "Bearer " + jwtService.emitirAccessToken(
                new UsuarioAutenticado(ID_USUARIO, "Ana Perez", "55555555-5555-5555-5555-555555555555", rol));
    }

    @Test
    void todoControllerDeUnAgregadoExigeOperadorOAdministrador() {
        List<Class<?>> controllers = contexto.getBeansWithAnnotation(RestController.class).values().stream()
                .<Class<?>>map(AopUtils::getTargetClass)
                .filter(clase -> clase.getPackageName().startsWith("com.maestria.kaffehausv4.BoundedContexts"))
                .toList();

        assertFalse(controllers.isEmpty());
        for (Class<?> controller : controllers) {
            RequiereRol anotacion = controller.getAnnotation(RequiereRol.class);
            assertNotNull(anotacion, controller.getSimpleName() + " no declara @RequiereRol");
            assertEquals(Set.of("Operador", "Administrador"), Set.of(anotacion.value()), controller.getSimpleName());
        }
    }

    @Test
    void unRecursoDeNegocioSinTokenResponde401() throws Exception {
        mockMvc.perform(get(RECURSO_DE_NEGOCIO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    void unRolAjenoAlNegocioResponde403() throws Exception {
        mockMvc.perform(get(RECURSO_DE_NEGOCIO).header("Authorization", conRol("Cliente")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    void operadorYAdministradorEntranAUnRecursoDeNegocio() throws Exception {
        mockMvc.perform(get(RECURSO_DE_NEGOCIO).header("Authorization", conRol("Operador"))).andExpect(status().isOk());
        mockMvc.perform(get(RECURSO_DE_NEGOCIO).header("Authorization", conRol("Administrador"))).andExpect(status().isOk());
    }

    @Test
    void elLoginSigueAbiertoSinToken() throws Exception {
        // Credenciales invalidas: lo que importa es que la cadena no lo corta con 401 por falta de token.
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(result -> assertFalse(
                        result.getResponse().getStatus() == 401
                                && result.getResponse().getContentAsString().contains("Falta la cabecera Authorization"),
                        "El login no puede exigir token"));
    }

    @Test
    void elUsuarioQueOperaSaleDelTokenYNoDelCuerpo() throws Exception {
        mockMvc.perform(post(SESION)
                        .header("Authorization", conRol("Operador"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\": \"99999999-9999-9999-9999-999999999999\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(ID_USUARIO));
    }
}
