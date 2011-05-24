/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.data;

import java.io.Serializable;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import jp.androdev.hambookmarks.IDeepCopiable;

import org.apache.commons.lang.StringUtils;

/**
 * タグクラス
 *
 * @version 1.0.0
 */
public final class TagItem implements Serializable, IDeepCopiable<TagItem>
{
	/** Serial ID */
	private static final long serialVersionUID = -1549161127034777726L;

	private final String mTagName;
	private final String mTagCaption;

	/**
	 * コンストラクタ
	 *
	 * @param tag タグ名
	 * @param caption タグの表示名
	 */
	public TagItem(String tag, String caption)
	{
		if(StringUtils.isBlank(tag))
			throw new NullPointerException("tag is null or empty.");

		mTagName = tag;
		mTagCaption = (StringUtils.isBlank(caption) ? tag : caption);
	}

	/**
	 * コンストラクタ
	 *
	 * @param tag タグ名
	 */
	public TagItem(String tag)
	{
		this(tag, null);
	}

	/**
	 * タグを取得する。
	 */
	public String getTag()
	{
		return mTagName;
	}

	/**
	 * タグの表示名を取得する。
	 */
	public String getTagCaption()
	{
		return mTagCaption;
	}

	/*
	 * (非 Javadoc)
	 * @see jp.androdev.hambookmarks.IDeepCopiable#deepCopy()
	 */
	@Override
	public TagItem deepCopy()
	{
		return new TagItem(mTagName);
	}

	/*
	 * (非 Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int multiplier = 37;

		StringCharacterIterator iter = new StringCharacterIterator(mTagName);
		int result = 17;

		for(char c = iter.first(); c != CharacterIterator.DONE; c = iter.next())
		{
			result = multiplier * result + (int)c;
		}

		iter.setText(mTagCaption);
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
		else if(!(o instanceof TagItem))
		{
			return false;
		}

		return mTagName.equals(((TagItem)o).mTagName);
	}

	/*
	 * (非 Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
//		return new StringBuilder()
//			.append("Tag { ")
//			.append(mTagName+", ")
//			.append(mTagCaption)
//			.append(" }")
//			.toString();
		return mTagCaption;
	}

//	/**
//	 * @see {@link ObjectInputStream}
//	 */
//	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
//	{
//		stream.defaultReadObject();
//	}
}
