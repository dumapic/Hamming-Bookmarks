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
 * ブックマークアイテム
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
	 * コンストラクタ
	 */
	public BookmarkItem()
	{
		//null-pointer防止
		mUrl = EMPTY;
		mTitle = EMPTY;
		mCreatedCaption = EMPTY;
		mLastAccessedCaption = EMPTY;

		mFavionImage = null;
	}

	/** URLを取得する */
	public String getUrl(){ return mUrl; }
	/** URLを設定する */
	public void setUrl(String url){ mUrl = url; }

	/** タイトルを取得する */
	public String getTitle(){ return mTitle; }
	/** タイトルを設定する */
	public void setTitle(String title){ mTitle = title; }

	/** 登録日を取得する */
	public long getCreated(){ return mCreated; }
	/** 最終アクセス日を取得する */
	public long getLastAccessed(){ return mLastAccessed; }

	/** 登録日の表示文字列を取得する */
	public String getCreatedCaption(){ return mCreatedCaption; }
	/** 最終アクセス日の表示文字列を取得する */
	public String getLastAccessedCaption(){ return mLastAccessedCaption; }

	/** 登録日を設定する */
	public void setCreated(long date, String label)
	{
		mCreated = date;
		java.util.Date d = new java.util.Date(date);
		if(d.getYear() >= 100)	//2000年以降
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

	/** 最終アクセス日を設定する */
	public void setLastAccessed(long date, String label)
	{
		mLastAccessed = date;
		java.util.Date d = new java.util.Date(date);
		if(d.getYear() >= 100)	//2000年以降
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

	/** アイコンを取得する */
	public Bitmap getFavicon(){ return mFavionImage; }
	/** アイコンを設定する */
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
	/** アイコンを設定する */
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
	 * (非 Javadoc)
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
	 * (非 Javadoc)
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
	 * (非 Javadoc)
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
	 * (非 Javadoc)
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
