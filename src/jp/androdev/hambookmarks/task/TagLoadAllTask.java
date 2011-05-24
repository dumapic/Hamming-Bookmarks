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
import jp.androdev.hambookmarks.R;
import jp.androdev.hambookmarks.context.MyBaseActivity;
import jp.androdev.hambookmarks.data.TagItem;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Pickup all tags, and put to string ArrayList.
 *
 * @version 1.0.0
 */
public final class TagLoadAllTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_QUERY | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_QUERY | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_QUERY | MessageFlags.STATE_CANCELED;

	private final ArrayList<TagItem> mLoadTags;
	private final boolean mIncludeNoTaggedTag;
	private final String mNoTaggedCaption;

	private Message mReturnMessage;

	/**
	 * Constructor. (load and put all tags to message bundle data)
	 *
	 * @param includeNoTaggedTag true if you need to include 'no tagged' tag.
	 * @param activity activity object.
	 * @param handler handler object.
	 */
	public TagLoadAllTask(
		final boolean includeNoTaggedTag,
		final MyBaseActivity activity,
		final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
		mLoadTags = new ArrayList<TagItem>();
		mIncludeNoTaggedTag = includeNoTaggedTag;
		mNoTaggedCaption = activity.getString(R.string.caption_label_no_tagged);
	}

	/* (�� Javadoc)
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

	/* (�� Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onRunning()
	 */
	@Override
	protected boolean onRunning() throws Throwable
	{
		Log.d(TAG, "START");

		Object[] keysTagsDB_Root   = new Object[1];	// { TAGS_ROOT }

		ArrayList<String> allTagNames;

		/*
		 * TagsDB�̃��[�g���R�[�h�iKey={ TAGS_ROOT }�j�̒l�iArrayList)��
		 * �߂�l���X�g�ɃZ�b�g
		 *
		 * @see jp.androdev.hambookmarks.data.TagItemAdapter#addAllByNames(Collection<String>)
		 */
		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		allTagNames = toStringArrayList(getDatabases().byTagsDB().find(keysTagsDB_Root), true);
		if(allTagNames.size() > 0)
		{
			for(String tagName : allTagNames)
			{
				if(!HamDBKeys.NO_TAGGED.equals(tagName))
				{
					mLoadTags.add(new TagItem(tagName));
				}
				else if(mIncludeNoTaggedTag)
				{
					mLoadTags.add(new TagItem(tagName, mNoTaggedCaption));
				}
			}
		}
		else
		{
			//�^�O���Ȃ��ꍇ�͖���
			mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
			return false;
		}

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
		mReturnMessage = obtainMessage(MESSAGEFLAGS_FAILED);
	}

	/* (�� Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onDatabaseClosed()
	 */
	@Override
	protected void onDatabaseClosed()
	{
		Log.d(TAG, "START");
		mReturnMessage = obtainMessage(MESSAGEFLAGS_CANCELED);
	}

	/* (�� Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onDispose()
	 */
	@Override
	protected void onDispose()
	{
		Log.d(TAG, "START");

		Bundle info = new Bundle();
		info.putSerializable(BundleKeys.ALL_TAG, mLoadTags);
		mReturnMessage.setData(info);

		getDatabases().closeDatabases();
		sendMessage(mReturnMessage);
	}
}
