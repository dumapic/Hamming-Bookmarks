/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.data;

import java.io.File;
import java.util.Collection;
import java.util.Vector;

import jp.androdev.debkit.lazyimageloader.ILazyDownloadingCachePolicy;
import jp.androdev.debkit.lazyimageloader.LazyLoadImageView;
import jp.androdev.hambookmarks.Constants.Path;
import jp.androdev.hambookmarks.Procedure;
import jp.androdev.hambookmarks.R;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * �u�b�N�}�[�N�A�C�e���̃A�_�v�^�B
 *
 * @version 1.0.0
 */
public final class BookmarkItemAdapter extends BaseAdapter
{
	/**
	 * �u�b�N�}�[�N�̕��я���`
	 */
	public static class SortOrder
	{
		public static final int BY_URL_ASC 			= 0x01;
		public static final int BY_TITLE_ASC 			= 0x02;
		public static final int BY_CREATED_DESC 		= 0x03;
		public static final int BY_LASTACCESSED_DESC 	= 0x04;
	}
	/**
	 * �u�b�N�}�[�N�̊���̕��я����擾����
	 */
	public static int getDefaultSortOrder()
	{
		return SortOrder.BY_TITLE_ASC;
	}

	private final Vector<BookmarkItem> mItems;
	private final LayoutInflater mInflater;
	private final ILazyDownloadingCachePolicy mThumbCachePolicy;
	private final Drawable mPendingDrawable;

	private boolean mIsLoading;

	/**
	 * �R���X�g���N�^
	 *
	 * @param context �R���e�L�X�g
	 */
	public BookmarkItemAdapter(Context context)
	{
		mItems = new Vector<BookmarkItem>();
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mIsLoading = false;

		mPendingDrawable = new ColorDrawable(Color.BLACK);
		mThumbCachePolicy = new ILazyDownloadingCachePolicy()
		{
			@Override
			public File getOrComposeLocalCacheFile(Context context, String url)
			{
				return Procedure.getThumbnailLocalCacheFile(context.getCacheDir().getPath(), url);
			}
		};
	}

	/* (�� Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder;
		BookmarkItem item = mItems.get(position);

		if(convertView == null)
		{
			convertView = mInflater.inflate(R.layout.parts_main_listitem_def_bookmarks, null);
			holder = new ViewHolder();
			holder.imgFavicon = (ImageView) convertView.findViewById(R.id.img_icon);
			holder.txtTitle = (TextView) convertView.findViewById(R.id.txt_title);
			holder.txtUrl = (TextView) convertView.findViewById(R.id.txt_url);
			holder.txtCreated = (TextView) convertView.findViewById(R.id.txt_created);
			holder.txtLastAccessed = (TextView) convertView.findViewById(R.id.txt_lastaccessed);

			holder.imgThumbnail = (LazyLoadImageView) convertView.findViewById(R.id.img_thumbnail);
			holder.imgThumbnail.setPendingImage(mPendingDrawable);
			holder.imgThumbnail.setCachePolicy(mThumbCachePolicy);

			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}

		holder.imgThumbnail.loadImage(Path.WEBPAGE_CAPTURE_SERVICE_PREFIX+item.getUrl(), false);
		holder.imgFavicon.setImageBitmap(item.getFavicon());
		holder.txtTitle.setText(item.getTitle());
		holder.txtUrl.setText(item.getUrl());
		holder.txtCreated.setText(item.getCreatedCaption());
		holder.txtLastAccessed.setText(item.getLastAccessedCaption());

		return convertView;
	}

	static class ViewHolder
	{
		ImageView imgFavicon;
		TextView txtTitle;
		TextView txtUrl;
		TextView txtCreated;
		TextView txtLastAccessed;
		LazyLoadImageView imgThumbnail;
	}

	/**
	 * �u�b�N�}�[�N���X�g��ǉ�����
	 */
	public void addAll(Collection<BookmarkItem> items)
	{
		if(items != null && items.size() > 0)
			mItems.addAll(items);
	}

	/**
	 * �u�b�N�}�[�N�A�C�e����ǉ�����
	 */
	public void add(BookmarkItem item)
	{
		if(item != null)
			mItems.add(item);
	}

	/**
	 * �u�b�N�}�[�N�A�C�e�����폜����B
	 */
	public void remove(BookmarkItem item)
	{
		if(item == null)
			return;

		int pos = mItems.indexOf(item);
		if(pos != -1)
		{
			mItems.remove(pos);
		}
	}

	/**
	 * �A�_�v�^�̃u�b�N�}�[�N���X�g��S�폜����B
	 */
	public void removeAll()
	{
		mItems.removeAllElements();
	}

	/**
	 * �u�b�N�}�[�N�A�C�e����u������
	 */
	public void replace(BookmarkItem oldItem, BookmarkItem newItem)
	{
		if(oldItem == null || newItem == null)
			return;

		int pos = mItems.indexOf(oldItem);
		if(pos != -1)
		{
			mItems.setElementAt(newItem, pos);
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
