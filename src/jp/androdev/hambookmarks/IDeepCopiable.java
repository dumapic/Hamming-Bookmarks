/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks;

/**
 * API for deep copy.
 *
 * @version 1.0.0
 */
public interface IDeepCopiable<T>
{
	/**
	 * Get the deep copied object.
	 */
	public T deepCopy();
}
