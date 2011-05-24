/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.context;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Base Activity class.
 *
 * @version 1.0.0
 */
public abstract class MyBaseActivity extends Activity
{
	protected static final String TAG = "HamBookmarks";

	/**
	 * プリファレンスの値を取得する。
	 */
	public SharedPreferences getPreferences()
	{
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	/**
	 * プリファレンスに値を保存する。
	 */
	public void setPreferences(String key, Object value)
	{
		SharedPreferences.Editor edit = getPreferences().edit();
		if(value instanceof String)
		{
			edit.putString(key, (String)value);
		}
		else if(value instanceof Integer)
		{
			edit.putInt(key, (Integer)value);
		}
		else if(value instanceof Boolean)
		{
			edit.putBoolean(key, (Boolean)value);
		}
		else if(value instanceof Long)
		{
			edit.putLong(key, (Long)value);
		}
		else if(value instanceof Float)
		{
			edit.putFloat(key, (Float)value);
		}
		else
		{
			throw new IllegalArgumentException("Illegal data type.");
		}
		edit.commit();
	}

	/**
	 * アクティビティのインスタンスを取得する。
	 */
	public MyBaseActivity getMyActivity()
	{
		return this;
	}

	/**
	 * アプリケーションのインスタンスを取得する。
	 */
	public MyApplication getMyApplication()
	{
		return (MyApplication) getApplication();
	}
}
