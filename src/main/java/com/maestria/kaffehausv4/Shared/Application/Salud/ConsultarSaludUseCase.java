package com.maestria.kaffehausv4.Shared.Application.Salud;

import org.springframework.stereotype.Service;

/**
 * Estado del sistema (GET api/v1/salud). Sin @Transactional a proposito: abrir una transaccion
 * pide una conexion antes de entrar, y con la base caida el caso de uso fallaria en vez de
 * responder "DOWN". La conexion la pide y la devuelve el verificador.
 */
@Service
public class ConsultarSaludUseCase {

    private final IVerificadorDeBaseDeDatos verificadorDeBaseDeDatos;

    public ConsultarSaludUseCase(IVerificadorDeBaseDeDatos verificadorDeBaseDeDatos) {
        this.verificadorDeBaseDeDatos = verificadorDeBaseDeDatos;
    }

    public SaludResponse execute() {
        return SaludResponse.crear(verificadorDeBaseDeDatos.estaDisponible());
    }
}
