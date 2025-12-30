
package com.clinikdb.dbcrypt;

public class Main {
	public static void main(String[] args) {
		SqliteTool tool = new SqliteTool();

		try {
			// Set your DB path and key
			String dbPath = "/Users/farabia/Desktop/testQuote1sDB.sqlite";
			String passkey = "passTest";

			System.out.println("=== Creating Encrypted Database ===");
			tool.createEncryptedDatabase(dbPath, passkey);

			// Create patient_field_templates table
			System.out.println("\n=== Creating patient_field_templates Table ===");
			String createTableSql = "CREATE TABLE IF NOT EXISTS patient_field_templates (" +
					"id TEXT PRIMARY KEY, " +
					"name TEXT, " +
					"prio INTEGER, " +
					"is_active INTEGER, " +
					"is_shared INTEGER, " +
					"doctor_id TEXT" +
					");";
			tool.executeSql(createTableSql);
			System.out.println("Table created successfully");

			// Insert test data with various quote scenarios
			System.out.println("\n=== Inserting Test Data ===");

			// Test 1: Normal insert with single quotes (correct way)
			String insert1 = "INSERT INTO patient_field_templates " +
					"(id, name, prio, is_active, is_shared, doctor_id) VALUES " +
					"('44d52a61-0aee-4ead-9fd8-ba8b6007f8e6', 'Default Template', 0, 1, 0, 'R7U23RX5gpOOj9tqyfYjd70bWNg1');";
			tool.executeSql(insert1);
			System.out.println("Inserted record 1: Default Template");

			// Test 2: Insert with apostrophe in name (using escaped single quote)
			String insert2 = "INSERT INTO patient_field_templates " +
					"(id, name, prio, is_active, is_shared, doctor_id) VALUES " +
					"('550e8400-e29b-41d4-a716-446655440000', 'Doctor''s Template', 1, 1, 0, 'R7U23RX5gpOOj9tqyfYjd70bWNg1');";
			tool.executeSql(insert2);
			System.out.println("Inserted record 2: Doctor's Template");

			// Test 3: Insert with special characters
			String insert3 = "INSERT INTO patient_field_templates " +
					"(id, name, prio, is_active, is_shared, doctor_id) VALUES " +
					"('660e8400-e29b-41d4-a716-446655440001', 'Template & Notes', 2, 1, 1, 'R7U23RX5gpOOj9tqyfYjd70bWNg1');";
			tool.executeSql(insert3);
			System.out.println("Inserted record 3: Template & Notes");

			// Test 4: SIMULATING THE PROBLEM - Insert with malformed doubled quotes
			// This simulates what happened in your original database
			System.out.println("\n=== Simulating Malformed Data (Doubled Quotes Issue) ===");
			String insert4 = "INSERT INTO patient_field_templates " +
					"(id, name, prio, is_active, is_shared, doctor_id) VALUES " +
					"('770e8400-e29b-41d4-a716-446655440002', '\"\"Malformed Template\"\"', 3, 1, 0, 'R7U23RX5gpOOj9tqyfYjd70bWNg1');";
			tool.executeSql(insert4);
			System.out.println("Inserted record 4: Malformed data with doubled quotes");

			// Query and display results - showing the problem
			System.out.println("\n=== Querying All Records (Notice the doubled quotes in record 4) ===");
			String selectAll = "SELECT * FROM patient_field_templates ORDER BY prio;";
			String result = tool.executeSqlGetResultAsJson(selectAll);
			System.out.println(result);

			// Demonstrate the cleanup solution
			System.out.println("\n=== Cleaning Up Malformed Data ===");
			cleanDoubledQuotesInColumn(tool, "patient_field_templates", "name");

			// Query again to show the fix
			System.out.println("\n=== After Cleanup (Doubled quotes removed) ===");
			String resultAfterCleanup = tool.executeSqlGetResultAsJson(selectAll);
			System.out.println(resultAfterCleanup);

			// Query specific record
			System.out.println("\n=== Querying Specific Record ===");
			String selectOne = "SELECT * FROM patient_field_templates WHERE id = '44d52a61-0aee-4ead-9fd8-ba8b6007f8e6' LIMIT 1;";
			String result2 = tool.executeSqlGetResultAsJson(selectOne);
			System.out.println("Query Result:");
			System.out.println(result2);

			// Update test
			System.out.println("\n=== Testing Update ===");
			String update = "UPDATE patient_field_templates SET name = 'Updated Default Template' WHERE id = '44d52a61-0aee-4ead-9fd8-ba8b6007f8e6';";
			tool.executeSql(update);
			System.out.println("Updated record");

			// Verify update
			String verifyUpdate = tool.executeSqlGetResultAsJson(selectOne);
			System.out.println("After Update:");
			System.out.println(verifyUpdate);

			tool.closeDb();
			System.out.println("\n=== Test Complete ===");

		} catch (Exception e) {
			System.err.println("Error occurred:");
			e.printStackTrace();
		}
	}

	/**
	 * Utility method to clean doubled quotes from a specific column in a table.
	 * This handles the case where data was incorrectly inserted with "" as part of
	 * the value.
	 * 
	 * @param tool       The SqliteTool instance
	 * @param tableName  The name of the table to clean
	 * @param columnName The name of the column to clean
	 */
	private static void cleanDoubledQuotesInColumn(SqliteTool tool, String tableName, String columnName) {
		try {
			// SQL to remove leading and trailing doubled quotes
			// REPLACE function removes all occurrences, so we need to be careful
			// This approach removes "" from the beginning and end of the string
			String cleanupSql = String.format(
					"UPDATE %s SET %s = " +
							"CASE " +
							"  WHEN %s LIKE '\"\"%%\"\"' THEN SUBSTR(%s, 3, LENGTH(%s) - 4) " +
							"  WHEN %s LIKE '\"\"%%' THEN SUBSTR(%s, 3) " +
							"  WHEN %s LIKE '%%\"\"' THEN SUBSTR(%s, 1, LENGTH(%s) - 2) " +
							"  ELSE %s " +
							"END " +
							"WHERE %s LIKE '%%\"\"%%';",
					tableName, columnName,
					columnName, columnName, columnName,
					columnName, columnName,
					columnName, columnName, columnName,
					columnName,
					columnName);

			tool.executeSql(cleanupSql);
			System.out.println("✓ Cleaned doubled quotes from column: " + columnName);

		} catch (Exception e) {
			System.err.println("Error cleaning doubled quotes: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
