/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.task;

import java.util.ArrayList;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Constants.HamDBKeys;
import jp.androdev.hambookmarks.Constants.MessageFlags;
import jp.androdev.hambookmarks.context.MyBaseActivity;
import android.os.Handler;
import android.os.Message;
import de.crupp.hamsterdb.DatabaseException;
import de.crupp.hamsterdb.Transaction;

/**
 * Create tags root record and 'no tagged' record.
 * If already exists these record, Do nothing.
 *
 * @version 1.0.0
 */
public class MiscInitialOperationTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_NONE | MessageFlags.OP_ONLINE_INIT | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_NONE | MessageFlags.OP_ONLINE_INIT | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_NONE | MessageFlags.OP_ONLINE_INIT | MessageFlags.STATE_CANCELED;

	private Transaction mHamTransByTags;

	private boolean mCommitable;
	private Message mReturnMessage;

	/**
	 * Constructor.
	 *
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public MiscInitialOperationTask(final MyBaseActivity activity, final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
	}

	/*
	 * (”ñ Javadoc)
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
		mHamTransByTags = getDatabases().byTagsDB().begin();

		return true;
	}

	/*
	 * (”ñ Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onRunning()
	 */
	@Override
	protected boolean onRunning() throws Throwable
	{
		Log.d(TAG, "START");

		Object[] keysTagsDB_Root   = new Object[1];	// { TAGS_ROOT }
		Object[] keysTagsDB_NoTag  = new Object[2];	// { TAGS_ROOT, NO_TAGGED}


		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		if(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Root) == null)
		{
			ArrayList<String> recordValue = new ArrayList<String>();
			recordValue.add(HamDBKeys.NO_TAGGED);

			getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Root, recordValue);
			Log.d(TAG, "tag root record inserted.");

			keysTagsDB_NoTag[0] = HamDBKeys.TAGS_ROOT;
			keysTagsDB_NoTag[1] = HamDBKeys.NO_TAGGED;
			getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_NoTag, new ArrayList<String>());
			Log.d(TAG, "'no tagged' record inserted.");

			mCommitable = true;
		}
		else
		{
			mCommitable = false;
		}

		mReturnMessage = obtainMessage(MESSAGEFLAGS_COMPLETE);
		Log.d(TAG, "END");
		return true;
	}

	/*
	 * (”ñ Javadoc)
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
	 * (”ñ Javadoc)
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
	 * (”ñ Javadoc)
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
