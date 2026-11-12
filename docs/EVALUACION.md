<!-- evaluacion:inicio -->
# Evaluación — Vicen-CaraTapia/catalog-sync-service

> Documento **generado automáticamente** por la cátedra de Programación II (UM): RepoSense + análisis de calidad de **7 dimensiones**. **No editar a mano**: se regenera en cada evaluación.
> Rama evaluada: `main`. Análisis **heurístico** (regex + métricas) + build: es una criba para orientar la revisión, **no una nota**. La **documentación** del repositorio no se evalúa.
> Generado: 2026-11-12.

## 1. Veredicto

| Aspecto | Resultado |
|---|---|
| Índice de calidad | **69/100** |
| Tipo de repositorio | `backend` |
| Build (`mvnw verify` / `gradlew check`) | OK |
| Código (LOC) | 10,703 |
| Archivos de código | 179 |
| Clases | 201 |
| Código dentro de clases (%POO) | 82% |
| Funciones fuera de clases | 0 |
| Duplicación (puerta) | 6.7% |
| Duplicación lógica | 7.0% |
| Duplicación de boilerplate | 7.0% |
| Cobertura | 98% |

> Este informe **describe** lo que el análisis midió con los criterios ya establecidos de la materia. No es una nota.

## 2. Semáforo de aprobación

Criterios de la cátedra (`CRITERIOS-CALIDAD.md`): **Aprobar** se juega con el perfil `estandar`; **destacar**, con el `exigente` (meta de excelencia).

| Semáforo | Resultado |
|---|---|
| Aprobar (`estandar`) | **no cumple** — 6/11 |
| Destacar (`exigente`) | **no cumple** — 6/11 |

**Para aprobar, falta cumplir:** SOLID; Duplicación

**Para destacar, falta cumplir:** SOLID; Duplicación

## 3. Autoría (RepoSense)

| Autor | Commits | Código (LOC) | Tests (LOC) | Docs (LOC) | Días activos | % del código |
|---|---:|---:|---:|---:|---:|---:|
| Vicente Cara Tapia | 54 | 3,845 | 8,838 | 252 | 14 | 100% |

> _El LOC de autoría lo calcula RepoSense sobre el historial y puede diferir del LOC de código del análisis (que mira el estado de `main`)._

## 4. Dimensiones evaluadas

Cada dimensión indica **qué mide**, **por qué importa** y **tu resultado**. Cuando el puntaje queda por debajo de la referencia, se agregan preguntas para **investigar la causa** (no hay recetas).

| Dimensión | Peso | Puntaje |
|:--|--:|--:|
| **D1** Diseño OO, SOLID y patrones (20%) | 20% | 1 |
| **D2** Arquitectura hexagonal — backend (15%) | 15% | 100 |
| **D3** Frontend KMP: MVVM y buenas prácticas (10%) | 10% | N/A |
| **D4** Integración y contrato (10%) | 10% | 50 |
| **D5** Seguridad y robustez (10%) | 10% | 100 |
| **D6** Pruebas y cobertura (20%) | 20% | 100 |
| **D7** Proceso y entrega (15%) | 15% | 77 |

### D1. Diseño OO, SOLID y patrones (20%)

- **Qué mide:** Métricas orientadas a objetos (clases, métodos, campos, cohesión, acoplamiento, herencia) y proxies de SOLID, más patrones de diseño.
- **Por qué importa:** El eje de la materia es el buen uso de la POO y su arquitectura: un diseño con clases claras y poco acopladas es más fácil de entender, probar y cambiar.
- **Tu resultado:** **1/100**
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

El análisis encontró las siguientes señales (archivo, sin número de línea a propósito):

