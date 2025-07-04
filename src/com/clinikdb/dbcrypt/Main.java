package com.clinikdb.dbcrypt;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        SqliteTool tool = new SqliteTool();

        try {
            // Set your DB path and key
            String dbPath = "C:\\Users\\dell\\Desktop\\Test.db";
            String passkey = "12341234";

            // Initialize tool
           tool.initDb(dbPath, passkey , true);

            // Execute a query
            String result = tool.executeSqlGetResultAsJson("SELECT * from patients;");
            tool.closeDb();

           System.out.println(result);


            // Print results
           

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
