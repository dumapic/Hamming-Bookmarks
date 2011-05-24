/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
 * Sort 'ALL' bookmarks by current order.
 *
 * @version 1.0.0
 */
public final class BookmarkItemSortReorderTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_SORT | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_SORT | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_SORT | MessageFlags.STATE_CANCELED;

	private Transaction mHamTransByTags;
	private Comparator<BookmarkItem> mBookmarkItemSort;
	private boolean mCommitable;
	private Message mReturnMessage;

	/**
	 * Constructor.
	 *
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public BookmarkItemSortReorderTask(
		final MyBaseActivity activity,
		final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
	}

	/* (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onPrepare()
	 */
	@Override
	protected boolean onPrepare() throws Throwable
	{
		Log.d(TAG, "START");

		mCommitable = false;

		mBookmarkItemSort = getBookmarkItemSortOrder();

		if(!getDatabases().tryOpenWritableDatabases())
		{
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
			return false;
		}

		// setup transactions.
		mHamTransByTags = getDatabases().byTagsDB().begin();

		return true;
	}

	/* (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onRunning()
	 */
	@Override
	protected boolean onRunning() throws Throwable
	{
		Log.d(TAG, "START");

		Object[] keysTagsDB_Root   = new Object[1];		// { TAGS_ROOT }
		Object[] keysTagsDB_Tag    = new Object[2];		// { TAGS_ROOT, tagname1 }
		Object[] keysUrlsDB_Url     = new Object[2];	// { BOOKMARKS_ROOT, url1 }

		ArrayList<String> allTagNames;
		ArrayList<String> allUrlsOfThisTag;
		ArrayList<BookmarkItem> bookmarksForSort;

		/*
		 * STEP 1
		 * 全部のタグ名リストを取得。
		 */
		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		allTagNames = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Root), true);

		//それぞれのタグレコードからURLリストを取得しつつ、
		//そのURLリストを並び替えを開始。
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = null;
		for(String currentTagName : allTagNames)
		{
			/*
			 * STEP 2
			 * このタグのURLリストを取得
			 */
			keysTagsDB_Tag[1] = currentTagName;
			allUrlsOfThisTag = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag), true);

			/*
			 * STEP 3
			 * 並び替え実施
			 *
			 * 1) URLリストを元にURLDBを読み込んでBookmarkItemsのArrayList構築。
			 * 2) 1)に対して現在のソートを適用
			 * 3) 2)からURLを抜き出して更新用URLリストを作成
			 * 4) 3)でタグレコードを更新
			 */
			// 1)
			keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
			keysUrlsDB_Url[1] = null;
			bookmarksForSort = new ArrayList<BookmarkItem>();
			for(String currentUrl : allUrlsOfThisTag)
			{
				keysUrlsDB_Url[1] = currentUrl;
				BookmarkItem item = (BookmarkItem) getDatabases().byUrlDB().find(keysUrlsDB_Url);
				if(item != null)
					bookmarksForSort.add(item);
			}
			// 2)
			Collections.sort(bookmarksForSort, mBookmarkItemSort);
			// 3)
			allUrlsOfThisTag.clear();
			for(BookmarkItem item : bookmarksForSort)
			{
				allUrlsOfThisTag.add(item.getUrl());
			}
			// 4)
			getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Tag, allUrlsOfThisTag);


			bookmarksForSort = null;
			allUrlsOfThisTag = null;
		}


		mCommitable = true;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_COMPLETE);
		Log.d(TAG, "END");
		return true;
	}

	/*
	 * (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onError(java.lang.Throwable)
	 */
	@Override
	protected void onError(Throwable e)
	{
		Log.d(TAG, "START");

		mCommitable = false;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
	}

	/*
	 * (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onDatabaseClosed()
	 */
	@Override
	protected void onDatabaseClosed()
	{
		Log.d(TAG, "START");

		mCommitable = false;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
	}

	/*
	 * (非 Javadoc)
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
				mHamTransByTags.commit();
				Log.d(TAG, "mHamTransByTags transaction is committed");
			}
			else
			{
				mHamTransByTags.abort();
				Log.d(TAG, "mHamTransByTags transaction is rollbacked");
			}
		}
		catch (DatabaseException e)
		{
			Log.e(TAG, e.getMessage() + " [ErrNo:"+e.getErrno()+"]", e);
		}

		getDatabases().closeDatabases();
		sendMessage(mReturnMessage);
	}
}
