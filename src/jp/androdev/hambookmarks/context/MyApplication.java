/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.context;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Constants.PrefKeys;
import jp.androdev.hambookmarks.MyHamDatabases;
import jp.androdev.hambookmarks.data.BookmarkItemAdapter;
import jp.androdev.hambookmarks.data.TagItemAdapter;
import jp.androdev.hambookmarks.error.UnexpectedDatabaseErrorException;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

/**
 * A Hamming Bookmarks application context.
 *
 * @version 1.0.0
 */
public final class MyApplication extends Application
{
	private static final String TAG = "HamBookmarks";

	private MyHamDatabases mHamDb;

	/*
	 * (非 Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate()
	{
		super.onCreate();

		setupLogger();
		Log.d(TAG, "START");

		int g;
		g = getBaseContext().checkPermission(
			android.Manifest.permission.INTERNET,
			android.os.Process.myPid(),
			android.os.Process.myUid());
		if(g != PackageManager.PERMISSION_GRANTED)
		{
			throw new SecurityException("No permission. required 'android.permission.INTERNET' permission.");
		}
		g = getBaseContext().checkPermission(
			android.Manifest.permission.ACCESS_NETWORK_STATE,
			android.os.Process.myPid(),
			android.os.Process.myUid());
		if(g != PackageManager.PERMISSION_GRANTED)
		{
			throw new SecurityException("No permission. required 'android.permission.ACCESS_NETWORK_STATE' permission.");
		}

		setupPreferences();

		mHamDb = new MyHamDatabases(getBaseContext());
		if(!mHamDb.tryCreateDatabases())
		{
			throw new UnexpectedDatabaseErrorException("Can't create hamsterdb databases.");
		}
	}

	/*
	 * (非 Javadoc)
	 * @see android.app.Application#onTerminate()
	 */
	@Override
	public void onTerminate()
	{
		Log.d(TAG, "START");

		if(mHamDb != null)
			mHamDb.closeDatabases();

		super.onTerminate();
	}

	/**
	 * hamsterdbのデータベースを取得する。
	 */
	public MyHamDatabases getDatabases()
	{
		return mHamDb;
	}

	/**
	 * プリファレンスのセットアップを行う。
	 */
	private void setupPreferences()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor edit = null;

		if(!pref.contains(PrefKeys.SORTORDER_TAG))
		{
			edit = pref.edit();
			edit.putInt(PrefKeys.SORTORDER_TAG, TagItemAdapter.SortOrder.BY_TITLE_ASC);
			edit.commit();
		}
		if(!pref.contains(PrefKeys.SORTORDER_BOOKMARKITEM))
		{
			edit = pref.edit();
			edit.putInt(PrefKeys.SORTORDER_BOOKMARKITEM, BookmarkItemAdapter.SortOrder.BY_TITLE_ASC);
			edit.commit();
		}
	}

	/**
	 * ロガーのセットアップを行う。
	 * AndroidManifest.xmlのLogLevelを元に設定する。
	 */
	private void setupLogger()
	{
		ApplicationInfo myApp = null;
		try
		{
			myApp = getBaseContext().getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
		}
		catch (Throwable e)
		{
			System.err.println(e.getMessage());
			System.err.println(android.util.Log.getStackTraceString(e));
			Log.setLogLevel(Log.Level.DISABLED);
			return;
		}

		Log.setLogLevel(myApp);
	}
}