**SRP/responsabilidad** (87)
- clase 'CatedraRedisCatalogDecoder' grande (LOC=191, métodos=25, campos=1) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/decoder/CatedraRedisCatalogDecoder.java`
- clase 'CatedraRedisClient' grande (LOC=90, métodos=13, campos=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClient.java`
- clase 'CatedraRedisClient' con baja cohesión (40%) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClient.java`
- clase 'CatedraRedisCatalogChangesAdapter' grande (LOC=69, métodos=15, campos=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/adapter/CatedraRedisCatalogChangesAdapter.java`
- clase 'CatalogUpdatedListener' sin métodos (4 campos): solo datos — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogUpdatedListener.java`
- método 'SynchronizationRetryExecutor.execute' de 35 líneas — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutor.java`
- método 'SynchronizationRetryExecutor.execute' con complejidad 8 — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutor.java`
- método 'CatalogVersionSynchronizationService.synchronizeTo' de 37 líneas — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationService.java`
- método 'CatalogVersionSynchronizationService.synchronizeTo' con complejidad 10 — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationService.java`
- clase 'CatalogPersistenceMapper' grande (LOC=72, métodos=12, campos=0) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/mapper/CatalogPersistenceMapper.java`
- clase 'JpaCatalogSnapshotPersistenceAdapter' con baja cohesión (17%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapter.java`
- método 'JpaCatalogSnapshotPersistenceAdapter.apply' de 48 líneas — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapter.java`
- clase 'CatalogSyncStateEntity' con baja cohesión (40%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/CatalogSyncStateEntity.java`
- clase 'ProfessionalCategoryEntity' con baja cohesión (0%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/ProfessionalCategoryEntity.java`
- clase 'WeeklyScheduleEntity' grande (LOC=71, métodos=10, campos=9) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/WeeklyScheduleEntity.java`
- clase 'WeeklyScheduleEntity' con baja cohesión (0%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/WeeklyScheduleEntity.java`
- clase 'ProfessionalEntity' con baja cohesión (0%) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/ProfessionalEntity.java`
- clase 'ProfessionalSearchCriteria' sin métodos (3 campos): solo datos — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/search/ProfessionalSearchCriteria.java`
- clase 'CatedraRedisCatalogDecoderTest' grande (LOC=154, métodos=16, campos=1) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/decoder/CatedraRedisCatalogDecoderTest.java`
- clase 'CatedraRedisIncrementalIT' grande (LOC=148, métodos=21, campos=5) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- clase 'CatedraRedisIncrementalIT' con baja cohesión (29%) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- método 'CatedraRedisIncrementalIT.readsMetadataChangesAndAffectedCurrentEntitiesThroughRealRedis' de 43 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- método 'CatedraRedisIncrementalIT.assertThatThrownBy' de 52 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- método 'CatedraRedisIncrementalIT.assertThatThrownBy' con complejidad 7 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- método 'CatedraRedisIncrementalIT.LocalRedisServer' de 49 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- método 'CatedraRedisIncrementalIT.LocalRedisServer' con complejidad 7 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- método 'LocalRedisServer.LocalRedisServer' de 49 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- método 'LocalRedisServer.LocalRedisServer' con complejidad 7 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- clase 'CatedraRedisCatalogChangesAdapterTest' grande (LOC=78, métodos=10, campos=1) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/adapter/CatedraRedisCatalogChangesAdapterTest.java`
- método 'CatedraRedisCatalogChangesAdapterTest.resolvesAffectedIdsToCurrentHashStateAndMapsToDomain' de 35 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/adapter/CatedraRedisCatalogChangesAdapterTest.java`
- clase 'CatedraRestClientTest' grande (LOC=203, métodos=23, campos=1) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java`
- clase 'CatedraCatalogListenerLifecycleTest' con baja cohesión (0%) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/startup/CatedraCatalogListenerLifecycleTest.java`
- método 'CatedraCatalogListenerLifecycleTest.Fixture' con complejidad 6 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/startup/CatedraCatalogListenerLifecycleTest.java`
- método 'Fixture.Fixture' con complejidad 6 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/startup/CatedraCatalogListenerLifecycleTest.java`
- método 'CatedraCatalogLiveNoOpIT.repeatedNotificationReadsRealMetadataWithoutChangingTheLocalCatalog' de 30 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatedraCatalogLiveNoOpIT.java`
- clase 'CatalogGapRecoveryIT' grande (LOC=291, métodos=33, campos=15) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogGapRecoveryIT.java`
- clase 'CatalogGapRecoveryIT' con baja cohesión (40%) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogGapRecoveryIT.java`
- clase 'CatalogIncrementalSynchronizationIT' grande (LOC=189, métodos=15, campos=14) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogIncrementalSynchronizationIT.java`
- método 'CatalogIncrementalSynchronizationIT.appliesEveryPendingVersionAndAcknowledgesAfterTheFinalCommit' de 33 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogIncrementalSynchronizationIT.java`
- método 'CatalogIncrementalSynchronizationIT.appliesEveryPendingVersionAndAcknowledgesAfterTheFinalCommit' con complejidad 6 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogIncrementalSynchronizationIT.java`
- método 'CatalogIncrementalSynchronizationIT.retriesFromTheNewDurableVersionAfterAnEarlierIncrementCommitted' con complejidad 6 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogIncrementalSynchronizationIT.java`
- clase 'CatalogUpdatedListenerTest' grande (LOC=84, métodos=12, campos=4) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogUpdatedListenerTest.java`
- clase 'CatalogUpdatedEventDecoderTest' grande (LOC=74, métodos=10, campos=1) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/decoder/CatalogUpdatedEventDecoderTest.java`
- clase 'CatedraKafkaConnectivityVerifierTest' con baja cohesión (21%) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/verification/CatedraKafkaConnectivityVerifierTest.java`
- método 'CatedraCatalogListenerConfigurationTest.reusesCatedraConsumerFactoryAndKeepsListenerInactiveWithManualAcknowledgment' de 30 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/config/CatedraCatalogListenerConfigurationTest.java`
- clase 'ApplyCatalogIncrementalVersionIT' grande (LOC=128, métodos=12, campos=19) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/ApplyCatalogIncrementalVersionIT.java`
- clase 'ApplyCatalogIncrementalVersionIT' con baja cohesión (44%) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/ApplyCatalogIncrementalVersionIT.java`
- clase 'CatalogReconciliationIT' grande (LOC=252, métodos=27, campos=14) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/CatalogReconciliationIT.java`
- clase 'CatalogReconciliationIT' con baja cohesión (33%) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/CatalogReconciliationIT.java`
- clase 'JpaProcessedCatalogEventAdapterIT' grande (LOC=198, métodos=19, campos=4) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapterIT.java`
- clase 'JpaProcessedCatalogEventAdapterIT' con baja cohesión (49%) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapterIT.java`
- método 'JpaProcessedCatalogEventAdapterIT.seesCompetingUncommittedInsertAfterWaitingForItsCommit' de 33 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapterIT.java`
- clase 'JpaProcessedCatalogEventAdapterTest' grande (LOC=73, métodos=10, campos=2) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapterTest.java`
- clase 'JpaCatalogIncrementalPersistenceAdapterTest' grande (LOC=84, métodos=6, campos=10) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaCatalogIncrementalPersistenceAdapterTest.java`
- clase 'TransientSynchronizationFailureClassifierTest' grande (LOC=73, métodos=10, campos=1) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/TransientSynchronizationFailureClassifierTest.java`
- clase 'SynchronizationRetryExecutorTest' grande (LOC=211, métodos=33, campos=2) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutorTest.java`
- clase 'CatalogVersionSynchronizationServiceTest' grande (LOC=251, métodos=27, campos=5) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationServiceTest.java`
- método 'CatalogVersionSynchronizationServiceTest.appliesEveryPendingVersionInOrder' con complejidad 6 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationServiceTest.java`
- clase 'CatalogInitializationIT' grande (LOC=206, métodos=22, campos=11) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationIT.java`
- clase 'CatalogInitializationIT' con baja cohesión (16%) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationIT.java`
- método 'CatalogInitializationIT.completeSnapshotJson' de 36 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationIT.java`
- clase 'CatedraCatalogSnapshotClientTest' grande (LOC=245, métodos=18, campos=2) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java`
- método 'CatedraCatalogSnapshotClientTest.fetchesAndDeserializesTheCompleteSnapshot' de 32 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java`
- método 'CatedraCatalogSnapshotClientTest.completeSnapshotJson' de 42 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java`
- método 'CatedraCatalogSnapshotMapperTest.isInstanceOf' de 37 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/mapper/CatedraCatalogSnapshotMapperTest.java`
- método 'CatedraCatalogSnapshotMapperTest.validResponse' de 34 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/mapper/CatedraCatalogSnapshotMapperTest.java`
- clase 'CatalogPersistenceIT' grande (LOC=67, métodos=1, campos=10) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/CatalogPersistenceIT.java`
- método 'CatalogPersistenceIT.persistsAndRecoversCatalogEntitiesAndInitialSyncState' de 49 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/CatalogPersistenceIT.java`
- clase 'ApplyCatalogSnapshotIT' grande (LOC=331, métodos=25, campos=14) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/ApplyCatalogSnapshotIT.java`
- clase 'ApplyCatalogSnapshotIT' con baja cohesión (21%) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/ApplyCatalogSnapshotIT.java`
- método 'ApplyCatalogSnapshotIT.assertThat' de 31 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/ApplyCatalogSnapshotIT.java`
- clase 'JpaProfessionalDetailsAdapterIT' grande (LOC=93, métodos=3, campos=14) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalDetailsAdapterIT.java`
- método 'JpaProfessionalDetailsAdapterIT.loadsCategoryAndOnlyRequestedProfessionalsSchedulesIncludingDisabledOnes' de 59 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalDetailsAdapterIT.java`
- clase 'JpaProfessionalSearchAdapterTest' con baja cohesión (29%) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterTest.java`
- método 'JpaProfessionalSearchAdapterTest.buildsNameCategoryAndEnabledPredicatesWithLiteralLikeEscaping' de 34 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterTest.java`
- método 'JpaProfessionalSearchAdapterTest.buildsNameCategoryAndEnabledPredicatesWithLiteralLikeEscaping' con complejidad 11 — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterTest.java`
- método 'JpaProfessionalSearchAdapterTest.addsCorrelatedExistsForEnabledScheduleOnRequestedWeekday' de 30 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterTest.java`
- método 'JpaProfessionalSearchAdapterTest.addsCorrelatedExistsForEnabledScheduleOnRequestedWeekday' con complejidad 11 — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterTest.java`
- clase 'JpaProfessionalSearchAdapterIT' grande (LOC=275, métodos=21, campos=19) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterIT.java`
- método 'JpaProfessionalSearchAdapterIT.search' con 5 parámetros — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterIT.java`
- método 'JpaProfessionalSearchAdapterIT.ProfessionalSearchCriteria' con 6 parámetros — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterIT.java`
- método 'ProfessionalSearchControllerTest.returnsTheRequestedPageAndDelegatesNormalizedCriteria' de 34 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalSearchControllerTest.java`
- método 'ProfessionalDetailsControllerTest.returnsProfessionalCategoryAndEnabledAndDisabledSchedules' de 33 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalDetailsControllerTest.java`
- método 'ApplyCatalogSnapshotUseCaseImplTest.assertThatThrownBy' de 37 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/application/usecases/ApplyCatalogSnapshotUseCaseImplTest.java`
- método 'ApplyCatalogSnapshotUseCaseImplTest.snapshot' de 34 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/application/usecases/ApplyCatalogSnapshotUseCaseImplTest.java`
- clase 'CatalogSnapshotTest' grande (LOC=111, métodos=12, campos=2) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/CatalogSnapshotTest.java`
- método 'CatedraIntegrationPropertiesValidationTest.remove' de 31 líneas — `src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java`

**DIP/concretos** (128)
- clase 'CatedraRedisCatalogDecoder' muy acoplada (CBO=16) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/decoder/CatedraRedisCatalogDecoder.java`
- clase 'CatedraRedisCatalogDecoder' instancia 7 concretos (CBO=16) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/decoder/CatedraRedisCatalogDecoder.java`
- clase 'CatedraRedisClient' muy acoplada (CBO=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClient.java`
- clase 'CatedraRedisClient' instancia 4 concretos (CBO=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClient.java`
- clase 'CatedraRedisCatalogChangesAdapter' muy acoplada (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/adapter/CatedraRedisCatalogChangesAdapter.java`
- clase 'CatedraRedisCatalogChangesAdapter' instancia 5 concretos (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/adapter/CatedraRedisCatalogChangesAdapter.java`
- clase 'CatalogReconciliationRunner' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/startup/CatalogReconciliationRunner.java`
- clase 'CatedraRestClient' muy acoplada (CBO=20) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClient.java`
- clase 'CatedraRestClient' instancia 5 concretos (CBO=20) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClient.java`
- clase 'CatedraCatalogListenerStarter' muy acoplada (CBO=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/startup/CatedraCatalogListenerStarter.java`
- clase 'CatalogUpdatedListener' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogUpdatedListener.java`
- clase 'CatalogUpdatedEventDecoder' muy acoplada (CBO=8) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/decoder/CatalogUpdatedEventDecoder.java`
- clase 'CatedraKafkaConnectivityVerifier' muy acoplada (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/verification/CatedraKafkaConnectivityVerifier.java`
- clase 'CatedraKafkaConfiguration' muy acoplada (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/config/CatedraKafkaConfiguration.java`
- clase 'ProcessedCatalogEventJpaRepository' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/repository/ProcessedCatalogEventJpaRepository.java`
- clase 'JpaProcessedCatalogEventAdapter' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapter.java`
- clase 'JpaCatalogIncrementalPersistenceAdapter' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaCatalogIncrementalPersistenceAdapter.java`
- clase 'SynchronizationRetryExecutor' muy acoplada (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutor.java`
- clase 'SynchronizationRetryExecutor' instancia 3 concretos (CBO=13) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutor.java`
- clase 'TransientSynchronizationFailureClassifier' muy acoplada (CBO=35) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/TransientSynchronizationFailureClassifier.java`
- clase 'CatalogVersionSynchronizationService' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationService.java`
- clase 'CatalogVersionSynchronizationService' instancia 4 concretos (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationService.java`
- clase 'ReconcileCatalogStateUseCaseImpl' muy acoplada (CBO=9) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/application/usecases/ReconcileCatalogStateUseCaseImpl.java`
- clase 'CatalogInitializationRunner' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationRunner.java`
- clase 'CatedraCatalogSnapshotMapper' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/mapper/CatedraCatalogSnapshotMapper.java`
- clase 'CatedraCatalogSnapshotMapper' instancia 5 concretos (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/mapper/CatedraCatalogSnapshotMapper.java`
- clase 'JpaCatalogSnapshotPersistenceAdapter' muy acoplada (CBO=16) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapter.java`
- clase 'JpaCatalogSnapshotPersistenceAdapter' instancia 3 concretos (CBO=16) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapter.java`
- clase 'JpaProfessionalSearchAdapter' muy acoplada (CBO=14) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapter.java`
- clase 'JpaProfessionalDetailsAdapter' muy acoplada (CBO=10) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalDetailsAdapter.java`
- clase 'ProfessionalSearchController' muy acoplada (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalSearchController.java`
- clase 'ProfessionalSearchController' instancia 3 concretos (CBO=11) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalSearchController.java`
- clase 'CatalogSyncApplicationTests' muy acoplada (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/CatalogSyncApplicationTests.java`
- clase 'CatedraRedisCatalogDecoderTest' muy acoplada (CBO=16) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/decoder/CatedraRedisCatalogDecoderTest.java`
- clase 'CatedraRedisClientTest' muy acoplada (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClientTest.java`
- clase 'CatedraRedisClientTest' instancia 6 concretos (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisClientTest.java`
- clase 'CatedraRedisIncrementalReadTest' muy acoplada (CBO=14) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisIncrementalReadTest.java`
- clase 'CatedraRedisIncrementalReadTest' instancia 4 concretos (CBO=14) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/client/CatedraRedisIncrementalReadTest.java`
- clase 'CatedraRedisIncrementalIT' muy acoplada (CBO=27) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- clase 'CatedraRedisIncrementalIT' instancia 7 concretos (CBO=27) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- clase 'LocalRedisServer' muy acoplada (CBO=17) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- clase 'LocalRedisServer' instancia 7 concretos (CBO=17) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/verification/CatedraRedisIncrementalIT.java`
- clase 'CatedraRedisCatalogChangesAdapterTest' muy acoplada (CBO=11) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/adapter/CatedraRedisCatalogChangesAdapterTest.java`
- clase 'CatedraRedisCatalogChangesAdapterTest' instancia 3 concretos (CBO=11) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/adapter/CatedraRedisCatalogChangesAdapterTest.java`
- clase 'CatalogReconciliationRunnerTest' muy acoplada (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/startup/CatalogReconciliationRunnerTest.java`
- clase 'CatalogReconciliationRunnerTest' instancia 14 concretos (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/startup/CatalogReconciliationRunnerTest.java`
- clase 'CatalogReconciliationStartupTest' muy acoplada (CBO=9) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/startup/CatalogReconciliationStartupTest.java`
- clase 'CatedraRestClientTest' muy acoplada (CBO=43) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java`
- clase 'CatedraRestClientTest' instancia 5 concretos (CBO=43) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java`
- clase 'CatedraCatalogListenerLifecycleTest' muy acoplada (CBO=29) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/startup/CatedraCatalogListenerLifecycleTest.java`
- clase 'CatedraCatalogListenerLifecycleTest' instancia 12 concretos (CBO=29) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/startup/CatedraCatalogListenerLifecycleTest.java`
- clase 'TestApplication' muy acoplada (CBO=8) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/startup/CatedraCatalogListenerLifecycleTest.java`
- clase 'CatedraCatalogLiveNoOpIT' muy acoplada (CBO=21) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatedraCatalogLiveNoOpIT.java`
- clase 'CatedraCatalogLiveNoOpIT' instancia 3 concretos (CBO=21) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatedraCatalogLiveNoOpIT.java`
- clase 'CatalogGapRecoveryIT' muy acoplada (CBO=74) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogGapRecoveryIT.java`
- clase 'CatalogGapRecoveryIT' instancia 17 concretos (CBO=74) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogGapRecoveryIT.java`
- clase 'CatalogIncrementalSynchronizationIT' muy acoplada (CBO=45) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogIncrementalSynchronizationIT.java`
- clase 'CatalogIncrementalSynchronizationIT' instancia 10 concretos (CBO=45) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogIncrementalSynchronizationIT.java`
- clase 'CatalogUpdatedListenerTest' muy acoplada (CBO=18) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogUpdatedListenerTest.java`
- clase 'CatalogUpdatedListenerTest' instancia 10 concretos (CBO=18) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/listener/CatalogUpdatedListenerTest.java`
- clase 'CatalogUpdatedEventDecoderTest' muy acoplada (CBO=9) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/decoder/CatalogUpdatedEventDecoderTest.java`
- clase 'CatedraKafkaConnectivityVerifierTest' muy acoplada (CBO=16) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/verification/CatedraKafkaConnectivityVerifierTest.java`
- clase 'CatedraKafkaConnectivityVerifierTest' instancia 7 concretos (CBO=16) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/verification/CatedraKafkaConnectivityVerifierTest.java`
- clase 'CatedraCatalogListenerConfigurationTest' muy acoplada (CBO=13) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/config/CatedraCatalogListenerConfigurationTest.java`
- clase 'CatedraKafkaConfigurationTest' muy acoplada (CBO=13) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/config/CatedraKafkaConfigurationTest.java`
- clase 'ApplyCatalogIncrementalVersionIT' muy acoplada (CBO=46) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/ApplyCatalogIncrementalVersionIT.java`
- clase 'ApplyCatalogIncrementalVersionIT' instancia 14 concretos (CBO=46) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/ApplyCatalogIncrementalVersionIT.java`
- clase 'CatalogReconciliationIT' muy acoplada (CBO=58) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/CatalogReconciliationIT.java`
- clase 'CatalogReconciliationIT' instancia 13 concretos (CBO=58) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/CatalogReconciliationIT.java`
- clase 'JpaProcessedCatalogEventAdapterIT' muy acoplada (CBO=70) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapterIT.java`
- clase 'JpaProcessedCatalogEventAdapterIT' instancia 5 concretos (CBO=70) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapterIT.java`
- clase 'JpaProcessedCatalogEventAdapterTest' muy acoplada (CBO=11) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapterTest.java`
- clase 'JpaProcessedCatalogEventAdapterTest' instancia 5 concretos (CBO=11) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaProcessedCatalogEventAdapterTest.java`
- clase 'JpaCatalogIncrementalPersistenceAdapterTest' muy acoplada (CBO=25) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaCatalogIncrementalPersistenceAdapterTest.java`
- clase 'JpaCatalogIncrementalPersistenceAdapterTest' instancia 8 concretos (CBO=25) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/persistence/adapter/JpaCatalogIncrementalPersistenceAdapterTest.java`
- clase 'TransientSynchronizationFailureClassifierTest' muy acoplada (CBO=42) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/TransientSynchronizationFailureClassifierTest.java`
- clase 'TransientSynchronizationFailureClassifierTest' instancia 53 concretos (CBO=42) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/TransientSynchronizationFailureClassifierTest.java`
- clase 'SynchronizationRetryExecutorTest' muy acoplada (CBO=20) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutorTest.java`
- clase 'SynchronizationRetryExecutorTest' instancia 50 concretos (CBO=20) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/SynchronizationRetryExecutorTest.java`
- clase 'CatalogVersionSynchronizationServiceTest' muy acoplada (CBO=17) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationServiceTest.java`
- clase 'CatalogVersionSynchronizationServiceTest' instancia 13 concretos (CBO=17) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/service/CatalogVersionSynchronizationServiceTest.java`
- clase 'CatalogUpdateNotificationUseCaseImplTest' muy acoplada (CBO=8) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/usecases/CatalogUpdateNotificationUseCaseImplTest.java`
- clase 'CatalogUpdateNotificationUseCaseImplTest' instancia 6 concretos (CBO=8) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/usecases/CatalogUpdateNotificationUseCaseImplTest.java`
- clase 'RecoverCatalogFromSnapshotUseCaseImplTest' muy acoplada (CBO=15) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/usecases/RecoverCatalogFromSnapshotUseCaseImplTest.java`
- clase 'RecoverCatalogFromSnapshotUseCaseImplTest' instancia 8 concretos (CBO=15) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/usecases/RecoverCatalogFromSnapshotUseCaseImplTest.java`
- clase 'ReconcileCatalogStateUseCaseImplTest' muy acoplada (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/usecases/ReconcileCatalogStateUseCaseImplTest.java`
- clase 'ReconcileCatalogStateUseCaseImplTest' instancia 5 concretos (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/application/usecases/ReconcileCatalogStateUseCaseImplTest.java`
- clase 'CatalogInitializationStartupTest' muy acoplada (CBO=13) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationStartupTest.java`
- clase 'CatalogInitializationStartupTest' instancia 3 concretos (CBO=13) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationStartupTest.java`
- clase 'CatalogInitializationIT' muy acoplada (CBO=52) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationIT.java`
- clase 'CatalogInitializationIT' instancia 9 concretos (CBO=52) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationIT.java`
- clase 'CatalogInitializationRunnerTest' muy acoplada (CBO=14) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationRunnerTest.java`
- clase 'CatalogInitializationRunnerTest' instancia 14 concretos (CBO=14) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/startup/CatalogInitializationRunnerTest.java`
- clase 'CatedraCatalogSnapshotClientTest' muy acoplada (CBO=40) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java`
- clase 'CatedraCatalogSnapshotClientTest' instancia 3 concretos (CBO=40) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java`
- clase 'CatedraCatalogSnapshotMapperTest' muy acoplada (CBO=15) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/mapper/CatedraCatalogSnapshotMapperTest.java`
- clase 'CatedraCatalogSnapshotMapperTest' instancia 6 concretos (CBO=15) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/mapper/CatedraCatalogSnapshotMapperTest.java`
- clase 'CatalogPersistenceIT' muy acoplada (CBO=17) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/CatalogPersistenceIT.java`
- clase 'CatalogPersistenceIT' instancia 3 concretos (CBO=17) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/CatalogPersistenceIT.java`
- clase 'ApplyCatalogSnapshotIT' muy acoplada (CBO=75) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/ApplyCatalogSnapshotIT.java`
- clase 'ApplyCatalogSnapshotIT' instancia 21 concretos (CBO=75) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/ApplyCatalogSnapshotIT.java`
- clase 'CatalogPersistenceMapperTest' muy acoplada (CBO=15) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/mapper/CatalogPersistenceMapperTest.java`
- clase 'CatalogPersistenceMapperTest' instancia 8 concretos (CBO=15) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/mapper/CatalogPersistenceMapperTest.java`
- clase 'JpaProfessionalDetailsAdapterTest' muy acoplada (CBO=16) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalDetailsAdapterTest.java`
- clase 'JpaProfessionalDetailsAdapterTest' instancia 6 concretos (CBO=16) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalDetailsAdapterTest.java`
- clase 'JpaProfessionalDetailsAdapterIT' muy acoplada (CBO=30) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalDetailsAdapterIT.java`
- clase 'JpaProfessionalDetailsAdapterIT' instancia 12 concretos (CBO=30) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalDetailsAdapterIT.java`
- clase 'JpaProfessionalSearchAdapterTest' muy acoplada (CBO=28) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterTest.java`
- clase 'JpaProfessionalSearchAdapterTest' instancia 7 concretos (CBO=28) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterTest.java`
- clase 'JpaProfessionalSearchAdapterIT' muy acoplada (CBO=37) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterIT.java`
- clase 'JpaProfessionalSearchAdapterIT' instancia 6 concretos (CBO=37) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaProfessionalSearchAdapterIT.java`
- clase 'JpaCatalogSnapshotPersistenceAdapterTest' muy acoplada (CBO=13) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapterTest.java`
- clase 'JpaCatalogSnapshotPersistenceAdapterTest' instancia 6 concretos (CBO=13) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/adapter/JpaCatalogSnapshotPersistenceAdapterTest.java`
- clase 'CatalogPersistenceEntitiesTest' muy acoplada (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/CatalogPersistenceEntitiesTest.java`
- clase 'CatalogPersistenceEntitiesTest' instancia 4 concretos (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/persistence/entity/CatalogPersistenceEntitiesTest.java`
- clase 'ProfessionalSearchControllerTest' muy acoplada (CBO=11) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalSearchControllerTest.java`
- clase 'ProfessionalSearchControllerTest' instancia 7 concretos (CBO=11) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalSearchControllerTest.java`
- clase 'ProfessionalDetailsControllerTest' muy acoplada (CBO=15) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalDetailsControllerTest.java`
- clase 'ProfessionalDetailsControllerTest' instancia 9 concretos (CBO=15) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/web/controller/ProfessionalDetailsControllerTest.java`
- clase 'ApplyCatalogSnapshotUseCaseImplTest' muy acoplada (CBO=14) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/application/usecases/ApplyCatalogSnapshotUseCaseImplTest.java`
- clase 'ApplyCatalogSnapshotUseCaseImplTest' instancia 6 concretos (CBO=14) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/application/usecases/ApplyCatalogSnapshotUseCaseImplTest.java`
- clase 'GetProfessionalDetailsUseCaseImplTest' muy acoplada (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/application/usecases/GetProfessionalDetailsUseCaseImplTest.java`
- clase 'GetProfessionalDetailsUseCaseImplTest' instancia 6 concretos (CBO=12) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/application/usecases/GetProfessionalDetailsUseCaseImplTest.java`
- clase 'InitializeCatalogUseCaseImplTest' muy acoplada (CBO=8) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/application/usecases/InitializeCatalogUseCaseImplTest.java`
- clase 'InitializeCatalogUseCaseImplTest' instancia 5 concretos (CBO=8) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/application/usecases/InitializeCatalogUseCaseImplTest.java`
- clase 'CatalogSnapshotTest' muy acoplada (CBO=13) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/CatalogSnapshotTest.java`
- clase 'CatalogSnapshotTest' instancia 8 concretos (CBO=13) — `src/test/java/ar/edu/um/prog2/catalogsync/catalog/domain/model/CatalogSnapshotTest.java`
- clase 'CatedraIntegrationPropertiesValidationTest' muy acoplada (CBO=13) — `src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java`

**?** (7)
- clase 'InvalidCatedraRedisDataException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/exception/InvalidCatedraRedisDataException.java`
- clase 'CatedraRedisUnavailableException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/exception/CatedraRedisUnavailableException.java`
- clase 'CatedraRedisAccessException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/redis/exception/CatedraRedisAccessException.java`
- clase 'CatedraHttpException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/exception/CatedraHttpException.java`
- clase 'CatedraTimeoutException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/exception/CatedraTimeoutException.java`
- clase 'CatedraTransportException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/exception/CatedraTransportException.java`
- clase 'InvalidCatalogSnapshotException' con herencia profunda (DIT=2) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/exception/InvalidCatalogSnapshotException.java`

**OCP/type-switch** (3)
- clase 'CatedraRestClient' con 3 type-checks — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClient.java`
- clase 'CatedraKafkaConnectivityVerifier' con 3 type-checks — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/kafka/verification/CatedraKafkaConnectivityVerifier.java`
- clase 'TransientSynchronizationFailureClassifier' con 35 type-checks — `src/main/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/retry/TransientSynchronizationFailureClassifier.java`

**POO** (1)
- clase 'CatalogSnapshotResponse' solo con métodos estáticos (¿utilitaria?) — `src/main/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/dto/CatalogSnapshotResponse.java`

**duplicación** (6)
- duplicación lógica: 6.7% · boilerplate: 7.0%
- bloque de 8 líneas repetido 7 veces en src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:29, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:30, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:31 — `src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java`
- bloque de 8 líneas repetido 3 veces en src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:216, src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java:225, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:66 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java`
- bloque de 8 líneas repetido 3 veces en src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:218, src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java:227, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:68 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java`
- bloque de 8 líneas repetido 3 veces en src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:220, src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java:229, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:70 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java`
- bloque de 8 líneas repetido 3 veces en src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java:221, src/test/java/ar/edu/um/prog2/catalogsync/catalog/infrastructure/rest/snapshot/client/CatedraCatalogSnapshotClientTest.java:230, src/test/java/ar/edu/um/prog2/catalogsync/config/CatedraIntegrationPropertiesValidationTest.java:71 — `src/test/java/ar/edu/um/prog2/catalogsync/synchronization/infrastructure/rest/client/CatedraRestClientTest.java`

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
| 3 | 2026-11-05 | `d80fc2b` | Vicente Cara Tapia | 10,703 | 58 | 82% | 6.7% | 0 |

---
_Informe generado por la cátedra. Las dimensiones y umbrales corresponden a `calidad.py` (perfil `exigente`); el semáforo, a `CRITERIOS-CALIDAD.md`. La documentación del repositorio no se evalúa. Ante dudas, revisá el archivo señalado._
<!-- evaluacion:fin -->
