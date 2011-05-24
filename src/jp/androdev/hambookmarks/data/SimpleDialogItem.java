/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.data;

import java.io.Serializable;

/**
 * �P���I�����X�g�̃A�C�e���N���X
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
	 * �R���X�g���N�^
	 *
	 * @param id �I���A�C�e���̎��ʒl
	 * @param caption �\��������
	 */
	public SimpleDialogItem(int id, String caption)
	{
		mId = id;
		mCaption = caption;
	}

	/**
	 * �\����������擾����B
	 */
	public String getCaption()
	{
		return mCaption;
	}

	/**
	 * ����ID���擾����B
	 */
	public int getId()
	{
		return mId;
	}

	/*
	 * (�� Javadoc)
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
	 * (�� Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return mCaption;
	}
}
