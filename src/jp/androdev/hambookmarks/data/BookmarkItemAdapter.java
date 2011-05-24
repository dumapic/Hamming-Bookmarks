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
 * ブックマークアイテムのアダプタ。
 *
 * @version 1.0.0
 */
public final class BookmarkItemAdapter extends BaseAdapter
{
	/**
	 * ブックマークの並び順定義
	 */
	public static class SortOrder
	{
		public static final int BY_URL_ASC 			= 0x01;
		public static final int BY_TITLE_ASC 			= 0x02;
		public static final int BY_CREATED_DESC 		= 0x03;
		public static final int BY_LASTACCESSED_DESC 	= 0x04;
	}
	/**
	 * ブックマークの既定の並び順を取得する
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
	 * コンストラクタ
	 *
	 * @param context コンテキスト
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

	/* (非 Javadoc)
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
	 * ブックマークリストを追加する
	 */
	public void addAll(Collection<BookmarkItem> items)
	{
		if(items != null && items.size() > 0)
			mItems.addAll(items);
	}

	/**
	 * ブックマークアイテムを追加する
	 */
	public void add(BookmarkItem item)
	{
		if(item != null)
			mItems.add(item);
	}

	/**
	 * ブックマークアイテムを削除する。
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
	 * アダプタのブックマークリストを全削除する。
	 */
	public void removeAll()
	{
		mItems.removeAllElements();
	}

	/**
	 * ブックマークアイテムを置換する
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
	 * このアダプターが変更中か否か
	 */
	public boolean nowLoading()
	{
		return mIsLoading;
	}

	/**
	 * このアダプタの状態を設定する。
	 * @param state 他の処理でアダプタ内部のリストを改変させたくない場合はtrue.
	 */
	public void setLoadingState(boolean state)
	{
		mIsLoading = state;
	}

	/* (非 Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount()
	{
		return mItems.size();
	}

	/* (非 Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position)
	{
		return mItems.get(position);
	}

	/* (非 Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Deprecated
	@Override
	public long getItemId(int position)
	{
		return 0;
	}
}
