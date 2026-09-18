package com.maestria.kaffehausv4.Shared.Domain.Security;

import java.util.Arrays;
import java.util.Locale;

import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;

/**
 * Los dos roles que el sistema reconoce para autorizar: Operador (1) y Administrador (2).
 *
 * Vive en el Shared Kernel y no dentro de un bounded context porque el rol de quien llama es
 * una preocupacion transversal: lo necesitan la capa de seguridad externa (para construir las
 * autoridades de Spring Security y para escribir las reglas de acceso) y los contextos de
 * negocio que deciden en funcion de el. Sigue siendo un POJO puro: ni Spring, ni Spring
 * Security, ni JPA.
 *
 * La tabla 'rol' identifica sus filas por UUID, como todo el proyecto, y ese esquema no se
 * toca. Este enum es el mapeo tipado: asocia cada rol con el codigo numerico estable que viaja
 * en el claim 'rolId' del token, y lo resuelve a partir de 'rol.nombre', que es el dato que si
 * comparten la base y el dominio.
 *
 * Consecuencia a tener presente: renombrar una fila de 'rol' en la base deja a ese usuario sin
 * rol reconocible y su login fallara con 403. Los nombres de estos dos roles son, de hecho,
 * parte del contrato del sistema.
 */
public enum RolDelSistema {

    OPERADOR(1, "Operador"),
    ADMINISTRADOR(2, "Administrador");

    /** Prefijo que Spring Security exige para que .hasRole(...) encuentre la autoridad. */
    public static final String PREFIJO_DE_AUTORIDAD = "ROLE_";

    private final int codigo;
    private final String nombre;

    RolDelSistema(int codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    /**
     * Traduce el nombre del rol almacenado en la base. Falla con 403 y no con 400 a proposito:
     * llegar aqui significa que hay un usuario valido con un rol que el sistema no sabe
     * autorizar, y eso es una negativa de acceso, no un error de formato del cliente.
     */
    public static RolDelSistema desdeNombre(String nombre) {
        return Arrays.stream(values())
                .filter(rol -> rol.nombre.equalsIgnoreCase(nombre))
                .findFirst()
                .orElseThrow(() -> AutorizacionException.throwException(403,
                        "El rol '" + nombre + "' no es un rol valido del sistema"));
    }

    public static RolDelSistema desdeCodigo(int codigo) {
        return Arrays.stream(values())
                .filter(rol -> rol.codigo == codigo)
                .findFirst()
                .orElseThrow(() -> AutorizacionException.throwException(403,
                        "El rol con codigo '" + codigo + "' no es un rol valido del sistema"));
    }

    /** true si este rol es el que identifica ese codigo. */
    public boolean tieneCodigo(int codigo) {
        return this.codigo == codigo;
    }

    /**
     * Normaliza un nombre cualquiera de la tabla 'rol' al formato de autoridad: mayusculas y
     * sin espacios ('Jefe de turno' -> 'JEFE_DE_TURNO').
     *
     * Es el unico sitio donde se decide esa traduccion. La usan por igual SecurityUser, que
     * construye la autoridad a partir del nombre que trae el token (y que salio de la tabla),
     * y SecurityConfig, que escribe las reglas contra este enum. Si las dos no coincidieran,
     * ningun .hasRole(...) casaria jamas.
     */
    public static String aNombreDeAutoridad(String nombre) {
        return nombre == null ? "" : nombre.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    /** El nombre de este rol en formato de autoridad, sin el prefijo 'ROLE_'. */
    public String getNombreDeAutoridad() {
        return aNombreDeAutoridad(nombre);
    }
}
