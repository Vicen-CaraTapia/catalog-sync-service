package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraHttpException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraRestException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraTimeoutException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraTransportException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class CatedraRestClient {

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	CatedraRestClient(RestClient restClient, ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
	}

	public <T> T get(String relativePath, Class<T> responseType) {
		try {
			return restClient.get()
					.uri(relativePath)
					.exchange((request, response) -> readResponse(response, responseType));
		}
		catch (CatedraRestException exception) {
			throw exception;
		}
		catch (RestClientException exception) {
			throw translateTransportException(exception);
		}
	}

	private <T> T readResponse(
			RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response,
			Class<T> responseType
	) throws IOException {
		HttpStatusCode status = response.getStatusCode();
		if (status.isError()) {
			CatedraProblemDetails problemDetails = parseProblemDetails(response);
			throw new CatedraHttpException(status, problemDetails.code(), problemDetails.detail());
		}

		T body = response.bodyTo(responseType);
		if (body == null) {
			throw new CatedraHttpException(status, null, null);
		}
		return body;
	}

	private CatedraProblemDetails parseProblemDetails(
			RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response
	) {
		MediaType contentType = response.getHeaders().getContentType();
		if (contentType == null || !contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)) {
			return CatedraProblemDetails.empty();
		}

		try {
			byte[] body = response.getBody().readAllBytes();
			if (body.length == 0) {
				return CatedraProblemDetails.empty();
			}

			JsonNode root = objectMapper.readTree(body);
			if (root == null || !root.isObject()) {
				return CatedraProblemDetails.empty();
			}

			return new CatedraProblemDetails(textValue(root.get("code")), textValue(root.get("detail")));
		}
		catch (IOException | RuntimeException exception) {
			return CatedraProblemDetails.empty();
		}
	}

	private @Nullable String textValue(@Nullable JsonNode value) {
		return value != null && value.isTextual() ? value.asText() : null;
	}

	private CatedraRestException translateTransportException(RestClientException exception) {
		return hasTimeoutCause(exception)
				? new CatedraTimeoutException(exception)
				: new CatedraTransportException(exception);
	}

	private boolean hasTimeoutCause(Throwable exception) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause instanceof HttpTimeoutException
					|| cause instanceof SocketTimeoutException
					|| cause instanceof TimeoutException) {
				return true;
			}
		}
		return false;
	}
}
