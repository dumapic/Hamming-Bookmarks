/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.task;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Constants.HamDBKeys;
import jp.androdev.hambookmarks.Constants.MessageFlags;
import jp.androdev.hambookmarks.context.MyBaseActivity;
import jp.androdev.hambookmarks.data.BookmarkItem;
import android.os.Handler;
import android.os.Message;
import de.crupp.hamsterdb.DatabaseException;
import de.crupp.hamsterdb.Transaction;

/**
 * Update a bookmarkItem record.
 *
 * Notes: Can not change url.
 *
 * @version 1.0.0
 */
public final class BookmarkItemUpdateTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_UPDATE | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_UPDATE | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_UPDATE | MessageFlags.STATE_CANCELED;

	private final BookmarkItem mOldItem;
	private final BookmarkItem mNewItem;
	private final boolean mBackgroundUpdate;

	private Transaction mHamTransByUrls;
	private boolean mCommitable;
	private Message mReturnMessage;


	/**
	 * Constructor.
	 *
	 * @param oldItem a old bookmark item.
	 * @param newItem a new bookmark item. DO NOT CHANGE URL.
	 * @param backgroundUpdate true if background updating.
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public BookmarkItemUpdateTask(
		final BookmarkItem oldItem,
		final BookmarkItem newItem,
		final boolean backgroundUpdate,
		final MyBaseActivity activity,
		final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
		mOldItem = oldItem;
		mNewItem = newItem;
		mBackgroundUpdate = backgroundUpdate;
	}

	/* (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onPrepare()
	 */
	@Override
	protected boolean onPrepare() throws Throwable
	{
		Log.d(TAG, "START");

		mCommitable = false;

		if(!getDatabases().tryOpenWritableDatabases())
		{
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
			return false;
		}

		// setup transactions.
		mHamTransByUrls = getDatabases().byUrlDB().begin();

		return true;
	}

	/* (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onRunning()
	 */
	@Override
	protected boolean onRunning() throws Throwable
	{
		Log.d(TAG, "START");

		Object[] keysUrlsDB_Url     = new Object[2];	// { BOOKMARKS_ROOT, url1 }

		String updateUrl = mOldItem.getUrl();

		/*
		 * STEP 1
		 * 新旧アイテムでURLが異なっていた場合はエラー
		 * （URL情報はDBキー項目なので更新は許可しない）
		 */
		if(!mOldItem.getUrl().equals(mNewItem.getUrl()))
		{
			Log.w(TAG, "Can not update url.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
			return false;
		}

		/*
		 * STEP 2
		 * DBに存在しないアイテムを更新しようとする場合はエラー
		 */
		keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_Url[1] = updateUrl;
		if(getDatabases().byUrlDB().find(mHamTransByUrls, keysUrlsDB_Url) == null)
		{
			Log.d(TAG, "Not found url. --> "+updateUrl);
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
			return false;
		}

		/*
		 * STEP 3
		 * URLDBのURLレコードを更新
		 */
		keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_Url[1] = updateUrl;
		getDatabases().byUrlDB().insertOrUpdate(mHamTransByUrls, keysUrlsDB_Url, mNewItem);


		mCommitable = true;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_COMPLETE);
		Log.d(TAG, "END");
		return true;
	}

	/* (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onError(java.lang.Throwable)
	 */
	@Override
	protected void onError(Throwable e)
	{
		Log.d(TAG, "START");

		mCommitable = false;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
	}

	/* (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onDatabaseClosed()
	 */
	@Override
	protected void onDatabaseClosed()
	{
		Log.d(TAG, "START");

		mCommitable = false;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
	}

	/* (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onDispose()
	 */
	@Override
	protected void onDispose()
	{
		Log.d(TAG, "START");

		try
		{
			Log.d(TAG, "mCommitable: "+mCommitable);
			if(mCommitable)
			{
				mHamTransByUrls.commit();
				Log.d(TAG, "mHamTransByURL transaction is committed");
			}
			else
			{
				mHamTransByUrls.abort();
				Log.d(TAG, "mHamTransByURL transaction is rollbacked");
			}
		}
		catch (DatabaseException e)
		{
			Log.e(TAG, e.getMessage() + " [ErrNo:"+e.getErrno()+"]", e);
		}

		getDatabases().closeDatabases();
		if(!mBackgroundUpdate)
		{
			sendMessage(mReturnMessage);
		}
	}
}
