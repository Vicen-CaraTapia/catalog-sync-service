# catalog-sync-service

Backend Spring Boot que mantiene una réplica local del catálogo y expone consultas de profesionales.

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
| `CATEDRA_REST_CONNECT_TIMEOUT` | Tiempo máximo para establecer una conexión REST, por ejemplo `2s`. |
| `CATEDRA_REST_READ_TIMEOUT` | Tiempo máximo de espera de una respuesta REST, por ejemplo `5s`. |
| `CATEDRA_REDIS_HOST` | Host del Redis provisto por la cátedra. |
| `CATEDRA_REDIS_PORT` | Puerto del Redis provisto por la cátedra. |
| `CATEDRA_REDIS_USERNAME` | Usuario técnico de Redis. |
| `CATEDRA_REDIS_PASSWORD` | Contraseña técnica de Redis. Es secreto. |
| `CATEDRA_REDIS_READ_NAMESPACE` | Namespace de Redis desde el que Catálogo leerá datos de sincronización. |
| `CATEDRA_REDIS_WRITE_NAMESPACE` | Namespace privado de escritura asignado al grupo. |
| `CATEDRA_REDIS_CONNECT_TIMEOUT` | Tiempo máximo para establecer una conexión Redis, por ejemplo `2s`. |
| `CATEDRA_REDIS_COMMAND_TIMEOUT` | Tiempo máximo de espera de un comando Redis, por ejemplo `5s`. |
| `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` | Direcciones iniciales de los brokers Kafka de la cátedra. |
| `CATEDRA_KAFKA_CONSUMER_GROUP_ID` | Identificador del grupo consumidor de Catálogo. |
| `CATEDRA_KAFKA_CATALOG_TOPIC` | Topic de Kafka con notificaciones de actualización del catálogo. |

El servicio obtiene el snapshot de la cátedra para inicializar una base local sin versión aplicada. La sincronización incremental mediante eventos Kafka y datos Redis todavía no está implementada.

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

## Consultas locales del catálogo

Las consultas de esta sección leen la réplica local de PostgreSQL. No consultan la API de la cátedra, Redis, Kafka ni las ocupaciones de turnos. Actualmente la configuración de seguridad solo permite el acceso anónimo a `/actuator/health`: las solicitudes HTTP a `/api/professionals` y `/api/professionals/{id}` reciben `403` hasta que se implemente la autenticación interna de C7.

### Búsqueda de profesionales

`GET /api/professionals` devuelve una página de profesionales. Los filtros son opcionales y se combinan con **AND**:

| Parámetro | Significado |
| --- | --- |
| `name` | Coincidencia parcial, sin distinguir mayúsculas, en `firstName` o `lastName`. Se recortan los espacios de los extremos; un valor en blanco no filtra. Los caracteres `%`, `_` y `!` se buscan literalmente. |
| `categoryId` | Coincidencia exacta con el ID de la categoría. |
| `enabled` | Coincidencia exacta con `true` o `false`. Si se omite, se incluyen ambos estados. |
| `availableOn` | Fecha ISO `YYYY-MM-DD`. Coincide si existe al menos un horario semanal local con `enabled=true` para el día de la semana de esa fecha. Si se omite, no se filtra por horarios. |
| `page` | Número de página desde cero; por defecto `0`. |
| `size` | Elementos por página; por defecto `20`, máximo `100`. |

Por ejemplo, `GET /api/professionals?name=Ana&categoryId=3&enabled=true&availableOn=2026-10-26&page=0&size=20` busca profesionales habilitados que cumplan los demás filtros y tengan una regla semanal habilitada para el lunes. `availableOn` admite fechas pasadas y futuras; no aplica una ventana de reserva.

El filtro `availableOn` representa **disponibilidad teórica de agenda semanal**. No exige implícitamente que el profesional tenga `enabled=true`: para eso se agrega `enabled=true` a la búsqueda. Tampoco comprueba una hora concreta, excepciones de calendario ni si existe un turno libre. El cálculo de turnos realmente disponibles corresponde al servicio de Turnos, que combina las reglas de agenda con las ocupaciones `HELD` y `CONFIRMED`.

