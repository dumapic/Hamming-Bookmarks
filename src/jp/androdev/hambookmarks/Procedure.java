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
	 * ネットワーク接続が有効か否かを調べる
	 *
	 * @return ネットワーク接続可否
	 */
	public static boolean isNetworkConnected(final Context context)
	{
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return isNetworkConnected(cm);
	}

	/**
	 *
	 * ネットワーク接続が有効か否かを調べる
	 *
	 * @param cm ConnectivityManagerオブジェクト
	 * @return ネットワーク接続可否
	 */
	public static boolean isNetworkConnected(final ConnectivityManager cm)
	{
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return (ni != null && ni.isConnected());
	}

	/**
	 *
	 * ネットワーク状態を監視するパーミッションが付与されているか否かをチェックする。
	 * 付与されていない場合、例外をスローする。
	 *
	 * @param context コンテキスト
	 * @return "android.permission.ACCESS_NETWORK_STATE"の付与有無
	 * @throws SecurityException パーミッションが付与されていない場合
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
	 * URLの形式妥当性を判定する。
	 *
	 * @param test 検査対象のURL
	 * @return URLが形式上妥当であればtrue.
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
	 * 制御文字列が含まれているか否かを検査する。
	 *
	 * @param test 検査対象
	 * @return 制御文字が含まれている場合にtrue
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
	 * Webページサムネイル保存先のFileオブジェクトを取得する。
	 * （必ずしも実体ファイルが存在するとは限らない）
	 *
	 * @param saveDir 保存先ディレクトリ
	 * @param url サムネイル取得先のWebページのURL
	 * @return Webページサムネイル保存先のFileオブジェクト
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
	 * ビットマップをbyte配列に変換する。
	 *
	 * @param bitmap ビットマップ
	 * @return 変換結果
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
	 * インテントを実行する
	 * @param activity アクティビティ
	 * @param intent インテント
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
	 * インテントを実行する
	 * @param activity アクティビティ
	 * @param intent インテント
	 * @param request リクエストコード
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
	 * コンテキストメニューのような選択肢のみ表示する選択ダイアログを表示する。
	 *
	 * @param activity アクティビティ
	 * @param adapter 選択肢のアダプタ
	 * @param listener 選択肢をタップした時のリスナ
	 * @return ダイアログ
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
	 * 単純なテキスト入力ダイアログを表示する。
	 *
	 * @param activity アクティビティ
	 * @param title ダイアログに表示するタイトル
	 * @param defaultInput 初期入力文字列
	 * @param listener OKボタン押下時のリスナ
	 * @return ダイアログ
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
	 * はい・いいえの選択ボタンを表示する確認メッセージダイアログ
	 *
	 * @param title ダイアログのタイトル
	 * @param message ダイアログに表示するメッセージ
	 * @param yesButtonCaption 「はい」ボタンの表示文字列
	 * @param noButtonCaption 「いいえ」ボタンの表示文字列
	 * @param activity アクティビティ
	 * @param onClickYes はい押下時のリスナ
	 * @param onClickNo いいえ押下時のリスナ
	 * @return ダイアログ
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
