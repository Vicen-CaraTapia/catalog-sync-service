# catalog-sync-service

Backend Spring Boot que servirá como base del servicio de catálogo y sincronización.

## Requisitos

- Java 25
- Maven

## Ejecución local

```bash
mvn spring-boot:run
```

El servicio inicia por defecto en:

```text
http://localhost:8081
```

Podés cambiar el puerto mediante la variable de entorno `SERVER_PORT`.

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
