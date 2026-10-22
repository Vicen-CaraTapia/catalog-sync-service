package ar.edu.um.prog2.catalogsync.catalog.domain.model.search;

public record ProfessionalSearchCriteria(
        String name,
        Long categoryId,
        Boolean enabled,
        int page,
        int size
) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public ProfessionalSearchCriteria {
        if (name != null) {
            name = name.strip();
            if (name.isEmpty()) {
                name = null;
            }
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be non-negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
    }
}
