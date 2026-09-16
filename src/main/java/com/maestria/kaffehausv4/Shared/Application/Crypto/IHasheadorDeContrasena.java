package com.maestria.kaffehausv4.Shared.Application.Crypto;

/**
 * Puerto de hasheo de credenciales.
 *
 * Sustituye al PasswordEncoder de Spring Security, que desaparecio al desmantelar la
 * seguridad del proyecto. Se declara aqui, en Application/, por la misma razon que
 * IEntidadRepository: el caso de uso lo recibe por constructor y no sabe que algoritmo
 * hay debajo.
 *
 * La columna 'usuario.contrasena' es NOT NULL, asi que el CRUD de Usuario sigue
 * necesitando hashear. Cuando se implemente el nuevo esquema de autenticacion y las
 * credenciales dejen de vivir en la base de datos local, este puerto y su adaptador
 * deben eliminarse junto con el campo 'contrasena' del agregado Usuario.
 */
public interface IHasheadorDeContrasena {

    /** Devuelve la representacion que se persiste. Nunca la contrasena en claro. */
    String hashear(String contrasenaEnClaro);

    /** Verifica una contrasena en claro contra un valor previamente hasheado. */
    boolean coincide(String contrasenaEnClaro, String hashAlmacenado);
}
