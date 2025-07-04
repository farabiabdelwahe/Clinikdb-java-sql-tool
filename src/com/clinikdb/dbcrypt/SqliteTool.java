
package com.clinikdb.dbcrypt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
 * A utility class for interacting with encrypted SQLite databases using SQLCipher and native binaries.
 */
public class SqliteTool
{

	private static final Logger		LOGGER			= Logger.getLogger( SqliteTool.class.getName());

	private File					sqlcipherBinary;												// Used for Windows
																									// (extracted) or
																									// macOS
																									// (system-installed)
	private File					dllFile;														// Only used on
																									// Windows
	private String					dbPath;
	private String					passkey;
	private File					workingDir;
	private boolean					enableLogging	= false;
	private static final String		OS				= System.getProperty( "os.name").toLowerCase();
	private static final boolean	IS_WINDOWS		= OS.contains( "win");
	private static final boolean	IS_MAC			= OS.contains( "mac");

	static
	{
		try
		{
			Logger rootLogger = LOGGER;

			if( rootLogger.getHandlers().length == 0)
			{
				ConsoleHandler consoleHandler = new ConsoleHandler();
				consoleHandler.setLevel( Level.ALL);
				consoleHandler.setFormatter( new SimpleFormatter());
				rootLogger.addHandler( consoleHandler);

				FileHandler fileHandler = new FileHandler( "sqlite_tool.log", true);
				fileHandler.setLevel( Level.ALL);
				fileHandler.setFormatter( new SimpleFormatter());
				rootLogger.addHandler( fileHandler);
			}
		}
		catch( IOException e)
		{
			LOGGER.log( Level.SEVERE, "Failed to configure logging", e);
		}
	}

