/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Constants.HamDBKeys;
import jp.androdev.hambookmarks.Constants.MessageFlags;
import jp.androdev.hambookmarks.Constants.Path;
import jp.androdev.hambookmarks.Procedure;
import jp.androdev.hambookmarks.R;
import jp.androdev.hambookmarks.context.MyBaseActivity;
import jp.androdev.hambookmarks.data.BookmarkItem;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import de.crupp.hamsterdb.DatabaseException;
import de.crupp.hamsterdb.Transaction;

/**
 * <p>
 * Sync with(import from) from Android standard browser(Chrome lite) bookmarks.
 * </p>
 * <p>
 * <b>1. ByTags database</b><br/>
 *  Holdings and store the tags data.<br/>
 *  A tag has multiple bookmarkItems(URL links) by string ArrayList object.<br/>
 *  A bookmarkItem must be related unique tag(not multiple tags).<br/>
 * </p>
 * <pre>
 *  -----KEY-----                  -----VALUE-----
 * { TAGS_ROOT }                 = { tagname1; tagname2... }
 * { TAGS_ROOT, NO_TAGGED}       = { url1; url2... } ---&gt; no tagged bookmarks.
 * { TAGS_ROOT, tagname1 }       = { url3; url4... } ---&gt; 'tagname1' tag hold 'url3','url4' bookmarks.
 * { TAGS_ROOT, tagname2 }       = { url5; url6... }
 * </pre>
 * <p>
 * <b>2. ByUrl database</b><br/>
 *  Holdings and store the Bookmark info data each url.<br/>
 * </p>
 * <pre>
 * -----KEY-----                        -----VALUE-----
 * { BOOKMARKS_ROOT, url1 }           = { BookmarkItem(of url1 info) object data }
 * { BOOKMARKS_ROOT, url1, TAG_REF }  = NO_TAGGED
 * { BOOKMARKS_ROOT, url2 }           = { BookmarkItem(of url2 info) object data }
 * { BOOKMARKS_ROOT, url2, TAG_REF }  = NO_TAGGED
 * { BOOKMARKS_ROOT, url3             = { BookmarkItem(of url3 info) object data }
 * { BOOKMARKS_ROOT, url3, TAG_REF }  = tagname1
 * { BOOKMARKS_ROOT, url4             = { BookmarkItem(of url4 info) object data }
 * { BOOKMARKS_ROOT, url4, TAG_REF }  = tagname1
 * </pre>
 *
 * @version 1.0.0
 */
public final class SyncChromeLiteBookmarksTask extends BaseSequentialTask
{
	private static final int MESSAGEFLAGS_COMPLETE =
		MessageFlags.KIND_NONE | MessageFlags.OP_ONLINE_SYNC | MessageFlags.STATE_COMPLETE;
	private static final int MESSAGEFLAGS_FAILED =
		MessageFlags.KIND_NONE | MessageFlags.OP_ONLINE_SYNC | MessageFlags.STATE_FAILED;
	private static final int MESSAGEFLAGS_CANCELED =
		MessageFlags.KIND_NONE | MessageFlags.OP_ONLINE_SYNC | MessageFlags.STATE_CANCELED;

	private static final String THIS_TAG = "#android_chrome";

	private Transaction mHamTransByTags;
	private Transaction mHamTransByUrls;
	private Comparator<String> mTagNameSort;
	private Comparator<BookmarkItem> mBookmarkItemSort;
	private String mCreatedLabel;
	private String mLastAccessedLabel;
	private boolean mCommitable;
	private Message mReturnMessage;

