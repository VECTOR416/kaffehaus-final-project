package com.maestria.kaffehausv4.Shared.Presentation.Security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Firma, lectura y vigencia de los JWT del sistema (HMAC-SHA256).
 *
 * Es el unico punto del proyecto que conoce la libreria jjwt. Lo usan solo dos clases, las dos
 * de esta misma capa: AutenticacionService, que emite los tokens al iniciar sesion, y
 * JwtAuthenticationFilter, que verifica el access token de cada peticion entrante.
 *
 * Decisiones que importan:
 *
 * - Access y refresh se firman con secretos distintos. Presentar un refresh como access (o al
 *   reves) falla en la verificacion de firma, no solo en una comprobacion de campo.
 * - Ademas del secreto, cada token lleva un claim 'tipo'. Es una segunda barrera por si algun
 *   dia los dos secretos se igualan por error de configuracion.
 * - El access token lleva los cuatro datos de UsuarioAutenticado. El refresh lleva solo el
 *   sub: cualquier otro dato quedaria congelado durante siete dias, y tanto el rol como el
 *   estado del usuario se releen de la base en cada renovacion.
 * - La verificacion traduce cualquier fallo de jjwt a AutorizacionException 401, sin decir si
 *   el token expiro, venia alterado o es de otro tipo: esa distincion solo ayuda a quien
 *   intenta forzar la entrada.
 */
@Component
public class JwtService {

    private static final String CLAIM_NOMBRE_COMPLETO = "nombreCompleto";
    private static final String CLAIM_ROL_ID = "rolId";
    private static final String CLAIM_ROL_NOMBRE = "rolNombre";
    private static final String CLAIM_TIPO = "tipo";
    private static final String TIPO_ACCESO = "acceso";
    private static final String TIPO_REFRESCO = "refresco";
    private static final String TOKEN_INVALIDO = "El token no es valido o ha expirado";

    /** HMAC-SHA256 exige una clave de al menos 256 bits. */
    private static final int LONGITUD_MINIMA_SECRETO = 32;

    private final SecretKey claveDeAcceso;
    private final SecretKey claveDeRefresco;
    private final String emisor;
    private final Duration vigenciaDeAcceso;
    private final Duration vigenciaDeRefresco;

    public JwtService(
            @Value("${seguridad.jwt.secreto-acceso}") String secretoDeAcceso,
            @Value("${seguridad.jwt.secreto-refresco}") String secretoDeRefresco,
            @Value("${seguridad.jwt.emisor}") String emisor,
            @Value("${seguridad.jwt.minutos-de-acceso}") long minutosDeAcceso,
            @Value("${seguridad.jwt.dias-de-refresco}") long diasDeRefresco) {

        this.claveDeAcceso = aClave(secretoDeAcceso, "seguridad.jwt.secreto-acceso");
        this.claveDeRefresco = aClave(secretoDeRefresco, "seguridad.jwt.secreto-refresco");

        if (secretoDeAcceso.equals(secretoDeRefresco)) {
            throw SystemException.throwException(500,
                    "Los secretos de acceso y de refresco deben ser distintos");
        }

        this.emisor = emisor;
        this.vigenciaDeAcceso = Duration.ofMinutes(minutosDeAcceso);
        this.vigenciaDeRefresco = Duration.ofDays(diasDeRefresco);
    }

    private static SecretKey aClave(String secreto, String propiedad) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO) {
            throw SystemException.throwException(500,
                    "La propiedad '" + propiedad + "' debe tener al menos " + LONGITUD_MINIMA_SECRETO + " caracteres");
        }
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------- firma

    /** Access token de vida corta con los cuatro datos de la identidad. */
    public String emitirAccessToken(UsuarioAutenticado usuario) {
        Instant ahora = Instant.now();

        return Jwts.builder()
                .issuer(emisor)
                .subject(usuario.usuarioId())
                .claim(CLAIM_NOMBRE_COMPLETO, usuario.nombreCompleto())
                .claim(CLAIM_ROL_ID, usuario.rolId())
                .claim(CLAIM_ROL_NOMBRE, usuario.rolNombre())
                .claim(CLAIM_TIPO, TIPO_ACCESO)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigenciaDeAcceso)))
                .signWith(claveDeAcceso)
                .compact();
    }

    /** Refresh token de vida larga; solo necesita identificar al usuario. */
    public String emitirRefreshToken(String usuarioId) {
        Instant ahora = Instant.now();

        return Jwts.builder()
                .issuer(emisor)
                .subject(usuarioId)
                .claim(CLAIM_TIPO, TIPO_REFRESCO)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigenciaDeRefresco)))
                .signWith(claveDeRefresco)
                .compact();
    }

    // -------------------------------------------------- verificacion

    /**
     * Verifica firma y vigencia de un access token y devuelve a quien identifica.
     * Lanza AutorizacionException 401 si es invalido, esta expirado o fue alterado.
     */
    public UsuarioAutenticado verificarAccessToken(String accessToken) {
        Claims claims = leerClaims(accessToken, claveDeAcceso, TIPO_ACCESO);

        String rolId = claims.get(CLAIM_ROL_ID, String.class);
        String rolNombre = claims.get(CLAIM_ROL_NOMBRE, String.class);
        String nombreCompleto = claims.get(CLAIM_NOMBRE_COMPLETO, String.class);

        if (rolId == null || rolNombre == null || claims.getSubject() == null) {
            throw AutorizacionException.throwException(401, TOKEN_INVALIDO);
        }

        return new UsuarioAutenticado(claims.getSubject(), nombreCompleto, rolId, rolNombre);
    }

    /**
     * Verifica firma y vigencia de un refresh token y devuelve el id de usuario que contiene.
     * Lanza AutorizacionException 401 en los mismos casos.
     */
    public String verificarRefreshToken(String refreshToken) {
        Claims claims = leerClaims(refreshToken, claveDeRefresco, TIPO_REFRESCO);

        String usuarioId = claims.getSubject();
        if (usuarioId == null || usuarioId.isBlank()) {
            throw AutorizacionException.throwException(401, TOKEN_INVALIDO);
        }

        return usuarioId;
    }

    // -------------------------------------------------- lectura de claims

    /** Id del usuario que firma el access token. Falla con 401 si el token no es valido. */
    public String extraerUsuarioId(String accessToken) {
        return extraerClaim(accessToken, Claims::getSubject);
    }

    /** Instante en que vence el access token. Falla con 401 si el token no es valido. */
    public Instant extraerExpiracion(String accessToken) {
        return extraerClaim(accessToken, claims -> claims.getExpiration().toInstant());
    }

    private <T> T extraerClaim(String accessToken, Function<Claims, T> extractor) {
        return extractor.apply(leerClaims(accessToken, claveDeAcceso, TIPO_ACCESO));
    }

    /**
     * Verifica firma, emisor y vigencia. jjwt rechaza por si mismo la firma que no cuadra y el
     * token expirado; aqui se anade que el tipo sea el esperado.
     */
    private Claims leerClaims(String token, SecretKey clave, String tipoEsperado) {
        if (token == null || token.isBlank()) {
            throw AutorizacionException.throwException(401, TOKEN_INVALIDO);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(emisor)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!tipoEsperado.equals(claims.get(CLAIM_TIPO, String.class))) {
                throw AutorizacionException.throwException(401, TOKEN_INVALIDO);
            }

            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw AutorizacionException.throwException(401, TOKEN_INVALIDO);
        }
    }
}
