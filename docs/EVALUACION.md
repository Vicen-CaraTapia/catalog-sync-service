<!-- evaluacion:inicio -->
# Evaluación — Vicen-CaraTapia/catalog-sync-service

> Documento **generado automáticamente** por la cátedra de Programación II (UM): RepoSense + análisis de calidad de **7 dimensiones**. **No editar a mano**: se regenera en cada evaluación.
> Rama evaluada: `main`. Análisis **heurístico** (regex + métricas) + build: es una criba para orientar la revisión, **no una nota**. La **documentación** del repositorio no se evalúa.
> Generado: 2026-11-18.

## 1. Veredicto

| Aspecto | Resultado |
|---|---|
| Índice de calidad | **69/100** |
| Tipo de repositorio | `backend` |
| Build (`mvnw verify` / `gradlew check`) | OK |
| Código (LOC) | 10,703 |
| Archivos de código | 179 |
| Clases | 201 |
| Código dentro de clases (%POO) | 79% |
| Funciones fuera de clases | 0 |
| Duplicación (puerta) | 6.7% |
| Duplicación lógica | 7.0% |
| Duplicación de boilerplate | 7.0% |
| Cobertura | 98% |
| Tope duro aplicado a D1 | `POO_BAJO` → D1 ≤ 60 |

> Este informe **describe** lo que el análisis midió con los criterios ya establecidos de la materia. No es una nota.

## 2. Semáforo de aprobación

Criterios de la cátedra (`CRITERIOS-CALIDAD.md`): **Aprobar** se juega con el perfil `estandar`; **destacar**, con el `exigente` (meta de excelencia).

| Semáforo | Resultado |
|---|---|
| Aprobar (`estandar`) | **no cumple** — 6/11 |
| Destacar (`exigente`) | **no cumple** — 5/11 |

**Para aprobar, falta cumplir:** SOLID; Duplicación

**Para destacar, falta cumplir:** SOLID; POO; Duplicación

## 3. Autoría (RepoSense)

| Autor | Commits | Código (LOC) | Tests (LOC) | Docs (LOC) | Días activos | % del código |
|---|---:|---:|---:|---:|---:|---:|
| Vicente Cara Tapia | 54 | 3,845 | 8,838 | 252 | 14 | 100% |

> _El LOC de autoría lo calcula RepoSense sobre el historial y puede diferir del LOC de código del análisis (que mira el estado de `main`)._

## 4. Dimensiones evaluadas

Cada dimensión indica **qué mide**, **por qué importa** y **tu resultado**. Cuando el puntaje queda por debajo de la referencia, se agregan preguntas para **investigar la causa** (no hay recetas).

| Dimensión | Peso | Puntaje |
|:--|--:|--:|
| **D1** Diseño OO, SOLID y patrones (20%) | 20% | 4 |
| **D2** Arquitectura hexagonal — backend (15%) | 15% | 100 |
| **D3** Frontend KMP: MVVM y buenas prácticas (10%) | 10% | N/A |
| **D4** Integración y contrato (10%) | 10% | 50 |
| **D5** Seguridad y robustez (10%) | 10% | 100 |
| **D6** Pruebas y cobertura (20%) | 20% | 100 |
| **D7** Proceso y entrega (15%) | 15% | 77 |

### D1. Diseño OO, SOLID y patrones (20%)

- **Qué mide:** Métricas orientadas a objetos (clases, métodos, campos, cohesión, acoplamiento, herencia) y proxies de SOLID, más patrones de diseño.
- **Por qué importa:** El eje de la materia es el buen uso de la POO y su arquitectura: un diseño con clases claras y poco acopladas es más fácil de entender, probar y cambiar.
- **Tu resultado:** **4/100**
- **Para investigar la causa:**
    - ¿Se pensó el modelo de clases antes de codificar o se agrupó después?
    - ¿Qué clases concentran muchas responsabilidades a la vez?
    - ¿Los colaboradores llegan como parámetros o se crean por dentro?

### D2. Arquitectura hexagonal — backend (15%)

- **Qué mide:** Separación en capas (dominio, puertos de entrada/salida, adaptadores) y dirección de las dependencias del backend.
- **Por qué importa:** Aislar el dominio de la infraestructura permite cambiar la base de datos, el framework o el transporte sin tocar las reglas de negocio.
- **Tu resultado:** **100/100**

### D4. Integración y contrato (10%)

- **Qué mide:** Cumplimiento del contrato de integración de la cátedra (formato de errores, paginación, autenticación, endpoints esperados).
- **Por qué importa:** Los servicios deben poder integrarse entre sí y con la app sin sorpresas; el contrato es lo que hacen cumplir las pruebas de integración.
- **Tu resultado:** **50/100**
- **Para investigar la causa:**
    - ¿Qué endpoints difieren del contrato acordado?
    - ¿Los errores se devuelven con un formato uniforme?
    - ¿La autenticación es consistente entre servicios?

