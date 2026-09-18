package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;

/**
 * Comprueba el id_token de Google y devuelve el correo verificado que contiene.
 *
 * Devuelve unicamente el correo porque es lo unico que el flujo necesita: el usuario debe
 * existir ya en la tabla 'usuario', no se provisiona nada con el resto del perfil.
 *
 * GoogleIdTokenVerifier hace el trabajo delicado: descarga y cachea las claves publicas de
 * Google, comprueba la firma RS256, el emisor, la vigencia y que el token vaya dirigido a
 * nuestro client id. Escribir eso a mano es justo el tipo de codigo donde un descuido se
 * convierte en un bypass de autenticacion, por eso se delega.
 *
 * Dos comprobaciones adicionales que la libreria no impone y aqui si:
 * - 'email_verified' debe ser true: un correo sin verificar no acredita a nadie.
 * - GOOGLE_CLIENT_ID debe estar configurado. Sin audiencia, el verificador aceptaria tokens
 *   emitidos para cualquier otra aplicacion, que es el error clasico de esta integracion.
 */
@Component
public class VerificadorDeTokenGoogle {

    private static final String TOKEN_INVALIDO = "El token de Google no es valido";

    private final GoogleIdTokenVerifier verificador;
    private final boolean configurado;

    public VerificadorDeTokenGoogle(@Value("${seguridad.google.client-id}") String clientId) {
        this.configurado = clientId != null && !clientId.isBlank();
        this.verificador = configurado
                ? new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(clientId))
                        .build()
                : null;
    }

    public String obtenerCorreoVerificado(String googleToken) {
        if (!configurado) {
            throw SystemException.throwException(500,
                    "El inicio de sesion con Google no esta configurado: falta GOOGLE_CLIENT_ID");
        }

        GoogleIdToken idToken;
        try {
            idToken = verificador.verify(googleToken);
        } catch (Exception ex) {
            // Incluye fallos de red al traer las claves de Google y tokens mal formados.
            throw AutorizacionException.throwException(401, TOKEN_INVALIDO);
        }

        if (idToken == null) {
            throw AutorizacionException.throwException(401, TOKEN_INVALIDO);
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw AutorizacionException.throwException(401, "El correo de la cuenta de Google no esta verificado");
        }

        String correo = payload.getEmail();
        if (correo == null || correo.isBlank()) {
            throw AutorizacionException.throwException(401, TOKEN_INVALIDO);
        }

        return correo;
    }
}
