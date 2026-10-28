# AgroAPI-Contreras — Backend de AgroSierra

Servicio REST del caso **AgroSierra / AgroCampo** para la evaluación práctica de Unidad 2. Implementado en **Kotlin, Java 21, Spring Boot 4.1.1, Spring Security con JWT, Spring Data JPA, Oracle y Flyway**. Se consume desde la aplicación Android **AgroCampo**.

- Android: [Proyect-PharmaMobil, rama de cierre](https://github.com/Lucky-pixel-web/Proyect-PharmaMobil/tree/fix/cierre-unidad2-contreras)
- Rama de trabajo del backend: `sc-c-contreras` (incluye la evolución de SC-B y SC-C).
- El código se organiza en `src/main/kotlin/pe/upeu/agrosierra`.

## 1. Preparación y ejecución

Requisitos: **Java 21**, Maven (o ciclo Maven de IntelliJ IDEA), **Oracle Database Free** accesible desde la computadora y un usuario de aplicación con permisos sobre su esquema. Configuración de desarrollo por defecto en `src/main/resources/application.yml`:

| Parámetro | Configuración de desarrollo |
| --- | --- |
| `server.port` | `8080` |
| `DB_URL` | `jdbc:oracle:thin:@//localhost:1522/FREEPDB1` |
| `DB_USER` | `AGRODB` |
| `DB_PASSWORD` | Definir fuera de Git (el YAML aún contiene un valor local de respaldo; sustituirlo antes de usar fuera del entorno de pruebas) |
| `JWT_SECRET_BASE64` | **Obligatoria**, secreto Base64 aleatorio de al menos 32 bytes al decodificar |
| `AGRO_SEED_PASSWORD` | Contraseña configurable para usuarios de prueba; usar solo en entorno de evaluación |

El esquema y las actualizaciones se ejecutan mediante Flyway (`V1__esquema_agrosierra.sql`, `V2__cantidad_jornales.sql`). Hibernate valida el esquema (`ddl-auto: validate`). El componente `DatosSemilla` crea roles, privilegios, usuarios y lotes que falten; las 20 labores iniciales se insertan solo si la tabla aún está vacía. **No borres la base del proyecto para repetir pruebas.**

En PowerShell, desde la raíz del backend (configura tus variables en la sesión; **no publiques los valores**):
```powershell
$env:DB_URL="jdbc:oracle:thin:@//localhost:1522/FREEPDB1"
$env:DB_USER="AGRODB"
$env:DB_PASSWORD="<tu-clave-de-Oracle>"
$env:JWT_SECRET_BASE64="<tu-secreto-base64>"
mvn spring-boot:run
```
Si usas IntelliJ IDEA, define las mismas variables en **Run > Edit Configurations > Environment variables**, sin guardarlas en archivos versionados. Debe mostrarse el arranque de Tomcat en el puerto **8080**. Prueba primero `POST http://localhost:8080/api/v1/auth/login`; `GET /api/health` **requiere token**.

La base de datos y el usuario Oracle deben existir previamente; este proyecto no inicia por sí solo un contenedor Docker.

## 2. API REST

URL base: `http://localhost:8080/api/v1`.

| Método | Ruta | Privilegio / regla |
| --- | --- | --- |
| POST | `/auth/login` | Público; genera JWT |
| GET | `/usuarios/me` | Token válido |
| GET | `/lotes?pagina=0&tamanio=20&valle=Chillon&campaniaCerrada=false` | `LOTE_LEER`; listado paginado y filtros |
| GET | `/lotes/{id}` | `LOTE_LEER` |
| GET | `/lotes/{id}/labores?pagina=0&tamanio=20` | `LABOR_LEER` |
| POST | `/labores` | `LABOR_CREAR` |
| PUT | `/labores/{id}` | `LABOR_EDITAR` |
| DELETE | `/labores/{id}` | `LABOR_ELIMINAR` |
| POST | `/lotes/{id}/cerrar-campania` | `CAMPANIA_CERRAR` |

`GET /api/health` está fuera del prefijo `/api/v1` y requiere JWT, ya que solo login está exento.

### Ejemplos de solicitud

Iniciar sesión:
```http
POST /api/v1/auth/login
Content-Type: application/json

{"usuario":"tecnico1","contrasena":"<clave-configurada>"}
```

Crear labor (reemplazar `<TOKEN>` por un JWT válido):
```http
POST /api/v1/labores
Authorization: Bearer <TOKEN>
Content-Type: application/json

{"loteId":1,"tipo":"RIEGO","fecha":"2026-10-28","cantidad":2.0,"observacion":"Riego programado","cantidadJornales":3}
```

El listado utiliza `PaginaRespuesta`: `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`, `ultima`. El DTO de labor contiene `id`, `loteId`, `tipo` (`RIEGO`, `FUMIGACION`, `COSECHA`), `fecha`, `responsable`, `cantidad`, `observacion`, `pendiente` y `cantidadJornales`.

### Códigos de respuesta y validación

- **200/201/204:** consultas/operaciones correctas; crear devuelve **201** y eliminar **204**.
- **400:** campos inválidos o JSON ilegible; errores de formulario en `validationErrors`.
- **401:** JWT ausente, inválido o caducado, o credenciales incorrectas al iniciar sesión.
- **403:** token válido sin el privilegio requerido o restricción de edición por propietario/plazo.
- **404:** lote o labor inexistente.
- **409:** campaña cerrada u otra operación en conflicto con reglas de negocio.

La respuesta de error incluye `status`, `error`, `message`, `path`, `timestamp` y `validationErrors` cuando corresponde. Las excepciones se convierten mediante `ManejadorErrores.kt`, y seguridad devuelve JSON desde `SecurityConfig.kt`.

## 3. Usuarios, roles y privilegios

`DatosSemilla.kt` define los siguientes roles:

| Usuario inicial | Rol | Privilegios |
| --- | --- | --- |
| `tecnico1` | `TECNICO` | `LOTE_LEER`, `LABOR_LEER`, `LABOR_CREAR`, `LABOR_EDITAR` |
| `jefe1` | `JEFE_CAMPO` | Los anteriores más `LABOR_ELIMINAR`, `CAMPANIA_CERRAR` |
| `admin` | `ADMINISTRADOR` | `LOTE_LEER`, `LABOR_LEER`, `USUARIO_GESTIONAR` |
| `supervisor1` | `SUPERVISOR_CALIDAD` (SC-B) | `LOTE_LEER`, `LABOR_LEER`, `LABOR_EDITAR` |

La contraseña de prueba se controla con `AGRO_SEED_PASSWORD` cuando se crean los usuarios; se almacena con hash. No se utiliza un token fijo. La seguridad combina validación del JWT con permisos cargados en el token y anotaciones `@PreAuthorize`.

## 4. Reglas de negocio y solicitudes de cambio

- **RN-01:** no crear ni editar labores en campañas cerradas (409).
- **RN-02:** un técnico solo puede editar labores propias durante las primeras 24 horas (403 si no cumple).
- **RN-04:** una fumigación no puede superar dosis máxima por hectárea multiplicada por hectáreas del lote (400 con detalle en `cantidad`).
- El cierre de campaña se valida en `LoteService`, según las reglas del caso (incluyendo la comprobación de cosecha).
- **SC-B:** rol `SUPERVISOR_CALIDAD`, con edición pero sin permiso de cierre.
- **SC-C:** `cantidadJornales` obligatoria y mayor o igual que 1, integrada en entidad, servicio, DTO, migración V2 y datos semilla.

El dataset inicial incluye **8 lotes de 3 valles** (Chillón, Mantaro y Santa), **20 labores** y usuarios por rol. Hay lotes abiertos y cerrados para las pruebas.

## 5. Arquitectura y carpetas

```text
Cliente AgroCampo (Android/Ktor)
    -> Web / REST controllers + DTO
    -> Service (validación, reglas, permisos)
    -> Repositorio JPA
    -> Oracle / Flyway
Seguridad: Spring Security -> JWT -> rol/privilegios
```

- `config/`: configuración, seguridad, firmas JWT y datos semilla.
- `dominio/`: entidades JPA y enumeración `TipoLabor`.
- `repositorio/`: interfaces Spring Data JPA.
- `seguridad/`: usuarios, roles, privilegios y autenticación.
- `service/`: reglas de negocio y operaciones.
- `web/`: controladores, manejo de excepciones y `dto/`.
- `resources/db/migration/`: scripts Flyway.
- `src/test/`: pruebas del backend.

## 6. Verificaciones y entrega

Desde la raíz:
```powershell
mvn test
mvn compile
```

En Postman: login correcto/incorrecto; 401 sin token y con token vencido; 403 sin permiso; 400 con validaciones de campos; 404 de entidad inexistente; 409 en campaña cerrada; listado paginado y operaciones permitidas por rol. En DBeaver: verificar datos semilla, relaciones y migraciones.

Exportar la colección Postman y conservar capturas del servicio en ejecución, de los distintos estados HTTP y de los resultados de pruebas. La aplicación Android tiene pruebas `commonTest` y evidencia adicional de funcionamiento en modo avión. Los dos proyectos requieren README, Pull Requests reales, fusiones de `develop` a `main` y etiqueta `v1.0-unidad2` sobre el commit evaluado.

> El README describe lo implementado en esta rama. No sustituye la ejecución de pruebas ni las evidencias solicitadas.
