
package com.clinikdb.dbcrypt;

public class Main
{
	public static void main(String[] args)
	{
		SqliteTool tool = new SqliteTool();

		try
		{
			// Set your DB path and key
			String dbPath = "/Users/farabiabdelwahed/Downloads/abdeal.sqlite";
			String passkey = "12341234";
			tool.createEncryptedDatabase( dbPath, passkey);

			// Initialize tool

			// Execute a query
			String result = tool.executeSqlGetResultAsJson( "SELECT * from patients;");
			System.out.println( "Query Result: " + result);
			tool.executeSql( "insert into init_table values( 'test', 'test');");
			tool.closeDb();

			// Print results

		}
		catch( Exception e)
		{
			e.printStackTrace();
		}
	}
}
