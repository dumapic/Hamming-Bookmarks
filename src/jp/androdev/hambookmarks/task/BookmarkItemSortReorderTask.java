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

	/* (�� Javadoc)
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

	/* (�� Javadoc)
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
		 * �S���̃^�O�����X�g���擾�B
		 */
		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		allTagNames = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Root), true);

		//���ꂼ��̃^�O���R�[�h����URL���X�g���擾���A
		//����URL���X�g����ёւ����J�n�B
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = null;
		for(String currentTagName : allTagNames)
		{
			/*
			 * STEP 2
			 * ���̃^�O��URL���X�g���擾
			 */
			keysTagsDB_Tag[1] = currentTagName;
			allUrlsOfThisTag = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag), true);

			/*
			 * STEP 3
			 * ���ёւ����{
			 *
			 * 1) URL���X�g������URLDB��ǂݍ����BookmarkItems��ArrayList�\�z�B
			 * 2) 1)�ɑ΂��Č��݂̃\�[�g��K�p
			 * 3) 2)����URL�𔲂��o���čX�V�pURL���X�g���쐬
			 * 4) 3)�Ń^�O���R�[�h���X�V
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
	 * (�� Javadoc)
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
	 * (�� Javadoc)
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
	 * (�� Javadoc)
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
