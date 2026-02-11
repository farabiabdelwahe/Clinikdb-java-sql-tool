package com.clinikdb.dbcrypt;

/**
 * Test class to verify that NULL values are properly represented in JSON output
 */
public class NullValueTest {
    public static void main(String[] args) {
        SqliteTool tool = new SqliteTool();

        try {
            String dbPath = "/tmp/test_null_values.sqlite";
            String passkey = "testPass";

            System.out.println("=== Creating database and table ===");
            tool.createEncryptedDatabase(dbPath, passkey);

            // Create table with nullable columns
            String createTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT, " +
                    "phone TEXT, " +
                    "age INTEGER" +
                    ");";
            tool.executeSql(createTable);
            System.out.println("✓ Table created\n");

            // Insert test data with NULL values
            System.out.println("=== Inserting test data ===");

            // User 1: All fields populated
            String insert1 = "INSERT INTO users (id, name, email, phone, age) " +
                    "VALUES (1, 'John Doe', 'john@example.com', '123-456-7890', 30);";
            tool.executeSql(insert1);
            System.out.println("✓ Inserted User 1 (all fields populated)");

            // User 2: email is NULL
            String insert2 = "INSERT INTO users (id, name, email, phone, age) " +
                    "VALUES (2, 'Jane Smith', NULL, '555-1234', 25);";
            tool.executeSql(insert2);
            System.out.println("✓ Inserted User 2 (email is NULL)");

            // User 3: phone and age are NULL
            String insert3 = "INSERT INTO users (id, name, email, phone, age) " +
                    "VALUES (3, 'Bob Johnson', 'bob@example.com', NULL, NULL);";
            tool.executeSql(insert3);
            System.out.println("✓ Inserted User 3 (phone and age are NULL)");

            // User 4: All optional fields are NULL
            String insert4 = "INSERT INTO users (id, name, email, phone, age) " +
                    "VALUES (4, 'Alice Brown', NULL, NULL, NULL);";
            tool.executeSql(insert4);
            System.out.println("✓ Inserted User 4 (all optional fields are NULL)\n");

            // Query and display results
            System.out.println("=== Querying all users ===");
            String query = "SELECT * FROM users ORDER BY id;";
            String result = tool.executeSqlGetResultAsJson(query);

            System.out.println("JSON Result:");
            System.out.println(result);

            // Verify NULL values are properly represented
            System.out.println("\n=== Verification ===");
            if (result.contains("\"email\": null")) {
                System.out.println("✓✓✓ SUCCESS: NULL values are properly represented as JSON null");
            } else if (result.contains("\"email\": \"\"")) {
                System.out.println("✗✗✗ FAILED: NULL values are still represented as empty strings");
            } else {
                System.out.println("⚠ WARNING: Unexpected format");
            }

            tool.closeDb();
            System.out.println("\n=== Test Complete ===");

        } catch (Exception e) {
            System.err.println("✗ Error occurred:");
            e.printStackTrace();
        }
    }
}
