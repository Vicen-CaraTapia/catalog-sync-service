package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.dto;

import org.jspecify.annotations.Nullable;

public record CatedraProblemDetails(
		@Nullable String code,
		@Nullable String detail
) {

	public static CatedraProblemDetails empty() {
		return new CatedraProblemDetails(null, null);
	}
}