	/**
	 *
	 * Constructor.
	 *
	 * @param activity the activity object.
	 * @param handler the handler object.
	 */
	public SyncChromeLiteBookmarksTask(final MyBaseActivity activity, final Handler handler)
	{
		super(activity.getBaseContext(), activity.getMyApplication().getDatabases(), handler);
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
		mCreatedLabel = getContext().getString(R.string.caption_label_created);
		mLastAccessedLabel = getContext().getString(R.string.caption_label_lastaccessed);

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
	 * (�� Javadoc)
	 * @see jp.androdev.hambookmarks.task.BaseSequentialTask#onRunning()
	 */
	@Override
	protected boolean onRunning() throws Throwable
	{
		Log.d(TAG, "START");

		Object[] keysTagsDB_Root    = new Object[1];		// { TAGS_ROOT }
		Object[] keysTagsDB_Tag     = new Object[2];		// { TAGS_ROOT, tagname1 }
		Object[] keysUrlsDB_Url     = new Object[2];		// { BOOKMARKS_ROOT, url1 }
		Object[] keysUrlsDB_RefTag  = new Object[3];		// { BOOKMARKS_ROOT, url1, TAG_REF }

		ArrayList<String> allTagNames;
		ArrayList<String> allUrlsOfThisTag;
		ArrayList<String> renewalUrlsOfThisTag;
		ArrayList<BookmarkItem> bookmarksForSort;

		/*
		 * STEP 1
		 * TagsDB�̃��[�g���R�[�h�iKey={ TAGS_ROOT }�j�̍X�V�B
		 *
		 * �E���̏����ŕt�^����铯���^�O���Ȃ��ꍇ�͒ǉ��E���ёւ������Ă���X�V����B
		 * �E���̏����ŕt�^����铯���^�O������ꍇ�͂Ȃɂ����Ȃ��B
		 */
		keysTagsDB_Root[0] = HamDBKeys.TAGS_ROOT;
		allTagNames = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Root), true);
		if(!allTagNames.contains(THIS_TAG))
		{
			//NO_TAGGED����Ɍ��ɗ���悤�ɕ��ёւ����čX�V
			allTagNames.remove(HamDBKeys.NO_TAGGED);
			allTagNames.add(THIS_TAG);
			Collections.sort(allTagNames, mTagNameSort);
			allTagNames.add(HamDBKeys.NO_TAGGED);

			getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Root, allTagNames);
		}

		/*
		 * STEP 2
		 * TagsDB�̃^�O���R�[�h�iKey={ TAGS_ROOT, THIS_TAG }�j�̒ǉ�
		 *
		 * �E���̏����ŕt�^����铯���^�O���Ȃ��ꍇ�̓��R�[�h��ǉ�����(Value�����͋󃊃X�g�Łj�B
		 * �E���̏����ŕt�^����铯���^�O������ꍇ�͉������Ȃ��B
		 */
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = THIS_TAG;
		allUrlsOfThisTag = toStringArrayList(getDatabases().byTagsDB().find(mHamTransByTags, keysTagsDB_Tag), false);
		if(allUrlsOfThisTag == null)
		{
			allUrlsOfThisTag = new ArrayList<String>();
			getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Tag, allUrlsOfThisTag);
		}
		renewalUrlsOfThisTag = new ArrayList<String>();

		/*
		 * STEP3
		 * �W���u���E�U�̃u�b�N�}�[�N�̓ǂݍ��݂�URLDB�ւ̊i�[�B
		 *
		 * �Ȃ��A�������[���͈ȉ��̂Ƃ���Ƃ���B
		 *
		 * (1)ham����URL���Ȃ�
		 * 		browser�̑S����ham����insert
		 * (2)ham����URL������ AND browser����created���V����
		 * 		browser�̑S����ham����insert
		 * (3)ham����URL������ AND browser����created���Â�
		 * 		�������Ȃ�
		 * (4)ham����URL������ AND browser����lastAccessed���V����
		 * 		browser�̑S����ham����insert
		 * (5)ham����URL������ AND browser����lastAccessed���Â�
		 * 		�������Ȃ�
		 *
		 * ��Ham���ɂ�����Browser���ɂȂ��t�����̓����͂Ƃ肠�����p�X
		 */
		Cursor c = obtainChromeLiteBookmarksCursor();
		boolean importing;
		String url;
		BookmarkItem foundBookmark;

		while(c.moveToNext())
		{
			importing = false;
			BookmarkItem newBookmarkItem = composeBookmarkItem(c);
			url = newBookmarkItem.getUrl();

			keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
			keysUrlsDB_Url[1] = url;

			// Ham���Ƀf�[�^����
			if((foundBookmark = (BookmarkItem)getDatabases().byUrlDB().find(mHamTransByUrls, keysUrlsDB_Url) ) != null)
			{
				Log.d(TAG, "Found old bookmark item --> "+foundBookmark);

				//�u���E�U����Created���V���� (2)
				//�u���E�U����LastAccessed���V���� (4)
				if(newBookmarkItem.getCreated() >= foundBookmark.getCreated()
				||(newBookmarkItem.getLastAccessed() >= foundBookmark.getLastAccessed()))
				{
					importing = true;
				}
				else
				{
					// �������Ȃ�
					// (3) ham����URL������ AND browser����created���Â�
					// (4) ham����URL������ AND browser����lastAccessed���Â�
					importing = false;
				}
			}
			// Ham���Ƀf�[�^�Ȃ� (1)
			else
			{
				importing = true;
			}

			if(!importing)
				continue;

			////////////////////////////////////////////////////////////////////

			Log.d(TAG, "Importing ... --> "+newBookmarkItem);

			/*
			 * STEP4
			 * ���Ƀ^�O���R�[�h�iKey={ TAGS_ROOT, THIS_TAG }�j��
			 * ���݂���̂ł���Α}���E�X�V�ΏۊO�B
			 */
			if(allUrlsOfThisTag.contains(url))
			{
				Log.d(TAG, "Already exists in tag db. --> "+newBookmarkItem);
				continue;
			}

			/*
			 * STEP5
			 * ����URL��DB�ɑ}���ρi��THIS_TAG�Ƃ͕ʂ̃^�O�Ɋ֘A�t�����Ă���j
			 * �̂ł���΁A�}���E�X�V�ΏۊO
			 */
			keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
			keysUrlsDB_Url[1] = url;
			if(getDatabases().byUrlDB().find(mHamTransByUrls, keysUrlsDB_Url) != null)
			{
				Log.d(TAG, "Already exists in urls db. --> "+newBookmarkItem);
				continue;
			}

			////////////////////////////////////////////////////////////////////

			/*
			 * STEP6
			 * �^�O���R�[�h�iKey={ TAGS_ROOT, THIS_TAG }�j�Ɏ�荞�ނ��߂�
			 * ���X�g�Ƀu�b�N�}�[�N��ǉ����Ă����B
			 *
			 * Notes: �ŏI�I�ɂ́A���[�v���E��ɕ��ёւ������{���ă��R�[�h�X�V���s���B
			 */
			renewalUrlsOfThisTag.add(url);

			/*
			 * STEP7
			 * URL���R�[�h�ɁA����̃u�b�N�}�[�N���֘A�t����i�ǉ��X�V����j�B
			 */
			keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
			keysUrlsDB_Url[1] = url;

			keysUrlsDB_RefTag[0] = HamDBKeys.BOOKMARKS_ROOT;
			keysUrlsDB_RefTag[1] = url;
			keysUrlsDB_RefTag[2] = HamDBKeys.TAG_REF;

			//���R�[�h�}���X�V
			getDatabases().byUrlDB().insertOrUpdate(mHamTransByUrls, keysUrlsDB_Url, newBookmarkItem);
			getDatabases().byUrlDB().insertOrUpdate(mHamTransByUrls, keysUrlsDB_RefTag, THIS_TAG);
			// �T���l�C����荞��
			tryImportThumbnail(c, newBookmarkItem);
		}

		/*
		 * STEP8
		 * �^�O���R�[�h���X�V����B
		 *
		 * 1) ���̃^�O�Ɋ֘A�t�����Ă�������URL���X�g�ƍ���̃u�b�N�}�[�N��URL���X�g���}�[�W
		 * 2) 1)�̃��X�g������URLDB��ǂݍ����BookmarkItems��ArrayList�\�z�B
		 * 3) 2)�ɑ΂��Č��݂̃\�[�g��K�p
		 * 4) 3)����URL�𔲂��o���āATHIS_TAG�X�V�p��URL���X�g���쐬
		 * 5) 4)��NO_THIS_TAG�̃��R�[�h���X�V
		 */
		//1)
		renewalUrlsOfThisTag.addAll(allUrlsOfThisTag);
		//2)
		bookmarksForSort = new ArrayList<BookmarkItem>();
		keysUrlsDB_Url[0] = HamDBKeys.BOOKMARKS_ROOT;
		keysUrlsDB_Url[1] = null;
		for(String currentUrl : renewalUrlsOfThisTag)
		{
			keysUrlsDB_Url[1] = currentUrl;
			BookmarkItem item = (BookmarkItem) getDatabases().byUrlDB().find(mHamTransByUrls, keysUrlsDB_Url);
			if(item != null)
				bookmarksForSort.add(item);
		}
		//3)
		Collections.sort(bookmarksForSort, mBookmarkItemSort);
		//4)
		renewalUrlsOfThisTag.clear();
		for(BookmarkItem item : bookmarksForSort)
		{
			renewalUrlsOfThisTag.add(item.getUrl());
		}
		//5)
		keysTagsDB_Tag[0] = HamDBKeys.TAGS_ROOT;
		keysTagsDB_Tag[1] = THIS_TAG;
		getDatabases().byTagsDB().insertOrUpdate(mHamTransByTags, keysTagsDB_Tag, renewalUrlsOfThisTag);


		//�����܂łł��ׂĂ̍X�V������
		mCommitable = true;
		mReturnMessage = obtainMessage(MESSAGEFLAGS_COMPLETE);
		Log.d(TAG, "END");
		return true;
	}

	/**
	 * Compose BookmarkItem object from the bookmark cursor.
	 */
	private BookmarkItem composeBookmarkItem(Cursor c)
	{
		BookmarkItem result = new BookmarkItem();
		result.setUrl(c.getString(c.getColumnIndexOrThrow(android.provider.Browser.BookmarkColumns.URL)));
		result.setTitle(c.getString(c.getColumnIndexOrThrow(android.provider.Browser.BookmarkColumns.TITLE)));

		long created = c.getLong(c.getColumnIndexOrThrow(android.provider.Browser.BookmarkColumns.CREATED));
		result.setCreated(created, mCreatedLabel);

		long lastAccessed = c.getLong(c.getColumnIndexOrThrow(android.provider.Browser.BookmarkColumns.DATE));
		result.setLastAccessed(lastAccessed, mLastAccessedLabel);

		byte[] favicon = c.getBlob(c.getColumnIndexOrThrow(android.provider.Browser.BookmarkColumns.FAVICON));
		if(favicon == null || favicon.length == 0)
		{
			Bitmap ic = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.app_web_browser_sm);
			result.setFavicon(ic);
			ic.recycle();
		}
		else
		{
			result.setFavicon(favicon);
		}

		return result;
	}

	/**
	 * Get the bookmark cursor.
	 */
	private Cursor obtainChromeLiteBookmarksCursor()
	{
		/*
			android.provider.Browser.BookmarkColumns.URL,		//URL
			android.provider.Browser.BookmarkColumns.TITLE,		//�y�[�W�^�C�g��(html��title�^�O)
			android.provider.Browser.BookmarkColumns.FAVICON,	//�y�[�W��Favicon
			android.provider.Browser.BookmarkColumns.CREATED,	//�u�b�N�}�[�N�o�^��
			android.provider.Browser.BookmarkColumns.DATE		//�ŏI�A�N�Z�X��
		 */

		Cursor result = getContext().getContentResolver().query(
			android.provider.Browser.BOOKMARKS_URI,
			null,
			android.provider.Browser.BookmarkColumns.BOOKMARK+"=?" ,
			new String[]{ "1" },
			android.provider.Browser.BookmarkColumns.URL+" ASC"
		);

		return result;
	}

	/**
	 * Copy thumbnail to cache direcotry.
	 *
	 * Notes: ONLY Android OS 2.2(Froyo) or the later.
	 */
	private void tryImportThumbnail(final Cursor c, final BookmarkItem bookmark)
	{
		if(c.getColumnIndex("thumbnail") == -1)
			return;

		final byte[] thumbnail = c.getBlob(c.getColumnIndexOrThrow("thumbnail"));
		if(thumbnail != null && thumbnail.length != 0)
		{
			final File thumbnailCacheFile = Procedure.getThumbnailLocalCacheFile(
				getContext().getCacheDir().getPath(),
				Path.WEBPAGE_CAPTURE_SERVICE_PREFIX+bookmark.getUrl());

			Runnable saveTask = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						FileOutputStream fos = new FileOutputStream(thumbnailCacheFile);
						fos.write(thumbnail);
						fos.flush();
						fos.close();
						Log.d(TAG, "Import/save web page thumbnail from browser provider. ("+bookmark.getUrl()+")");
					}
					catch (FileNotFoundException e)
					{
						Log.e(TAG, e.getMessage(), e);
					}
					catch (IOException e)
					{
						Log.e(TAG, e.getMessage(), e);
					}
				}
			};
			Thread task = new Thread(saveTask);
			task.setPriority(Thread.NORM_PRIORITY-1);
			task.start();
		}
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
