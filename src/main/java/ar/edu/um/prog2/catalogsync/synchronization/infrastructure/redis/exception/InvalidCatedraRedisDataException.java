package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception;

public final class InvalidCatedraRedisDataException extends CatedraRedisException {

    public InvalidCatedraRedisDataException() {
        super("Invalid Catedra Redis synchronization data.", null);
    }
}
