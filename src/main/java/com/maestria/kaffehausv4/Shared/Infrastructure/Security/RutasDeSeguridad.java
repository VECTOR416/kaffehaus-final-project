package com.maestria.kaffehausv4.Shared.Infrastructure.Security;

import java.util.Arrays;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * Catalogo de rutas de la API agrupadas por politica de acceso.
 *
 * Existe para que las rutas se declaren una sola vez y las lean los dos componentes que las
 * necesitan: SecurityConfig, que construye las reglas de autorizacion, y
 * JwtAuthenticationFilter, que se salta las rutas publicas. Tener la lista en dos sitios seria
 * la via clasica a que una ruta quede abierta por descuido.
 *
 * Los grupos reflejan la politica de negocio acordada, no la estructura del codigo:
 *
 * - PUBLICAS: los tres endpoints de autenticacion. Sin ellos no hay forma de obtener un token.
 * - ADMINISTRACION: gestion de personas, accesos y terceros. Solo Administrador.
 * - POLITICA_COMERCIAL: quien puede definir descuentos. Solo Administrador.
 * - CATALOGOS_MAESTROS: catalogos que el operador consulta pero no modifica. La lectura queda
 *   abierta a cualquier usuario autenticado; POST, PUT y DELETE, solo Administrador.
 * - VENTAS: la operacion diaria del mostrador. Operador y Administrador.
 *
 * Todo lo que no figure aqui cae en 'anyRequest().authenticated()': exige token valido pero no
 * un rol concreto. Anadir un controller nuevo no lo deja abierto por accidente.
 */
public final class RutasDeSeguridad {

    private static final PathMatcher MATCHER = new AntPathMatcher();

    private static final String[] PUBLICAS = {
        "/api/v1/auth/**"
    };

    private static final String[] ADMINISTRACION = {
        "/api/v1/usuario/**",
        "/api/v1/rol/**",
        "/api/v1/entidad/**",
        "/api/v1/proveedor/**"
    };

    private static final String[] POLITICA_COMERCIAL = {
        "/api/v1/descuento/**",
        "/api/v1/tipo-descuento/**"
    };

    private static final String[] CATALOGOS_MAESTROS = {
        "/api/v1/categoria/**",
        "/api/v1/unidad/**",
        "/api/v1/conversion/**",
        "/api/v1/producto/**",
        "/api/v1/receta/**",
        "/api/v1/tipo-movimiento/**",
        "/api/v1/estado-existencia/**",
        "/api/v1/estado-pedido/**",
        "/api/v1/estado-venta/**"
    };

    private static final String[] VENTAS = {
        "/api/v1/venta/**",
        "/api/v1/pedido/**",
        "/api/v1/cliente/**"
    };

    private RutasDeSeguridad() {}

    public static String[] publicas() {
        return PUBLICAS.clone();
    }

    public static String[] administracion() {
        return ADMINISTRACION.clone();
    }

    public static String[] politicaComercial() {
        return POLITICA_COMERCIAL.clone();
    }

    public static String[] catalogosMaestros() {
        return CATALOGOS_MAESTROS.clone();
    }

    public static String[] ventas() {
        return VENTAS.clone();
    }

    /** true si la ruta pedida es uno de los endpoints abiertos de autenticacion. */
    public static boolean esPublica(String ruta) {
        return ruta != null && Arrays.stream(PUBLICAS).anyMatch(patron -> MATCHER.match(patron, ruta));
    }
}
