package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.CatedraRestClientConfiguration;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraHttpException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

class CatedraCatalogSnapshotClientTest {

    private static final AtomicReference<HttpHandler> HANDLER = new AtomicReference<>();

    private static HttpServer server;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> HANDLER.get().handle(exchange));
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void fetchesAndDeserializesTheCompleteSnapshot() {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        HANDLER.set(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            path.set(exchange.getRequestURI().getPath());
            respond(exchange, 200, MediaType.APPLICATION_JSON_VALUE, completeSnapshotJson());
        });

        withClient(client -> {
            CatalogSnapshotResponse snapshot = client.fetchSnapshot();

            assertThat(path.get()).isEqualTo("/api/synchronization/snapshot");
            assertThat(authorization.get()).isEqualTo("Bearer test-technical-jwt");
            assertThat(snapshot.snapshotVersion()).isEqualTo(7L);
            assertThat(snapshot.generatedAt()).isEqualTo(Instant.parse("2026-09-10T13:44:59Z"));

            assertThat(snapshot.professionalCategories()).singleElement().satisfies(category -> {
                assertThat(category.id()).isEqualTo(1L);
                assertThat(category.enabled()).isFalse();
            });
            assertThat(snapshot.professionals()).singleElement().satisfies(professional -> {
                assertThat(professional.categoryId()).isEqualTo(1L);
                assertThat(professional.enabled()).isFalse();
            });
            assertThat(snapshot.weeklySchedules()).singleElement().satisfies(schedule -> {
                assertThat(schedule.professionalId()).isEqualTo(15L);
                assertThat(schedule.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
                assertThat(schedule.startTime()).isEqualTo(LocalTime.of(9, 0));
                assertThat(schedule.endTime()).isEqualTo(LocalTime.of(12, 0));
                assertThat(schedule.slotDurationMinutes()).isEqualTo(30);
                assertThat(schedule.enabled()).isFalse();
            });
        });
    }

    @Test
    void acceptsEmptySnapshotCollections() {
        HANDLER.set(exchange -> respond(
                exchange,
                200,
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "snapshotVersion": 8,
                          "generatedAt": "2026-09-10T13:44:59Z",
                          "professionalCategories": [],
                          "professionals": [],
                          "weeklySchedules": []
                        }
                        """
        ));

        withClient(client -> {
            CatalogSnapshotResponse snapshot = client.fetchSnapshot();

            assertThat(snapshot.professionalCategories()).isEmpty();
            assertThat(snapshot.professionals()).isEmpty();
            assertThat(snapshot.weeklySchedules()).isEmpty();
        });
    }

    @Test
    void preservesHttpProblemResponses() {
        HANDLER.set(exchange -> respond(
                exchange,
                503,
                MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                "{\"code\":\"CATALOG_UNAVAILABLE\",\"detail\":\"Unavailable\"}"
        ));

        withClient(client -> assertThatThrownBy(client::fetchSnapshot)
                .isInstanceOfSatisfying(CatedraHttpException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.code()).isEqualTo("CATALOG_UNAVAILABLE");
                }));
    }

    @Test
    void translatesMalformedJsonAsAnInvalidSnapshot() {
        HANDLER.set(exchange -> respond(exchange, 200, MediaType.APPLICATION_JSON_VALUE, "{not-json"));

        withClient(client -> assertThatThrownBy(client::fetchSnapshot)
                .isInstanceOf(InvalidCatalogSnapshotException.class));
    }

    @Test
    void translatesIncompleteSnapshotStructureAsAnInvalidSnapshot() {
        HANDLER.set(exchange -> respond(
                exchange,
                200,
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "snapshotVersion": 7,
                          "generatedAt": "2026-09-10T13:44:59Z",
                          "professionalCategories": [],
                          "professionals": []
                        }
                        """
        ));

        withClient(client -> assertThatThrownBy(client::fetchSnapshot)
                .isInstanceOf(InvalidCatalogSnapshotException.class));
    }

    @Test
    void translatesNullCollectionElementsAsAnInvalidSnapshot() {
        HANDLER.set(exchange -> respond(
                exchange,
                200,
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "snapshotVersion": 7,
                          "generatedAt": "2026-09-10T13:44:59Z",
                          "professionalCategories": [null],
                          "professionals": [],
                          "weeklySchedules": []
                        }
                        """
        ));

        withClient(client -> assertThatThrownBy(client::fetchSnapshot)
                .isInstanceOf(InvalidCatalogSnapshotException.class));
    }

    @Test
    void translatesMissingRequiredElementFieldsAsAnInvalidSnapshot() {
        HANDLER.set(exchange -> respond(
                exchange,
                200,
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "snapshotVersion": 7,
                          "generatedAt": "2026-09-10T13:44:59Z",
                          "professionalCategories": [
                            {
                              "id": 1,
                              "name": "Clinical medicine",
                              "description": null,
                              "createdAt": "2026-07-01T12:00:00Z",
                              "updatedAt": "2026-09-10T13:40:00Z"
                            }
                          ],
                          "professionals": [],
                          "weeklySchedules": []
                        }
                        """
        ));

        withClient(client -> assertThatThrownBy(client::fetchSnapshot)
                .isInstanceOf(InvalidCatalogSnapshotException.class));
    }

    private void withClient(java.util.function.Consumer<CatedraCatalogSnapshotClient> assertion) {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        CatedraRestClientConfiguration.class,
                        CatedraCatalogSnapshotClient.class
                )
                .withBean(CatedraIntegrationProperties.class, this::properties)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(context -> assertion.accept(context.getBean(CatedraCatalogSnapshotClient.class)));
    }

    private CatedraIntegrationProperties properties() {
        return new CatedraIntegrationProperties(
                "test-catalog-sync",
                "test-technical-jwt",
                new CatedraIntegrationProperties.RestProperties(
                        "http://localhost:" + server.getAddress().getPort(),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1)
                ),
                new CatedraIntegrationProperties.RedisProperties(
                        "localhost",
                        6379,
                        "test-user",
                        "test-password",
                        "catedra:sync:*",
                        "alumnos:test-catalog-sync:*",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5)
                ),
                new CatedraIntegrationProperties.KafkaProperties(
                        "localhost:19092",
                        "test-catalog-sync-consumer",
                        "catedra.catalog.test-catalog-sync"
                )
        );
    }

    private static void respond(
            HttpExchange exchange,
            int status,
            String contentType,
            String body
    ) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] responseBody = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBody.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(responseBody);
        }
    }

    private static String completeSnapshotJson() {
        return """
                {
                  "snapshotVersion": 7,
                  "generatedAt": "2026-09-10T13:44:59Z",
                  "professionalCategories": [
                    {
                      "id": 1,
                      "name": "Clinical medicine",
                      "description": null,
                      "enabled": false,
                      "createdAt": "2026-07-01T12:00:00Z",
                      "updatedAt": "2026-09-10T13:40:00Z"
                    }
                  ],
                  "professionals": [
                    {
                      "id": 15,
                      "categoryId": 1,
                      "firstName": "Ana",
                      "lastName": "Gomez",
                      "enabled": false,
                      "createdAt": "2026-07-01T12:00:00Z",
                      "updatedAt": "2026-09-10T13:40:00Z"
                    }
                  ],
                  "weeklySchedules": [
                    {
                      "id": 21,
                      "professionalId": 15,
                      "dayOfWeek": "MONDAY",
                      "startTime": "09:00:00",
                      "endTime": "12:00:00",
                      "slotDurationMinutes": 30,
                      "enabled": false,
                      "createdAt": "2026-07-01T12:00:00Z",
                      "updatedAt": "2026-09-10T13:40:00Z"
                    }
                  ]
                }
                """;
    }
}
