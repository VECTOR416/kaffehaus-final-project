package com.maestria.kaffehausv4.Shared.Infrastructure.Salud;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.maestria.kaffehausv4.Shared.Application.Salud.IVerificadorDeBaseDeDatos;

/**
 * Adaptador de IVerificadorDeBaseDeDatos sobre JDBC. Pide una conexion al pool y la valida con
 * Connection.isValid, que comprueba la conexion con el servidor sin escribir SQL (arquitectura.md
 * 7.2, regla 18). Cualquier fallo se traduce a 'false' y no se propaga (regla 22).
 *
 * Con la base caida, pedir la conexion puede tardar hasta el connection-timeout del pool.
 */
@Component
public class VerificadorDeBaseDeDatosJdbc implements IVerificadorDeBaseDeDatos {

    private static final int SEGUNDOS_DE_VALIDACION = 2;

    private final DataSource dataSource;

    public VerificadorDeBaseDeDatosJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean estaDisponible() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(SEGUNDOS_DE_VALIDACION);
        } catch (SQLException | RuntimeException e) {
            return false;
        }
    }
}
