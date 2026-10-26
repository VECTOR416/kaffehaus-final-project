package com.maestria.kaffehausv4.Shared.Infrastructure.Crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.maestria.kaffehausv4.Shared.Domain.Error.SystemException;

/** Pruebas unitarias del hasheador PBKDF2: solo usa el JDK, no necesita dobles. */
class HasheadorDeContrasenaPbkdf2Test {

    private final HasheadorDeContrasenaPbkdf2 hasheador = new HasheadorDeContrasenaPbkdf2();

    @Test
    void hashearProduceElFormatoConPrefijoIteracionesSalYHash() {
        // Act
        String hash = hasheador.hashear("secreta");

        // Assert
        String[] partes = hash.split("\\$");
        assertEquals(4, partes.length);
        assertEquals("pbkdf2", partes[0]);
        assertEquals("210000", partes[1]);
        assertFalse(hash.contains("secreta"));
    }

    @Test
    void hashearUsaUnaSalDistintaCadaVez() {
        // Act
        String primero = hasheador.hashear("secreta");
        String segundo = hasheador.hashear("secreta");

        // Assert
        assertNotEquals(primero, segundo);
    }

    @Test
    void hashearRechazaUnaContrasenaNula() {
        // Act
        SystemException error = assertThrows(SystemException.class, () -> hasheador.hashear(null));

        // Assert
        assertEquals(500, error.getCodigo());
    }

    @Test
    void coincideReconoceLaContrasenaOriginal() {
        // Arrange
        String hash = hasheador.hashear("secreta");

        // Act
        boolean coincide = hasheador.coincide("secreta", hash);

        // Assert
        assertTrue(coincide);
    }

    @Test
    void coincideRechazaUnaContrasenaDistinta() {
        // Arrange
        String hash = hasheador.hashear("secreta");

        // Act
        boolean coincide = hasheador.coincide("Secreta", hash);

        // Assert
        assertFalse(coincide);
    }

    @Test
    void coincideRechazaNulos() {
        // Arrange
        String hash = hasheador.hashear("secreta");

        // Act
        boolean sinContrasena = hasheador.coincide(null, hash);
        boolean sinHash = hasheador.coincide("secreta", null);

        // Assert
        assertFalse(sinContrasena);
        assertFalse(sinHash);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "texto-plano",
            "bcrypt$10$c2Fs$aGFzaA==",
            "pbkdf2$10$c2Fs",
            "pbkdf2$diez$c2Fs$aGFzaA==",
            "pbkdf2$10$***$aGFzaA=="})
    void coincideDevuelveFalsoAnteUnHashMalFormado(String hashAlmacenado) {
        // Act
        boolean coincide = hasheador.coincide("secreta", hashAlmacenado);

        // Assert
        assertFalse(coincide);
    }

    @Test
    void coincideVerificaConLasIteracionesGuardadasEnElHash() {
        // Arrange: un hash generado con otro numero de iteraciones debe seguir sin coincidir
        // si se le cambia ese parametro, porque la derivacion depende de el.
        String hash = hasheador.hashear("secreta");
        String conOtrasIteraciones = hash.replace("pbkdf2$210000$", "pbkdf2$1000$");

        // Act
        boolean coincide = hasheador.coincide("secreta", conOtrasIteraciones);

        // Assert
        assertFalse(coincide);
    }
}
