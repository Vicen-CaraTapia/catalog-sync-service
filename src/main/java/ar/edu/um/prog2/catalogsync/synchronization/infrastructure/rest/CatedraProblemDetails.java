package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest;

import org.jspecify.annotations.Nullable;

record CatedraProblemDetails(
		@Nullable String code,
		@Nullable String detail
) {

	static CatedraProblemDetails empty() {
		return new CatedraProblemDetails(null, null);
	}
}
