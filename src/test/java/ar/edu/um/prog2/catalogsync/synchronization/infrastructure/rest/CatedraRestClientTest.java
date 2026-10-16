package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraHttpException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraTimeoutException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraTransportException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

class CatedraRestClientTest {

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void getAddsTechnicalBearerTokenAndMapsSuccessfulResponse() throws IOException {
		AtomicReference<String> authorization = new AtomicReference<>();
		CatedraRestClient client = startClient(exchange -> {
			authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
			respond(exchange, 200, MediaType.APPLICATION_JSON_VALUE, "{\"value\":\"ok\"}");
		});

		TestResponse response = client.get("/probe", TestResponse.class);

		assertThat(response.value()).isEqualTo("ok");
		assertThat(authorization.get()).isEqualTo("Bearer test-technical-jwt");
	}

	@Test
	void getTranslatesClientProblemResponse() throws IOException {
		CatedraRestClient client = startClient(exchange -> respond(
				exchange,
				400,
				MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				"{\"code\":\"INVALID_REQUEST\",\"detail\":\"Invalid request\"}"
		));

		assertThatThrownBy(() -> client.get("/probe", TestResponse.class))
				.isInstanceOfSatisfying(CatedraHttpException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(exception.code()).isEqualTo("INVALID_REQUEST");
					assertThat(exception.detail()).isEqualTo("Invalid request");
				});
	}

	@Test
	void getTranslatesServerProblemResponseWithCode() throws IOException {
		CatedraRestClient client = startClient(exchange -> respond(
				exchange,
				503,
				MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				"{\"code\":\"CATALOG_UNAVAILABLE\",\"detail\":\"Unavailable\"}"
		));

		assertThatThrownBy(() -> client.get("/probe", TestResponse.class))
				.isInstanceOfSatisfying(CatedraHttpException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
					assertThat(exception.code()).isEqualTo("CATALOG_UNAVAILABLE");
				});
	}

	@Test
	void getPreservesServerStatusWhenProblemResponseHasNoCode() throws IOException {
		CatedraRestClient client = startClient(exchange -> respond(
				exchange,
				500,
				MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				"{\"detail\":\"Unexpected error\"}"
		));

		assertThatThrownBy(() -> client.get("/probe", TestResponse.class))
				.isInstanceOfSatisfying(CatedraHttpException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
					assertThat(exception.code()).isNull();
					assertThat(exception.detail()).isEqualTo("Unexpected error");
				});
	}

	@Test
	void getPreservesStatusWhenProblemBodyIsEmpty() throws IOException {
		CatedraRestClient client = startClient(exchange -> respond(
				exchange,
				404,
				MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				null
		));

		assertThatThrownBy(() -> client.get("/probe", TestResponse.class))
				.isInstanceOfSatisfying(CatedraHttpException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(exception.code()).isNull();
					assertThat(exception.detail()).isNull();
				});
	}

	@Test
	void getPreservesStatusWhenProblemBodyIsMalformed() throws IOException {
		CatedraRestClient client = startClient(exchange -> respond(
				exchange,
				502,
				MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				"{not-json"
		));

		assertThatThrownBy(() -> client.get("/probe", TestResponse.class))
				.isInstanceOfSatisfying(CatedraHttpException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
					assertThat(exception.code()).isNull();
					assertThat(exception.detail()).isNull();
				});
	}

	@Test
	void getPreservesStatusWhenErrorBodyIsNotProblemJson() throws IOException {
		CatedraRestClient client = startClient(exchange -> respond(
				exchange,
				500,
				MediaType.TEXT_PLAIN_VALUE,
				"not a problem response"
		));

		assertThatThrownBy(() -> client.get("/probe", TestResponse.class))
				.isInstanceOfSatisfying(CatedraHttpException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
					assertThat(exception.code()).isNull();
					assertThat(exception.detail()).isNull();
				});
	}

	@Test
	void getRejectsSuccessfulResponseWithoutBody() throws IOException {
		CatedraRestClient client = startClient(exchange -> respond(exchange, 204, null, null));

		assertThatThrownBy(() -> client.get("/probe", TestResponse.class))
				.isInstanceOfSatisfying(CatedraHttpException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.NO_CONTENT);
					assertThat(exception.code()).isNull();
					assertThat(exception.detail()).isNull();
				});
	}

	@Test
	void getTranslatesReadTimeout() throws IOException {
		CatedraRestClient client = startClient(exchange -> {
			try {
				Thread.sleep(250);
				respond(exchange, 200, MediaType.APPLICATION_JSON_VALUE, "{\"value\":\"late\"}");
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
			catch (IOException ignored) {
				// The client may have disconnected after its timeout.
			}
		}, Duration.ofMillis(25));

		assertThatThrownBy(() -> client.get("/timeout", TestResponse.class))
				.isInstanceOf(CatedraTimeoutException.class);
	}

	@Test
	void getTranslatesTransportFailure() throws IOException {
		int unavailablePort;
		try (ServerSocket socket = new ServerSocket(0)) {
			unavailablePort = socket.getLocalPort();
		}
		CatedraRestClient client = clientFor("http://localhost:" + unavailablePort, Duration.ofSeconds(1));

		assertThatThrownBy(() -> client.get("/probe", TestResponse.class))
				.isInstanceOf(CatedraTransportException.class);
	}

	private CatedraRestClient startClient(HttpHandler handler) throws IOException {
		return startClient(handler, Duration.ofSeconds(1));
	}

	private CatedraRestClient startClient(HttpHandler handler, Duration readTimeout) throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/", handler);
		server.start();
		return clientFor("http://localhost:" + server.getAddress().getPort(), readTimeout);
	}

	private CatedraRestClient clientFor(String baseUrl, Duration readTimeout) {
		CatedraIntegrationProperties properties = new CatedraIntegrationProperties(
				"test-catalog-sync",
				"test-technical-jwt",
				new CatedraIntegrationProperties.RestProperties(
						baseUrl,
						Duration.ofSeconds(1),
						readTimeout
				),
				new CatedraIntegrationProperties.RedisProperties(
						"localhost",
						6379,
						"test-user",
						"test-password",
						"catedra:sync:*",
						"alumnos:test-catalog-sync:*"
				),
				new CatedraIntegrationProperties.KafkaProperties(
						"localhost:19092",
						"test-catalog-sync-consumer",
						"catedra.catalog.test-catalog-sync"
				)
		);

		return new CatedraRestClientConfiguration().catedraRestClient(properties, new ObjectMapper());
	}

	private static void respond(
			HttpExchange exchange,
			int status,
			String contentType,
			String body
	) throws IOException {
		if (contentType != null) {
			exchange.getResponseHeaders().set("Content-Type", contentType);
		}
		if (body == null) {
			exchange.sendResponseHeaders(status, -1);
			return;
		}

		byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, responseBody.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(responseBody);
		}
	}

	private record TestResponse(String value) {
	}
}
