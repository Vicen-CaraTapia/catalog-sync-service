# catalog-sync-service

Backend Spring Boot que mantiene en PostgreSQL una réplica local del catálogo de profesionales, categorías y horarios semanales. La cátedra es la fuente autoritativa; Appointment consulta el catálogo mediante una API interna protegida.

## Requisitos

- Java 25
- Docker, para usar PostgreSQL local, o una instancia PostgreSQL compatible
- Acceso a REST, Redis y Kafka de la cátedra para ejecutar la sincronización completa

## Configurar y ejecutar

La aplicación necesita estas variables para conectarse a PostgreSQL:

```bash
export CATALOG_DB_URL='jdbc:postgresql://localhost:55432/catalog_sync'
export CATALOG_DB_USERNAME='catalog_sync_app'
read -r -s -p 'CATALOG_DB_PASSWORD: ' CATALOG_DB_PASSWORD
echo
export CATALOG_DB_PASSWORD
```

También requiere la configuración externa de la cátedra y la clave pública del JWT interno. No guardes credenciales, tokens ni claves en el repositorio. `.env.example` sirve como referencia, pero Spring Boot no carga ese archivo automáticamente.

```bash
./mvnw spring-boot:run
```

El servidor escucha por defecto en `http://localhost:8081`; `SERVER_PORT` permite cambiar el puerto. La primera ejecución aplica las migraciones Flyway, carga el snapshot cuando la base no tiene catálogo y reconcilia la versión local antes de iniciar el consumidor Kafka.

Las variables y sus valores predeterminados están en [application.yml](src/main/resources/application.yml). Para levantar PostgreSQL local se puede usar la configuración de `../project-infrastructure/`.

La autenticación interna requiere estas variables:

- `INTERNAL_CATALOG_JWT_PUBLIC_KEY_LOCATION`: URI `file:` de una clave pública RSA externa en PEM SPKI (`BEGIN PUBLIC KEY`), de al menos 2048 bits.
- `INTERNAL_CATALOG_JWT_ISSUER`: `appointment-reservation-service`.
- `INTERNAL_CATALOG_JWT_AUDIENCE`: `catalog-sync-service`.

Appointment conserva la clave privada correspondiente; Catalog solo necesita la pública.

## Endpoints útiles

- `GET /actuator/health`: estado de salud del servicio.
- `GET /api/professionals`: búsqueda local de profesionales.
- `GET /api/professionals/{id}`: detalle local; las rutas públicas C4 permanecen denegadas por la política actual.
- `GET /internal/api/professionals/{id}`: contrato de lectura para Appointment; requiere un JWT interno RS256 válido con el permiso `catalog:read`.

El JWT interno requiere también `sub=appointment-reservation-service` y expiración válida. La API devuelve `401` sin autenticación válida y `403` sin el permiso requerido.

## Verificación

```bash
./mvnw clean verify
```

Las pruebas con PostgreSQL requieren una base desechable y se ejecutan con `./mvnw -Ppostgres-integration clean verify`. Los demás perfiles de integración están definidos en [pom.xml](pom.xml); algunos consultan infraestructura externa y requieren configuración adicional.
