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

Configurá los valores reales desde la shell, IDE, CI/CD, Docker u otro entorno externo.

| Variable | Descripción |
| --- | --- |
| `SERVER_PORT` | Puerto HTTP local del servicio. Es opcional; por defecto usa `8081`. |
| `CATEDRA_GROUP_ID` | Identificador técnico del grupo asignado por la cátedra. |
| `CATEDRA_TECHNICAL_JWT` | JWT de la cuenta técnica para autenticarse ante la cátedra. Es secreto. |
| `CATEDRA_REST_BASE_URL` | URL base de la API REST de la cátedra. |
| `CATEDRA_REDIS_HOST` | Host del Redis provisto por la cátedra. |
| `CATEDRA_REDIS_PORT` | Puerto del Redis provisto por la cátedra. |
| `CATEDRA_REDIS_USERNAME` | Usuario técnico de Redis. |
| `CATEDRA_REDIS_PASSWORD` | Contraseña técnica de Redis. Es secreto. |
| `CATEDRA_REDIS_READ_NAMESPACE` | Namespace de Redis desde el que Catálogo leerá datos de sincronización. |
| `CATEDRA_REDIS_WRITE_NAMESPACE` | Namespace privado de escritura asignado al grupo. |
| `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` | Direcciones iniciales de los brokers Kafka de la cátedra. |
| `CATEDRA_KAFKA_CONSUMER_GROUP_ID` | Identificador del grupo consumidor de Catálogo. |
| `CATEDRA_KAFKA_CATALOG_TOPIC` | Topic de Kafka con notificaciones de actualización del catálogo. |

Esta Issue solo configura y valida estas propiedades; todavía no realiza conexiones REST, Redis ni Kafka.

No versionar secretos ni archivos locales como `.env` o `application-local.*`.

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