El controlador convierte la fecha a `LocalDate` y construye `ProfessionalSearchCriteria`. El caso de uso llama al puerto de búsqueda; `JpaProfessionalSearchAdapter` añade una subconsulta `EXISTS` sobre `weekly_schedules`, correlacionada por ID de profesional. Así, varios horarios coincidentes no duplican profesionales ni alteran los totales paginados.

La respuesta del controlador contiene `content`, `page`, `size`, `totalElements` y `totalPages`. Cada elemento de `content` contiene `id`, `categoryId`, `firstName`, `lastName` y `enabled`; no expone entidades JPA ni marcas de tiempo. El orden es `lastName`, `firstName`, `id`, todos ascendentes. Una búsqueda sin coincidencias produce `200` con `content` vacío. Una fecha mal formada o una paginación inválida produce `400`, siempre que la solicitud llegue al controlador después del control de seguridad.

### Detalle de un profesional

`GET /api/professionals/{id}` devuelve el profesional, su categoría y sus horarios semanales locales, incluidos sus estados `enabled`. Los horarios deshabilitados permanecen visibles como tales. Un ID inexistente produce un `404` controlado. La respuesta tampoco expone entidades JPA.

## Verificación

```bash
mvn clean verify
```

Para probar la lectura incremental contra un Redis real y aislado, instalá `redis-server` en el `PATH` y ejecutá:

```bash
./mvnw -Predis-local-integration clean verify
```

Ese perfil inicia un Redis temporal en `127.0.0.1` con credenciales ficticias, carga únicamente datos de prueba y cierra el proceso al terminar. Comprueba la lectura de metadata, changesets y entidades, incluidos los datos ausentes o inválidos. No usa las credenciales ni modifica el Redis de la cátedra; la verificación normal no requiere `redis-server`.

Para comprobar la lectura contra el Redis de la cátedra, configurá `CATEDRA_REDIS_HOST`, `CATEDRA_REDIS_PORT`, `CATEDRA_REDIS_USERNAME` y `CATEDRA_REDIS_PASSWORD` fuera del repositorio y ejecutá:

```bash
./mvnw -Predis-integration clean verify
```

Este perfil solo lee: comprueba acceso, metadata y el changeset de la versión actual. Si esa clave no está retenida, la prueba de entidades se omite explícitamente. No carga fixtures ni modifica claves o PostgreSQL.

Para ejecutar las pruebas de integración con PostgreSQL, usá una base de pruebas separada y descartable:

```bash
mvn -Ppostgres-integration clean verify
```

Ese perfil incluye pruebas de aplicación del snapshot que reemplazan datos del catálogo y la versión local. No lo ejecutes contra una base con datos que quieras conservar. También ejecuta `JpaProfessionalSearchAdapterIT`, que comprueba la consulta local, la combinación de filtros y que múltiples horarios no dupliquen profesionales ni inflen los totales; ese test revierte sus propios datos de prueba al finalizar. Las pruebas unitarias y HTTP cubren la fecha opcional, su formato, los errores `400` y la página vacía. La verificación normal no requiere PostgreSQL externo.

Para verificar los metadatos del topic de Kafka de la cátedra, configurá las tres variables `CATEDRA_KAFKA_*` indicadas arriba y ejecutá:

```bash
mvn -Pkafka-integration clean verify
```

Esta prueba consulta metadatos; no consume mensajes ni modifica offsets.

Para comprobar el caso de una notificación repetida con el PostgreSQL local y el Redis reales, cargá las variables externas y ejecutá:

```bash
./mvnw -Pcatedra-live-noop verify
```

Este perfil compara ambas versiones antes de invocar el listener con una notificación de prueba para la versión ya aplicada. Si difieren, falla antes de entrar en el flujo. Comprueba que la versión y los conteos locales no cambien. No consume un registro del broker ni confirma un offset real; la confirmación se verifica con un objeto de prueba. El perfil no se ejecuta durante `./mvnw verify` normal.
