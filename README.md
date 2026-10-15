# catalog-sync-service

Backend Spring Boot que servirá como base del servicio de catálogo y sincronización.

## Requisitos

- Java 25
- Maven
- PostgreSQL

## Configuración de base de datos

La aplicación requiere una base PostgreSQL de Catálogo configurada mediante variables de entorno:

```bash
export CATALOG_DB_URL='jdbc:postgresql://localhost:5432/catalog_sync'
export CATALOG_DB_USERNAME='catalog_sync_app'
read -r -s -p 'CATALOG_DB_PASSWORD: ' CATALOG_DB_PASSWORD
echo
export CATALOG_DB_PASSWORD
```

No guardes credenciales reales en el repositorio.

## Ejecución local

```bash
mvn spring-boot:run
```

El servicio inicia por defecto en:

```text
http://localhost:8081
```

Podés cambiar el puerto mediante la variable de entorno `SERVER_PORT`.

## Configuración externa

`.env.example` es una plantilla de referencia; Spring Boot no la carga automáticamente.

Configurá los valores reales desde la shell, IDE, CI/CD, Docker u otro entorno externo. Por ahora:

- `SERVER_PORT`: opcional, por defecto `8081`.

No versionar secretos ni archivos locales como `.env` o `application-local.*`.

Las futuras configuraciones de infraestructura se externalizarán en sus Issues correspondientes.

## Health check

Con el servicio iniciado:

```bash
curl http://localhost:8081/actuator/health
```

La respuesta debe incluir:

```json
{"status":"UP"}
```

## Verificación

```bash
mvn clean verify
```

Para verificar la conexión real a PostgreSQL:

```bash
mvn -Ppostgres-integration clean verify
```
