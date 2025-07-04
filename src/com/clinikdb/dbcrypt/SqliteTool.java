package com.clinikdb.dbcrypt;

import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * A utility class for interacting with encrypted SQLite databases using SQLCipher and native binaries.
 */
public class SqliteTool {

    private static final Logger LOGGER = Logger.getLogger(SqliteTool.class.getName());

    private File exeFile;
    private File dllFile;
    private String dbPath;
    private String passkey;
    private File workingDir;
    private boolean enableLogging = false;

    static {
        try {
            Logger rootLogger = LOGGER;

            if (rootLogger.getHandlers().length == 0) {
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(Level.ALL);
                consoleHandler.setFormatter(new SimpleFormatter());
                rootLogger.addHandler(consoleHandler);

                FileHandler fileHandler = new FileHandler("sqlite_tool.log", true);
                fileHandler.setLevel(Level.ALL);
                fileHandler.setFormatter(new SimpleFormatter());
                rootLogger.addHandler(fileHandler);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to configure logging", e);
        }
    }

    public void initDb(String dbPath, String passkey, boolean enableLogging) throws IOException {
        this.dbPath = dbPath;
        this.passkey = passkey;
        this.enableLogging = enableLogging;

        log(Level.INFO, "Initializing SQLite tool with database path: {0}, logging enabled: {1}", dbPath, enableLogging);

        workingDir = new File(System.getProperty("java.io.tmpdir"), "sqlite-temp");
        workingDir.mkdirs();

        log(Level.FINE, "Creating temporary directory: {0}", workingDir.getAbsolutePath());

        exeFile = extractResourceTo("sqlite3.exe", workingDir);
        dllFile = extractResourceTo("sqlite3.dll", workingDir);

        log(Level.INFO, "Successfully extracted SQLite resources to: {0}", workingDir.getAbsolutePath());
    }

    public void createEncryptedDatabase(String dbPath, String passkey) throws IOException, InterruptedException {
        log(Level.INFO, "Creating encrypted database at: {0}", dbPath);

        initDb(dbPath, passkey, enableLogging);

        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            dbFile.createNewFile();
            log(Level.FINE, "Database file created: {0}", dbPath);
        }

        String sql = "CREATE TABLE IF NOT EXISTS init_table (id INTEGER PRIMARY KEY, message TEXT);";
        log(Level.FINE, "Executing initialization SQL: {0}", sql);

        executeSql(sql);

        log(Level.INFO, "Encrypted database initialized successfully");
    }

    public List<String> executeSql(String sql) throws IOException, InterruptedException {
        log(Level.INFO, "Executing SQL query: {0}", sql);

        if (exeFile == null || dbPath == null || passkey == null) {
            log(Level.SEVERE, "SQLite tool not initialized");
            throw new IllegalStateException("Call initDb() before executing SQL.");
        }

        List<String> output = new ArrayList<>();

        ProcessBuilder builder = new ProcessBuilder(exeFile.getAbsolutePath(), dbPath);
        builder.directory(workingDir);
        builder.redirectErrorStream(true);
        builder.environment().put("PATH", workingDir.getAbsolutePath() + ";" + System.getenv("PATH"));

        log(Level.FINE, "Starting SQLite process for database: {0}", dbPath);

        Process process = builder.start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write("PRAGMA cipher_compatibility = 3;\n");
            writer.write("PRAGMA key = '" + passkey + "';\n");
            writer.write(".mode csv\n");
            writer.write(".headers on\n");
            writer.write(sql + "\n");
            writer.write(".exit\n");
            writer.flush();

            log(Level.FINE, "SQL commands written to process");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }

            log(Level.FINE, "SQL query output collected, lines: {0}", output.size());
        }

        int exitCode = process.waitFor();
        log(Level.INFO, "SQL process completed with exit code: {0}", exitCode);

        return output;
    }

    public String executeSqlAsString(String sql) throws IOException, InterruptedException {
        return String.join("\n", executeSql(sql));
    }

    public String executeSqlGetResultAsJson(String sql) throws IOException, InterruptedException {
        log(Level.INFO, "Executing SQL query for JSON output: {0}", sql);

        List<String> output = executeSql(sql);

        if (output.isEmpty()) {
            log(Level.WARNING, "No output from SQL query");
            return "[]";
        }

        if (!output.get(0).contains(",")) {
            String rawOutput = String.join("\\n", output);
            log(Level.FINE, "Non-CSV output, converting to JSON message");
            return "{\"message\":\"" + escapeJson(rawOutput) + "\"}";
        }

        String[] headers = output.get(0).split(",");
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        for (int i = 1; i < output.size(); i++) {
            String[] values = output.get(i).split(",", -1);
            jsonBuilder.append("{");

            for (int j = 0; j < headers.length; j++) {
                jsonBuilder.append("\"").append(escapeJson(headers[j])).append("\":");
                if (j < values.length) {
                    jsonBuilder.append("\"").append(escapeJson(values[j])).append("\"");
                } else {
                    jsonBuilder.append("\"\"");
                }
                if (j < headers.length - 1) {
                    jsonBuilder.append(",");
                }
            }

            jsonBuilder.append("}");
            if (i < output.size() - 1) {
                jsonBuilder.append(",");
            }
        }

        jsonBuilder.append("]");
        log(Level.INFO, "SQL query results converted to JSON");
        return jsonBuilder.toString();
    }

    public void closeDb() {
        log(Level.INFO, "Closing SQLite tool and cleaning up resources");

        if (exeFile != null && exeFile.exists()) {
            exeFile.delete();
            log(Level.FINE, "Deleted executable: {0}", exeFile.getAbsolutePath());
        }

        if (dllFile != null && dllFile.exists()) {
            dllFile.delete();
            log(Level.FINE, "Deleted DLL: {0}", dllFile.getAbsolutePath());
        }

        exeFile = null;
        dllFile = null;
        dbPath = null;
        passkey = null;
        workingDir = null;

        log(Level.INFO, "SQLite tool cleanup complete");
    }

    private String escapeJson(String s) {
        if (s == null) {
            log(Level.FINE, "Null string passed to JSON escape");
            return "";
        }

        String escaped = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        log(Level.FINEST, "Escaped JSON string: {0}", escaped);
        return escaped;
    }

    private File extractResourceTo(String resourceName, File targetDir) throws IOException {
        log(Level.INFO, "Extracting resource: {0} to {1}", resourceName, targetDir.getAbsolutePath());

        InputStream in = SqliteTool.class.getResourceAsStream("/" + resourceName);
        if (in == null) {
            log(Level.SEVERE, "Resource not found: {0}", resourceName);
            throw new FileNotFoundException("Resource not found: " + resourceName);
        }

        File outFile = new File(targetDir, resourceName);
        try (OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            log(Level.FINE, "Successfully extracted resource: {0}", resourceName);
        }

        if (resourceName.endsWith(".exe")) {
            outFile.setExecutable(true);
            log(Level.FINE, "Set executable permission for: {0}", outFile.getAbsolutePath());
        }

        return outFile;
    }

    // Unified logging helpers
    private void log(Level level, String message) {
        if (enableLogging) {
            LOGGER.log(level, message);
        }
    }

    private void log(Level level, String message, Object... params) {
        if (enableLogging) {
            LOGGER.log(level, message, params);
        }
    }
}
