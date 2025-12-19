# SQLCipher Database Tool

A Java utility for interacting with encrypted SQLite databases using SQLCipher and native binaries.

## Features
- **Cross-platform**: Support for Windows and macOS.
- **Robust JSON**: Pretty-printed output with correct character escaping and comma-safe CSV parsing.
- **Gradle Build**: Modernized structure with shadow JAR support for easy deployment.
- **Bundled Binaries**: Windows binaries are bundled and extracted automatically.

## Prerequisites
- **macOS**: `brew install sqlcipher`
- **Windows**: No setup required.

## Building and Running
Create the fat JAR:
```bash
gradle shadowJar
```
Run the JAR:
```bash
java -jar build/libs/Clinikdb-java-sql-tool-all.jar
```

## Usage Example
```java
SqliteTool tool = new SqliteTool();
tool.initDb("/path/to/db.sqlite", "key", true);
String json = tool.executeSqlGetResultAsJson("SELECT * FROM patients;");
System.out.println(json);
tool.closeDb();
```

## Output Format
```json
[
  {
    "id": "1",
    "name": "Jane Doe",
    "notes": "Values with \"quotes\" and , commas work!"
  }
]
```
