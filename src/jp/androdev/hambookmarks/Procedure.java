/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * Sample Demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Constructor;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Constants.Path;
import jp.androdev.hambookmarks.data.SimpleDialogItem;

import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Common methods for this application.
 *
 * @version 1.0.0
 */
public final class Procedure
{
	private static final String TAG = "HamBookmarks";

	/**
	 *
	 * �l�b�g���[�N�ڑ����L�����ۂ��𒲂ׂ�
	 *
	 * @return �l�b�g���[�N�ڑ���
	 */
	public static boolean isNetworkConnected(final Context context)
	{
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return isNetworkConnected(cm);
	}

	/**
	 *
	 * �l�b�g���[�N�ڑ����L�����ۂ��𒲂ׂ�
	 *
	 * @param cm ConnectivityManager�I�u�W�F�N�g
	 * @return �l�b�g���[�N�ڑ���
	 */
	public static boolean isNetworkConnected(final ConnectivityManager cm)
	{
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return (ni != null && ni.isConnected());
	}

	/**
	 *
	 * �l�b�g���[�N��Ԃ��Ď�����p�[�~�b�V�������t�^����Ă��邩�ۂ����`�F�b�N����B
	 * �t�^����Ă��Ȃ��ꍇ�A��O���X���[����B
	 *
	 * @param context �R���e�L�X�g
	 * @return "android.permission.ACCESS_NETWORK_STATE"�̕t�^�L��
	 * @throws SecurityException �p�[�~�b�V�������t�^����Ă��Ȃ��ꍇ
	 */
	public static boolean checkAccessNetworkStatePermission(final Context context)
	{
		int g = context.checkPermission(
				android.Manifest.permission.ACCESS_NETWORK_STATE,
				android.os.Process.myPid(),
				android.os.Process.myUid());
		if(g != PackageManager.PERMISSION_GRANTED)
		{
			throw new SecurityException("No permission. required 'android.permission.ACCESS_NETWORK_STATE' permission.");
		}

		return true;
	}

	/**
	 * URL�̌`���Ó����𔻒肷��B
	 *
	 * @param test �����Ώۂ�URL
	 * @return URL���`����Ó��ł����true.
	 * @see android.net.WebAddress (hide class)
	 */
	public static boolean isValidUrl(final String test)
	{
		boolean result = false;
		try
		{
			if(URLUtil.isValidUrl(test))
			{
				if(!containsControlChars(test))
				{
					Class<?> classWebAddress = Context.class.getClassLoader().loadClass("android.net.WebAddress");
					Constructor<?> ctor = classWebAddress.getConstructor(new Class[]{ String.class });
					Object webAddress = ctor.newInstance(new Object[]{ test });
					if(webAddress.toString() != null)
					{
						result = true;
					}
				}
			}
		}
		catch (Throwable e)
		{
			Log.e(TAG, e.getMessage(), e);
			result = false;
		}
		return result;
	}

	/**
	 *
	 * ���䕶���񂪊܂܂�Ă��邩�ۂ�����������B
	 *
	 * @param test �����Ώ�
	 * @return ���䕶�����܂܂�Ă���ꍇ��true
	 */
	public static boolean containsControlChars(final String test)
	{
		if(StringUtils.isBlank(test))
		{
			return false;
		}

		final int inputLength = test.length();

		for(int i = 0; i < inputLength; ++i)
		{
			char c = test.charAt(i);
			switch(c)
			{
				case '\r': return true;
				case '\n': return true;
				case '\t': return true;
				default:   break;
			}
		}

		return false;
	}

	/**
	 * Web�y�[�W�T���l�C���ۑ����File�I�u�W�F�N�g���擾����B
	 * �i�K���������̃t�@�C�������݂���Ƃ͌���Ȃ��j
	 *
	 * @param saveDir �ۑ���f�B���N�g��
	 * @param url �T���l�C���擾���Web�y�[�W��URL
	 * @return Web�y�[�W�T���l�C���ۑ����File�I�u�W�F�N�g
	 */
	public static File getThumbnailLocalCacheFile(String saveDir, String url)
	{
		StringBuilder fileName = new StringBuilder();

		byte[] bytes = url.getBytes();
		int bt = 0;
		for(byte b : bytes)
		{
			bt = b & 0xFF;
			if(bt < 0x10)
			{
				fileName.append("0");
			}
			fileName.append(Integer.toHexString(bt));
		}
		fileName.append(Path.WEBPAGE_CAPTURE_THUMBNAILFILE_EXT);

		File result = new File(new StringBuilder()
			.append(saveDir)
			.append(File.separator)
			.append(fileName.toString())
			.toString());

		return result;
	}

	/**
	 * �r�b�g�}�b�v��byte�z��ɕϊ�����B
	 *
	 * @param bitmap �r�b�g�}�b�v
	 * @return �ϊ�����
	 * @see com.android.browser.Bookmarks#bitmapToBytes(Bitmap)
	 */
	public static byte[] bitmapToBytes(Bitmap bitmap)
	{
		if (bitmap == null)
		{
		    return null;
		}

		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
		return os.toByteArray();
    }

