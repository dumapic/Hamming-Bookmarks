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
import jp.androdev.hambookmarks.data.TagItem;
import android.os.Handler;
import android.os.Message;
import de.crupp.hamsterdb.DatabaseException;
import de.crupp.hamsterdb.Transaction;

/**
 * Create new BookmarkItem, and add to tag.
 *
 * @version 1.0.0
 */
public final class BookmarkItemAddTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_CREATE | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_CREATE | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_CREATE | MessageFlags.STATE_CANCELED;

	private final BookmarkItem mNewItem;
	private final TagItem mAddToTag;

	private Transaction mHamTransByTags;
	private Transaction mHamTransByUrls;
	private Comparator<BookmarkItem> mBookmarkItemSort;
	private boolean mCommitable;
	private Message mReturnMessage;

	/**
	 * Constructor.
	 *
	 * @param addToTag a tag for add.
	 * @param newItem new bookmarkitem for add.
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public BookmarkItemAddTask(
		final TagItem addToTag,
		final BookmarkItem newItem,
		final MyBaseActivity activity,
		final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
		mAddToTag = addToTag;
		mNewItem = newItem;
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

		Object[] keysTagsDB_Tag    = new Object[2];		// { TAGS_ROOT, tagname1 }
		Object[] keysUrlsDB_Url     = new Object[2];	// { BOOKMARKS_ROOT, url1 }
		Object[] keysUrlsDB_RefTag  = new Object[3];	// { BOOKMARKS_ROOT, url1, TAG_REF }

		ArrayList<String> allUrlsOfThisTag;
		ArrayList<BookmarkItem> bookmarksForSort;
		String newUrl = mNewItem.getUrl();
		String tagNameForAddUrl = mAddToTag.getTag();

		/*
		 * STEP 1
		 * NO_TAGGEDのタグに追加しようとした場合は無効。
		 */
		if(HamDBKeys.NO_TAGGED.equals(tagNameForAddUrl))
		{
			Log.d(TAG, "Can not create/update '"+HamDBKeys.NO_TAGGED+"' tag.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
			return false;
		}

		/*
		 * STEP 2
		 * DBに既に存在しているURLの場合は無効。
		 */
		keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_Url[1] = newUrl;
		if(getDatabases().byUrlDB().find(mHamTransByUrls, keysUrlsDB_Url) != null)
		{
			Log.d(TAG, "This bookmark already exists. -->"+newUrl);
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
			return false;
		}

		/*
		 * STEP 3
		 * URLDB側のレコードを新規作成
		 *
		 * 1) URLDBのURLレコード（Key={ BOOKMARKS_ROOT, newUrl }）を新規作成
		 * 2) URLタグ参照用レコード（Key={ BOOKMARKS_ROOT, newUrl, TAG_REF }）を新規作成
		 */
		// 1)
		keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_Url[1] = newUrl;
		getDatabases().byUrlDB().insertOrUpdate(mHamTransByUrls, keysUrlsDB_Url, mNewItem);
		// 2)
		keysUrlsDB_RefTag[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_RefTag[1] = newUrl;
		keysUrlsDB_RefTag[2] = HamDBKeys.TAG_REF;
		getDatabases().byUrlDB().insertOrUpdate(mHamTransByUrls, keysUrlsDB_RefTag, tagNameForAddUrl);

		/*
		 * STEP 4
		 * TagsDB内のタグレコードに、対象ブックマークのURLを新規追加。
		 * 並び替えのうえ再保存。
		 *
		 * 1) タグレコード（Key={ TAGS_ROOT, tagNameForAddUrl }）からURL名リストを取得。
		 * 2) 1)のリストに対象URLを追加
		 * 3) 2)のリストを元にURLDBを読み込んでBookmarkItemsのArrayList構築。
		 * 4) 3)に対して現在のソートを適用
		 * 5) 4)からURLを抜き出して、更新用URLリストを作成
		 * 6) 5)でタグレコードを更新
		 */
		// 1)
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = tagNameForAddUrl;
		allUrlsOfThisTag = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag), true);
		// 2)
		allUrlsOfThisTag.add(newUrl);
		// 3)
		bookmarksForSort = new ArrayList<BookmarkItem>();
		keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_Url[1] = null;
		for(String currentUrl : allUrlsOfThisTag)
		{
			keysUrlsDB_Url[1] = currentUrl;
			BookmarkItem item = (BookmarkItem) getDatabases().byUrlDB().find(mHamTransByUrls, keysUrlsDB_Url);
			if(item != null)
				bookmarksForSort.add(item);
		}
		// 4)
		Collections.sort(bookmarksForSort, mBookmarkItemSort);
		// 5)
		allUrlsOfThisTag.clear();
		for(BookmarkItem item : bookmarksForSort)
		{
			allUrlsOfThisTag.add(item.getUrl());
		}
		// 6)
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Tag, allUrlsOfThisTag);


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
				mHamTransByTags.commit();
				Log.d(TAG, "mHamTransByTags transaction is committed");
				mHamTransByUrls.commit();
				Log.d(TAG, "mHamTransByUrls transaction is committed");
			}
			else
			{
				mHamTransByTags.abort();
				Log.d(TAG, "mHamTransByTags transaction is rollbacked");
				mHamTransByUrls.abort();
				Log.d(TAG, "mHamTransByUrls transaction is rollbacked");
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
