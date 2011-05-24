/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.task;

import java.util.ArrayList;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Constants.BundleKeys;
import jp.androdev.hambookmarks.Constants.HamDBKeys;
import jp.androdev.hambookmarks.Constants.MessageFlags;
import jp.androdev.hambookmarks.context.MyBaseActivity;
import jp.androdev.hambookmarks.data.BookmarkItem;
import jp.androdev.hambookmarks.data.TagItem;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Load bookmarkItem records by selected tag.
 *
 * @version 1.0.0
 */
public final class BookmarkItemLoadByTagTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_QUERY | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_QUERY | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_QUERY | MessageFlags.STATE_CANCELED;

	private final ArrayList<BookmarkItem> mLoadItems;
	private final TagItem mSelectedTag;

	private Message mReturnMessage;

	/**
	 * Constructor.
	 *
	 * @param selectTag selected tag object.
	 * @param adapter the adapter of listview window.
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public BookmarkItemLoadByTagTask(
		final TagItem selectTag,
		final MyBaseActivity activity,
		final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
		mSelectedTag = selectTag;
		mLoadItems = new ArrayList<BookmarkItem>();
	}

	/*
	 * (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onPrepare()
	 */
	@Override
	protected boolean onPrepare() throws Throwable
	{
		Log.d(TAG, "START");

		if(!getDatabases().tryOpenReadableDatabases())
		{
			mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
			return false;
		}

		return true;
	}

	/*
	 * (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onRunning()
	 */
	@Override
	protected boolean onRunning() throws Throwable
	{
		Log.d(TAG, "START");

		Object[] keysTagsDB_Tag    = new Object[2];		// { TAGS_ROOT, tagname1 }
		Object[] keysUrlsDB_Url     = new Object[2];	// { BOOKMARKS_ROOT, url1 }

		ArrayList<String> allUrlsOfThisTag;
		String tagNameForLoad = mSelectedTag.getTag();

		/*
		 * STEP 1
		 * 対象タグのURLリストを読み込み
		 */
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = tagNameForLoad;
		allUrlsOfThisTag = toStringArrayList(getDatabases().byTagsDB().find(keysTagsDB_Tag), true);

		/*
		 * STEP 2
		 * URLからBookmarkitemオブジェクトを取得してadapterにセット。
		 */
		keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_Url[1] = null;
		for(String currentUrl : allUrlsOfThisTag)
		{
			keysUrlsDB_Url[1] = currentUrl;
			BookmarkItem item = (BookmarkItem) getDatabases().byUrlDB().find(keysUrlsDB_Url);
			mLoadItems.add(item);
		}

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

		Bundle info = new Bundle();
		info.putSerializable(BundleKeys.ALL_BOOKMARK, mLoadItems);
		mReturnMessage.setData(info);

		getDatabases().closeDatabases();
		sendMessage(mReturnMessage);
	}
}
