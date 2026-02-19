package com.clinikdb.dbcrypt;

/**
 * Test class to verify that NULL values are omitted but empty strings are
 * preserved in JSON output
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

            // Insert test data
            System.out.println("=== Inserting test data ===");

            // User 1: All fields populated
            tool.executeSql("INSERT INTO users (id, name, email, phone, age) " +
                    "VALUES (1, 'John Doe', 'john@example.com', '123-456-7890', 30);");
            System.out.println("✓ User 1: all fields populated");

            // User 2: email is NULL
            tool.executeSql("INSERT INTO users (id, name, email, phone, age) " +
                    "VALUES (2, 'Jane Smith', NULL, '555-1234', 25);");
            System.out.println("✓ User 2: email is NULL");

            // User 3: email is EMPTY STRING (not NULL!)
            tool.executeSql("INSERT INTO users (id, name, email, phone, age) " +
                    "VALUES (3, 'Bob Johnson', '', '555-5678', 40);");
            System.out.println("✓ User 3: email is EMPTY STRING");

            // User 4: phone is NULL, age is NULL
            tool.executeSql("INSERT INTO users (id, name, email, phone, age) " +
                    "VALUES (4, 'Alice Brown', 'alice@example.com', NULL, NULL);");
            System.out.println("✓ User 4: phone and age are NULL\n");

            // Query and display results
            System.out.println("=== JSON Output ===");
            String result = tool.executeSqlGetResultAsJson("SELECT * FROM users ORDER BY id;");
            System.out.println(result);

            // Verification
            System.out.println("\n=== Verification ===");

            // User 2 should NOT have "email" key (NULL)
            boolean user2NoEmail = !result.contains("\"name\": \"Jane Smith\"") ||
                    !resultForUser(result, "Jane Smith").contains("\"email\"");
            System.out.println(user2NoEmail
                    ? "✓ User 2: NULL email correctly omitted"
                    : "✗ User 2: NULL email should be omitted");

            // User 3 SHOULD have "email": "" (empty string)
            boolean user3HasEmptyEmail = resultForUser(result, "Bob Johnson").contains("\"email\": \"\"");
            System.out.println(user3HasEmptyEmail
                    ? "✓ User 3: empty string email correctly included as \"\""
                    : "✗ User 3: empty string email should be included as \"\"");

            // User 4 should NOT have "phone" or "age" keys (NULL)
            String user4Json = resultForUser(result, "Alice Brown");
            boolean user4NoPhone = !user4Json.contains("\"phone\"");
            boolean user4NoAge = !user4Json.contains("\"age\"");
            System.out.println(user4NoPhone
                    ? "✓ User 4: NULL phone correctly omitted"
                    : "✗ User 4: NULL phone should be omitted");
            System.out.println(user4NoAge
                    ? "✓ User 4: NULL age correctly omitted"
                    : "✗ User 4: NULL age should be omitted");

            tool.closeDb();
            System.out.println("\n=== Test Complete ===");

        } catch (Exception e) {
            System.err.println("✗ Error occurred:");
            e.printStackTrace();
        }
    }

    /** Extract the JSON object segment for a given user name */
    private static String resultForUser(String json, String name) {
        int idx = json.indexOf("\"" + name + "\"");
        if (idx < 0)
            return "";
        // Find the enclosing { ... }
        int start = json.lastIndexOf('{', idx);
        int end = json.indexOf('}', idx);
        if (start < 0 || end < 0)
            return "";
        return json.substring(start, end + 1);
    }
}
