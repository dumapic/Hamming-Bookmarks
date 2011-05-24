/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.error;

/**
 * Exception when database have been closed.
 *
 * @version 1.0.0
 */
public final class DatabaseHaveBeenClosedException extends RuntimeException
{
	/** Serial ID */
	private static final long serialVersionUID = 6451981361177971438L;

	/**
	 * Constructor.
	 */
	public DatabaseHaveBeenClosedException()
	{
		super("Database have been closed,");
	}
}
