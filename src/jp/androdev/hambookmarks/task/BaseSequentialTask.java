/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.task;

import java.util.ArrayList;
import java.util.Comparator;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Comparators;
import jp.androdev.hambookmarks.Constants.PrefKeys;
import jp.androdev.hambookmarks.MyHamDatabases;
import jp.androdev.hambookmarks.data.BookmarkItem;
import jp.androdev.hambookmarks.data.BookmarkItemAdapter;
import jp.androdev.hambookmarks.data.TagItemAdapter;
import jp.androdev.hambookmarks.error.DatabaseHaveBeenClosedException;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import de.crupp.hamsterdb.DatabaseException;

/**
 * Abstract class for sequential task class.
 *
 * @version 1.0.0
 */
public abstract class BaseSequentialTask implements Runnable
{
	protected static final String TAG = "HamBookmarks";

	private final Context mContext;
	private final MyHamDatabases mDatabases;
	private final Handler mHandler;

	/**
	 * Constructor.
	 *
	 * @param context the context object.
	 * @param holder the hamsterdb holder object.
	 * @param handler the handler object.
	 */
	protected BaseSequentialTask(Context context, MyHamDatabases holder, Handler handler)
	{
		mContext = context;
		mDatabases = holder;
		mHandler = handler;
	}

	/* (”ñ Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public final void run()
	{
		boolean prepareOK = false;
		boolean disposable = false;

		try
		{
			prepareOK = onPrepare();
			disposable = false;
		}
		catch (DatabaseException e)
		{
			Log.e(TAG, e.getMessage() + " [ErrNo:"+e.getErrno()+"]", e);
			disposable = true;
			onError(e);
			return;
		}
		catch (DatabaseHaveBeenClosedException e)
		{
			disposable = true;
			onDatabaseClosed();
			return;
		}
		catch (Throwable e)
		{
			Log.e(TAG, e.getMessage(), e);
			disposable = true;
			onError(e);
			return;
		}
		finally
		{
			if(disposable)
			{
				onDispose();
				return;
			}
		}

		try
		{
			if(prepareOK)
			{
				try
				{
					onRunning();
				}
				catch (DatabaseException e)
				{
					Log.e(TAG, e.getMessage() + " [ErrNo:"+e.getErrno()+"]", e);
					onError(e);
					return;
				}
				catch (DatabaseHaveBeenClosedException e)
				{
					onDatabaseClosed();
					return;
				}
				catch (Throwable e)
				{
					Log.e(TAG, e.getMessage(), e);
					onError(e);
					return;
				}
			}
		}
		finally
		{
			onDispose();
		}
	}

	/**
	 * Prepare this task.
	 *
	 * @return If this task have been prepared, return true. otherwise, return false(the next callback is onDispose).
	 */
	protected abstract boolean onPrepare() throws Throwable;

	/**
	 * Main works of this task.
	 *
	 * @return If this task have been completed, return true. otherwise, return false(the next callback is onDispose).
	 */
	protected abstract boolean onRunning() throws Throwable;

	/**
	 * If the unexpected error occured, this callback method will be called.
	 *
	 * @param e Unexpected error object.
	 */
	protected abstract void onError(Throwable e);
	protected abstract void onDatabaseClosed();
	protected abstract void onDispose();

	/**
	 * Get the hamsterdb holder.
	 */
	protected final MyHamDatabases getDatabases()
	{
		if(mHandler == null || mDatabases == null)
		{
			Log.w(TAG, "application or databases have been closed.");
			throw new DatabaseHaveBeenClosedException();
		}

		return mDatabases;
	}

	/**
	 * Get the handler object.
	 */
	protected final Handler getHandler()
	{
		if(mHandler == null)
		{
			Log.w(TAG, "mHandler is null. The databases have been closed.");
			throw new DatabaseHaveBeenClosedException();
		}
		return mHandler;
	}

	/**
	 * Get the Android Context object.
	 */
	protected final Context getContext()
	{
		if(mHandler == null)
		{
			Log.w(TAG, "mHandler is null. The databases have been closed.");
			throw new DatabaseHaveBeenClosedException();
		}
		return mContext;
	}

	/**
	 * Send message object.
	 * Handler object will be handled this message.
	 *
	 * @param msg the Android Message object.
	 */
	protected final void sendMessage(Message msg)
	{
		if(mHandler == null)
		{
			Log.w(TAG, "mHandler is null. Can't send messages. The databases have been closed.");
			throw new DatabaseHaveBeenClosedException();
		}
		mHandler.sendMessage(msg);
	}

	/**
	 * Get the message instance.
	 *
	 * @param what A value of {@link Message#what }.
	 */
	protected final Message obtainMessage(int what)
	{
		if(mHandler == null)
		{
			Log.w(TAG, "mHandler is null. The databases have been closed.");
			throw new DatabaseHaveBeenClosedException();
		}

		Message result = mHandler.obtainMessage();
		result.what = what;
		return result;
	}

	/**
	 * Get shared preference of this context.
	 */
	protected SharedPreferences getPreferences()
	{
		return PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	/**
	 *  Cast to string ArrayList object.
	 */
	@SuppressWarnings("unchecked")
	protected ArrayList<String> toStringArrayList(Object obj, boolean returnEmptyStringListIfNull)
	{
		if(obj != null)
			return (ArrayList<String>) obj;

		if(returnEmptyStringListIfNull)
			return new ArrayList<String>();

		return null;
	}

	/**
	 * Get current comparator for tag names.
	 */
	protected Comparator<String> getTagNameSortOrder()
	{
		int sortOrder = getPreferences().getInt(
			PrefKeys.SORTORDER_TAG, TagItemAdapter.getDefaultSortOrder());
		Comparator<String> result = null;

		switch(sortOrder)
		{
			case TagItemAdapter.SortOrder.BY_TITLE_ASC:
				result = Comparators.ForString.SORT_BY_ASC;
				break;
			case TagItemAdapter.SortOrder.BY_TITLE_DESC:
				result = Comparators.ForString.SORT_BY_DESC;
				break;
			default:
				throw new IllegalStateException("Illegal sort order. -->"+sortOrder);
		}

		return result;
	}

	/**
	 * Get current comparator for bookmark items.
	 */
	protected Comparator<BookmarkItem> getBookmarkItemSortOrder()
	{
		int sortOrder = getPreferences().getInt(
			PrefKeys.SORTORDER_BOOKMARKITEM, BookmarkItemAdapter.getDefaultSortOrder());
		Comparator<BookmarkItem> result = Comparators.ForBookmarkItems.getComparatorByOrder(sortOrder);

		return result;
	}
}
