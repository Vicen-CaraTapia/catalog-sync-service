<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-69%2F100-green) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-6.7%25-orange) ![%POO](https://img.shields.io/badge/%25POO-79%25-yellow) ![Sueltas](https://img.shields.io/badge/Sueltas-0-green) ![D1](https://img.shields.io/badge/D1-4-red) ![D2](https://img.shields.io/badge/D2-100-green) ![D4](https://img.shields.io/badge/D4-50-orange) ![D5](https://img.shields.io/badge/D5-100-green) ![D6](https://img.shields.io/badge/D6-100-green) ![D7](https://img.shields.io/badge/D7-77-yellow)

**Calidad (heurístico, 7 dimensiones):** índice **69/100** · build **OK** · aprobar **no (6/11)** · destacar **no**
Detalle completo en [`docs/EVALUACION.md`](docs/EVALUACION.md).
<!-- calidad:fin -->

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

El [contrato interno para Appointment](docs/contracts/catalog-appointment/INTERNAL_CATALOG_API.md)
describe los DTO, la autenticación, los errores y ejemplos de uso. Esta copia debe mantenerse
sincronizada con `project-docs/contracts/catalog-appointment/INTERNAL_CATALOG_API.md`.

## Verificación

```bash
./mvnw clean verify
```

Las pruebas con PostgreSQL requieren una base desechable y se ejecutan con `./mvnw -Ppostgres-integration clean verify`. Los demás perfiles de integración están definidos en [pom.xml](pom.xml); algunos consultan infraestructura externa y requieren configuración adicional.
