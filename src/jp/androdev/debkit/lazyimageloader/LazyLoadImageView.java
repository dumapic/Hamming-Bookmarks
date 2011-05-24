
package jp.androdev.debkit.lazyimageloader;

import java.io.File;

import jp.androdev.debkit.util.Log;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * ImageView for lazy downloading image file.
 *
 * @see http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 */
public class LazyLoadImageView extends ImageView
{
	/**
	 * Loading status code.
	 */
	public static final class LoadingImageStatus
	{
		public static final int LOAD_COMPLETE = 0x00001000;
	}

	private static final String TAG = "LazyDownloader";

	private String mDownloadUrl;
	private ILazyDownloadingCachePolicy mCachePolicy;
	private Drawable mPendingDrawable;
	private LazyDownloadingThread mThread;

	/**
	 * Constructor.
	 */
	public LazyLoadImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		mDownloadUrl = null;
		mCachePolicy = null;
		mPendingDrawable = new ColorDrawable(Color.BLACK);
		mThread = null;
	}

	/**
	 * Constructor.
	 */
	public LazyLoadImageView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	/**
	 * Set image while loading.
	 * @param d
	 */
	public void setPendingImage(Drawable d)
	{
		if(d != null)
			mPendingDrawable = d;
	}

	/**
	 * Set cache policy object.
	 */
	public void setCachePolicy(ILazyDownloadingCachePolicy cachePolicy)
	{
		if(cachePolicy != null)
			mCachePolicy = cachePolicy;
	}

	/**
	 * Lazy load or downloading image with url.
	 *
	 * @param url download image path.
	 * @param forceDownload Delete local cache file and download from url.
	 */
	public void loadImage(String url, boolean forceDownload)
	{
		mDownloadUrl = url;

		if(!forceDownload)
		{
			final File localCacheFile = mCachePolicy.getOrComposeLocalCacheFile(getContext(), mDownloadUrl);
			if(localCacheFile.exists())
			{
				setImageFromLocalFile(localCacheFile);
				return;
			}
		}

		queue();
	}

	/*
	 * (”ñ Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	public void finalize()
	{
		if (mThread != null)
		{
			LazyDownloadingQueue queue = LazyDownloadingQueue.getInstance();
			queue.dequeue(mThread);
		}
	}

	/**
	 * Queue the downloading thread.
	 */
	private void queue()
	{
		if (mThread == null)
		{
			mThread = new LazyDownloadingThread(getContext(), mCachePolicy, mDownloadUrl, mHandler);
			LazyDownloadingQueue queue = LazyDownloadingQueue.getInstance();
			queue.enqueue(mThread, LazyDownloadingQueue.PRIORITY_HIGH);
		}
		setVisibility(View.INVISIBLE);
		//setImageDrawable(mPendingDrawable);
	}

	/**
	 * Load from local cache file.
	 * @param file local cache image file.
	 */
	private void setImageFromLocalFile(final File file)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				Log.d(TAG, "START - setImageFromLocalFile");
				Log.d(TAG, "  --> mDownloadUrl: "+mDownloadUrl);
				Log.d(TAG, "  --> localCacheFile: "+file.getPath());

				mThread = null;
				Bitmap bm = BitmapFactory.decodeFile(file.getPath());

				if (bm != null)
				{
					setImageBitmap(bm);
					Log.d(TAG, "  --> Load image from local file.");
				}
				else
				{
					setImageDrawable(mPendingDrawable);
					Log.d(TAG, "  --> set pending drawable.(decodeFile result is null)");
				}
				setVisibility(View.VISIBLE);
			}
		});
	}

	Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
				case LoadingImageStatus.LOAD_COMPLETE:
					File localCacheFile = mCachePolicy.getOrComposeLocalCacheFile(getContext(), mDownloadUrl);
					setImageFromLocalFile(localCacheFile);
					break;

				default:
					throw new IllegalStateException("Illegal message. -->msg.what:"+msg.what);
			}
		}
	};
}
