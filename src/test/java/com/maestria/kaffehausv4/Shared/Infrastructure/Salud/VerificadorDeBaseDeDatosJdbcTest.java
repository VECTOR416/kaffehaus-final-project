package com.maestria.kaffehausv4.Shared.Infrastructure.Salud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificadorDeBaseDeDatosJdbcTest {

    @Mock private DataSource dataSource;
    @Mock private Connection conexion;
    @InjectMocks private VerificadorDeBaseDeDatosJdbc verificador;

    @Test
    void conUnaConexionValidaEstaDisponibleYLaDevuelveAlPool() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(anyInt())).thenReturn(true);

        // Act / Assert
        assertTrue(verificador.estaDisponible());
        verify(conexion).close();
    }

    @Test
    void conUnaConexionInvalidaNoEstaDisponible() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(anyInt())).thenReturn(false);

        // Act / Assert
        assertFalse(verificador.estaDisponible());
    }

    @Test
    void siNoSeConsigueConexionNoEstaDisponibleYNoLanza() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        // Act / Assert
        assertFalse(verificador.estaDisponible());
    }
}
