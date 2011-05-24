/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.dialog;

import java.io.Serializable;


/**
 * 選択肢アイテム
 *
 * @version 1.0.0
 */
public class ChooserDialogItem implements Serializable
{
	/** Serial ID */
	private static final long serialVersionUID = 1467969763821828697L;

	private final int mId;
	private final String mCaption;
	private final Serializable mValue;
	private final boolean mSelected;

	/**
	 * コンストラクタ
	 *
	 * @param id 識別ID
	 * @param caption 表示文字列
	 * @param value 持ち回る値
	 * @param selected 初期選択状態
	 */
	public ChooserDialogItem(int id, String caption, Serializable value, boolean selected)
	{
		mId = id;
		mCaption = caption;
		mValue = value;
		mSelected = selected;
	}

	/**
	 * コンストラクタ
	 *
	 * @param id 識別ID
	 * @param caption 表示文字列
	 * @param value 持ち回る値
	 */
	public ChooserDialogItem(int id, String caption, Serializable value)
	{
		this(id, caption, value, false);
	}

	/**
	 * コンストラクタ
	 *
	 * @param id 識別ID
	 * @param caption 表示文字列
	 */
	public ChooserDialogItem(int id, String caption)
	{
		this(id, caption, null, false);
	}

	/**
	 * 表示文字列IDを取得する
	 */
	public int getId(){ return mId; }

	/**
	 * 表示文字列を取得する
	 */
	public String getCaption(){ return this.mCaption; }

	/**
	 * 選択状態を取得する
	 */
	public boolean getSelected(){ return mSelected; }

	/**
	 * 持ち回り用の値を取得する
	 */
	public Object getValue(){ return mValue; }

	/*
	 * (非 Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof ChooserDialogItem))
		{
			return false;
		}

		return mId == ((ChooserDialogItem)o).mId;
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
