# AgroAPI-Contreras

Proyecto **inicial**, desarrollado en Kotlin para el caso AgroSierra. No sustituye el backend completo del examen.

## ConfiguraciÃ³n

- Spring Boot 4.1.1 y Kotlin 2.3.21
- Java 21, Maven
- Spring Web MVC, Security, Data JPA, Validation
- Oracle JDBC y Flyway
- Puerto de desarrollo: **8080**

## Abrir el proyecto

1. Guarda los archivos con las rutas que se muestran abajo, en una carpeta **AgroAPI-Contreras** separada de PharmaMobil.
2. Abre IntelliJ IDEA y elige **File > Open**; selecciona `AgroAPI-Contreras` (la carpeta donde estÃ¡ `pom.xml`).
3. Espera la carga de Maven. Selecciona **JDK 21** para el proyecto.
4. Inicia Oracle en Docker (en el puerto local **1522**), prepara un usuario/esquema propio y define `DB_USER` y `DB_PASSWORD`.
5. Ejecuta desde la **terminal de ese nuevo proyecto**, no desde Android Studio.

### Windows PowerShell

```powershell
$env:DB_USER = "AGRODB"
$env:DB_PASSWORD = "COLOCA_AQUI_TU_CLAVE_LOCAL"
mvn test
mvn spring-boot:run
```

Si IntelliJ ya reconoce Maven pero `mvn` no funciona en la terminal, usa la ventana **Maven** de IntelliJ para ejecutar los objetivos `test` y `spring-boot:run`; tambiÃ©n puedes generar un Maven Wrapper.

Prueba en el navegador: `http://localhost:8080/api/health`.

## Estructura del proyecto

```
AgroAPI-Contreras/
  pom.xml
  .gitignore
  README.md
  src/main/
    kotlin/pe/upeu/agrosierra/
      Aplicacion.kt
      config/SecurityConfig.kt
      web/HealthController.kt
    resources/
      application.yml
      db/migration/LEEME.txt
  src/test/kotlin/pe/upeu/agrosierra/
    PruebaInicialTest.kt
```

## QuÃ© incluye y quÃ© falta

- Incluye la configuraciÃ³n Maven, clase principal, control de acceso inicial y un endpoint pÃºblico `/api/health`.
- Las demÃ¡s rutas quedan restringidas mientras no se implemente la autenticaciÃ³n JWT real.
- Faltan el esquema completo de AgroSierra, migraciones SQL, JWT firmado, roles, privilegios, CRUD de lotes/labores y datos semilla. No se presenta como proyecto de examen terminado.
- La aplicaciÃ³n depende de tener disponible Oracle y de configurar `DB_PASSWORD`; no se conecta por sÃ­ sola a Docker ni crea usuarios de Oracle.
- **No subas contraseÃ±as** o claves JWT a Git. ConfigÃºralas fuera del repositorio.

## Git: solo desde el nuevo proyecto

Tras comprobar que `git status` estÃ¡ limpio y que no existe ya un repositorio Git:

```powershell
git init -b main
git config user.name "Diego Contreras"
git add .
git commit -m "chore: crear estructura inicial de AgroAPI"
```

Para enlazar con GitHub, primero debes crear un repositorio vacÃ­o a tu nombre; solo entonces define el `origin` y haz `push`. Nunca ejecutes estos comandos desde la carpeta de PharmaMobil.
