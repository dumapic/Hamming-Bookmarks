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
	 * �v���t�@�����X�̒l���擾����B
	 */
	public SharedPreferences getPreferences()
	{
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	/**
	 * �v���t�@�����X�ɒl��ۑ�����B
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
	 * �A�N�e�B�r�e�B�̃C���X�^���X���擾����B
	 */
	public MyBaseActivity getMyActivity()
	{
		return this;
	}

	/**
	 * �A�v���P�[�V�����̃C���X�^���X���擾����B
	 */
	public MyApplication getMyApplication()
	{
		return (MyApplication) getApplication();
	}
}
