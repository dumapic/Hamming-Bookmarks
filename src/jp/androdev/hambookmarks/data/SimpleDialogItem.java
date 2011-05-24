/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.data;

import java.io.Serializable;

/**
 * 単純選択リストのアイテムクラス
 *
 * @version 1.0.0
 */
public class SimpleDialogItem implements Serializable
{
	/** Serial ID */
	private static final long serialVersionUID = 4865444443006953668L;

	private final String mCaption;
	private final int mId;

	/**
	 * コンストラクタ
	 *
	 * @param id 選択アイテムの識別値
	 * @param caption 表示文字列
	 */
	public SimpleDialogItem(int id, String caption)
	{
		mId = id;
		mCaption = caption;
	}

	/**
	 * 表示文字列を取得する。
	 */
	public String getCaption()
	{
		return mCaption;
	}

	/**
	 * 識別IDを取得する。
	 */
	public int getId()
	{
		return mId;
	}

	/*
	 * (非 Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof SimpleDialogItem))
		{
			return false;
		}

		return mId == ((SimpleDialogItem)o).mId;
	}

	/*
	 * (非 Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return mCaption;
	}
}
