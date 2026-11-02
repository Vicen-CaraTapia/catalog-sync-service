package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.StaleCatalogSnapshotException;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.exception.InvalidCatalogSnapshotException;
import ar.edu.um.prog2.catalogsync.synchronization.application.exception.ConflictingCatalogEventException;
import ar.edu.um.prog2.catalogsync.synchronization.application.exception.InsufficientCatalogSnapshotVersionException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.InvalidCatalogUpdatedEventException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisAccessException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.InvalidCatedraRedisDataException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraHttpException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraTimeoutException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraTransportException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.protocol.RedisProtocolException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.ProtocolException;
import java.net.http.HttpTimeoutException;
import java.security.cert.CertificateException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class TransientSynchronizationFailureClassifierTest {

    private final TransientSynchronizationFailureClassifier classifier = new TransientSynchronizationFailureClassifier();

    @ParameterizedTest
    @ValueSource(ints = {502, 503, 504})
    void retriesOnlyApprovedHttpStatusesRegardlessOfDiagnosticText(int status) {
        assertThat(classifier.isRetryable(new CatedraHttpException(HttpStatusCode.valueOf(status),
                "diagnostic-code", "diagnostic-text"))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 400, 401, 403, 404, 408, 409, 422, 429, 500, 501, 505})
    void otherHttpStatusesAreNotRetriedEvenIfDetailSoundsTransient(int status) {
        assertThat(classifier.isRetryable(new CatedraHttpException(HttpStatusCode.valueOf(status),
                "TIMEOUT", "connection timed out; retry HTTP 503"))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("networkCauses")
    void recognizesTransportFailuresThroughTypedCauseChains(Throwable cause) {
        assertThat(classifier.isRetryable(new CatedraTransportException(new RuntimeException(cause)))).isTrue();
        assertThat(classifier.isRetryable(new CatedraRedisUnavailableException(new RuntimeException(cause)))).isTrue();
    }

    @Test
    void knownTimeoutCategoryIsRetryable() {
        assertThat(classifier.isRetryable(new CatedraTimeoutException(new HttpTimeoutException("private")))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("permanentFailures")
    void permanentFailuresVetoTransientCausesEvenWhenWrappedByATransientCategory(Throwable failure) {
        assertThat(classifier.isRetryable(failure)).isFalse();
        assertThat(classifier.isRetryable(new CatedraTimeoutException(failure))).isFalse();
        assertThat(classifier.isRetryable(new CatedraRedisUnavailableException(failure))).isFalse();
    }

    @Test
    void unknownWrappersAndMessagesDoNotCreateARetryClassification() {
        var timeout = new CatedraTimeoutException(new SocketTimeoutException());
        assertThat(classifier.isRetryable(new IllegalStateException("Redis version mismatch", timeout))).isFalse();
        assertThat(classifier.isRetryable(new IllegalStateException("connection refused HTTP 503"))).isFalse();
        assertThat(classifier.isRetryable(new CatedraTransportException(new IllegalStateException("timeout"))))
                .isFalse();
        assertThat(classifier.isRetryable(new CatedraRedisUnavailableException(null))).isFalse();
        assertThat(classifier.isRetryable(null)).isFalse();
    }

    @Test
    void causeCyclesCannotCauseUnboundedInspection() {
        var connection = new ConnectException();
        var failure = new CatedraTransportException(connection);
        connection.initCause(failure);

        assertThat(classifier.isRetryable(failure)).isTrue();
        assertThat(classifier.isInterruptedFailure(failure)).isFalse();
    }

    private static Stream<Throwable> networkCauses() {
        return Stream.of(new ConnectException(), new SocketException(), new SocketTimeoutException(),
                new HttpTimeoutException("private"), new UnknownHostException(), new TimeoutException(),
                new RedisConnectionFailureException("private"), new RedisConnectionException("private"),
                new RedisCommandTimeoutException("private"), new QueryTimeoutException("private"));
    }

    private static Stream<Throwable> permanentFailures() {
        var timeout = new CatedraTimeoutException(new SocketTimeoutException("private"));
        return Stream.of(new InvalidCatalogSnapshotException(timeout),
                new CatedraRedisAccessException(timeout), new InvalidCatedraRedisDataException(),
                new InvalidCatalogUpdatedEventException(), new ConflictingCatalogEventException(),
                new StaleCatalogSnapshotException(), new InsufficientCatalogSnapshotVersionException(),
                new DataIntegrityViolationException("private", timeout),
                new InvalidDataAccessApiUsageException("private", timeout),
                new InvalidDataAccessResourceUsageException("private", timeout),
                new BadCredentialsException("private", timeout), new AccessDeniedException("private", timeout),
                new RedisCommandExecutionException("private"), new SSLException("private", timeout),
                new CertificateException("private", timeout), new InterruptedException("private"),
                new GeneralSecurityException("private", timeout), new ProtocolException("private"),
                new RedisProtocolException("private"),
                new CancellationException("private"),
                new CatedraHttpException(HttpStatusCode.valueOf(401), null, "private"));
    }
}
