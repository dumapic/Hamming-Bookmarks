/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import jp.androdev.hambookmarks.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * �^�O���X�g�p�̃A�_�v�^
 *
 * @version 1.0.0
 */
public final class TagItemAdapter extends BaseAdapter implements Iterable<TagItem>
{
	/**
	 * �^�O���X�g�̕��я�
	 */
	public static class SortOrder
	{
		public static final int BY_TITLE_ASC 			= 0x01;
		public static final int BY_TITLE_DESC 		= 0x02;
	}
	/**
	 * �^�O���X�g�̊���̕��я����擾����B
	 */
	public static int getDefaultSortOrder()
	{
		return SortOrder.BY_TITLE_ASC;
	}

	private final Vector<TagItem> mItems;
	private final LayoutInflater mInflater;

	private boolean mIsLoading;

	/**
	 * �R���X�g���N�^
	 *
	 * @param context �R���e�L�X�g
	 */
	public TagItemAdapter(Context context)
	{
		mItems = new Vector<TagItem>();
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mIsLoading = false;
	}

	/* (�� Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder;
		TagItem item = mItems.get(position);

		if(convertView == null)
		{
			convertView = mInflater.inflate(R.layout.parts_main_listitem_def_tags, null);
			holder = new ViewHolder();
			holder.imgIcon = (ImageView) convertView.findViewById(R.id.img_icon);
			holder.txtTitle = (TextView) convertView.findViewById(R.id.txt_title);

			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}

		holder.txtTitle.setText(item.getTagCaption());

		return convertView;
	}

	static class ViewHolder
	{
		ImageView imgIcon;
		TextView txtTitle;
	}

	/**
	 * �^�O�̃��X�g���A�_�v�^�ɒǉ�����B
	 */
	public void addAll(Collection<TagItem> tags)
	{
		if(tags != null && tags.size() > 0)
			mItems.addAll(tags);
	}

	/**
	 * �^�O���A�_�v�^�ǉ�����B
	 */
	public void add(TagItem tags)
	{
		if(tags != null)
			mItems.add(tags);
	}

	/**
	 * �^�O���A�_�v�^����폜����B
	 */
	public void remove(TagItem tag)
	{
		if(tag == null)
			return;

		int pos = mItems.indexOf(tag);
		if(pos != -1)
		{
			mItems.remove(pos);
		}
	}

	/**
	 * �A�_�v�^�̃^�O���X�g��S�폜����B
	 */
	public void removeAll()
	{
		mItems.removeAllElements();
	}

	/**
	 * �^�O��ʂ̃^�O�ɒu��������B
	 */
	public void replace(TagItem oldTag, TagItem newTag)
	{
		if(oldTag == null || newTag == null)
			return;

		int pos = mItems.indexOf(oldTag);
		if(pos != -1)
		{
			mItems.setElementAt(newTag, pos);
		}
	}

	/**
	 * ���̃A�_�v�^�[���ύX�����ۂ�
	 */
	public boolean nowLoading()
	{
		return mIsLoading;
	}

	/**
	 * ���̃A�_�v�^�̏�Ԃ�ݒ肷��B
	 * @param state ���̏����ŃA�_�v�^�����̃��X�g�����ς��������Ȃ��ꍇ��true.
	 */
	public void setLoadingState(boolean state)
	{
		mIsLoading = state;
	}

	/*
	 * (�� Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<TagItem> iterator()
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
		return 0;
	}
}
