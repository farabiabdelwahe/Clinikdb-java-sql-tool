# SQLCipher Database Tool

A Java utility class for interacting with encrypted SQLite databases using SQLCipher and native binaries. This tool provides a simple interface to execute SQL queries on encrypted databases across Windows and macOS platforms.

## Features

- Cross-platform support (Windows and macOS)
- Encrypted SQLite database operations using SQLCipher
- Automatic resource extraction on Windows
- JSON output formatting for query results
- Comprehensive logging support
- Database creation and initialization

## Prerequisites

### Windows
- No additional setup required - the tool automatically extracts the necessary `sqlite3.exe` and `sqlite3.dll` files

### macOS
- **SQLCipher must be installed via Homebrew:**
  ```bash
  brew install sqlcipher
  ```
- The tool will automatically locate SQLCipher at `/opt/homebrew/bin/sqlcipher` or use the system PATH

## Installation

1. Include the `SqliteTool.java` class in your project under the package `com.clinikdb.dbcrypt`
2. Ensure the SQLCipher resources are available in your classpath (for Windows)
3. On macOS, install SQLCipher via Homebrew as mentioned above

## Usage

### Basic Example

```java
package com.clinikdb.dbcrypt;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        SqliteTool tool = new SqliteTool();

        try {
            // Set your DB path and passkey
            String dbPath = "/path/to/your/database.sqlite";
            String passkey = "your_encryption_key";

            // Initialize the tool
            tool.initDb(dbPath, passkey, true); // true enables logging

            // Execute a query and get JSON results
            String result = tool.executeSqlGetResultAsJson("SELECT * FROM patients;");
            System.out.println(result);

            // Close and cleanup
            tool.closeDb();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Creating a New Encrypted Database

```java
SqliteTool tool = new SqliteTool();

try {
    String dbPath = "/path/to/new/database.sqlite";
    String passkey = "your_encryption_key";

    // Create and initialize a new encrypted database
    tool.createEncryptedDatabase(dbPath, passkey);

    // The database is now ready for use with an initial table created
    String result = tool.executeSqlGetResultAsJson("SELECT * FROM init_table;");
    System.out.println(result);

    tool.closeDb();
} catch (Exception e) {
    e.printStackTrace();
}
```

### Advanced Usage Examples

```java
SqliteTool tool = new SqliteTool();

try {
    // Initialize
    tool.initDb("/path/to/database.sqlite", "encryption_key", true);

    // Create a table
    tool.executeSql("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, email TEXT);");

    // Insert data
    tool.executeSql("INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com');");

    // Query with different output formats
    List<String> rawResults = tool.executeSql("SELECT * FROM users;");
    String stringResult = tool.executeSqlAsString("SELECT COUNT(*) FROM users;");
    String jsonResult = tool.executeSqlGetResultAsJson("SELECT * FROM users;");

    System.out.println("Raw results: " + rawResults);
    System.out.println("String result: " + stringResult);
    System.out.println("JSON result: " + jsonResult);

    tool.closeDb();
} catch (Exception e) {
    e.printStackTrace();
}
```

## API Reference

### Core Methods

#### `initDb(String dbPath, String passkey, boolean enableLogging)`
Initializes the SQLite tool with database path, encryption key, and logging preference.

#### `createEncryptedDatabase(String dbPath, String passkey)`
Creates a new encrypted database file and initializes it with a basic table structure.

#### `executeSql(String sql)`
Executes SQL commands and returns results as a List of strings.

#### `executeSqlAsString(String sql)`
Executes SQL commands and returns results as a single newline-separated string.

#### `executeSqlGetResultAsJson(String sql)`
Executes SQL commands and returns results formatted as JSON.

#### `closeDb()`
Closes the database connection and cleans up temporary resources.

## Output Format

The `executeSqlGetResultAsJson()` method returns results in JSON format:

```json
[
  {
    "id": "1",
    "first_name": "John",
    "last_name": "Doe",
    "birth_date": "2025-06-27 18:00:25.874",
    "record_number": "123",
    "notes": "",
    "last_modified": "1751052544",
    "template_id": "1"
  },
  {
    "id": "4",
    "first_name": "Jane",
    "last_name": "Smith",
    "birth_date": "2025-06-27 18:00:25.874",
    "record_number": "1236",
    "notes": "",
    "last_modified": "1751052544",
    "template_id": "1"
  }
]
```

## Error Handling

The tool includes comprehensive error handling and logging:

- **Initialization errors**: Throws `IOException` if SQLCipher binary is not found or accessible
- **SQL execution errors**: Throws `IOException` or `InterruptedException` for database operation failures
- **Resource extraction errors**: Throws `FileNotFoundException` if required resources are missing

## Logging

The tool supports detailed logging with different levels:
- `INFO`: General operation information
- `FINE`: Detailed operation steps
- `FINEST`: Verbose debugging information
- `SEVERE`: Error conditions
- `WARNING`: Warning conditions

Logs are written to both console and `sqlite_tool.log` file.

## Platform-Specific Notes

### Windows
- Automatically extracts and uses bundled `sqlite3.exe` and `sqlite3.dll`
- Creates temporary files in system temp directory
- Handles Windows-specific path and environment variables

### macOS
- Requires SQLCipher installation via Homebrew: `brew install sqlcipher`
- Uses system-installed SQLCipher binary
- Falls back to PATH lookup if default location not found

## Security Considerations

- Encryption keys are passed as command-line arguments to the SQLCipher process
- Temporary files are created in system temp directory
- Consider securing the passkey in production environments
- The tool uses SQLCipher compatibility mode 3

## Dependencies

- Java 8 or higher
- SQLCipher (automatically handled on Windows, manual installation required on macOS)
- Standard Java libraries (no external JAR dependencies)
