/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.dialog;

import java.io.Serializable;


/**
 * �I�����A�C�e��
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
	 * �R���X�g���N�^
	 *
	 * @param id ����ID
	 * @param caption �\��������
	 * @param value �������l
	 * @param selected �����I�����
	 */
	public ChooserDialogItem(int id, String caption, Serializable value, boolean selected)
	{
		mId = id;
		mCaption = caption;
		mValue = value;
		mSelected = selected;
	}

	/**
	 * �R���X�g���N�^
	 *
	 * @param id ����ID
	 * @param caption �\��������
	 * @param value �������l
	 */
	public ChooserDialogItem(int id, String caption, Serializable value)
	{
		this(id, caption, value, false);
	}

	/**
	 * �R���X�g���N�^
	 *
	 * @param id ����ID
	 * @param caption �\��������
	 */
	public ChooserDialogItem(int id, String caption)
	{
		this(id, caption, null, false);
	}

	/**
	 * �\��������ID���擾����
	 */
	public int getId(){ return mId; }

	/**
	 * �\����������擾����
	 */
	public String getCaption(){ return this.mCaption; }

	/**
	 * �I����Ԃ��擾����
	 */
	public boolean getSelected(){ return mSelected; }

	/**
	 * �������p�̒l���擾����
	 */
	public Object getValue(){ return mValue; }

	/*
	 * (�� Javadoc)
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
	 * (�� Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return mCaption;
	}
}
