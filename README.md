# EcoAPI — servicio REST de EcoRecicla

Servicio que custodia rutas, puntos de acopio y pesajes de EcoRecicla. Lo consume la app Android **EcoRutas**.
Kotlin 2.2 · Spring Boot 4.0.7 · Spring Security + JWT (jjwt 0.12) · JPA · Flyway · PostgreSQL 17.

## Ejecución (un solo comando)

```bash
docker compose up --build
```

- Levanta PostgreSQL (`localhost:5433`, base `ecorecicla`, usuario `eco` / `eco2026`) y el API en `http://localhost:8080`.
- Flyway crea el esquema (`V1__esquema_inicial.sql`) y carga los datos semilla (`V2__datos_semilla.sql`) al iniciar.
- Desde el emulador Android el API se ve en `http://10.0.2.2:8080/api/v1/`.
- Las fechas semilla son relativas al día en que se crea la base. Para volver a tener «la ruta de hoy» con los datos iniciales:
  `docker compose down -v && docker compose up --build`.

Sin Docker para el API (solo la base en Docker): `docker compose up -d db` y luego `./mvnw spring-boot:run` (Java 21+).

## Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| operario1 | Eco2026* | OPERARIO |
| operario2 | Eco2026* | OPERARIO (autor de un pesaje ajeno, para RN-02) |
| supervisor1 | Eco2026* | SUPERVISOR |
| admin | Eco2026* | ADMINISTRADOR |
| auditor1 | Eco2026* | AUDITOR (SC-B, solo lectura) |

Datos semilla: 12 puntos en 3 zonas (NORTE, CENTRO, SUR); ruta de ayer **CERRADA** (id 1, puntos 1–6) y ruta de hoy
**ABIERTA** (id 2, puntos 7–12); 15 pesajes (uno anulado, uno de operario2; los puntos 10–12 de hoy sin pesar).
Las contraseñas se guardan con BCrypt (`crypt(..., gen_salt('bf'))` de pgcrypto).

## Estructura de paquetes

```
src/main/kotlin/pe/upeu/ecorecicla/
├── config/       SecurityConfig, JwtAuthenticationFilter, JwtService, ManejadorDeErrores (+ EscritorDeErrores)
├── seguridad/    Usuario (+ UsuarioAutenticado), Rol, Privilegio (+ Privilegios), UsuarioRepository (+ UsuarioDetailsService), AuthController
├── dominio/      Entidades: PuntoAcopio, Ruta, RutaPunto, Pesaje y las excepciones de negocio
├── repositorio/  Repositorios Spring Data
├── servicio/     RutaService, PuntoService, PesajeService: reglas RN-01 a RN-04 y transacciones
├── web/          Controladores REST bajo /api/v1
│   └── dto/      Peticiones, respuestas, envoltorio Pagina y ErrorRespuesta
└── Aplicacion.kt
```

## Contrato del API

Todas las rutas, salvo el login, exigen `Authorization: Bearer <token>`.

| Operación | Verbo y ruta | Privilegio | Éxito | Errores |
|---|---|---|---|---|
| Inicio de sesión | `POST /api/v1/auth/login` `{usuario, contrasena}` | Público | 200 token, nombre, rol, privilegios | 400, 401 |
| Usuario actual | `GET /api/v1/auth/me` | Autenticado | 200 | 401 |
| Ruta del día | `GET /api/v1/rutas/del-dia?fecha=AAAA-MM-DD` (por defecto hoy) | PUNTO_LEER | 200 ruta con puntos en orden de visita y estado | 401, 403, 404 |
| Cerrar ruta | `POST /api/v1/rutas/{id}/cierre` | RUTA_CERRAR | 200 ruta cerrada | 401, 403, 404, 409 (RN-04) |
| Puntos de acopio | `GET /api/v1/puntos?zona=&pagina=0&tamanio=20` | PUNTO_LEER | 200 página | 400, 401, 403 |
| Crear punto | `POST /api/v1/puntos` | PUNTO_GESTIONAR | 201 | 400, 401, 403 |
| Pesajes de un punto | `GET /api/v1/puntos/{id}/pesajes?pagina=0&tamanio=20` | PESAJE_LEER | 200 página, del más reciente al más antiguo | 401, 403, 404 |
| Ver pesaje | `GET /api/v1/pesajes/{id}` | PESAJE_LEER | 200 | 401, 403, 404 |
| Registrar pesaje | `POST /api/v1/pesajes` `{rutaId, puntoId, material, pesoBruto, tara, observacion}` | PESAJE_CREAR | **201** con id definitivo y `pesoNeto` | 400 (RN-03), 401, 403, 404, 409 (RN-01) |
| Corregir pesaje | `PUT /api/v1/pesajes/{id}` `{material, pesoBruto, tara, observacion}` | PESAJE_EDITAR | 200 | 400, 401, 403 (RN-02), 404, 409 (RN-01) |
| Anular pesaje | `DELETE /api/v1/pesajes/{id}?motivo=...` | PESAJE_ANULAR | **204** (anulación lógica: el registro queda marcado) | 400, 401, 403, 404, 409 |

