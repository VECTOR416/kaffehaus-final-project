package com.maestria.kaffehausv4.Shared.Presentation.Security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Application.Repositories.IRolRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.Aggregates.Usuario;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Application.Crypto.IHasheadorDeContrasena;
import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;
import com.maestria.kaffehausv4.Shared.Domain.Error.AutorizacionException;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.AccessTokenResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.AutenticacionResponse;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.IniciarSesionConGoogleRequest;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.IniciarSesionRequest;
import com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos.RenovarTokenRequest;

/**
 * Los tres flujos de autenticacion: login clasico, login con Google y renovacion del access
 * token.
 *
 * **No sigue la arquitectura de agregados del proyecto y es deliberado.** La autenticacion no
 * es un bounded context ni tiene tablas propias: es un servicio tecnico que vive en la capa
 * externa y que **reutiliza** lo que el sistema ya tiene. De ahi que no haya casos de uso,
 * puertos ni datasource nuevo: se apoya en los repositorios de Usuario, Persona y Rol que ya
 * existen, y en el hasheador compartido. Ninguna tabla se lee por un camino distinto al que ya
 * usaba su propio CRUD.
 *
 * El rol se resuelve leyendo la fila de 'rol' que apunta el usuario, y su nombre es el que
 * viaja en el token y el que compara @RequiereRol. No hay catalogo de roles en el codigo.
 *
 * Es de solo lectura: aqui no se escribe nada en la base, ni siquiera en el login con Google.
 */
@Service
@Transactional(readOnly = true)
public class AutenticacionService {

    private static final String CREDENCIALES_INVALIDAS = "Las credenciales no son validas";
    private static final String SIN_ACCESO = "La cuenta de Google no corresponde a un usuario registrado";
    private static final String REFRESH_INVALIDO = "El refresh token no es valido";
    private static final String SIN_ROL = "El usuario no tiene un rol asignado valido";

    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;
    private final IRolRepository rolRepository;
    private final IHasheadorDeContrasena hasheadorDeContrasena;
    private final VerificadorDeTokenGoogle verificadorDeTokenGoogle;
    private final JwtService jwtService;

    public AutenticacionService(
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository,
            IRolRepository rolRepository,
            IHasheadorDeContrasena hasheadorDeContrasena,
            VerificadorDeTokenGoogle verificadorDeTokenGoogle,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.hasheadorDeContrasena = hasheadorDeContrasena;
        this.verificadorDeTokenGoogle = verificadorDeTokenGoogle;
        this.jwtService = jwtService;
    }

    /**
     * Login con correo y contrasena.
     *
     * Responde 401 con el mismo mensaje tanto si el correo no existe como si la contrasena no
     * coincide. Distinguir ambos casos convertiria el login en un oraculo para averiguar que
     * correos estan registrados.
     */
    public AutenticacionResponse iniciarSesion(IniciarSesionRequest peticion) {
        String email = Validador.textoRequerido(peticion == null ? null : peticion.email(), "email");
        String password = Validador.textoRequerido(peticion.password(), "password");

        Usuario usuario = usuarioRepository.obtenerPorCorreo(email)
                .orElseThrow(() -> AutorizacionException.throwException(401, CREDENCIALES_INVALIDAS));

        // obtenerPorCorreo ya filtra los borrados, pero la comprobacion queda explicita: que un
        // usuario dado de baja no pueda entrar es una regla, no un efecto colateral del filtro.
        if (usuario.isBorrado()) {
            throw AutorizacionException.throwException(401, CREDENCIALES_INVALIDAS);
        }

        if (!hasheadorDeContrasena.coincide(password, usuario.getContrasena())) {
            throw AutorizacionException.throwException(401, CREDENCIALES_INVALIDAS);
        }

        return armarSesion(usuario, CREDENCIALES_INVALIDAS);
    }

    /**
     * Login con Google.
     *
     * Google solo acredita quien es el que llama; no da acceso por si mismo. El usuario debe
     * estar ya registrado con ese correo: si no existe, la respuesta es 401 y no se crea nada.
     * Es una decision deliberada, y evita que cualquier cuenta de Gmail se convierta en usuario
     * del sistema.
     */
    public AutenticacionResponse iniciarSesionConGoogle(IniciarSesionConGoogleRequest peticion) {
        String googleToken = Validador.textoRequerido(peticion == null ? null : peticion.googleToken(), "googleToken");

        String correo = verificadorDeTokenGoogle.obtenerCorreoVerificado(googleToken);

        Usuario usuario = usuarioRepository.obtenerPorCorreo(correo)
                .orElseThrow(() -> AutorizacionException.throwException(401, SIN_ACCESO));

        if (usuario.isBorrado()) {
            throw AutorizacionException.throwException(401, SIN_ACCESO);
        }

        return armarSesion(usuario, SIN_ACCESO);
    }

    /**
     * Renovacion del access token a partir del refresh token.
     *
     * No se limita a verificar la firma: vuelve a leer al usuario y su rol de la base. Asi un
     * usuario dado de baja, o al que le cambiaron el rol, deja de renovar (o renueva con su rol
     * nuevo) sin esperar a que caduquen los 7 dias.
     *
     * Limitacion consciente del diseno elegido: al ser un JWT autocontenido y no haber
     * almacenamiento de tokens, un refresh token robado sigue siendo valido hasta su
     * vencimiento; lo unico que lo corta antes es dar de baja al usuario.
     */
    public AccessTokenResponse renovarAccessToken(RenovarTokenRequest peticion) {
        String refreshToken = Validador.textoRequerido(peticion == null ? null : peticion.refreshToken(), "refreshToken");

        String usuarioId = jwtService.verificarRefreshToken(refreshToken);

        Usuario usuario = usuarioRepository.obtenerPorId(new UsuarioId(usuarioId))
                .orElseThrow(() -> AutorizacionException.throwException(401, REFRESH_INVALIDO));

        if (usuario.isBorrado()) {
            throw AutorizacionException.throwException(401, REFRESH_INVALIDO);
        }

        return new AccessTokenResponse(jwtService.emitirAccessToken(identidadDe(usuario, REFRESH_INVALIDO)));
    }

    private AutenticacionResponse armarSesion(Usuario usuario, String mensajeSiNoHayPersona) {
        UsuarioAutenticado identidad = identidadDe(usuario, mensajeSiNoHayPersona);

        return new AutenticacionResponse(
                identidad,
                jwtService.emitirAccessToken(identidad),
                jwtService.emitirRefreshToken(identidad.usuarioId()));
    }

    /**
     * Compone la identidad con lo que ya guarda la base: el usuario, su persona (de donde sale
     * el nombre completo, que no existe como columna) y su rol.
     */
    private UsuarioAutenticado identidadDe(Usuario usuario, String mensajeSiNoHayPersona) {
        Persona persona = personaRepository.obtenerPorId(usuario.getPersonaId())
                .orElseThrow(() -> AutorizacionException.throwException(401, mensajeSiNoHayPersona));

        Rol rol = rolRepository.obtenerPorId(usuario.getRolId())
                .orElseThrow(() -> AutorizacionException.throwException(403, SIN_ROL));

        return new UsuarioAutenticado(
                usuario.getId().getValue(),
                (persona.getNombres() + " " + persona.getApellidos()).trim(),
                rol.getId().getValue(),
                rol.getNombre());
    }
}
