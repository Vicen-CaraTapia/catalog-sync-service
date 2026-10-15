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

	@Test
	void dataSourceConnectionIsValid(@Autowired DataSource dataSource) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertTrue(connection.isValid(2));
		}
	}

}
