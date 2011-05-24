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

	/* (�� Javadoc)
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

	/* (�� Javadoc)
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
		 * NO_TAGGED�Ɠ����̃^�O�̏ꍇ�͖����B
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
		 * ���l�[���O�̃^�O�����ADB�ɑ��݂��Ă��Ȃ��ꍇ�͖����B
		 * �i�{�����肦�Ȃ����߃G���[�ł���j
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
		 * ���l�[����̃^�O��������DB�ɑ��݂��Ă���^�O�̏ꍇ�͖���
		 * �i�����^�O�̏d���s�j�B
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
		 * TagsDB�̃��[�g���R�[�h�̋��^�O��V�^�O���ɍX�V�B
		 * ��NO_TAGGED����Ɍ��ɗ���悤�ɂ���B
		 * �����݂̕��ёւ��w��ɏ]���ă\�[�g������ɍX�V����B
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
		 * �����̂ł̃^�O���R�[�h�iKey={ TAGS_ROOT, oldTagName }�j��
		 * �V���̂ł̃^�O���R�[�h�ɒu�������B
		 *
		 * 1)�����̂ł̃^�O���R�[�h�̒l����U�ޔ�
		 * 2)�����̂ł̃^�O���R�[�h���폜
		 * 3)1)��Value�Ɏ��V���̂ł̃��R�[�h��}��
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
		 * URL��DB�̃^�O�֘A�t�����R�[�h�i{ BOOKMARKS_ROOT, url, TAG_REF }�j��
		 * Value�ɂ���^�O���� newTagName �ɕύX
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
