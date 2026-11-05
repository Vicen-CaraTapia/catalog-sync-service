package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception;

public abstract class CatedraRedisException extends RuntimeException {
    protected CatedraRedisException(String message, Throwable cause) {
        super(message, cause);
    }
}