	/**
	 * �C���e���g�����s����
	 * @param activity �A�N�e�B�r�e�B
	 * @param intent �C���e���g
	 */
	public static void startActivity(final Activity activity, final Intent intent)
	{
		try
		{
			activity.startActivity(intent);
		}
		catch(android.content.ActivityNotFoundException e)
		{
			Log.w(TAG, e.getMessage(), e);
			Toast.makeText(activity, R.string.msg_error_activity_not_found, Toast.LENGTH_LONG).show();
		}
		catch(Throwable e)
		{
			Log.e(TAG, e.getMessage(), e);
			Toast.makeText(activity, R.string.msg_error_unexpectederror, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * �C���e���g�����s����
	 * @param activity �A�N�e�B�r�e�B
	 * @param intent �C���e���g
	 * @param request ���N�G�X�g�R�[�h
	 */
	public static void startActivityForResult(final Activity activity, final Intent intent, final int request)
	{
		try
		{
			activity.startActivityForResult(intent, request);
		}
		catch(android.content.ActivityNotFoundException e)
		{
			Log.w(TAG, e.getMessage(), e);
			Toast.makeText(activity, R.string.msg_error_activity_not_found, Toast.LENGTH_LONG).show();
		}
		catch(Throwable e)
		{
			Log.e(TAG, e.getMessage(), e);
			Toast.makeText(activity, R.string.msg_error_unexpectederror, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * �R���e�L�X�g���j���[�̂悤�ȑI�����̂ݕ\������I���_�C�A���O��\������B
	 *
	 * @param activity �A�N�e�B�r�e�B
	 * @param adapter �I�����̃A�_�v�^
	 * @param listener �I�������^�b�v�������̃��X�i
	 * @return �_�C�A���O
	 */
	public static AlertDialog createSimpleChooserDialog(
		final Activity activity,
		final ArrayAdapter<? extends SimpleDialogItem> adapter,
		final IDialogOnClickListener onClickOk)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
		dialog.setTitle(null);
		dialog.setCancelable(true);
		dialog.setInverseBackgroundForced(false);
		dialog.setAdapter(adapter, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				IDialogOnClickListener.DialogEventArgs e =
					new IDialogOnClickListener.DialogEventArgs(dialog, which, null);
				onClickOk.onClick(e);
			}
		});
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				dialog.dismiss();
			}
		});

		return dialog.create();
	}

	/**
	 * �P���ȃe�L�X�g���̓_�C�A���O��\������B
	 *
	 * @param activity �A�N�e�B�r�e�B
	 * @param title �_�C�A���O�ɕ\������^�C�g��
	 * @param defaultInput �������͕�����
	 * @param listener OK�{�^���������̃��X�i
	 * @return �_�C�A���O
	 */
	public static AlertDialog createSimpleInputDialog(
		final Activity activity,
		final String title,
		final String defaultInput,
		final IDialogOnClickListener onClickOk)
	{
		final EditText editInput = new EditText(activity);
		if(!StringUtils.isBlank(defaultInput))
		{
			editInput.setText(defaultInput);
		}

        final AlertDialog dialog = new AlertDialog.Builder(activity)
        	.setTitle(title)
        	.setView(editInput)
        	.setPositiveButton(activity.getString(R.string.caption_button_ok), new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					IDialogOnClickListener.DialogEventArgs e =
						new IDialogOnClickListener.DialogEventArgs(dialog, which, editInput.getText().toString());
					onClickOk.onClick(e);
				}
			})
        	.setNegativeButton(activity.getString(R.string.caption_button_cancel), new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			})
			.create();

		editInput.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				if(hasFocus)
				{
					dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});

		return dialog;
	}

	/**
	 * �͂��E�������̑I���{�^����\������m�F���b�Z�[�W�_�C�A���O
	 *
	 * @param title �_�C�A���O�̃^�C�g��
	 * @param message �_�C�A���O�ɕ\�����郁�b�Z�[�W
	 * @param yesButtonCaption �u�͂��v�{�^���̕\��������
	 * @param noButtonCaption �u�������v�{�^���̕\��������
	 * @param activity �A�N�e�B�r�e�B
	 * @param onClickYes �͂��������̃��X�i
	 * @param onClickNo �������������̃��X�i
	 * @return �_�C�A���O
	 */
	public static AlertDialog createSimpleYesNoDialog(
		final String title,
		final String message,
		final String yesButtonCaption,
		final String noButtonCaption,
		final Activity activity,
		final IDialogOnClickListener onClickYes,
		final IDialogOnClickListener onClickNo)
	{
        final AlertDialog dialog = new AlertDialog.Builder(activity)
        	.setTitle(title)
        	.setMessage(message)
        	.setPositiveButton(yesButtonCaption, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					IDialogOnClickListener.DialogEventArgs e =
						new IDialogOnClickListener.DialogEventArgs(dialog, which, null);
					onClickYes.onClick(e);
				}
			})
        	.setNegativeButton(noButtonCaption, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					IDialogOnClickListener.DialogEventArgs e =
						new IDialogOnClickListener.DialogEventArgs(dialog, which, null);
					onClickNo.onClick(e);
				}
			})
			.create();

		return dialog;
	}
}
