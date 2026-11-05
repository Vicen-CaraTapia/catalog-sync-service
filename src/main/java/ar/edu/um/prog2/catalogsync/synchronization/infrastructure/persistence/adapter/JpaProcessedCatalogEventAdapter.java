package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.adapter;

import ar.edu.um.prog2.catalogsync.synchronization.application.exception.ConflictingCatalogEventException;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ProcessedCatalogEventPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.repository.ProcessedCatalogEventJpaRepository;
import java.util.Objects;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaProcessedCatalogEventAdapter implements ProcessedCatalogEventPort {

    private final ProcessedCatalogEventJpaRepository repository;

    public JpaProcessedCatalogEventAdapter(ProcessedCatalogEventJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public OptionalLong findProcessedVersion(String eventId) {
        var version = repository.findProcessedVersion(Objects.requireNonNull(eventId));
        return version.isPresent() ? OptionalLong.of(version.get()) : OptionalLong.empty();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void registerProcessed(String eventId, long newVersion) {
        repository.insertIfAbsent(Objects.requireNonNull(eventId), newVersion);
        // Separate statement: READ_COMMITTED sees a competing insert after its commit.
        long storedVersion = repository.findProcessedVersion(eventId)
                .orElseThrow(() -> new IllegalStateException("Processed catalog event registration is missing"));
        if (storedVersion != newVersion) {
            throw new ConflictingCatalogEventException();
        }
    }
}
