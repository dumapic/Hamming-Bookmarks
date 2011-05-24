/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks;

import java.io.File;

import de.crupp.hamsterdb.Const;
import de.crupp.hamsterdb.DatabaseException;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.error.DatabaseHaveBeenClosedException;
import jp.androiddevelopers.ham4droid.Ham4DroidDatabase;
import android.content.Context;

/**
 * The class for managing hamsterdb databases.
 *
 * @version 1.0.0
 */
public final class MyHamDatabases
{
	private static final String TAG = "HamBookmarks";
	private static final String HAM_DBNAME_BYURL = "HamBookmarks.ByUrl.db";
	private static final String HAM_DBNAME_BYTAGS = "HamBookmarks.ByTags.db";

	private Object mState;

	private File mHamFileByUrlDB;
	private File mHamFileByTagsDB;

	private Ham4DroidDatabase mByURL;
	private Ham4DroidDatabase mByTags;

	/**
	 * Constructor.
	 * @param context context object.
	 */
	public MyHamDatabases(Context context)
	{
		mHamFileByUrlDB = new File(context.getFilesDir().getPath()+File.separator+HAM_DBNAME_BYURL);
		mHamFileByTagsDB = new File(context.getFilesDir().getPath()+File.separator+HAM_DBNAME_BYTAGS);

		mByURL = new Ham4DroidDatabase();
		mByTags = new Ham4DroidDatabase();

		mState = null;
	}

	/**
	 * Create hamsterdb.
	 */
	public boolean tryCreateDatabases()
	{
		Log.d(TAG, "START");
		Log.d(TAG, "mHamFileByUrlDB path--> "+mHamFileByUrlDB.getPath());
		Log.d(TAG, "mHamFileByTagsDB path--> "+mHamFileByTagsDB.getPath());

		try
		{
			if(!mHamFileByUrlDB.exists() && !mHamFileByTagsDB.exists())
			{
				mState = new Object();

				synchronized(mState)
				{
					mByURL.createDatabase(mHamFileByUrlDB.getPath(), 	Const.HAM_ENABLE_TRANSACTIONS);
					mByTags.createDatabase(mHamFileByTagsDB.getPath(),	Const.HAM_ENABLE_TRANSACTIONS | Const.HAM_ENABLE_DUPLICATES);

					Log.d(TAG, "mHamDbHolder databases is created.");

					mByURL.closeDatabase(Const.HAM_TXN_AUTO_ABORT);
					mByTags.closeDatabase(Const.HAM_TXN_AUTO_ABORT);
				}
			}

			return true;
		}
		catch (DatabaseException e)
		{
			Log.e(TAG, e.getMessage() + " [ErrNo:"+e.getErrno()+"]", e);
			return false;
		}
	}

	/**
	 * Open hamsterdb as writable database.
	 */
	public boolean tryOpenWritableDatabases()
	{
		Log.d(TAG, "START");

		if(mState != null)
		{
			closeDatabases();
		}
		mState = new Object();

		synchronized(mState)
		{
			try
			{
				mByURL.openDatabase(mHamFileByUrlDB.getPath(),
					Const.HAM_ENABLE_TRANSACTIONS | Const.HAM_ENABLE_RECOVERY);
				mByTags.openDatabase(mHamFileByTagsDB.getPath(),
					Const.HAM_ENABLE_TRANSACTIONS | Const.HAM_ENABLE_RECOVERY);

				Log.d(TAG, "mHamDbHolder databases is opened(writable).");
				return true;
			}
			catch (DatabaseException e)
			{
				Log.e(TAG, e.getMessage() + " [ErrNo:"+e.getErrno()+"]", e);
				return false;
			}
		}
	}

	/**
	 * Open hamsterdb as readable database.
	 */
	public boolean tryOpenReadableDatabases()
	{
		Log.d(TAG, "START");

		if(mState != null)
		{
			closeDatabases();
		}
		mState = new Object();

		synchronized(mState)
		{
			try
			{
				mByURL.openDatabase(mHamFileByUrlDB.getPath(), Const.HAM_READ_ONLY);
				mByTags.openDatabase(mHamFileByTagsDB.getPath(), Const.HAM_READ_ONLY);

				Log.d(TAG, "mHamDbHolder databases is opened(readable).");
				return true;
			}
			catch (DatabaseException e)
			{
				Log.e(TAG, e.getMessage() + " [ErrNo:"+e.getErrno()+"]", e);
				return false;
			}
		}
	}

	/**
	 * Close hamsterdb database.
	 * Transactional data will be aborting.
	 */
	public void closeDatabases()
	{
		Log.d(TAG, "START");

		if(mState == null)
			return;

		synchronized(mState)
		{
			if(mByURL != null)
			{
				if((mByURL.getOpenOrCreateDbFlags() & Const.HAM_READ_ONLY) != Const.HAM_READ_ONLY)
				{
					if(mByURL.flush() == Const.HAM_SUCCESS)
						Log.d(TAG, "Flush 'ByURL' database is done.");
					if(mByURL.closeDatabase(Const.HAM_TXN_AUTO_ABORT) == Const.HAM_SUCCESS)
						Log.d(TAG, "Closing 'ByURL' database successfully.");
				}
				else
				{
					if(mByURL.closeDatabase() == Const.HAM_SUCCESS)
						Log.d(TAG, "Closing 'ByURL' database successfully.");
				}
			}
			if(mByTags != null)
			{
				if((mByTags.getOpenOrCreateDbFlags() & Const.HAM_READ_ONLY) != Const.HAM_READ_ONLY)
				{
					if(mByTags.flush() == Const.HAM_SUCCESS)
						Log.d(TAG, "Flush 'ByTags' database is done.");
					if(mByTags.closeDatabase(Const.HAM_TXN_AUTO_ABORT) == Const.HAM_SUCCESS)
						Log.d(TAG, "Closing 'ByTags' database successfully.");
				}
				else
				{
					if(mByTags.closeDatabase() == Const.HAM_SUCCESS)
						Log.d(TAG, "Closing 'ByTags' database successfully.");
				}
			}

			mState = null;
		}
	}

	/**
	 * Get the url database.
	 */
	public Ham4DroidDatabase byUrlDB()
	{
		//Log.d(TAG, "START");

		if(mState != null)
		{
			if(mByURL.isOpened())
			{
				return mByURL;
			}
			else
			{
				Log.d(TAG, "DBOpened --> closed");
				throw new DatabaseHaveBeenClosedException();
			}
		}
		else
		{
			Log.d(TAG, "mState --> null(died)");
			throw new DatabaseHaveBeenClosedException();
		}
	}

	/**
	 * Get the tags database.
	 */
	public Ham4DroidDatabase byTagsDB()
	{
		//Log.d(TAG, "START");

		if(mState != null)
		{
			if(mByTags.isOpened())
			{
				return mByTags;
			}
			else
			{
				Log.d(TAG, "DBOpened --> closed");
				throw new DatabaseHaveBeenClosedException();
			}
		}
		else
		{
			Log.d(TAG, "mState --> null(died)");
			throw new DatabaseHaveBeenClosedException();
		}
	}
}
