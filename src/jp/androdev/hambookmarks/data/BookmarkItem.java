/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.CharacterIterator;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.Locale;

import jp.androdev.hambookmarks.IDeepCopiable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * �u�b�N�}�[�N�A�C�e��
 *
 * @version 1.0.0
 */
public final class BookmarkItem implements Serializable, IDeepCopiable<BookmarkItem>
{
	private static final long serialVersionUID = -4939112660902802076L;
	private static final String EMPTY = "";
	private static final String UNKNOWN_DATE_CAPTION = " - ";
	private static final long UNKNOWN_DATE_TIME = new java.util.Date(0, 0, 1).getTime();

	private String mUrl;
	private String mTitle;
	private long mCreated;
	private long mLastAccessed;
	private byte[] mFaviconData;

	private String mCreatedCaption;
	private String mLastAccessedCaption;

	private transient Bitmap mFavionImage;

	/**
	 * �R���X�g���N�^
	 */
	public BookmarkItem()
	{
		//null-pointer�h�~
		mUrl = EMPTY;
		mTitle = EMPTY;
		mCreatedCaption = EMPTY;
		mLastAccessedCaption = EMPTY;

		mFavionImage = null;
	}

	/** URL���擾���� */
	public String getUrl(){ return mUrl; }
	/** URL��ݒ肷�� */
	public void setUrl(String url){ mUrl = url; }

	/** �^�C�g�����擾���� */
	public String getTitle(){ return mTitle; }
	/** �^�C�g����ݒ肷�� */
	public void setTitle(String title){ mTitle = title; }

	/** �o�^�����擾���� */
	public long getCreated(){ return mCreated; }
	/** �ŏI�A�N�Z�X�����擾���� */
	public long getLastAccessed(){ return mLastAccessed; }

	/** �o�^���̕\����������擾���� */
	public String getCreatedCaption(){ return mCreatedCaption; }
	/** �ŏI�A�N�Z�X���̕\����������擾���� */
	public String getLastAccessedCaption(){ return mLastAccessedCaption; }

	/** �o�^����ݒ肷�� */
	public void setCreated(long date, String label)
	{
		mCreated = date;
		java.util.Date d = new java.util.Date(date);
		if(d.getYear() >= 100)	//2000�N�ȍ~
		{
			mCreatedCaption = new StringBuilder()
				.append(label)
				.append(new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(d))
				.toString();
		}
		else
		{
			mCreated = UNKNOWN_DATE_TIME;
			mCreatedCaption = new StringBuilder()
				.append(label)
				.append(UNKNOWN_DATE_CAPTION)
				.toString();
		}
	}

	/** �ŏI�A�N�Z�X����ݒ肷�� */
	public void setLastAccessed(long date, String label)
	{
		mLastAccessed = date;
		java.util.Date d = new java.util.Date(date);
		if(d.getYear() >= 100)	//2000�N�ȍ~
		{
			mLastAccessedCaption = new StringBuilder()
				.append(label)
				.append(new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(d))
				.toString();
		}
		else
		{
			mLastAccessed = UNKNOWN_DATE_TIME;
			mLastAccessedCaption = new StringBuilder()
				.append(label)
				.append(UNKNOWN_DATE_CAPTION)
				.toString();
		}
	}

	/** �A�C�R�����擾���� */
	public Bitmap getFavicon(){ return mFavionImage; }
	/** �A�C�R����ݒ肷�� */
	public void setFavicon(Bitmap icon)
	{
		if(icon == null)
			throw new NullPointerException("Favicon is null.");

		if(mFavionImage != null)
			mFavionImage.recycle();

		mFavionImage = null;
		mFaviconData = null;

		mFavionImage = icon.copy(Bitmap.Config.ARGB_8888, false);
		ByteArrayOutputStream stream =new ByteArrayOutputStream();
		mFavionImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
		mFaviconData = stream.toByteArray();
	}
	/** �A�C�R����ݒ肷�� */
	public void setFavicon(byte[] data)
	{
		if(data == null || data.length == 0)
			throw new NullPointerException("Favicon is null/zero byte.");

		if(mFavionImage != null)
			mFavionImage.recycle();

		mFavionImage = null;
		mFaviconData = null;

		mFaviconData = data;
		mFavionImage = BitmapFactory.decodeByteArray(mFaviconData, 0, mFaviconData.length);
	}

	/*
	 * (�� Javadoc)
	 * @see jp.androdev.hambookmarks.IDeepCopiable#deepCopy()
	 */
	@Override
	public BookmarkItem deepCopy()
	{
		BookmarkItem clone = new BookmarkItem();
		clone.mUrl = mUrl;
		clone.mTitle = mTitle;

		clone.mCreated = mCreated;
		clone.mCreatedCaption = mCreatedCaption;
		clone.mLastAccessed = mLastAccessed;
		clone.mLastAccessedCaption = mLastAccessedCaption;

		clone.setFavicon(mFaviconData);

		return clone;
	}

	/*
	 * (�� Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int multiplier = 37;
		final StringCharacterIterator iter = new StringCharacterIterator(toString());
		int result = 17;

		for(char c = iter.first(); c != CharacterIterator.DONE; c = iter.next())
		{
			result = multiplier * result + (int)c;
		}

		return result;
	}

	/*
	 * (�� Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o)
	{
		if(o == this)
		{
			return true;
		}
		else if(!(o instanceof BookmarkItem))
		{
			return false;
		}

		return toString().equals(((BookmarkItem)o).toString());
	}

	/*
	 * (�� Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return new StringBuilder()
			.append("BookmarkItem{ ")
			.append("Url: ").append(mUrl).append(", ")
			.append("Title: ").append(mTitle).append(", ")
			.append("Created: ").append(mCreated).append(", ")
			.append("LastAccessed: ").append(mLastAccessed).append(", ")
			.append("Favicon(len): ").append(mFaviconData!=null?mFaviconData.length:"null").append(" ")
			.append("}")
			.toString();
	}

	/**
	 * @see {@link ObjectInputStream}
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
		stream.defaultReadObject();

		mFavionImage = BitmapFactory.decodeByteArray(mFaviconData, 0, mFaviconData.length);
	}
}
