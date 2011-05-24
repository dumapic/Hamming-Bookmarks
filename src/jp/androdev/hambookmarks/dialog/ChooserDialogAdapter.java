/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.dialog;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import jp.androdev.hambookmarks.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

/**
 * �I���_�C�A���O�̃A�_�v�^�B
 *
 * @version 1.0.0
 */
public final class ChooserDialogAdapter<T extends ChooserDialogItem> extends BaseAdapter implements Iterable<T>
{
	private final Vector<T> mItems;
	private final LayoutInflater mInflater;
	private final int mLayoutId;
	private final int mListItemLayoutId;

	/**
	 * �R���X�g���N�^
	 *
	 * @param context �R���e�L�X�g
	 * @param choiceMode �I�����[�h�i{@link ListView#CHOICE_MODE_SINGLE} Or {@link ListView#CHOICE_MODE_MULTIPLE}�j
	 */
	public ChooserDialogAdapter(Context context, int choiceMode)
	{
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mItems = new Vector<T>();

		switch(choiceMode)
		{
			case ListView.CHOICE_MODE_SINGLE:
			{
				mLayoutId = R.layout.parts_chooserdialog_listitem_singlechoice;
				mListItemLayoutId = R.id.chooserdialog_listitem;
				break;
			}
			case ListView.CHOICE_MODE_MULTIPLE:
			{
				mLayoutId = R.layout.parts_chooserdialog_listitem_multiplechoice;
				mListItemLayoutId = R.id.chooserdialog_listitem;
				break;
			}
			default:
			{
				throw new UnsupportedOperationException("This choice mode was wrong value. - "+choiceMode);
			}
		}
	}

	/* (�� Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder;
		T item = mItems.get(position);

		if(convertView == null)
		{
			convertView = mInflater.inflate(mLayoutId, null);
			holder = new ViewHolder();
			holder.chkView = (CheckedTextView) convertView.findViewById(mListItemLayoutId);
			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}

		holder.chkView.setText(item.getCaption());

		return convertView;
	}

	static class ViewHolder
	{
		CheckedTextView chkView;
	}

	/**
	 * �I�����A�C�e����ǉ�����B
	 *
	 * @param items �I�����A�C�e��
	 */
	public void addAll(Collection<T> items)
	{
		mItems.addAll(items);
	}

	/**
	 * �I�����A�C�e����ǉ�����B
	 *
	 * @param item �I�����A�C�e��
	 */
	public void add(T item)
	{
		mItems.add(item);
	}

	@Override
	public Iterator<T> iterator()
	{
		return mItems.iterator();
	}

	/* (�� Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount()
	{
		return mItems.size();
	}

	/* (�� Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position)
	{
		return mItems.get(position);
	}

	/* (�� Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Deprecated
	@Override
	public long getItemId(int position)
	{
		return -1;
	}
}
