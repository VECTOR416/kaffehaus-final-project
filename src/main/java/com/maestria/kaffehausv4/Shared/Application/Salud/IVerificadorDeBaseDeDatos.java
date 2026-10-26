package com.maestria.kaffehausv4.Shared.Application.Salud;

/** Puerto para saber si la base de datos responde. Nunca lanza: un fallo es 'false'. */
public interface IVerificadorDeBaseDeDatos {
    boolean estaDisponible();
}
