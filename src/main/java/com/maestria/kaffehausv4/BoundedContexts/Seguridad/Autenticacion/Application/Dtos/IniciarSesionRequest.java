package com.maestria.kaffehausv4.BoundedContexts.Seguridad.Autenticacion.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Credenciales del login clasico. Los campos se llaman 'email' y 'password' porque asi los fija
 * el contrato de la API, aunque el resto del proyecto nombre en espanol.
 */
public class IniciarSesionRequest {
    private String email;
    private String password;

    private IniciarSesionRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static IniciarSesionRequest createFromRequest(Map<String, Object> request){
        String email = Validador.textoRequerido(request.get("email"), "email");
        String password = Validador.textoRequerido(request.get("password"), "password");

        return new IniciarSesionRequest(email, password);
    }

    public String getEmail() {return email;}
    public String getPassword() {return password;}
}
