/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.context;

import java.util.ArrayList;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Constants.BundleKeys;
import jp.androdev.hambookmarks.Constants.MessageFlags;
import jp.androdev.hambookmarks.Procedure;
import jp.androdev.hambookmarks.R;
import jp.androdev.hambookmarks.data.BookmarkItem;
import jp.androdev.hambookmarks.data.TagItem;
import jp.androdev.hambookmarks.task.BaseSequentialTask;
import jp.androdev.hambookmarks.task.BookmarkItemAddTask;
import jp.androdev.hambookmarks.task.TagLoadAllTask;

import org.apache.commons.lang.StringUtils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


/**
 *
 * Add bookmark page activity.
 *
 */
public final class AddBookmarkActivity extends MyBaseActivity
{
	private EditText mEditTitle;
	private EditText mEditURL;
	private Spinner mSpinnerSelectTag;
	private Button mBtnSave;
	private Button mBtnCancel;
	private ArrayAdapter<TagItem> mSpinnerAdapter;

	private TagItem mFromTag;

	/*
	 * (非 Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "START");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.additemdialog_main);

		// タイトル設定
		LinearLayout actionBar = (LinearLayout) findViewById(R.id.panel_actionbar_controls);
		TextView actionBarTitle = (TextView) findViewById(R.id.txt_actionbar_title);
		actionBarTitle.setText(getString(R.string.caption_title_addbookmark));
		actionBar.setVisibility(View.GONE);

		mEditTitle = (EditText) findViewById(R.id.txt_title);
		mEditURL = (EditText) findViewById(R.id.txt_url);
		mSpinnerSelectTag = (Spinner) findViewById(R.id.spn_choosetag);
		mBtnSave = (Button) findViewById(R.id.btn_ok);
		mBtnCancel = (Button) findViewById(R.id.btn_cancel);

		mFromTag = null;

		if(getIntent() != null)
		{
			String action = getIntent().getAction();
			if(StringUtils.equals(Intent.ACTION_VIEW, action))
			{
				String link = getIntent().getDataString();
				mEditURL.setText(link);
			}
			else if(StringUtils.equals(Intent.ACTION_SEND, action))
			{
				Bundle info = getIntent().getExtras();
				if(info.containsKey(Intent.EXTRA_TITLE))
				{
					mEditTitle.setText(info.getString(Intent.EXTRA_TITLE));
				}
				if(info.containsKey(Intent.EXTRA_TEXT))
				{
					mEditURL.setText(info.getString(Intent.EXTRA_TEXT));
				}
			}
			else if(StringUtils.isBlank(action))
			{
				mFromTag = (TagItem) getIntent().getSerializableExtra(BundleKeys.SELECTED_TAG);
			}
		}

		// タグ選択スピナ
		mSpinnerAdapter = new ArrayAdapter<TagItem>(this, android.R.layout.simple_spinner_item);
		mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerSelectTag.setAdapter(mSpinnerAdapter);
		// タグ読み込み指示
		mHandler.sendEmptyMessage(MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);

		// 保存ボタン
		mBtnSave.setEnabled(false);
		mBtnSave.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_CREATE|MessageFlags.STATE_START);
			}
		});

		// キャンセルボタン
		mBtnCancel.setEnabled(false);
		mBtnCancel.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});
	}

	/*
	 * (非 Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause()
	{
		Log.d(TAG, "START");
		super.onPause();

		finish();
	}

	/**
	 *
	 * 指示内容に従い、適切なスレッドを起動するハンドラ
	 *
	 */
	Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			int kind = MessageFlags.getKind(msg.what);
			int operation = MessageFlags.getOperation(msg.what);
			int state = MessageFlags.getState(msg.what);

			if(Log.isLoggable(Log.Level.DEBUG))
			{
				Log.d(TAG, "kind - "	+MessageFlags.getFlagName(kind));
				Log.d(TAG, "operation - "+MessageFlags.getFlagName(operation));
				Log.d(TAG, "state - "	+MessageFlags.getFlagName(state));
			}

			BaseSequentialTask task;
			Bundle info;
			TagItem selectedTag;
			BookmarkItem item;

