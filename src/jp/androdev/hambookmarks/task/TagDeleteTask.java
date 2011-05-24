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
 *
 * Delete a tag record.
 *
 * @version 1.0.0
 */
public final class TagDeleteTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_DELETE | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_DELETE | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_DELETE | MessageFlags.STATE_CANCELED;

	private final TagItem mDeleteTag;

	private Transaction mHamTransByTags;
	private Transaction mHamTransByUrls;
	private Comparator<String> mTagNameSort;
	private Comparator<BookmarkItem> mBookmarkItemSort;
	private boolean mCommitable;
	private Message mReturnMessage;

	/**
	 * Constructor.
	 *
	 * @param deleteTag the delete tag item.
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public TagDeleteTask(
		final TagItem deleteTag,
		final MyBaseActivity activity,
		final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
		mDeleteTag = deleteTag;
	}

	/*
	 * (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onPrepare()
	 */
	@Override
	protected boolean onPrepare() throws Throwable
	{
		Log.d(TAG, "START");

		mCommitable = false;

		mTagNameSort = getTagNameSortOrder();
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

	/*
	 * (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onRunning()
	 */
	@Override
	protected boolean onRunning() throws Throwable
	{
		Log.d(TAG, "START");

		Object[] keysTagsDB_Root   = new Object[1];		// { TAGS_ROOT }
		Object[] keysTagsDB_Tag    = new Object[2];		// { TAGS_ROOT, tagname1 }
		Object[] keysUrlsDB_Url     = new Object[2];	// { BOOKMARKS_ROOT, url1 }
		Object[] keysUrlsDB_RefTag  = new Object[3];	// { BOOKMARKS_ROOT, url1, TAG_REF }

		ArrayList<String> allTagNames;
		ArrayList<String> allUrlsOfThisTag;
		ArrayList<String> allUrlsOfNoTagged;
		ArrayList<BookmarkItem> bookmarksForSort;
		String deleteTagName = mDeleteTag.getTag();

		/*
		 * STEP 1
		 * NO_TAGGEDと同名のタグの場合は無効。
		 */
		if(HamDBKeys.NO_TAGGED.equals(deleteTagName))
		{
			Log.d(TAG, "Can not create/update '"+HamDBKeys.NO_TAGGED+"' tag.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
			return false;
		}

		/*
		 * STEP 2
		 * DBに存在していないタグの場合は無効。
		 * （本来ありえないためエラーである）
		 */
		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		allTagNames = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Root), true);
		if(!allTagNames.contains(deleteTagName))
		{
			Log.d(TAG, "Not found "+deleteTagName+" tag.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
			return false;
		}

		/*
		 * STEP 3
		 * TagsDBのルートレコードから対象タグを削除。
		 * ※削除後、NO_TAGGEDが常に後ろに来るようにする。
		 * ※現在の並び替え指定に従ってソートした後に更新する。
		 */
		allTagNames.remove(HamDBKeys.NO_TAGGED);
		allTagNames.remove(deleteTagName);
		Collections.sort(allTagNames, mTagNameSort);
		allTagNames.add(HamDBKeys.NO_TAGGED);

		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Root, allTagNames);

		/*
		 * STEP 4
		 * タグレコード（Key={ TAGS_ROOT, tagname1 }）を削除。
		 * ただし、削除する前にValueに入っているURLのリストを退避する(URL側DBレコード更新用）
		 */
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = deleteTagName;
		allUrlsOfThisTag = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag), true);
		getDatabases().byTagsDB().erase(mHamTransByTags, keysTagsDB_Tag);

		/*
		 * STEP 5
		 * URL側DBのタグ関連付けレコード（{ BOOKMARKS_ROOT, url, TAG_REF }）の
		 * Valueにあるタグ名をNO_TAGGEDに変更
		 */
		keysUrlsDB_RefTag[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_RefTag[1] = null;
		keysUrlsDB_RefTag[2] = HamDBKeys.TAG_REF;
		for(String currentUrl : allUrlsOfThisTag)
		{
			keysUrlsDB_RefTag[1] = currentUrl;
			getDatabases().byUrlDB().insertOrUpdate(mHamTransByUrls, keysUrlsDB_RefTag, HamDBKeys.NO_TAGGED);
		}

		/*
		 * STEP 6
		 *
		 * タグDBのタグなしURLをまとめたレコード（Key={ TAGS_ROOT, NO_TAGGED}）に
		 * これらのURLを追加。
		 *
		 * 1) 既存のURLリストに今回未分類となったURLをマージ
		 * 2) 1)のリストを元にURLDBを読み込んでBookmarkItemsのArrayList構築。
		 * 3) 2)に対して現在のソートを適用
		 * 4) 3)からURLを抜き出してNO_TAGGED更新用のURLリストを作成
		 * 5) 4)でNO_TAGGEDのレコードを更新
		 */
		//1)
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = HamDBKeys.NO_TAGGED;
		allUrlsOfNoTagged = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag), true);
		allUrlsOfNoTagged.addAll(allUrlsOfThisTag);
		//2)
		bookmarksForSort = new ArrayList<BookmarkItem>();
		keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_Url[1] = null;
		for(String currentUrl : allUrlsOfNoTagged)
		{
			keysUrlsDB_Url[1] = currentUrl;
			BookmarkItem item = (BookmarkItem) getDatabases().byUrlDB().find(mHamTransByUrls, keysUrlsDB_Url);
			if(item != null)
				bookmarksForSort.add(item);
		}
		//3)
		Collections.sort(bookmarksForSort, mBookmarkItemSort);
		//4)
		allUrlsOfNoTagged.clear();
		for(BookmarkItem item : bookmarksForSort)
		{
			allUrlsOfNoTagged.add(item.getUrl());
		}
		//5)
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = HamDBKeys.NO_TAGGED;
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Tag, allUrlsOfNoTagged);


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
