/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.error;

/**
 * Unexpected error at hamsterdb.
 *
 * @version 1.0.0
 */
public final class UnexpectedDatabaseErrorException extends RuntimeException
{
	/** Serial ID */
	private static final long serialVersionUID = -3206917256283431021L;

	/** Constructor */
	public UnexpectedDatabaseErrorException()
	{
		super();
	}

	/** Constructor */
	public UnexpectedDatabaseErrorException(String detailMessage)
	{
		super(detailMessage);
	}
}
