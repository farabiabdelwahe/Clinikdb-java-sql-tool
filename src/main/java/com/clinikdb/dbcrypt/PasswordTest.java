package com.clinikdb.dbcrypt;

/**
 * Test class to verify that wrong passwords throw exceptions properly
 */
public class PasswordTest {
    public static void main(String[] args) {
        SqliteTool tool = new SqliteTool();

        // Test 1: Create database with correct password
        System.out.println("=== Test 1: Creating database with password 'correctPass' ===");
        try {
            String dbPath = "/tmp/test_password_db.sqlite";
            String correctPass = "correctPass";

            tool.createEncryptedDatabase(dbPath, correctPass);
            System.out.println("✓ Database created successfully");

            // Insert some data
            String createTable = "CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY, name TEXT);";
            tool.executeSql(createTable);
            System.out.println("✓ Table created successfully");

            String insert = "INSERT INTO test_table (id, name) VALUES (1, 'Test Data');";
            tool.executeSql(insert);
            System.out.println("✓ Data inserted successfully");

            tool.closeDb();
            System.out.println("✓ Database closed\n");

        } catch (Exception e) {
            System.err.println("✗ Unexpected error in Test 1:");
            e.printStackTrace();
            return;
        }

        // Test 2: Try to open with WRONG password - should throw exception
        System.out.println("=== Test 2: Opening database with WRONG password 'wrongPass' ===");
        try {
            String dbPath = "/tmp/test_password_db.sqlite";
            String wrongPass = "wrongPass";

            tool.initDb(dbPath, wrongPass, true);

            // Try to query - this should fail
            String query = "SELECT * FROM test_table;";
            tool.executeSql(query);

            // If we reach here, the test FAILED
            System.err.println("✗✗✗ TEST FAILED: No exception was thrown for wrong password! ✗✗✗");
            tool.closeDb();

        } catch (SQLCipherException e) {
            System.out.println("✓✓✓ TEST PASSED: SQLCipherException was properly thrown! ✓✓✓");
            System.out.println("Exception message: " + e.getMessage());
            System.out.println("Exit code: " + e.getExitCode());
            System.out.println("Error codes: " + e.getErrorCodes());
        } catch (Exception e) {
            System.err.println("✗ Unexpected exception type:");
            e.printStackTrace();
        }

        // Test 3: Open with CORRECT password - should work
        System.out.println("\n=== Test 3: Opening database with CORRECT password 'correctPass' ===");
        try {
            String dbPath = "/tmp/test_password_db.sqlite";
            String correctPass = "correctPass";

            tool.initDb(dbPath, correctPass, true);

            String query = "SELECT * FROM test_table;";
            String result = tool.executeSqlGetResultAsJson(query);
            System.out.println("✓ Query executed successfully");
            System.out.println("Result: " + result);

            tool.closeDb();
            System.out.println("✓ Test 3 completed successfully");

        } catch (Exception e) {
            System.err.println("✗ Unexpected error in Test 3:");
            e.printStackTrace();
        }

        System.out.println("\n=== All Tests Complete ===");
    }
}