El cliente nunca envía el peso neto: lo calcula el servicio. Cualquier error no previsto responde 500 sin detalles internos.

**Paginación** (todos los listados): `{"contenido": [...], "pagina": 0, "tamanio": 20, "totalElementos": 37, "totalPaginas": 2, "ultima": false}`

**Error común:** `{"timestamp", "status", "error", "message", "path", "validationErrors": {"campo": "mensaje"}}`

## Roles y privilegios

La autorización se verifica siempre por **privilegio**. La asignación rol → privilegios vive en la base de datos
(`rol_privilegio`) y las reglas de cada ruta en `SecurityConfig` (`hasAuthority`). Ningún controlador pregunta por el nombre del rol.

| Privilegio | OPERARIO | SUPERVISOR | ADMINISTRADOR | AUDITOR |
|---|---|---|---|---|
| PUNTO_LEER | Sí | Sí | Sí | Sí |
| PESAJE_LEER | Sí | Sí | Sí | Sí |
| RUTA_LEER_TODAS | No | No | Sí | Sí |
| PESAJE_CREAR | Sí | Sí | No | No |
| PESAJE_EDITAR | Solo los propios (RN-02) | Todos | No | No |
| PESAJE_EDITAR_TODOS | No | Sí | No | No |
| PESAJE_ANULAR | No | Sí | No | No |
| RUTA_CERRAR | No | Sí | No | No |
| PUNTO_GESTIONAR | No | No | Sí | No |

**SC-B — rol AUDITOR.** Se incorporó solo con la migración `V3__rol_auditor.sql` (rol, privilegios y usuario `auditor1`).
`SecurityConfig` y los controladores no cambiaron: ya autorizan por privilegio. `RUTA_LEER_TODAS` es un privilegio de lectura
nuevo que permite ver la ruta del día sin estar asignado a ella (`RutaService.delDia` pregunta por el privilegio, no por el rol).

`PESAJE_EDITAR_TODOS` expresa la columna «Todos» de la matriz: así `PesajeService` aplica RN-02 sin comparar nombres de rol.

## Reglas de negocio (en la capa `servicio/`)

| Regla | Dónde | Respuesta |
|---|---|---|
| RN-01 Ruta cerrada no admite registrar ni corregir | `PesajeService.exigirRutaAbierta` | 409 con el motivo |
| RN-02 Operario corrige solo lo propio y del mismo día | `PesajeService.corregir` | 403 |
| RN-03 Neto = bruto − tara > 0, calculado en el servicio | `PesajeService.pesos` (y `CHECK` en la tabla) | 400 con `validationErrors.tara` |
| RN-04 No se cierra la ruta con puntos sin pesaje ni motivo de omisión | `RutaService.cerrar` | 409 «faltan N puntos» |

## Seguridad

- `POST /auth/login` autentica con `AuthenticationManager` (BCrypt) y `JwtService` firma un token HS256 que vence en **1 hora**
  (`sub` = usuario, `uid`, `nombre`, `rol`, `privilegios`, `iat`, `exp`). No hay renovación.
- `JwtAuthenticationFilter` verifica firma y vencimiento en cada petición y relee el usuario de la base (un usuario desactivado deja de entrar).
- Token ausente, alterado o vencido → **401**; autenticado sin el privilegio → **403** (ambos con el formato de error común).

## Pruebas del API

`postman/EcoAPI.postman_collection.json` incluye el login de los tres roles (guarda los tokens en variables) y los casos
200, 201, 204, 400, 401 (sin token y token alterado), 403, 404 y 409.

## Decisiones de arquitectura

- Kotlin + Spring Boot (framework de servicios del curso) con capas config / seguridad / dominio / repositorio / servicio / web.
- Las reglas viven en los servicios transaccionales; los controladores solo delegan.
- Flyway versiona el esquema y la semilla; Hibernate solo mapea (`ddl-auto: none`).
- Docker Compose con build multi-etapa: no hace falta Java ni Maven instalados para levantar el servicio.
