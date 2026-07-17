package com.vetri.smartcampus;

import com.vetri.smartcampus.models.common.DataBaseConnection;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class SmartCampusApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SmartCampusApplication.class, args);
		seedSupabaseSampleData(context);
	}

	private static void seedSupabaseSampleData(ConfigurableApplicationContext context) {
		boolean enabled = context.getEnvironment().getProperty("smartcampus.seed-sample-data", Boolean.class, true);
		if (!enabled) {
			System.out.println("SmartCampus sample data seed skipped.");
			return;
		}

		try (Connection connection = DataBaseConnection.getConnection()) {
			if (connection == null) {
				throw new IllegalStateException("Database connection is not available.");
			}

			runSqlScript(connection, "db/supabase/schema.sql");
			runSqlScript(connection, "db/supabase/sample_data.sql");
			System.out.println("SmartCampus Supabase schema and sample data seeded successfully.");
		} catch (Exception e) {
			throw new IllegalStateException("Failed to seed SmartCampus Supabase sample data.", e);
		}
	}

	private static void runSqlScript(Connection connection, String classpathLocation) throws Exception {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		String sql = resource.getContentAsString(StandardCharsets.UTF_8);

		try (Statement statement = connection.createStatement()) {
			for (String command : splitSqlStatements(sql)) {
				if (!command.isBlank()) {
					statement.execute(command);
				}
			}
		}
	}

	private static List<String> splitSqlStatements(String sql) {
		List<String> statements = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inSingleQuote = false;

		for (int i = 0; i < sql.length(); i++) {
			char currentChar = sql.charAt(i);
			char nextChar = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

			if (!inSingleQuote && currentChar == '-' && nextChar == '-') {
				while (i < sql.length() && sql.charAt(i) != '\n') {
					i++;
				}
				continue;
			}

			current.append(currentChar);

			if (currentChar == '\'' && nextChar == '\'') {
				current.append(nextChar);
				i++;
				continue;
			}

			if (currentChar == '\'') {
				inSingleQuote = !inSingleQuote;
				continue;
			}

			if (currentChar == ';' && !inSingleQuote) {
				statements.add(current.substring(0, current.length() - 1).trim());
				current.setLength(0);
			}
		}

		if (!current.isEmpty()) {
			statements.add(current.toString().trim());
		}
		return statements;
	}
}
