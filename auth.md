# Autenticacion y autorizacion en KaffeHaus v4

> Guia practica: como usar los endpoints de autenticacion y como proteger rutas.
> El **por que** de este diseno esta en `arquitectura.md` (seccion 1 y reglas 6.2.10 / 6.2.11).
> Todo el codigo de seguridad vive en un solo paquete: `Shared/Presentation/Security`.

---

## 1. El esquema en tres frases

1. El cliente llama a `/api/v1/auth/login` y recibe un **access token** (15 min) y un **refresh
   token** (7 dias), mas los datos basicos del usuario.
2. En cada peticion manda `Authorization: Bearer <accessToken>`. `JwtAuthenticationFilter` lo
   valida y deja al usuario disponible para el controller.
3. **El API esta cerrada por defecto**: cualquier ruta que no sea `/api/v1/auth/**` exige un
   token valido. Para exigir ademas un rol concreto se anota el endpoint con `@RequiereRol`.

---

## 2. Endpoints de autenticacion

Son los tres unicos publicos. Devuelven `200` (iniciar sesion no crea ningun recurso) y **no**
aceptan ni necesitan cabecera `Authorization`: un token viejo o caducado guardado en el cliente
no impide pedir uno nuevo.

### 2.1 `POST /api/v1/auth/login` — correo y contrasena

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@kaffehaus.bo","password":"su-contrasena"}'
```

Respuesta `200`:

```json
{
  "usuario": {
    "usuarioId": "3f1c7a9e-...",
    "nombreCompleto": "Ana Perez Lopez",
    "rolId": "8d2b4f10-...",
    "rolNombre": "Administrador"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

- `usuarioId` es el id de la fila `usuario`; `rolId` y `rolNombre` son la fila de `rol` que ese
  usuario tiene asignada.
- `nombreCompleto` se arma con `persona.nombre + persona.apellidos`; no existe como columna.
- El bloque `usuario` es exactamente lo que va firmado dentro del access token: lo que pintas en
  pantalla y lo que el token afirma no pueden desincronizarse.

Errores:

| Situacion | Codigo | `mensaje` |
|---|---|---|
| Falta `email` o `password` | `400` | `El campo 'email' es obligatorio y debe ser un texto no vacio` |
| Correo inexistente **o** contrasena incorrecta | `401` | `Las credenciales no son validas` |
| Usuario dado de baja (`borrado = true`) | `401` | `Las credenciales no son validas` |
| El usuario apunta a un rol que ya no existe | `403` | `El usuario no tiene un rol asignado valido` |

El mensaje de los tres primeros casos de `401` es deliberadamente el mismo: distinguirlos
convertiria el login en un oraculo para averiguar que correos estan registrados.

### 2.2 `POST /api/v1/auth/google` — cuenta de Google

```bash
curl -X POST http://localhost:8081/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"googleToken":"<id_token que devolvio Google al frontend>"}'
```

Devuelve **la misma estructura** que `/login`. El cliente no deberia distinguir por donde entro
el usuario.

Regla importante: **Google acredita, no da acceso**. El correo verificado debe corresponder a un
usuario que ya existe en la tabla `usuario`; si no existe, la respuesta es `401` y **no se crea
nada**.

| Situacion | Codigo | `mensaje` |
|---|---|---|
| `id_token` invalido, caducado o de otra aplicacion | `401` | `El token de Google no es valido` |
| Google no da el correo por verificado | `401` | `El correo de la cuenta de Google no esta verificado` |
| El correo no corresponde a ningun usuario registrado | `401` | `La cuenta de Google no corresponde a un usuario registrado` |
| Falta configurar `GOOGLE_CLIENT_ID` en el servidor | `500` | `El inicio de sesion con Google no esta configurado: falta GOOGLE_CLIENT_ID` |

### 2.3 `POST /api/v1/auth/refresh` — renovar el access token

```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGciOiJIUzI1NiJ9..."}'
```

Respuesta `200`:

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

No devuelve un refresh token nuevo: el original sigue siendo valido sus 7 dias.

La renovacion **vuelve a leer al usuario y su rol de la base**, no se limita a comprobar la
firma. Consecuencia util: si a alguien le cambian el rol o le dan de baja, deja de renovar (o
renueva ya con su rol nuevo) sin esperar a que caduque nada.

| Situacion | Codigo | `mensaje` |
|---|---|---|
| Falta `refreshToken` | `400` | `El campo 'refreshToken' es obligatorio y debe ser un texto no vacio` |
| Token invalido, alterado, caducado, o se mando el **access** token | `401` | `El token no es valido o ha expirado` |
| El usuario ya no existe o fue dado de baja | `401` | `El refresh token no es valido` |

Access y refresh se firman con **secretos distintos**, asi que presentar uno donde va el otro
falla en la verificacion de firma.

---

## 3. Como consume esto el frontend

1. **Login**: guarda `accessToken`, `refreshToken` y el bloque `usuario` (sirve para pintar el
   nombre y decidir que menus mostrar).
2. **Cada peticion**: cabecera `Authorization: Bearer <accessToken>`.
3. **Si llega un `401`** en cualquier endpoint: llama a `/api/v1/auth/refresh` y reintenta la
   peticion original una sola vez. Si el refresh tambien responde `401`, la sesion murio: borra
   lo guardado y manda al login.
4. **Ocultar un boton no es proteger**: usa `rolNombre` para la UI, pero la decision real la
   toma el backend con `@RequiereRol`.

Limitacion consciente del diseno: los JWT son autocontenidos y no hay almacenamiento de tokens,
asi que **no existe un logout del lado del servidor**. Cerrar sesion es borrar los tokens del
cliente; un refresh token robado sigue valiendo hasta vencer, y lo unico que lo corta antes es
dar de baja al usuario.

---

## 4. Como proteger una ruta

### 4.1 Por defecto ya esta protegida

No hay que hacer nada. Un controller nuevo nace exigiendo token: la cadena de seguridad cierra
todo lo que no sea `/api/v1/auth/**` (`anyRequest().authenticated()` en `SecurityConfig`). Sin
cabecera `Authorization` valida, la respuesta es `401` y la peticion nunca llega al controller.

### 4.2 Exigir un rol: `@RequiereRol`

```java
import com.maestria.kaffehausv4.Shared.Presentation.Security.RequiereRol;

@RequiereRol("Administrador")
@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminar(@PathVariable String id) {
    eliminarProductoUseCase.execute(id);
    return ResponseEntity.noContent().build();
}
```

- El valor es el **nombre de la fila en la tabla `rol`**, escrito tal cual (la comparacion ignora
  mayusculas). No hay enum ni lista de roles en el codigo.
- Varios roles admitidos, basta con tener uno:

  ```java
  @RequiereRol({"Administrador", "Operador"})
  ```

- Tambien se puede poner sobre la **clase** entera del controller; la anotacion del metodo manda
  sobre la de la clase.

  ```java
  @RestController
  @RequestMapping("api/v1/usuario")
  @RequiereRol("Administrador")          // todo el CRUD de usuarios, solo administradores
  public class UsuarioControllerV1 { ... }
  ```

Que responde:

| Peticion | Respuesta |
|---|---|
| Sin token, o con token invalido | `401` — primero se decide **quien** llama |
| Con token valido pero rol que no figura | `403` — `El rol 'Operador' no tiene acceso a este recurso` |
| Con token valido y rol admitido | pasa al controller |

Quien aplica la anotacion es `GuardiaDeRoles`, un `HandlerInterceptor`. Corre despues del filtro,
ya dentro de Spring MVC, que es donde se sabe que metodo va a atender la peticion.

### 4.3 Saber quien esta llamando

```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.maestria.kaffehausv4.Shared.Presentation.Security.SecurityUser;

@RequiereRol("Operador")
@PostMapping
public ResponseEntity<VentaResponse> registrar(
        @AuthenticationPrincipal SecurityUser usuario,
        @RequestBody Map<String, Object> body) {

    usuario.getUsuarioId();      // id de la fila 'usuario'
    usuario.getNombreCompleto();
    usuario.getRolId();          // id de la fila 'rol'
    usuario.getRolNombre();
    ...
}
```

Es el equivalente tipado del `req.user` de otros entornos. Los datos salen del token, asi que no
cuesta ninguna consulta a la base. En un endpoint protegido nunca llega `null`.

**No pases el `SecurityUser` hacia abajo.** Si un caso de uso necesita saber quien hizo algo,
pasale el `String` del `usuarioId`: el dominio y la aplicacion no deben conocer nada de
seguridad (regla 6.2.10 de `arquitectura.md`).

### 4.4 Abrir una ruta al publico

Es la unica operacion que toca `SecurityConfig`. Anade el patron antes de `anyRequest()`:

```java
.authorizeHttpRequests(rutas -> rutas
        .requestMatchers(RUTA_DE_AUTENTICACION + "/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/v1/catalogo-publico/**").permitAll()   // nuevo
        .anyRequest().authenticated())
```

Piensalo dos veces: cada linea aqui es una puerta que hay que vigilar a mano.

---

## 5. Roles

Los roles **son filas de la tabla `rol`**. No hay catalogo en el codigo, asi que anadir uno es:

1. Insertar la fila (por el CRUD `POST /api/v1/rol`, o por SQL).
2. Asignarsela a un usuario (`usuario.id_rol`).
3. Nombrarla donde haga falta: `@RequiereRol("Jefe de turno")`.

No hay que recompilar nada para crear el rol; solo para usarlo en una anotacion nueva.

Dos consecuencias que conviene tener presentes:

- **Renombrar una fila de `rol` rompe las anotaciones que la mencionan.** El nombre es parte del
  contrato; si cambias "Administrador" por "Admin", ningun `@RequiereRol("Administrador")` vuelve
  a casar y esos endpoints empiezan a responder `403`.
- **El rol viaja dentro del access token.** Un cambio de rol no afecta a las sesiones vivas hasta
  que el access token caduque (15 min) o el cliente haga refresh, lo que ocurra antes.

---

## 6. Formato de los errores

Todos los errores del sistema —los del login, los del filtro y los del guardia de roles— tienen
la misma forma, la de `GlobalExceptionHandler.ErrorResponse`:

```json
{
  "codigo": 403,
  "tipo": "Authorization Exception",
  "mensaje": "El rol 'Operador' no tiene acceso a este recurso",
  "timestamp": "2026-09-18T10:31:05.123-04:00"
}
```

| `tipo` | Cuando |
|---|---|
| `Domain Exception` | Entrada invalida o recurso inexistente (`400`, `404`) |
| `Authorization Exception` | Falta token, token invalido o rol insuficiente (`401`, `403`) |
| `System Exception` | Fallo tecnico o de configuracion (`500`) |

Mensajes que produce la capa de seguridad ante un `401`:

- `Falta la cabecera Authorization con un token Bearer valido` — no se mando ninguna cabecera, o
  no empieza por `Bearer `.
- `El token no es valido o ha expirado` — se mando un token, pero esta caducado, alterado, firmado
  con otro secreto, emitido por otro sistema o es un refresh token usado como access token. No se
  distingue entre esos casos a proposito: esa informacion solo le sirve a quien intenta entrar.

---

## 7. Configuracion

Todo vive bajo `seguridad:` en `application.yaml` y llega por variable de entorno:

| Variable | Por defecto | Para que |
|---|---|---|
| `JWT_SECRET` | valor de desarrollo | Secreto de firma del access token. **Minimo 32 caracteres** |
| `JWT_REFRESH_SECRET` | valor de desarrollo | Secreto del refresh token. Minimo 32 caracteres y **distinto** del anterior |
| `JWT_ISSUER` | `kaffehausv4` | Emisor que se exige al verificar |
| `JWT_ACCESS_MINUTES` | `15` | Vigencia del access token |
| `JWT_REFRESH_DAYS` | `7` | Vigencia del refresh token |
| `GOOGLE_CLIENT_ID` | vacio | Client id de Google. Sin el, `/auth/google` responde `500` |

La aplicacion **no arranca** si un secreto tiene menos de 32 caracteres o si los dos secretos son
iguales: son fallos de configuracion que es mejor descubrir en el arranque que en produccion.

Los valores por defecto son para desarrollo. En cualquier despliegue real llegan por variable de
entorno; cambiar un secreto invalida todos los tokens ya emitidos, que es justo lo que se quiere
si se sospecha de una filtracion.

---

## 8. Primer arranque: el primer administrador

Hay un huevo y una gallina que conviene conocer: para crear un usuario hay que llamar a
`POST /api/v1/usuario`, que exige token, y para tener un token hace falta un usuario.

La contrasena se guarda hasheada con PBKDF2 en el formato `pbkdf2$iteraciones$sal$hash`, asi que
**no se puede insertar un usuario utilizable con un `INSERT` a mano**.

En una base vacia, la forma mas corta de salir:

1. Inserta al menos una fila en `rol` (por ejemplo `Administrador`).
2. Abre temporalmente la creacion de usuarios, anadiendo una linea en `SecurityConfig`:

   ```java
   .requestMatchers(HttpMethod.POST, "/api/v1/usuario").permitAll()   // TEMPORAL
   ```

3. Arranca, crea el administrador:

   ```bash
   curl -X POST http://localhost:8081/api/v1/usuario \
     -H "Content-Type: application/json" \
     -d '{"nombres":"Ana","apellidos":"Perez Lopez","telefono":"70000000",
          "carnet":"1234567","correo":"ana@kaffehaus.bo",
          "contrasena":"su-contrasena","idRol":"<id del rol Administrador>"}'
   ```

4. **Quita esa linea** y vuelve a arrancar. A partir de ahi todo se hace con token.

---

## 9. Lo que no hay que hacer

- **No validar tokens fuera del filtro.** `JwtAuthenticationFilter` es el unico sitio; si un
  controller o un caso de uso empieza a leer la cabecera `Authorization`, el esquema se rompio.
- **No escribir un enum, constante o lista de roles** en el codigo. Los roles son filas de `rol`.
- **No importar nada de `org.springframework.security` ni `io.jsonwebtoken`** fuera de
  `Shared/Presentation/Security`.
- **No darle a la autenticacion repositorio, datasource ni tablas propias.** `AutenticacionService`
  reutiliza `IUsuarioRepository`, `IPersonaRepository` e `IRolRepository`.
- **No devolver errores de seguridad a mano** desde un controller. Lanza `AutorizacionException`
  (401/403) y deja que `GlobalExceptionHandler` la traduzca.
