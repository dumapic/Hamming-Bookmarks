
package jp.androdev.debkit.lazyimageloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.UnknownHostException;

import jp.androdev.debkit.lazyimageloader.LazyLoadImageView.LoadingImageStatus;
import jp.androdev.debkit.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.os.Handler;

/**
 * Thread for download image file.
 *
 * @see http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 */
public class LazyDownloadingThread extends Thread
{
	/**
	 * Download status code.
	 */
	public static class DownloadStatus
	{
		public static final int STATUS_PENDING = 0x00000001;
		public static final int STATUS_RUNNING = 0x00000002;
		public static final int STATUS_FINISHED = 0x00000003;
	}

	private static final String TAG = "LazyDownloader";

	private final String mUrlPath;
	private final Context mContext;

	private int mStatus = DownloadStatus.STATUS_PENDING;
	private SoftReference<ILazyDownloadingCachePolicy> mCachePolicy;
	private SoftReference<Handler> mHandler;

	/**
	 * Constructor.
	 *
	 * @param context Context object.
	 * @param cachePolicy CachePolicy object.
	 * @param url Image file link for downloading.
	 * @param handler Handler object.
	 */
	public LazyDownloadingThread(
		final Context context,
		final ILazyDownloadingCachePolicy cachePolicy,
		final String url,
		final Handler handler)
	{
		mContext = context;
		mUrlPath = url;

		mStatus = DownloadStatus.STATUS_PENDING;
		mCachePolicy = new SoftReference<ILazyDownloadingCachePolicy>(cachePolicy);
		mHandler = new SoftReference<Handler>(handler);

		setName("LazyDownloadingThread:"+getId());
	}

	/*
	 * (”ñ Javadoc)
	 * @see java.lang.Thread#getId()
	 */
	@Override
	public long getId()
	{
		return mUrlPath.hashCode();
	}

	/*
	 * (”ñ Javadoc)
	 * @see java.lang.Thread#start()
	 */
	@Override
	public void start()
	{
		if (getStatus() == DownloadStatus.STATUS_PENDING)
		{
			synchronized (this)
			{
				mStatus = DownloadStatus.STATUS_RUNNING;
			}
			super.start();
		}
	}

	/*
	 * (”ñ Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run()
	{
		Log.d(TAG, "START - "+getName());
		Log.d(TAG, " --> Load web contents from : "+mUrlPath);

		// AndroidHttpClient is not allowed to be used from the main thread
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(mUrlPath);

		File localCacheFile = null;
		ILazyDownloadingCachePolicy cachePolicy = getCachePolicy();
		if(cachePolicy != null)
		{
			localCacheFile = cachePolicy.getOrComposeLocalCacheFile(mContext, mUrlPath);
		}
		boolean downloadComplete = false;

		try
		{
			if(localCacheFile == null)
			{
				Log.w(TAG, "ILazyDownloadingCachePolicy reference is aborted.");
				return;		// goto finally block.
			}

			if(localCacheFile.exists())
			{
				localCacheFile.delete();
				Log.d(TAG, "localCacheFile have been deleted.");
			}
			if(!localCacheFile.getParentFile().exists())
			{
				if(!localCacheFile.getParentFile().mkdirs())
				{
					throw new IOException("Making parent directory for cache files is failed.");
				}
				Log.d(TAG, "Parent directory for localCacheFiles have been created.");
			}

			final HttpResponse response = client.execute(request);
			final int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == HttpStatus.SC_OK)
			{
				final HttpEntity entity = response.getEntity();
				if (entity != null)
				{
					FlushedInputStream fis = null;
					FileOutputStream fos = null;

					try
					{
						fis = new FlushedInputStream(entity.getContent());
						fos = new FileOutputStream(localCacheFile);
						byte[] buffer = new byte[4096];
						int l;
						while ((l = fis.read(buffer)) != -1)
						{
							fos.write(buffer, 0, l);
						}

						fis.close();
						fos.flush();
						fos.close();

						downloadComplete = true;
						Log.d(TAG, "Downloaded contents -->"+mUrlPath);
					}
					catch (IOException e)
					{
						Log.w(TAG, "I/O error while retrieving file from " + mUrlPath, e);
					}
					catch (Throwable e)
					{
						Log.w(TAG, "Unexpected error while retrieving file from " + mUrlPath+":"+e.getMessage(), e);
					}
					finally
					{
						entity.consumeContent();
					}
				}
				else
				{
					Log.w(TAG, "HttpEntity is null.");
				}
			}
			else
			{
				Log.w(TAG, "HTTP_RESPONSE_ERROR: statusCode is " + statusCode + " while retrieving file from " + mUrlPath);
			}
		}
		catch (UnknownHostException e)
		{
			request.abort();
			Log.w(TAG, "Unknown/unreachable host or network error. "+e.getMessage() + " --> "+mUrlPath);
		}
		catch (IllegalStateException e)
		{
			request.abort();
			Log.w(TAG, "Incorrect URL: " + mUrlPath);
		}
		catch (Throwable e)
		{
			request.abort();
			Log.w(TAG, e.getMessage(), e);
		}
		finally
		{
			synchronized (this)
			{
				mStatus = DownloadStatus.STATUS_FINISHED;
			}

			if(downloadComplete)
			{
				Handler handler = getHandler();
				if (handler != null)
				{
					handler.sendEmptyMessage(LoadingImageStatus.LOAD_COMPLETE);
					Log.d(TAG, "Post load complete message. (from "+getName()+")");
				}
			}
			else
			{
				Log.d(TAG, "Can not post message. downloading thread("+getName()+") has error.");
			}
		}

		Log.d(TAG, "END");
	}

	/**
	 * Get this thread status({@link DownloadStatus}).
	 */
	public int getStatus()
	{
		synchronized (this)
		{
			return mStatus;
		}
	}

	/**
	 * Get handler instance.
	 */
	public Handler getHandler()
	{
		if(mHandler != null)
		{
			return mHandler.get();
		}
		return null;
	}

	/**
	 * Set handler instance.
	 */
	public void setHandler(Handler handler)
	{
		mHandler = new SoftReference<Handler>(handler);
	}

	/**
	 * Get cache policy instance.
	 */
	private ILazyDownloadingCachePolicy getCachePolicy()
	{
		if(mCachePolicy != null)
		{
			return mCachePolicy.get();
		}
		return null;
	}

	/**
	 * An InputStream that skips the exact number of bytes provided,
	 * unless it reaches EOF.
	 *
	 * @see http://android-developers.blogspot.com/2010/07/multithreading-for-performance.html
	 */
	static class FlushedInputStream extends FilterInputStream
	{
		public FlushedInputStream(InputStream inputStream)
		{
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException
		{
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n)
			{
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L)
				{
					int b = read();
					if (b < 0)
					{
						break; // we reached EOF
					}
					else
					{
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}
}
