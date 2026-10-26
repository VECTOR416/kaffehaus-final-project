package com.maestria.kaffehausv4.Shared.Application.Inicializacion;

import java.util.List;

/** Resultado de la siembra: que filas creo esta llamada y cuales ya estaban. */
public class SembrarSistemaResponse {
    private List<String> creados;
    private List<String> existentes;

    private SembrarSistemaResponse(List<String> creados, List<String> existentes) {
        this.creados = creados;
        this.existentes = existentes;
    }

    public static SembrarSistemaResponse crear(List<String> creados, List<String> existentes) {
        return new SembrarSistemaResponse(List.copyOf(creados), List.copyOf(existentes));
    }

    public List<String> getCreados() {return creados;}
    public List<String> getExistentes() {return existentes;}
}
