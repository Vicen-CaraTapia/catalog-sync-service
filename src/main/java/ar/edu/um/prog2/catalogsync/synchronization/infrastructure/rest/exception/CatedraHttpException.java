package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;

public final class CatedraHttpException extends CatedraRestException {

	private final HttpStatusCode status;
	private final @Nullable String code;
	private final @Nullable String detail;

	public CatedraHttpException(
			HttpStatusCode status,
			@Nullable String code,
			@Nullable String detail
	) {
		super("Catedra request failed with HTTP status " + status.value(), null);
		this.status = status;
		this.code = code;
		this.detail = detail;
	}

	public HttpStatusCode status() {
		return status;
	}

	public @Nullable String code() {
		return code;
	}

	public @Nullable String detail() {
		return detail;
	}
}
