
package com.clinikdb.dbcrypt;

public class Main
{
	public static void main(String[] args)
	{
		SqliteTool tool = new SqliteTool();

		try
		{
			// Set your DB path and key
			String dbPath = "/Users/farabiabdelwahed/Downloads/abdel.sqlite";
			String passkey = "12341234";

			// Initialize tool
			tool.initDb( dbPath, passkey, true);

			// Execute a query
			String result = tool.executeSqlGetResultAsJson( "SELECT * from patients;");
			tool.closeDb();

			System.out.println( result);

			// Print results

		}
		catch( Exception e)
		{
			e.printStackTrace();
		}
	}
}
