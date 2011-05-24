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
import jp.androdev.hambookmarks.data.TagItem;
import android.os.Handler;
import android.os.Message;
import de.crupp.hamsterdb.DatabaseException;
import de.crupp.hamsterdb.Transaction;

/**
 * Rename a tag name.
 *
 * @version 1.0.0
 */
public final class TagRenameTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_UPDATE | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_UPDATE | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_UPDATE | MessageFlags.STATE_CANCELED;

	private final TagItem mOldTag;
	private final TagItem mNewTag;

	private Transaction mHamTransByTags;
	private Transaction mHamTransByUrls;
	private Comparator<String> mTagNameSort;
	private boolean mCommitable;
	private Message mReturnMessage;

	/**
	 * Constructor.
	 *
	 * @param oldTag a tag as old name.
	 * @param newTag a tag as new name.
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public TagRenameTask(
		final TagItem oldTag,
		final TagItem newTag,
		final MyBaseActivity activity,
		final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
		mOldTag = oldTag;
		mNewTag = newTag;
	}

	/* (非 Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onPrepare()
	 */
	@Override
	protected boolean onPrepare() throws Throwable
	{
		Log.d(TAG, "START");

		mCommitable = false;

		mTagNameSort = getTagNameSortOrder();

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

		Object[] keysTagsDB_Root	= new Object[1];	// { TAGS_ROOT }
		Object[] keysTagsDB_Tag		= new Object[2];	// { TAGS_ROOT, tagname1 }
		Object[] keysUrlsDB_RefTag  = new Object[3];	// { BOOKMARKS_ROOT, url1, TAG_REF }

		ArrayList<String> allTagNames;
		ArrayList<String> allUrlsOfThisTag;
		String oldTagName = mOldTag.getTag();
		String newTagName = mNewTag.getTag();

		/*
		 * STEP 1
		 * NO_TAGGEDと同名のタグの場合は無効。
		 */
		if(HamDBKeys.NO_TAGGED.equals(oldTagName) || HamDBKeys.NO_TAGGED.equals(newTagName))
		{
			Log.d(TAG, "Can not create/update '"+HamDBKeys.NO_TAGGED+"' tag.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
			return false;
		}

		/*
		 * STEP 2
		 * リネーム前のタグ名が、DBに存在していない場合は無効。
		 * （本来ありえないためエラーである）
		 */
		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		allTagNames = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Root), true);
		if(!allTagNames.contains(oldTagName))
		{
			Log.d(TAG, "Not found "+oldTagName+" tag.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
			return false;
		}

		/*
		 * STEP 3
		 * リネーム後のタグ名が既にDBに存在しているタグの場合は無効
		 * （同名タグの重複不可）。
		 */
		if(allTagNames.contains(newTagName))
		{
			Log.d(TAG, "'"+newTagName+"' tag name already exists.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
			return false;
		}

		/*
		 * STEP 4
		 * TagsDBのルートレコードの旧タグを新タグ名に更新。
		 * ※NO_TAGGEDが常に後ろに来るようにする。
		 * ※現在の並び替え指定に従ってソートした後に更新する。
		 */
		allTagNames.remove(HamDBKeys.NO_TAGGED);
		allTagNames.remove(oldTagName);
		allTagNames.add(newTagName);
		Collections.sort(allTagNames, mTagNameSort);
		allTagNames.add(HamDBKeys.NO_TAGGED);

		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Root, allTagNames);

		/*
		 * STEP 5
		 * 旧名称でのタグレコード（Key={ TAGS_ROOT, oldTagName }）を
		 * 新名称でのタグレコードに置き換え。
		 *
		 * 1)旧名称でのタグレコードの値を一旦退避
		 * 2)旧名称でのタグレコードを削除
		 * 3)1)をValueに持つ新名称でのレコードを挿入
		 */
		// 1)
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = oldTagName;
		allUrlsOfThisTag = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag), true);
		// 2)
		getDatabases().byTagsDB().erase(mHamTransByTags, keysTagsDB_Tag);
		// 3)
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = mNewTag.getTag();
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Tag, allUrlsOfThisTag);

		/*
		 * STEP 6
		 * URL側DBのタグ関連付けレコード（{ BOOKMARKS_ROOT, url, TAG_REF }）の
		 * Valueにあるタグ名を newTagName に変更
		 */
		keysUrlsDB_RefTag[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_RefTag[1] = null;
		keysUrlsDB_RefTag[2] = HamDBKeys.TAG_REF;
		for(String currentUrl : allUrlsOfThisTag)
		{
			keysUrlsDB_RefTag[1] = currentUrl;
			getDatabases().byUrlDB().insertOrUpdate(mHamTransByUrls, keysUrlsDB_RefTag, newTagName);
		}


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
