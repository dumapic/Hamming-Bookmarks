
package jp.androdev.debkit.lazyimageloader;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Handler;
import android.os.Message;

/**
 * Queue class for lazy downloading.
 *
 * @see http://evancharlton.com/thoughts/lazy-loading-images-in-a-listview/
 */
public class LazyDownloadingQueue
{
	public static final int PRIORITY_LOW = 0;
	public static final int PRIORITY_HIGH = 1;

	private static LazyDownloadingQueue Singleton = new LazyDownloadingQueue();

	private final ArrayList<LazyDownloadingThread> mQueue;
	private final HashMap<Long, Boolean> mThreads;
	private final Object mLock;

	private Handler mQueuedHandler = null;

	/** Hide constructor */
	private LazyDownloadingQueue()
	{
		mQueue = new ArrayList<LazyDownloadingThread>();
		mThreads = new HashMap<Long, Boolean>();
		mLock = new Object();

		mQueuedHandler = null;
	}

	/**
	 * Get LazyDownloadingQueue instance.
	 */
	public static LazyDownloadingQueue getInstance()
	{
		return Singleton;
	}

	/**
	 * Enqueue the downloading task.
	 * @param task downloading thread.
	 */
	public void enqueue(LazyDownloadingThread task)
	{
		enqueue(task, PRIORITY_LOW);
	}

	/**
	 * Enqueue the downloading task with priority.
	 * @param task downloading thread.
	 * @param priority {@link LazyDownloadingQueue#PRIORITY_HIGH} or {@link LazyDownloadingQueue#PRIORITY_LOW}.
	 */
	public void enqueue(final LazyDownloadingThread task, final int priority)
	{
		synchronized(mLock)
		{
			Boolean exists = mThreads.get(task.getId());
			if (exists == null)
			{
				if (mQueue.size() == 0 || priority == PRIORITY_LOW)
				{
					mQueue.add(task);
				}
				else
				{
					mQueue.add(1, task);
				}
				mThreads.put(task.getId(), true);
			}
			runFirst();
		}
	}

	/**
	 * Dequeue the downloading task.
	 * @param task downloading thread.
	 */
	public void dequeue(final LazyDownloadingThread task)
	{
		synchronized(mLock)
		{
			mThreads.remove(task.getId());
			mQueue.remove(task);
		}
	}

	/**
	 * finishing method.
	 */
	private void finished(int result)
	{
		synchronized(mLock)
		{
			if(mQueuedHandler != null)
			{
				mQueuedHandler.sendEmptyMessage(result);
			}
			runFirst();
		}
	}

	/**
	 * Run downloading thread.
	 */
	private void runFirst()
	{
		synchronized(mLock)
		{
			if (mQueue.size() > 0)
			{
				LazyDownloadingThread task = mQueue.get(0);

				if (task.getStatus() == LazyDownloadingThread.DownloadStatus.STATUS_PENDING)
				{
					mQueuedHandler = task.getHandler();
					task.setHandler(mHandler);
					task.start();
				}
				else if (task.getStatus() == LazyDownloadingThread.DownloadStatus.STATUS_FINISHED)
				{
					LazyDownloadingThread thread = mQueue.remove(0);
					mThreads.remove(thread.getId());
					runFirst();
				}
			}
		}
	}


	Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message message)
		{
			finished(message.what);
		}
	};
}
