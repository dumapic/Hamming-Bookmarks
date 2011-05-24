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
 * Move bookmarkItem to the other tag.
 *
 * @version 1.0.0
 */
public final class BookmarkItemMoveTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_MOVE | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_MOVE | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_MOVE | MessageFlags.STATE_CANCELED;

	private final BookmarkItem mSelectedItem;
	private final TagItem mMoveToTag;

	private Transaction mHamTransByTags;
	private Transaction mHamTransByUrls;
	private Comparator<BookmarkItem> mBookmarkItemSort;
	private boolean mCommitable;
	private Message mReturnMessage;

	/**
	 * Constructor.
	 *
	 * @param item boolmarkItem to move.
	 * @param tag tag for move to.
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public BookmarkItemMoveTask(
		final BookmarkItem item,
		final TagItem tag,
		final MyBaseActivity activity,
		final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
		mSelectedItem = item;
		mMoveToTag = tag;
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
		mHamTransByUrls = getDatabases().byUrlDB().begin();

		return true;
	}

	/* (�� Javadoc)
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
		String moveUrl = mSelectedItem.getUrl();
		String tagNameOfMoveFrom;
		String tagNameOfMoveTo = mMoveToTag.getTag();

		/*
		 * STEP 1
		 * �ړ���̃^�O��NO_TAGGED�̏ꍇ�͖���
		 */
		if(HamDBKeys.NO_TAGGED.equals(tagNameOfMoveTo))
		{
			Log.d(TAG, "Can not create/update '"+HamDBKeys.NO_TAGGED+"' tag.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
			return false;
		}

		/*
		 * STEP 2
		 * DB�ɑ��݂��Ȃ��A�C�e�����ړ����悤�Ƃ���ꍇ�̓G���[
		 */
		keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_Url[1] = moveUrl;
		if(getDatabases().byUrlDB().find(mHamTransByUrls, keysUrlsDB_Url) == null)
		{
			Log.d(TAG, "Not found url. --> "+moveUrl);
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
			return false;
		}

		/*
		 * STEP 3
		 * �ړ���̃^�O�����݂��Ȃ��ꍇ�̓G���[
		 */
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = tagNameOfMoveTo;
		if(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag) == null)
		{
			Log.d(TAG, "Not found tag(move to). --> "+tagNameOfMoveTo);
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
			return false;
		}

		/*
		 * STEP 4
		 * �ړ��O�̃^�O�����擾
		 */
		keysUrlsDB_RefTag[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_RefTag[1] = moveUrl;
		keysUrlsDB_RefTag[2] = HamDBKeys.TAG_REF;
		tagNameOfMoveFrom = (String) getDatabases().byUrlDB().find(mHamTransByUrls, keysUrlsDB_RefTag);

		/*
		 * STEP 5
		 * URL��DB�̃^�O�֘A�t�����R�[�h�i{ BOOKMARKS_ROOT, url, TAG_REF }�j��
		 * Value�ɂ���^�O���� tagNameOfMoveTo �ɕύX
		 */
		getDatabases().byUrlDB().insertOrUpdate(mHamTransByUrls, keysUrlsDB_RefTag, tagNameOfMoveTo);

		/*
		 * STEP 6
		 * �^�ODB�̈ړ����^�O���R�[�h����Ώ�URL���폜
		 */
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = tagNameOfMoveFrom;
		allUrlsOfThisTag = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag), true);
		allUrlsOfThisTag.remove(moveUrl);
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Tag, allUrlsOfThisTag);

		/*
		 * STEP 7
		 * �^�ODB�̈ړ���^�O���R�[�h�ɑΏ�URL��ǉ�
		 *
		 * 1) �ړ���^�O�̃��R�[�h�iURL�j���X�g���擾���Ĉړ��Ώۂ�URL��ǉ�
		 * 2) 1)�̃��X�g������URLDB��ǂݍ����BookmarkItems��ArrayList�\�z�B
		 * 3) 2)�ɑ΂��Č��݂̃\�[�g��K�p
		 * 4) 3)����URL�𔲂��o���čX�V�pURL���X�g���쐬
		 * 5) 4)�ňړ���^�O���R�[�h���X�V
		 */
		allUrlsOfThisTag.clear();
		// 1)
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = tagNameOfMoveTo;
		allUrlsOfThisTag = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag), true);
		allUrlsOfThisTag.add(moveUrl);
		// 2)
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
		//3)
		Collections.sort(bookmarksForSort, mBookmarkItemSort);
		//4)
		allUrlsOfThisTag.clear();
		for(BookmarkItem item : bookmarksForSort)
		{
			allUrlsOfThisTag.add(item.getUrl());
		}
		//5)
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = tagNameOfMoveTo;
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Tag, allUrlsOfThisTag);


		mCommitable = true;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_COMPLETE);
		Log.d(TAG, "END");
		return true;
	}

	/* (�� Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onError(java.lang.Throwable)
	 */
	@Override
	protected void onError(Throwable e)
	{
		Log.d(TAG, "START");

		mCommitable = false;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
	}

	/* (�� Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onDatabaseClosed()
	 */
	@Override
	protected void onDatabaseClosed()
	{
		Log.d(TAG, "START");

		mCommitable = false;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
	}

	/* (�� Javadoc)
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
