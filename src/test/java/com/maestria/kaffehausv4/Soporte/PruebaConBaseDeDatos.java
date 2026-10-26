package com.maestria.kaffehausv4.Soporte;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de las pruebas de persistencia: levanta el contexto completo contra H2 en memoria (perfil
 * 'test', src/test/resources/application-test.yaml). Cada prueba corre en una transaccion que se
 * revierte al terminar, asi que ninguna deja datos para la siguiente.
 *
 * H2 no reproduce los indices unicos parciales ni los bloqueos de PostgreSQL: lo que dependa de
 * ellos no se prueba aqui (arquitectura.md 6.5).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class PruebaConBaseDeDatos {
}
