package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry;

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
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/** Fail closed for unknown outer errors; never infer transient behavior from diagnostic text. */
@Component
public final class TransientSynchronizationFailureClassifier {

    public boolean isRetryable(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean recognizedNetworkCause = false;
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (isPermanent(cause)) {
                return false;
            }
            recognizedNetworkCause |= isNetworkFailure(cause);
        }
        if (failure instanceof CatedraHttpException http) {
            return retryableStatus(http);
        }
        if (failure instanceof CatedraTimeoutException) {
            return true;
        }
        return (failure instanceof CatedraTransportException || failure instanceof CatedraRedisUnavailableException)
                && recognizedNetworkCause;
    }

    boolean isInterruptedFailure(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof InterruptedException) {
                return true;
            }
        }
        return false;
    }

    private static boolean retryableStatus(CatedraHttpException http) {
        int status = http.status().value();
        return status == 502 || status == 503 || status == 504;
    }

    private static boolean isPermanent(Throwable failure) {
        return failure instanceof CatedraRedisAccessException
                || failure instanceof InvalidCatedraRedisDataException
                || failure instanceof InvalidCatalogSnapshotException
                || failure instanceof InvalidCatalogUpdatedEventException
                || failure instanceof ConflictingCatalogEventException
                || failure instanceof StaleCatalogSnapshotException
                || failure instanceof InsufficientCatalogSnapshotVersionException
                || failure instanceof DataIntegrityViolationException
                || failure instanceof InvalidDataAccessApiUsageException
                || failure instanceof InvalidDataAccessResourceUsageException
                || failure instanceof AuthenticationException
                || failure instanceof AccessDeniedException
                || failure instanceof RedisCommandExecutionException
                || failure instanceof RedisProtocolException
                || failure instanceof ProtocolException
                || failure instanceof SSLException
                || failure instanceof GeneralSecurityException
                || failure instanceof InterruptedException
                || failure instanceof CancellationException
                || (failure instanceof CatedraHttpException http && !retryableStatus(http));
    }

    private static boolean isNetworkFailure(Throwable failure) {
        return failure instanceof ConnectException
                || failure instanceof SocketException
                || failure instanceof SocketTimeoutException
                || failure instanceof HttpTimeoutException
                || failure instanceof UnknownHostException
                || failure instanceof TimeoutException
                || failure instanceof RedisConnectionFailureException
                || failure instanceof RedisConnectionException
                || failure instanceof RedisCommandTimeoutException
                || failure instanceof QueryTimeoutException;
    }
}