	public void initDb(String dbPath, String passkey, boolean enableLogging) throws IOException
	{
		this.dbPath = dbPath;
		this.passkey = passkey;
		this.enableLogging = enableLogging;

		log( Level.INFO, "Initializing SQLite tool with database path: {0}, logging enabled: {1}", dbPath, enableLogging);

		workingDir = new File( System.getProperty( "java.io.tmpdir"), "sqlite-temp");
		workingDir.mkdirs();

		log( Level.FINE, "Creating temporary directory: {0}", workingDir.getAbsolutePath());

		if( IS_WINDOWS)
		{
			sqlcipherBinary = extractResourceTo( "sqlite3.exe", workingDir);
			dllFile = extractResourceTo( "sqlite3.dll", workingDir);
		}
		else if( IS_MAC)
		{
			// Use system-installed sqlcipher
			sqlcipherBinary = new File( "/opt/homebrew/bin/sqlcipher");
			if( !sqlcipherBinary.exists() || !sqlcipherBinary.canExecute())
			{
				// Fallback to PATH lookup
				try
				{
					ProcessBuilder pb = new ProcessBuilder( "which", "sqlcipher");
					Process process = pb.start();
					BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream()));
					String path = reader.readLine();
					if( path != null && !path.isEmpty())
					{
						sqlcipherBinary = new File( path.trim());
					}
					process.waitFor();
					if( !sqlcipherBinary.exists() || !sqlcipherBinary.canExecute())
					{
						log( Level.SEVERE, "sqlcipher not found or not executable on macOS");
						throw new IOException( "sqlcipher not found or not executable on macOS");
					}
				}
				catch( InterruptedException e)
				{
					Thread.currentThread().interrupt();
					log( Level.SEVERE, "Interrupted while locating sqlcipher", e);
					throw new IOException( "Failed to locate sqlcipher", e);
				}
			}
			log( Level.INFO, "Using system sqlcipher at: {0}", sqlcipherBinary.getAbsolutePath());
		}
		else
		{
			log( Level.SEVERE, "Unsupported operating system: {0}", OS);
			throw new UnsupportedOperationException( "Unsupported operating system: " + OS);
		}

		log( Level.INFO, "Successfully initialized SQLCipher resources");
	}

	public void createEncryptedDatabase(String dbPath, String passkey) throws IOException, InterruptedException
	{
		log( Level.INFO, "Creating encrypted database at: {0}", dbPath);

		initDb( dbPath, passkey, enableLogging);

		File dbFile = new File( dbPath);
		if( !dbFile.exists())
		{
			dbFile.createNewFile();
			log( Level.FINE, "Database file created: {0}", dbPath);
		}

		String sql = "CREATE TABLE IF NOT EXISTS init_table (id INTEGER PRIMARY KEY, message TEXT);";
		log( Level.FINE, "Executing initialization SQL: {0}", sql);

		executeSql( sql);

		log( Level.INFO, "Encrypted database initialized successfully");
	}

	public List<String> executeSql(String sql) throws IOException, InterruptedException
	{
		log( Level.INFO, "Executing SQL query: {0}", sql);

		if( sqlcipherBinary == null || dbPath == null || passkey == null)
		{
			log( Level.SEVERE, "SQLite tool not initialized");
			throw new IllegalStateException( "Call initDb() before executing SQL.");
		}

		List<String> output = new ArrayList<>();

		ProcessBuilder builder = new ProcessBuilder( sqlcipherBinary.getAbsolutePath(), dbPath);
		builder.directory( workingDir);
		builder.redirectErrorStream( true);

		if( IS_WINDOWS)
		{
			builder.environment().put( "PATH", workingDir.getAbsolutePath() + ";" + System.getenv( "PATH"));
		}
		else if( IS_MAC)
		{
			builder.environment().put( "DYLD_LIBRARY_PATH", workingDir.getAbsolutePath());
		}

		log( Level.FINE, "Starting SQLCipher process for database: {0}", dbPath);

		Process process = builder.start();

		try (BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( process.getOutputStream())))
		{
			writer.write( "PRAGMA cipher_compatibility = 3;\n");
			writer.write( "PRAGMA key = '" + passkey + "';\n");
			writer.write( ".mode csv\n");
			writer.write( ".headers on\n");
			writer.write( sql + "\n");
			writer.write( ".exit\n");
			writer.flush();

			log( Level.FINE, "SQL commands written to process");
		}

		try (BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream())))
		{
			String line;
			while( (line = reader.readLine()) != null)
			{
				output.add( line);
			}

			log( Level.FINE, "SQL query output collected, lines: {0}", output.size());
		}

		int exitCode = process.waitFor();
		log( Level.INFO, "SQL process completed with exit code: {0}", exitCode);

		return output;
	}

	public String executeSqlAsString(String sql) throws IOException, InterruptedException
	{
		return String.join( "\n", executeSql( sql));
	}

	public String executeSqlGetResultAsJson(String sql) throws IOException, InterruptedException
	{
		log( Level.INFO, "Executing SQL query for JSON output: {0}", sql);

		List<String> output = executeSql( sql);

		if( output.isEmpty())
		{
			log( Level.WARNING, "No output from SQL query");
			return "[]";
		}

		if( output.get( 0).equals( "ok"))
		{
			output.remove( 0); // Remove the "ok" response

		}

		if( !output.get( 0).contains( ","))
		{
			String rawOutput = String.join( "\\n", output);
			log( Level.FINE, "Non-CSV output, converting to JSON message");
			return "{\"message\":\"" + escapeJson( rawOutput) + "\"}";
		}

		String[] headers = output.get( 0).split( ",");
		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append( "[");

		for( int i = 1; i < output.size(); i++)
		{
			String[] values = output.get( i).split( ",", -1);
			jsonBuilder.append( "{");

			for( int j = 0; j < headers.length; j++)
			{
				jsonBuilder.append( "\"").append( escapeJson( headers[j])).append( "\":");
				if( j < values.length)
				{
					jsonBuilder.append( "\"").append( escapeJson( values[j])).append( "\"");
				}
				else
				{
					jsonBuilder.append( "\"\"");
				}
				if( j < headers.length - 1)
				{
					jsonBuilder.append( ",");
				}
			}

			jsonBuilder.append( "}");
			if( i < output.size() - 1)
			{
				jsonBuilder.append( ",");
			}
		}

		jsonBuilder.append( "]");
		log( Level.INFO, "SQL query results converted to JSON");
		return jsonBuilder.toString();
	}

	public void closeDb()
	{
		log( Level.INFO, "Closing SQLite tool and cleaning up resources");

		if( IS_WINDOWS && sqlcipherBinary != null && sqlcipherBinary.exists())
		{
			sqlcipherBinary.delete();
			log( Level.FINE, "Deleted binary: {0}", sqlcipherBinary.getAbsolutePath());
		}

		if( dllFile != null && dllFile.exists())
		{
			dllFile.delete();
			log( Level.FINE, "Deleted DLL: {0}", dllFile.getAbsolutePath());
		}

		sqlcipherBinary = null;
		dllFile = null;
		dbPath = null;
		passkey = null;
		workingDir = null;

		log( Level.INFO, "SQLite tool cleanup complete");
	}

	private String escapeJson(String s)
	{
		if( s == null)
		{
			log( Level.FINE, "Null string passed to JSON escape");
			return "";
		}

		String escaped = s.replace( "\\", "\\\\").replace( "\"", "\\\"").replace( "\b", "\\b").replace( "\f", "\\f").replace( "\n", "\\n")
			.replace( "\r", "\\r").replace( "\t", "\\t");

		log( Level.FINEST, "Escaped JSON string: {0}", escaped);
		return escaped;
	}

	private File extractResourceTo(String resourceName, File targetDir) throws IOException
	{
		log( Level.INFO, "Extracting resource: {0} to {1}", resourceName, targetDir.getAbsolutePath());

		InputStream in = SqliteTool.class.getResourceAsStream( "/" + resourceName);
		if( in == null)
		{
			log( Level.SEVERE, "Resource not found: {0}", resourceName);
			throw new FileNotFoundException( "Resource not found: " + resourceName);
		}

		File outFile = new File( targetDir, resourceName);
		try (OutputStream out = new FileOutputStream( outFile))
		{
			byte[] buffer = new byte[4096];
			int len;
			while( (len = in.read( buffer)) != -1)
			{
				out.write( buffer, 0, len);
			}

			log( Level.FINE, "Successfully extracted resource: {0}", resourceName);
		}

		if( resourceName.endsWith( ".exe"))
		{
			outFile.setExecutable( true);
			log( Level.FINE, "Set executable permission for: {0}", outFile.getAbsolutePath());
		}

		return outFile;
	}

	// Unified logging helpers
	private void log(Level level, String message)
	{
		if( enableLogging)
		{
			LOGGER.log( level, message);
		}
	}

	private void log(Level level, String message, Object... params)
	{
		if( enableLogging)
		{
			LOGGER.log( level, message, params);
		}
	}
}