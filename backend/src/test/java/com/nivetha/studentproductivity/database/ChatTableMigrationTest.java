package com.nivetha.studentproductivity.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class ChatTableMigrationTest {

	@Test
	void removesChatTablesAndPreservesProductivityData() throws Exception {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:chat-table-migration;DB_CLOSE_DELAY=-1");

		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE notifications (type ENUM('TASK_OVERDUE'))");
			statement.execute("CREATE TABLE chat_conversations (id BIGINT PRIMARY KEY)");
			statement.execute("""
					CREATE TABLE chat_messages (
						id BIGINT PRIMARY KEY,
						conversation_id BIGINT,
						FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id)
					)
					""");
			statement.execute("CREATE TABLE tasks (id BIGINT PRIMARY KEY, title VARCHAR(120))");
			statement.execute("INSERT INTO tasks (id, title) VALUES (1, 'Keep this task')");

			ScriptUtils.executeSqlScript(
					connection,
					new ClassPathResource("db/dev-migrations.sql")
			);

			assertFalse(tableExists(connection, "CHAT_MESSAGES"));
			assertFalse(tableExists(connection, "CHAT_CONVERSATIONS"));
			try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM tasks")) {
				result.next();
				assertEquals(1, result.getInt(1));
			}
			try (ResultSet result = statement.executeQuery("SELECT title FROM tasks WHERE id = 1")) {
				result.next();
				assertEquals("Keep this task", result.getString(1));
			}
		}
	}

	private boolean tableExists(Connection connection, String tableName) throws Exception {
		try (ResultSet tables = connection.getMetaData().getTables(null, "PUBLIC", tableName, null)) {
			return tables.next();
		}
	}
}
