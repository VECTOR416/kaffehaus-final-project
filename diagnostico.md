# Diagnóstico de KaffeHaus v4

> Estado del sistema al 2026-09-28, rama `feature-inventarios` (commit `3b2b574`).
> Responde a una pregunta: **¿puede este sistema operar hoy en una cafetería real?**
> Las reglas de negocio citadas (`INV-xx`, `VEN-xx`) están en `inventario.md` y `ventas.md`.

---

## Veredicto

**Todavía no está listo para operar.** El backend tiene un núcleo de negocio sólido y bien
probado. Inventario, ventas, recetas, producción y pedidos funcionan de punta a punta y
resisten la concurrencia. Pero a una cafetería real le falta todo lo que rodea al núcleo:

- una interfaz para el cajero y el barista;
- el cobro (forma de pago, caja y cierre de turno);
- la facturación;
- una separación real de permisos;
- reportes;
- una configuración apta para producción.

En su estado actual sirve como **MVP técnico o demo académica** del backend. No sirve todavía
como sistema de punto de venta en producción.

---

## 1. Lo que ya funciona

| Área | Estado |
|---|---|
| **Arquitectura** | Monolito modular con 4 bounded contexts (Inventario, Ventas, Pedidos, Personal) y 24 agregados con la misma plantilla por capas (`arquitectura.md`). Unos 150 endpoints REST bajo `/api/v1`. |
| **Inventario** | Compras por lote, apertura de presentaciones con conversión de unidades, existencias, consumo FEFO con bloqueo pesimista, backflush de recetas (MTO), producción (MTS), mermas, vencimientos, ajustes por conteo físico, libro mayor de movimientos inmutable y costeo por lote. Casi toda la sección 6 de `inventario.md` está implementada. |
| **Ventas** | Precio tomado del producto, descuento vigente más favorable, descuento de inventario en la misma transacción, costo de lo vendido por línea, anulación con o sin reintegro, idempotencia del alta (`clave_idempotencia`) y ventas inmutables. |
| **Pedidos a domicilio** | Uno por venta, con transiciones de estado. Cancelar el pedido anula la venta. |
| **Seguridad** | API cerrada por defecto, JWT de acceso (15 min) y refresco (7 días), login con correo y contraseña o con Google, contraseñas con PBKDF2 y `@RequiereRol` en cada controller. |
| **Integridad** | Borrado lógico en todo el sistema, `CHECK` de saldos e importes en la BD, FKs `RESTRICT` que protegen el historial y errores con formato uniforme. |
| **Tests** | **200 tests, todos en verde** (`./mvnw test`, 2026-09-28): dominio puro, integración contra PostgreSQL real (incluida una prueba de dos ventas simultáneas) y seguridad HTTP. |

---

## 2. Lo que falta para operar

Ordenado de mayor a menor impacto. Los puntos de **2.1** bloquean la apertura; los de **2.2**
se pueden resolver en las primeras semanas de uso; los de **2.3** son mejoras.

### 2.1 Bloqueantes

1. **No hay frontend.** El repositorio contiene solo el API. Sin una pantalla de POS (tomar
   pedidos, cobrar, abrir presentaciones cuando el 409 de INV-34 lo pida) y sin pantallas de
   administración (productos, recetas, compras), el personal no puede usar el sistema.

2. **No se registra cómo se cobra.** Ni `venta` ni ninguna otra tabla tiene forma de pago
   (efectivo, tarjeta, QR), monto recibido o cambio. No hay concepto de **caja**: ni apertura
   con fondo inicial, ni arqueo, ni cierre de turno por cajero. Una cafetería necesita saber
   al final del día cuánto efectivo debería haber en el cajón.

3. **No hay facturación.** No se emite comprobante ni factura, no se guarda NIT o razón
   social del comprador en la venta y no hay integración con el sistema de facturación
   tributaria vigente (en Bolivia, la facturación del SIN). Si la cafetería debe facturar,
   esto es obligatorio antes de abrir. Como mínimo hace falta un ticket imprimible.

4. **Todos los roles de negocio pueden hacer todo.** Todo controller lleva
   `@RequiereRol({"Operador", "Administrador"})` y **ningún endpoint** se restringe a
   `Administrador`. Un operador puede, por tanto:
   - crear usuarios y cambiarles el rol, incluso ascenderse a sí mismo;
   - editar precios y descuentos;
   - anular ventas (`ventas.md` §8 ya lo marca como pendiente);
   - dar de alta compras y ajustar existencias.

   Hay que restringir con `@RequiereRol("Administrador")` al menos: `usuario`, `rol`,
   precio de `producto`, `descuento`, `anulacion` de venta, `ajuste` de existencia y
   borrado de catálogos.

