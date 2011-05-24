
package jp.androdev.debkit.lazyimageloader;

import java.io.File;

import android.content.Context;

/**
 * Policy class for lazy downloading.
 */
public interface ILazyDownloadingCachePolicy
{
	/**
	 * Get the local cache file.
	 * @param context Context object.
	 * @param url downloading link.
	 * @return Local cache file.
	 */
	public File getOrComposeLocalCacheFile(final Context context, final String url);
}
