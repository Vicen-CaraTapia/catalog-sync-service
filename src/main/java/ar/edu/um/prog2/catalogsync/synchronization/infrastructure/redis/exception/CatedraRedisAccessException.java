package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception;

public final class CatedraRedisAccessException extends CatedraRedisException {

    public CatedraRedisAccessException(Throwable cause) {
		super("Catedra Redis rejected the requested access.", cause);
    }
}