### D5. Seguridad y robustez (10%)

- **Qué mide:** Credenciales embebidas, consultas SQL por concatenación, y presencia de config de entorno; aislamiento de secretos.
- **Por qué importa:** Un secreto en el repositorio o una consulta construida por concatenación son riesgos concretos, no hipotéticos.
- **Tu resultado:** **100/100**

### D6. Pruebas y cobertura (20%)

- **Qué mide:** Cantidad y resultado de pruebas y cobertura de línea (JaCoCo/Kover, Surefire/Failsafe) cuando hay build.
- **Por qué importa:** Las pruebas son la red de seguridad para refactorizar e integrar; la cobertura acota las zonas sin verificar.
- **Tu resultado:** **100/100**

### D7. Proceso y entrega (15%)

- **Qué mide:** Artefactos verificables de proceso/entrega: `compose`, `.env.example`, CI (`.github/workflows/`), `CODEOWNERS` y wrapper de build (`mvnw`/`gradlew`). **No** puntúa documentación.
- **Por qué importa:** Un proyecto que compila, se levanta y se valida solo es más fácil de revisar y de integrar en equipo.
- **Tu resultado:** **77/100**

## 5. Señales detectadas

El análisis encontró las siguientes señales, con el archivo y la línea donde se originan cuando están disponibles:

**Archivos con más señales**

| Archivo | Señales |
|---|---:|
| `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClient.java` | 4 |
| `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutor.java` | 4 |
| `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationService.java` | 4 |
| `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapter.java` | 4 |
| `src/main/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationProperties.java` | 4 |
| `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java` | 4 |
| `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/decoder/CatedraRedisCatalogDecoder.java` | 3 |
| `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClient.java` | 3 |
| `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapter.java` | 3 |
| `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/WeeklyScheduleEntity.java` | 3 |

