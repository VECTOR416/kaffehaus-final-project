package com.maestria.kaffehausv4.Shared.Infrastructure.Crypto;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;

/**
 * Adaptador de IHasheadorDeContrasena sobre PBKDF2, que forma parte del JDK.
 *
 * Se elige PBKDF2 y no BCrypt porque BCrypt llegaba con spring-security-crypto y el
 * objetivo del desmantelamiento es que no quede ninguna dependencia de seguridad en
 * el classpath. El comportamiento visible del CRUD no cambia: entra una contrasena en
 * claro y se persiste un valor derivado con sal aleatoria por usuario.
 *
 * Formato almacenado: pbkdf2$&lt;iteraciones&gt;$&lt;sal base64&gt;$&lt;hash base64&gt;. Al llevar los
 * parametros dentro, los hashes viejos se siguen verificando si manana se suben las
 * iteraciones.
 */
@Component
public class HasheadorDeContrasenaPbkdf2 implements IHasheadorDeContrasena {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2";
    private static final String SEPARADOR = Pattern.quote("$");
    private static final int ITERACIONES = 210_000;
    private static final int LONGITUD_SAL_BYTES = 16;
    private static final int LONGITUD_HASH_BITS = 256;

    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public String hashear(String contrasenaEnClaro) {
        if (contrasenaEnClaro == null) {
            throw SystemException.throwException(500, "No se puede hashear una contrasena nula");
        }

        byte[] sal = new byte[LONGITUD_SAL_BYTES];
        aleatorio.nextBytes(sal);

        byte[] hash = derivar(contrasenaEnClaro, sal, ITERACIONES);

        return PREFIJO + "$" + ITERACIONES
                + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public boolean coincide(String contrasenaEnClaro, String hashAlmacenado) {
        if (contrasenaEnClaro == null || hashAlmacenado == null) {
            return false;
        }

        String[] partes = hashAlmacenado.split(SEPARADOR);
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }

        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);

            // Comparacion en tiempo constante: no filtra cuantos bytes coincidieron.
            return MessageDigest.isEqual(esperado, derivar(contrasenaEnClaro, sal, iteraciones));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] derivar(String contrasenaEnClaro, byte[] sal, int iteraciones) {
        try {
            KeySpec spec = new PBEKeySpec(contrasenaEnClaro.toCharArray(), sal, iteraciones, LONGITUD_HASH_BITS);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw SystemException.throwException(500, "No se pudo derivar el hash de la contrasena");
        }
    }
}