			if(kind == MessageFlags.KIND_TAG)
			{
				switch(operation | state)
				{
					case MessageFlags.OP_ONLINE_QUERY | MessageFlags.STATE_START:
						task = new TagLoadAllTask(false, getMyActivity(), this);
						runTask(task);
						break;

					case MessageFlags.OP_ONLINE_QUERY | MessageFlags.STATE_COMPLETE:
						info = msg.getData();
						@SuppressWarnings("unchecked") ArrayList<TagItem> tags =
							(ArrayList<TagItem>) info.getSerializable(BundleKeys.ALL_TAG);
						runAsReloadSpinner(tags);
						break;

					default:
						if(state == MessageFlags.STATE_FAILED)
						{
							post(mUIWorker_UnexpectedError);
						}
						else if(state == MessageFlags.STATE_CANCELED)
						{
							post(mUIWorker_OperationCanceled);
						}
				}
			}
			else if(kind == MessageFlags.KIND_BOOKMARKITEM)
			{
				switch(operation | state)
				{
					case MessageFlags.OP_ONLINE_CREATE | MessageFlags.STATE_START:
						int invalid = 0;
						String title = mEditTitle.getText().toString();
						String url = mEditURL.getText().toString();
						selectedTag = (TagItem) mSpinnerSelectTag.getSelectedItem();

						if(StringUtils.isBlank(title))
						{
							invalid = R.string.msg_error_text_is_null_or_empty;
						}
						else if(Procedure.containsControlChars(title))
						{
							invalid = R.string.msg_error_text_is_invalid;
						}
						else if(StringUtils.isBlank(url))
						{
							invalid = R.string.msg_error_text_is_null_or_empty;
						}
						else if(!Procedure.isValidUrl(url))
						{
							invalid = R.string.msg_error_text_is_invalid;
						}

						if(invalid != 0)
						{
							runAsInputError(invalid);
							return;
						}

						item = new BookmarkItem();
						item.setTitle(title);
						item.setUrl(url);
						item.setCreated(System.currentTimeMillis(), getString(R.string.caption_label_created));
						item.setLastAccessed(System.currentTimeMillis(), getString(R.string.caption_label_lastaccessed));
						Bitmap ic = BitmapFactory.decodeResource(getResources(), R.drawable.app_web_browser_sm);
						item.setFavicon(ic);
						ic.recycle();

						task = new BookmarkItemAddTask(selectedTag, item, getMyActivity(), this);
						runTask(task);
						break;

					case MessageFlags.OP_ONLINE_CREATE | MessageFlags.STATE_COMPLETE:
						post(mUIWorker_Added);
						break;

					default:
						if(state == MessageFlags.STATE_FAILED)
						{
							post(mUIWorker_UnexpectedError);
						}
						else if(state == MessageFlags.STATE_CANCELED)
						{
							post(mUIWorker_OperationCanceled);
						}
				}
			}
		}

		/**
		 * タスクを実行する。
		 */
		private void runTask(BaseSequentialTask task)
		{
			Thread t = new Thread(task);
			t.setPriority(Thread.NORM_PRIORITY - 1);
			t.start();
		}
	};

	/*
	 *
	 * スレッド復帰後のUI更新処理
	 *
	 */

	/** スピナー準備完了 */
	private void runAsReloadSpinner(final ArrayList<TagItem> tags)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mSpinnerAdapter.clear();

				if(tags.size() == 0)
					return;

				int len = tags.size();
				TagItem tag;
				for(int i = 0; i < len; ++i)
				{
					tag = tags.get(i);
					mSpinnerAdapter.add(tag);
					if(mFromTag != null && tag.equals(mFromTag))
					{
						mSpinnerSelectTag.setSelection(i);
					}
				}
				mSpinnerAdapter.notifyDataSetChanged();

				mBtnSave.setEnabled(true);
				mBtnCancel.setEnabled(true);
			}
		});
	}
	/** 入力エラー */
	private void runAsInputError(final int msg)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(getMyActivity(), msg, Toast.LENGTH_SHORT).show();
			}
		});
	}
	/** 新規追加完了 */
	Runnable mUIWorker_Added = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_add, Toast.LENGTH_SHORT).show();
			getMyActivity().finish();
		}
	};
	/** 予期しないエラーが発生 */
	Runnable mUIWorker_UnexpectedError = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_unexpectederror, Toast.LENGTH_LONG).show();
		}
	};
	/** 処理中断 */
	Runnable mUIWorker_OperationCanceled = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_operation_canceled, Toast.LENGTH_LONG).show();
		}
	};
}
