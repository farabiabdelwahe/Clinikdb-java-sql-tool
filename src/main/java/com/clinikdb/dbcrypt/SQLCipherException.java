
package com.clinikdb.dbcrypt;

import java.util.ArrayList;
import java.util.List;


public class SQLCipherException extends Exception
{
	private final int			exitCode;
	private final List<String>	errorCodes;

	public SQLCipherException(String message, int exitCode, List<String> errorCodes)
	{
		super( message);
		this.exitCode = exitCode;
		this.errorCodes = new ArrayList<>( errorCodes);
	}

	public int getExitCode()
	{
		return exitCode;
	}

	public List<String> getErrorCodes()
	{
		return new ArrayList<>( errorCodes);
	}

	@Override
	public String toString()
	{
		return super.toString() + " (Exit Code: " + exitCode + ", Error Count: " + errorCodes.size() + ")";
	}
}
