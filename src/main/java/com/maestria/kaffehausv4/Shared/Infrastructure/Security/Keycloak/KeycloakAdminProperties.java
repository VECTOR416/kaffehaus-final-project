package com.maestria.kaffehausv4.Shared.Infrastructure.Security.Keycloak;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracion de acceso a la Admin REST API de Keycloak.
 *
 * El backend NO reutiliza el token del usuario final para administrar: usa un client
 * confidencial propio con service account y los roles 'manage-users' / 'manage-realm'
 * del client 'realm-management'.
 *
 * El secret se toma de variable de entorno y nunca debe versionarse.
 */
@Component
@ConfigurationProperties(prefix = "kaffehaus.keycloak.admin")
public class KeycloakAdminProperties {

    /** Ej. http://localhost:8080 */
    private String baseUrl = "http://localhost:8080";

    private String realm = "kaffehaus";

    private String clientId = "kaffehaus-admin-cli";

    private String clientSecret = "";

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(10);

    /** Margen de seguridad para renovar el token de servicio antes de su expiracion. */
    private Duration margenRenovacionToken = Duration.ofSeconds(30);

    public boolean estaConfigurado() {
        return clientSecret != null && !clientSecret.trim().isEmpty();
    }

    public String urlDeToken() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String urlDeAdmin() {
        return baseUrl + "/admin/realms/" + realm;
    }

    public String getBaseUrl() {return baseUrl;}
    public void setBaseUrl(String baseUrl) {this.baseUrl = baseUrl;}
    public String getRealm() {return realm;}
    public void setRealm(String realm) {this.realm = realm;}
    public String getClientId() {return clientId;}
    public void setClientId(String clientId) {this.clientId = clientId;}
    public String getClientSecret() {return clientSecret;}
    public void setClientSecret(String clientSecret) {this.clientSecret = clientSecret;}
    public Duration getConnectTimeout() {return connectTimeout;}
    public void setConnectTimeout(Duration connectTimeout) {this.connectTimeout = connectTimeout;}
    public Duration getReadTimeout() {return readTimeout;}
    public void setReadTimeout(Duration readTimeout) {this.readTimeout = readTimeout;}
    public Duration getMargenRenovacionToken() {return margenRenovacionToken;}
    public void setMargenRenovacionToken(Duration margenRenovacionToken) {this.margenRenovacionToken = margenRenovacionToken;}
}
