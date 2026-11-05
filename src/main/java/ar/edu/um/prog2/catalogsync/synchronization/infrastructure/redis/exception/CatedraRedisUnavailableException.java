package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception;

public final class CatedraRedisUnavailableException extends CatedraRedisException {

    public CatedraRedisUnavailableException(Throwable cause) {
		super("Catedra Redis is unavailable.", cause);
    }
}
