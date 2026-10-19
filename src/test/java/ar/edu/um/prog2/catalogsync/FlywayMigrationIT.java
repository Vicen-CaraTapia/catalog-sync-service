package ar.edu.um.prog2.catalogsync;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void flywaySchemaHistoryExistsAfterApplicationStartup() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("SELECT to_regclass('flyway_schema_history')");
             ResultSet resultSet = statement.executeQuery()) {

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString(1)).isNotNull();
        }
    }
}