**SRP/responsabilidad** (44)
- método 'CatalogSnapshot.CatalogSnapshot' de 55 líneas — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/CatalogSnapshot.java:10`
- método 'CatalogSnapshot.CatalogSnapshot' con 5 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/CatalogSnapshot.java:10`
- método 'CatalogSnapshot.CatalogSnapshot' con complejidad 6 — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/CatalogSnapshot.java:10`
- método 'Professional.Professional' con 7 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/Professional.java:6`
- método 'ProfessionalCategory.ProfessionalCategory' con 6 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/ProfessionalCategory.java:6`
- método 'WeeklySchedule.WeeklySchedule' con 9 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/WeeklySchedule.java:8`
- método 'ProfessionalSearchCriteria.ProfessionalSearchCriteria' con 6 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/search/ProfessionalSearchCriteria.java:5`
- método 'ProfessionalSearchResult.ProfessionalSearchResult' con 5 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/search/ProfessionalSearchResult.java:7`
- método 'ProfessionalSearchResult.ProfessionalSearchResult' con complejidad 6 — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/search/ProfessionalSearchResult.java:7`
- clase 'JpaCatalogSnapshotPersistenceAdapter' con baja cohesión (30%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapter.java:19`
- método 'JpaCatalogSnapshotPersistenceAdapter.apply' de 48 líneas — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapter.java:45`
- método 'JpaProfessionalSearchAdapter.specification' de 45 líneas — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapter.java:56`
- método 'JpaProfessionalSearchAdapter.specification' con complejidad 7 — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapter.java:56`
- clase 'CatalogSyncStateEntity' con baja cohesión (40%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/CatalogSyncStateEntity.java:10`
- clase 'ProfessionalCategoryEntity' con baja cohesión (21%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/ProfessionalCategoryEntity.java:12`
- método 'ProfessionalCategoryEntity.ProfessionalCategoryEntity' con 6 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/ProfessionalCategoryEntity.java:35`
- clase 'ProfessionalEntity' con baja cohesión (19%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/ProfessionalEntity.java:12`
- método 'ProfessionalEntity.ProfessionalEntity' con 7 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/ProfessionalEntity.java:38`
- clase 'WeeklyScheduleEntity' grande (LOC=71, métodos=11, campos=9) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/WeeklyScheduleEntity.java:16`
- clase 'WeeklyScheduleEntity' con baja cohesión (16%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/WeeklyScheduleEntity.java:16`
- método 'WeeklyScheduleEntity.WeeklyScheduleEntity' con 9 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/WeeklyScheduleEntity.java:49`
- método 'CatalogSnapshotResponse.CatalogSnapshotResponse' con 5 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/dto/CatalogSnapshotResponse.java:7`
- método 'ProfessionalCategoryResponse.ProfessionalCategoryResponse' con 6 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/dto/ProfessionalCategoryResponse.java:6`
- método 'ProfessionalResponse.ProfessionalResponse' con 7 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/dto/ProfessionalResponse.java:6`
- método 'WeeklyScheduleResponse.WeeklyScheduleResponse' con 9 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/dto/WeeklyScheduleResponse.java:8`
- método 'ProfessionalDetailsResponse.ProfessionalDetailsResponse' con 6 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/dto/ProfessionalDetailsResponse.java:7`
- método 'ProfessionalSearchPageResponse.ProfessionalSearchPageResponse' con 5 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/dto/ProfessionalSearchPageResponse.java:9`
- método 'ProfessionalSummaryResponse.ProfessionalSummaryResponse' con 5 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/dto/ProfessionalSummaryResponse.java:8`
- método 'WeeklyScheduleDetailResponse.WeeklyScheduleDetailResponse' con 6 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/dto/WeeklyScheduleDetailResponse.java:7`
- método 'CatedraIntegrationProperties.CatedraIntegrationProperties' de 52 líneas — `src/main/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationProperties.java:15`
- método 'CatedraIntegrationProperties.CatedraIntegrationProperties' con 5 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationProperties.java:15`
- método 'CatedraIntegrationProperties.RedisProperties' con 8 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationProperties.java:41`
- método 'RedisProperties.RedisProperties' con 8 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationProperties.java:41`
- método 'CatalogVersionSynchronizationService.synchronizeTo' de 37 líneas — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationService.java:32`
- método 'CatalogVersionSynchronizationService.synchronizeTo' con complejidad 10 — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationService.java:32`
- método 'CatalogUpdatedEvent.CatalogUpdatedEvent' con 6 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/dto/CatalogUpdatedEvent.java:5`
- clase 'CatedraRedisClient' grande (LOC=90, métodos=11, campos=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClient.java:17`
- clase 'CatedraRedisClient' con baja cohesión (18%) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClient.java:17`
- clase 'CatedraRedisCatalogDecoder' grande (LOC=191, métodos=21, campos=1) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/decoder/CatedraRedisCatalogDecoder.java:22`
- método 'RedisProfessional.RedisProfessional' con 7 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/dto/RedisProfessional.java:5`
- método 'RedisProfessionalCategory.RedisProfessionalCategory' con 6 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/dto/RedisProfessionalCategory.java:5`
- método 'RedisWeeklySchedule.RedisWeeklySchedule' con 9 parámetros — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/dto/RedisWeeklySchedule.java:7`
- método 'SynchronizationRetryExecutor.execute' de 35 líneas — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutor.java:49`
- método 'SynchronizationRetryExecutor.execute' con complejidad 8 — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutor.java:49`

**DIP/concretos** (32)
- clase 'JpaCatalogSnapshotPersistenceAdapter' muy acoplada (CBO=16) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapter.java:19`
- clase 'JpaCatalogSnapshotPersistenceAdapter' instancia 3 concretos (CBO=16) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapter.java:19`
- clase 'JpaProfessionalDetailsAdapter' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalDetailsAdapter.java:16`
- clase 'JpaProfessionalSearchAdapter' muy acoplada (CBO=14) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapter.java:22`
- clase 'CatedraCatalogSnapshotMapper' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/mapper/CatedraCatalogSnapshotMapper.java:14`
- clase 'CatedraCatalogSnapshotMapper' instancia 5 concretos (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/mapper/CatedraCatalogSnapshotMapper.java:14`
- clase 'CatalogInitializationRunner' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationRunner.java:23`
- clase 'ProfessionalSearchController' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalSearchController.java:24`
- clase 'ProfessionalSearchController' instancia 3 concretos (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalSearchController.java:24`
- clase 'CatalogVersionSynchronizationService' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationService.java:13`
- clase 'CatalogVersionSynchronizationService' instancia 4 concretos (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationService.java:13`
- clase 'ReconcileCatalogStateUseCaseImpl' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/usecases/ReconcileCatalogStateUseCaseImpl.java:13`
- clase 'CatedraKafkaConfiguration' muy acoplada (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/config/CatedraKafkaConfiguration.java:15`
- clase 'CatalogUpdatedEventDecoder' muy acoplada (CBO=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/decoder/CatalogUpdatedEventDecoder.java:13`
- clase 'CatalogUpdatedListener' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogUpdatedListener.java:16`
- clase 'CatedraCatalogListenerStarter' muy acoplada (CBO=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/startup/CatedraCatalogListenerStarter.java:19`
- clase 'CatedraKafkaConnectivityVerifier' muy acoplada (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/verification/CatedraKafkaConnectivityVerifier.java:15`
- clase 'JpaCatalogIncrementalPersistenceAdapter' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaCatalogIncrementalPersistenceAdapter.java:17`
- clase 'JpaProcessedCatalogEventAdapter' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapter.java:14`
- clase 'ProcessedCatalogEventJpaRepository' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/repository/ProcessedCatalogEventJpaRepository.java:10`
- clase 'CatedraRedisCatalogChangesAdapter' muy acoplada (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/adapter/CatedraRedisCatalogChangesAdapter.java:22`
- clase 'CatedraRedisCatalogChangesAdapter' instancia 5 concretos (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/adapter/CatedraRedisCatalogChangesAdapter.java:22`
- clase 'CatedraRedisClient' muy acoplada (CBO=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClient.java:17`
- clase 'CatedraRedisClient' instancia 4 concretos (CBO=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClient.java:17`
- clase 'CatedraRedisCatalogDecoder' muy acoplada (CBO=16) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/decoder/CatedraRedisCatalogDecoder.java:22`
- clase 'CatedraRedisCatalogDecoder' instancia 7 concretos (CBO=16) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/decoder/CatedraRedisCatalogDecoder.java:22`
- clase 'CatedraRestClient' muy acoplada (CBO=20) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClient.java:21`
- clase 'CatedraRestClient' instancia 5 concretos (CBO=20) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClient.java:21`
- clase 'SynchronizationRetryExecutor' muy acoplada (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutor.java:14`
- clase 'SynchronizationRetryExecutor' instancia 3 concretos (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutor.java:14`
- clase 'TransientSynchronizationFailureClassifier' muy acoplada (CBO=35) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/TransientSynchronizationFailureClassifier.java:42`
- clase 'CatalogReconciliationRunner' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/startup/CatalogReconciliationRunner.java:23`

**?** (7)
- clase 'InvalidCatalogSnapshotException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/exception/InvalidCatalogSnapshotException.java:5`
- clase 'CatedraRedisAccessException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/exception/CatedraRedisAccessException.java:3`
- clase 'CatedraRedisUnavailableException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/exception/CatedraRedisUnavailableException.java:3`
- clase 'InvalidCatedraRedisDataException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/exception/InvalidCatedraRedisDataException.java:3`
- clase 'CatedraHttpException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/exception/CatedraHttpException.java:6`
- clase 'CatedraTimeoutException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/exception/CatedraTimeoutException.java:3`
- clase 'CatedraTransportException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/exception/CatedraTransportException.java:3`

**OCP/type-switch** (3)
- clase 'CatedraKafkaConnectivityVerifier' con 3 type-checks — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/verification/CatedraKafkaConnectivityVerifier.java:15`
- clase 'CatedraRestClient' con 3 type-checks — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClient.java:21`
- clase 'TransientSynchronizationFailureClassifier' con 35 type-checks — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/TransientSynchronizationFailureClassifier.java:42`

**duplicación** (6)
- duplicación lógica: 6.7% · boilerplate: 7.0%
- bloque de 8 líneas repetido 7 veces en src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:29, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:30, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:31 — `src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:29`
- bloque de 8 líneas repetido 3 veces en src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:216, src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java:225, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:66 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:216`
- bloque de 8 líneas repetido 3 veces en src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:218, src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java:227, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:68 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:218`
- bloque de 8 líneas repetido 3 veces en src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:220, src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java:229, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:70 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:220`
- bloque de 8 líneas repetido 3 veces en src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:221, src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java:230, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:71 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:221`

## 6. Evolución por commit

Métricas calculadas sobre el estado de `main` en **cada commit** (3 commits con trabajo), del primero al último. La calidad usa el perfil `exigente`; el aporte de código lo mide RepoSense. El índice puede **bajar** al crecer el proyecto: se acumulan señales.

```mermaid
xychart-beta
    title "Índice de calidad a lo largo de los commits"
    x-axis ["afc001c", "9353e2d", "d80fc2b"]
    y-axis "Índice" 0 --> 100
    line [0, 15, 58]
```

| # | Fecha | Commit | Autor | LOC | Índice | %POO | Duplic. | Sueltas |
|---|---|---|---|---:|---:|---:|---:|---:|
| 1 | 2026-09-14 | `afc001c` | Ing. Daniel Quinteros | 0 | 0 | 0% | 0.0% | 0 |
| 2 | 2026-09-14 | `9353e2d` | Ing. Daniel Quinteros | 0 | 15 | 0% | 0.0% | 0 |
| 3 | 2026-11-05 | `d80fc2b` | Vicente Cara Tapia | 10,703 | 58 | 79% | 6.7% | 0 |

---
_Informe generado por la cátedra. Las dimensiones y umbrales corresponden a `calidad.py` (perfil `exigente`); el semáforo, a `CRITERIOS-CALIDAD.md`. La documentación del repositorio no se evalúa. Ante dudas, revisá el archivo señalado._
<!-- evaluacion:fin -->
