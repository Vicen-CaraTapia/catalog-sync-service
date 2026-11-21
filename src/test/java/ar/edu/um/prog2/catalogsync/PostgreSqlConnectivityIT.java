package ar.edu.um.prog2.catalogsync;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PostgreSqlConnectivityIT {
    @org.springframework.test.context.DynamicPropertySource
    static void internalJwtTestKey(org.springframework.test.context.DynamicPropertyRegistry registry) {
        ar.edu.um.prog2.catalogsynctest.security.InternalJwtTestKeys.register(registry);
    }


	@Test
	void dataSourceConnectionIsValid(@Autowired DataSource dataSource) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertTrue(connection.isValid(2));
		}
	}

}
