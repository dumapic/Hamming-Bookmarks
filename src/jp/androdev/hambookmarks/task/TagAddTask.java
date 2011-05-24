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
 * Create a new tag record.
 *
 * @version 1.0.0
 */
public final class TagAddTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_CREATE | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_CREATE | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_CREATE | MessageFlags.STATE_CANCELED;

	private final TagItem mNewTag;

	private Transaction mHamTransByTags;
	private Comparator<String> mTagNameSort;
	private boolean mCommitable;
	private Message mReturnMessage;

	/**
	 * Constructor.
	 *
	 * @param newTag the new tag item.
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public TagAddTask(
		final TagItem newTag,
		final MyBaseActivity activity,
		final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
		mNewTag = newTag;
	}

	/*
	 * (�� Javadoc)
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

		return true;
	}

	/*
	 * (�� Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onRunning()
	 */
	@Override
	protected boolean onRunning() throws Throwable
	{
		Log.d(TAG, "START");

		Object[] keysTagsDB_Root = new Object[1];	// { TAGS_ROOT }
		Object[] keysTagsDB_Tag  = new Object[2];	// { TAGS_ROOT, tagname1 }

		ArrayList<String> allTagNames;
		String newTagName = mNewTag.getTag();

		/*
		 * STEP 1
		 * NO_TAGGED�Ɠ����̃^�O�̏ꍇ�͖����B
		 */
		if(HamDBKeys.NO_TAGGED.equals(newTagName))
		{
			Log.d(TAG, "Can not create/update '"+HamDBKeys.NO_TAGGED+"' tag.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
			return false;
		}

		/*
		 * STEP 2
		 * ���ɑ��݂���^�O�̏ꍇ�͖����i�����^�O�̏d���s�j�B
		 */
		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		allTagNames = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Root), true);
		if(allTagNames.contains(newTagName))
		{
			Log.d(TAG, "'"+newTagName+"' tag name already exists.");
			mCommitable = false;
			mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
			return false;
		}

		/*
		 * STEP 3
		 * TagsDB�̃��[�g���R�[�h�ɐV�K�^�O��ǉ��E�X�V�B
		 * ��NO_TAGGED����Ɍ��ɗ���悤�ɂ���B
		 * �����݂̕��ёւ��w��ɏ]���ă\�[�g������ɍX�V����B
		 */
		allTagNames.remove(HamDBKeys.NO_TAGGED);
		allTagNames.add(newTagName);
		Collections.sort(allTagNames, mTagNameSort);
		allTagNames.add(HamDBKeys.NO_TAGGED);

		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Root, allTagNames);

		/*
		 * STEP 4
		 * �^�O���R�[�h�iKey={ TAGS_ROOT, newTagName }�j��V�K�쐬�B
		 * �l�����͋��ArrayList<String>�Ƃ���B
		 */
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = newTagName;
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Tag, new ArrayList<String>());


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