5. **Poner en marcha una base vacía exige un procedimiento manual.** Para crear el primer
   administrador hay que editar `SecurityConfig` temporalmente (`auth.md` §8). Además, los
   catálogos que el flujo resuelve por nombre se siembran a mano con
   `databasev4.sql`: tipos de movimiento, estados de existencia, venta y pedido, y tipos de
   descuento. Si falta uno, las ventas fallan. Hace falta un arranque reproducible:
   migraciones versionadas (Flyway o Liquibase) más un comando o variable de entorno que cree
   el primer administrador.

### 2.2 Necesarios en las primeras semanas

6. **No hay reportes.** Los datos existen (`costo_total`, estados, movimientos), pero faltan
   los endpoints de la sección 7.3 de `ventas.md`: ventas del día o del período, ingreso,
   costo de lo vendido, margen, pérdidas por anulación y productos más vendidos. Además,
   `GET /api/v1/venta` solo filtra por `search`, no por rango de fechas ni por cajero.

7. **No hay alertas de reposición consolidadas.** El "bajo mínimo" se calcula por producto en
   `GET /api/v1/producto/{id}/inventario`. Falta una vista única de lo que hay que comprar y
   de lo que vence pronto.

8. **Quedan anulaciones sin cerrar.**
   - Anular directamente una venta con un pedido vivo **no** cancela el pedido. El sentido
     inverso sí funciona (`ventas.md` §8, "Límite conocido").
   - No se puede anular una producción ni dar de baja un lote con consumo registrado. Falta
     el mecanismo de movimientos compensatorios (INV-22, INV-63).

9. **La configuración no está lista para producción.**
   - `application.yaml` trae la contraseña de PostgreSQL en claro y secretos JWT de
     desarrollo por defecto. Si no se definen `JWT_SECRET` y `JWT_REFRESH_SECRET`, la
     aplicación arranca con secretos conocidos.
   - `ddl-auto: update` deja que Hibernate altere el esquema en producción.
   - El CORS acepta cualquier origen (`setAllowedOriginPatterns("*")`).
   - No hay perfiles (`dev`/`prod`), `Dockerfile` de la aplicación, HTTPS, ni política de
     respaldo de la base de datos.

10. **No hay observabilidad.** No existe ningún logger en el código (`arquitectura.md` 5.10),
    ni Actuator ni health check. Ante un error en caja no hay rastro para diagnosticarlo.

11. **La cuenta y la sesión son limitadas.** No hay cambio de contraseña por el propio
    usuario, recuperación de contraseña, cierre de sesión ni revocación de tokens. Tampoco hay
    límite de intentos en `/auth/login`.

### 2.3 Mejoras del modelo de negocio de una cafetería

12. **No hay variantes ni modificadores.** Tamaño (chico o grande), tipo de leche, shot extra
    o sin azúcar solo se pueden modelar como productos distintos, cada uno con su receta. Esto
    multiplica el catálogo y complica el POS.
13. **No hay mesas ni comandas.** No existe el consumo en local con cuenta abierta. La venta
    nace cobrada (`Completada`), así que no hay cuenta que se va llenando ni división de
    cuenta. Tampoco hay cola de preparación para barra o cocina, salvo para pedidos a domicilio.
14. **Faltan datos en el pedido a domicilio.** No hay costo de envío, repartidor asignado ni
    teléfono de contacto propio del pedido.
15. **No hay propinas, descuentos por venta ni promociones compuestas.** Los descuentos son
    solo por producto (2×1, combos y cupones no existen).
16. **No hay documentación del API** (OpenAPI/Swagger) para quien construya el frontend.

---

## 3. Hoja de ruta sugerida

| Fase | Contenido | Resultado |
|---|---|---|
| **1. Mínimo para abrir** | Puntos 1 a 5: frontend POS y administración, forma de pago y caja, comprobante o factura, permisos de `Administrador`, migraciones y primer arranque | Una cafetería puede vender, cobrar y cerrar caja |
| **2. Operación estable** | Puntos 6 a 11: reportes, alertas de reposición, anulaciones pendientes, configuración de producción, logs y cuentas | Se puede administrar el negocio con datos y operar sin el desarrollador al lado |
| **3. Crecimiento** | Puntos 12 a 16: modificadores, mesas y comandas, mejoras de delivery y promociones | El sistema se adapta a cómo trabaja realmente la barra |

---

## 4. Conclusión

La parte más difícil de un sistema de cafetería ya está resuelta y probada: el inventario con
conversión de unidades, las recetas con backflush y la venta atómica que descuenta stock sin
sobreventa. Lo que falta es, sobre todo, **la capa comercial y operativa**: interfaz, cobro,
caja, factura, permisos, reportes y despliegue. Sin la fase 1 del cuadro anterior, el sistema
no debe usarse con clientes reales.
