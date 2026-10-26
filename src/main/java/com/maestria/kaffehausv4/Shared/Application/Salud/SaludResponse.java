package com.maestria.kaffehausv4.Shared.Application.Salud;

/**
 * Estado del sistema y de la base de datos, cada uno "UP" o "DOWN". Es publico, asi que no
 * lleva versiones, latencias ni mensajes de error.
 */
public class SaludResponse {
    public static final String DISPONIBLE = "UP";
    public static final String NO_DISPONIBLE = "DOWN";

    private String estado;
    private String baseDeDatos;

    private SaludResponse(String estado, String baseDeDatos) {
        this.estado = estado;
        this.baseDeDatos = baseDeDatos;
    }

    /** El sistema esta disponible solo si la base de datos lo esta. */
    public static SaludResponse crear(boolean baseDeDatosDisponible) {
        String estadoBaseDeDatos = baseDeDatosDisponible ? DISPONIBLE : NO_DISPONIBLE;
        return new SaludResponse(estadoBaseDeDatos, estadoBaseDeDatos);
    }

    public String getEstado() {return estado;}
    public String getBaseDeDatos() {return baseDeDatos;}
}
