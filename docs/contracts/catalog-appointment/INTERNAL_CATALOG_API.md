# Catalog → Appointment internal HTTP contract

Status: implemented by C7.1/C7.2, consolidated and verified in C7.3 (Catalog Issue #28).
These are project conventions, not official cátedra authentication constants.

## Operation

`GET /internal/api/professionals/{id}` reads one professional from Catalog's current local replica.
The base URL is deployment configuration (for example, `http://localhost:8081` during local development).
No request body or query parameters are required. `id` is a signed 64-bit integer;
there is no additional positivity validation. A syntactically valid unknown ID returns 404.

Required headers:

```http
Authorization: Bearer <internal-service-jwt>
Accept: application/json
```

The operation uses only Catalog's local persistence. It does not call cátedra REST,
Redis or Kafka, or read Appointment's database. Successful responses have
`Content-Type: application/json`. Appointment must access this HTTP contract and
maintain its own client DTOs; it must not import Catalog classes, JPA entities or repositories.

## Successful response

```json
{
  "id": 201,
  "firstName": "Ana",
  "lastName": "Torres",
  "enabled": true,
  "category": {
    "id": 101,
    "name": "Cardiologia",
    "enabled": true
  },
  "weeklySchedules": [
    {
      "id": 301,
      "dayOfWeek": "MONDAY",
      "startTime": "12:00:00",
      "endTime": "15:00:00",
      "slotDurationMinutes": 30,
      "enabled": true
    }
  ]
}
```

All listed properties are present and non-null. The response DTOs contain only
these fields; no audit timestamps, persistence metadata or category description.

| Object | Properties and JSON types |
| --- | --- |
| Professional | `id`: int64, `firstName`: string, `lastName`: string, `enabled`: boolean, `category`: object, `weeklySchedules`: array |
| Category | `id`: int64, `name`: string, `enabled`: boolean |
| Weekly schedule | `id`: int64, `dayOfWeek`: string enum, `startTime`: string, `endTime`: string, `slotDurationMinutes`: integer, `enabled`: boolean |

`dayOfWeek` is one of `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`,
`SATURDAY`, `SUNDAY`. Times use ISO local time, such as `09:00:00`, with an optional
fractional-second component when present in the stored time. They contain no date,
time zone or UTC offset. Appointment must preserve int64 identifiers without loss of precision.

Disabled professionals and categories remain visible with `enabled=false`.
Schedules include both enabled and disabled entries; no schedules is represented by
`"weeklySchedules": []`. The implementation returns schedules by ascending ID;
clients should identify them by ID and must not interpret array position as weekday order.
These are weekly rules, not actual free slots. Appointment combines suitable rules
with its own availability and occupancy logic. A 200 response does not guarantee
that a professional is enabled or that an appointment can be booked.

## Authentication and authorization

Appointment signs the dedicated internal JWT with its RSA private key (RS256).
Catalog verifies it with the corresponding external RSA public key (at least 2048 bits).
The PEM format is X.509 SubjectPublicKeyInfo, delimited by `BEGIN PUBLIC KEY`.
Never share the private signing key with Catalog or commit key material or real JWTs.

| Claim | Required semantics |
| --- | --- |
| `iss` | Exactly `appointment-reservation-service` with the approved configuration |
| `aud` | Must contain `catalog-sync-service`; a string or array of strings is accepted |
| `sub` | Exactly `appointment-reservation-service` |
| `exp` | Mandatory valid expiration, expressed as JWT NumericDate |
| `nbf` | Validated when present, expressed as JWT NumericDate |
| `scope` | Space-separated string containing the exact permission `catalog:read` |

Catalog accepts a 30-second clock-skew tolerance for `exp` and `nbf`. A missing scope
or a different permission cannot authorize reading; malformed security claims are
rejected. `catalog:read-extra` does not grant `catalog:read`, and `scp` does not replace
`scope`. Other valid space-separated permissions may accompany `catalog:read`.

Example unsigned claim values for documentation only (not a usable token):

```json
{
  "iss": "appointment-reservation-service",
  "aud": ["catalog-sync-service"],
  "sub": "appointment-reservation-service",
  "exp": 2000000300,
  "scope": "catalog:read"
}
```

The cátedra technical JWT and the final-user JWT are separate identities and must
not be forwarded as the internal token. There is no login/token-issuing endpoint
in Catalog. Remote OIDC discovery, JWKS hosting and key rotation infrastructure are
outside this contract's current scope. Deployment must supply the matching keys.

Catalog configuration:

- `INTERNAL_CATALOG_JWT_PUBLIC_KEY_LOCATION`: external PEM URI, `file:/absolute/path/public.pem`.
- `INTERNAL_CATALOG_JWT_ISSUER`: `appointment-reservation-service`.
- `INTERNAL_CATALOG_JWT_AUDIENCE`: `catalog-sync-service`.

The internal chain is stateless and does not accept session authentication, form
login or HTTP Basic. CSRF protection remains enabled; this GET does not require a
CSRF token. Other internal routes are denied. An internal JWT does not authorize
the legacy `/api/professionals` routes. Use a protected transport for deployed
service communication; localhost HTTP examples are development examples.

## HTTP outcomes and errors

Authentication and authorization run before path conversion and professional lookup.
An unauthenticated request must not be used to distinguish unknown professionals.

| Status | Meaning | Content type |
| --- | --- | --- |
| 200 | Professional exists, including disabled professionals | `application/json` |
| 400 | Authorized request with ID not convertible to int64, including overflow | `application/problem+json` |
| 401 | Missing, invalid or expired internal JWT | `application/problem+json` |
| 403 | Authenticated JWT lacks the exact required permission | `application/problem+json` |
| 404 | Authorized request with syntactically valid but unknown ID | `application/problem+json` |

A missing professional returns:

```json
{
  "detail": "Professional not found.",
  "instance": "/internal/api/professionals/9223372036854775807",
  "status": 404,
  "title": "Not Found"
}
```

For 400, `title` is `Bad Request`, `detail` is
`Professional ID must be a valid 64-bit integer.`, and `instance` identifies the path.
For 401, the body has `status=401`, `title=Unauthorized` and
`detail=Internal service authentication is required.`; the header is
`WWW-Authenticate: Bearer`. For insufficient permission, the body has `status=403`,
`title=Forbidden` and `detail=Access to this internal operation is denied.`;
the header is `WWW-Authenticate: Bearer error="insufficient_scope"`.
These security error bodies currently omit `instance`.

Clients should branch on HTTP status rather than parse human-readable `detail`.
Problem responses must not expose JWT contents, claims or internal exception causes.
Unexpected server failures and transport timeouts are not equivalent to a missing
professional or an empty schedule list; no stable problem-body shape is promised
for unexpected 5xx failures. Client timeout/retry policy belongs to Appointment.

Example request, using an external header file to avoid embedding a real token:

```bash
curl -i --header @/path/to/authorization.header \
  --header 'Accept: application/json' \
  http://localhost:8081/internal/api/professionals/201
```

The local header file contains `Authorization: Bearer <internal-service-jwt>` and
must not be committed. Requests with an unknown numeric ID or `abc` exercise the
404 and 400 outcomes respectively after valid authentication.

## Contract verification

Catalog's `InternalProfessionalDetailsIT` exercises the production security chain,
a genuinely signed ephemeral RS256 JWT, controller, use case and PostgreSQL adapter.
It covers the exact success JSON, enabled/disabled entities, both schedule states,
empty arrays, int64 conversion errors, 404, 401 and 403. External synchronization
ports are controlled and no real cátedra infrastructure is required.

Run `./mvnw clean verify` for unit/MVC/security tests and
`./mvnw -Ppostgres-integration clean verify` with an externally configured disposable
PostgreSQL test database for integration tests. The profile changes test data and
must not target a development or production catalog database.

These are provider-side contract tests. They do not demonstrate a deployed
Appointment client or an end-to-end booking workflow. Appointment can implement
its HTTP client using this document without accessing Catalog's internals.
