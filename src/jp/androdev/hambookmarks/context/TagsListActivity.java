/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.context;

import java.lang.reflect.Field;
import java.util.ArrayList;

import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Constants.BundleKeys;
import jp.androdev.hambookmarks.Constants.HamDBKeys;
import jp.androdev.hambookmarks.Constants.MessageFlags;
import jp.androdev.hambookmarks.Constants.PrefKeys;
import jp.androdev.hambookmarks.IDialogOnClickListener;
import jp.androdev.hambookmarks.MyHamDatabases;
import jp.androdev.hambookmarks.Procedure;
import jp.androdev.hambookmarks.R;
import jp.androdev.hambookmarks.data.SimpleDialogItem;
import jp.androdev.hambookmarks.data.TagItem;
import jp.androdev.hambookmarks.data.TagItemAdapter;
import jp.androdev.hambookmarks.error.UnexpectedDatabaseErrorException;
import jp.androdev.hambookmarks.task.BaseSequentialTask;
import jp.androdev.hambookmarks.task.MiscInitialOperationTask;
import jp.androdev.hambookmarks.task.SyncChromeLiteBookmarksTask;
import jp.androdev.hambookmarks.task.TagAddTask;
import jp.androdev.hambookmarks.task.TagDeleteTask;
import jp.androdev.hambookmarks.task.TagLoadAllTask;
import jp.androdev.hambookmarks.task.TagRenameTask;
import jp.androdev.hambookmarks.task.TagSortReorderTask;

import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.crupp.hamsterdb.DatabaseException;

/**
 * Activity for list up tags.
 *
 * @version 1.0.0
 */
public final class TagsListActivity extends MyBaseActivity implements AdapterView.OnItemClickListener
{
	private TagItemAdapter mAdapter;
	private ListView mListView;
	private View mTopPanelView;
	private AlertDialog mChooserDialog;
	private ProgressDialog mProgressDialog;

	/*
	 * (�� Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "START");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// �^�C�g���ݒ�
		TextView txtActionTitle = (TextView) findViewById(R.id.txt_actionbar_title);
		txtActionTitle.setText(R.string.app_name_default);

		// �A�_�v�^�E���X�g�r���[�ݒ�
		mListView = (ListView) findViewById(R.id.list_main);
		mListView.setOnItemClickListener(this);
		registerForContextMenu(mListView);
		mAdapter = new TagItemAdapter(getBaseContext());
		mListView.setAdapter(mAdapter);

		// �߂�{�^���͔�\��
		ImageButton btnBack = (ImageButton) findViewById(R.id.btn_prev);
		btnBack.setVisibility(View.GONE);
		// ����{�^��
		ImageButton btnClose = (ImageButton) findViewById(R.id.btn_close);
		btnClose.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		//������Ԋm�F
		if(needsInitialSetup())
		{
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mTopPanelView = inflater.inflate(R.layout.parts_main_paneltop_syncstart, null);
			LinearLayout panelTop = (LinearLayout) findViewById(R.id.panel_top);
			panelTop.addView(mTopPanelView);
		}
		else
		{
			//�^�O���X�g�̃��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	}

	/*
	 * (�� Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause()
	{
		Log.d(TAG, "START");
		super.onPause();

		closeDialog();
	}

	/**
	 * ��������Ԃ��K�v���ۂ��𔻒肷��B
	 */
	private boolean needsInitialSetup()
	{
		Log.d(TAG, "START");

		MyHamDatabases db = getMyApplication().getDatabases();
		if(!db.tryOpenReadableDatabases())
		{
			throw new UnexpectedDatabaseErrorException("Can't open readable database.");
		}

		// Tags�̃��[�g���R�[�h��Value����A�S�^�O�擾�����݂�B
		// DB�����݂��Ă���΁A���̃��R�[�h�͕K������͂��B
		Object allTags = null;
		try
		{
			allTags = db.byTagsDB().find(new Object[]{ HamDBKeys.TAGS_ROOT });
			db.closeDatabases();
		}
		catch (DatabaseException e)
		{
			Log.e(TAG, e.getMessage(), e);
			//fall thru
		}

		//�^�O�̃��[�g���R�[�h��������Ώ�����ԂƂ݂Ȃ��B
		return (allTags == null);
	}

	/**
	 * �������b�Z�[�W�r���[���N���b�N�������ɃR�[������鏈���B
	 * @see res/layout/parts_main_paneltop_syncstart.xml (android:onClick attribute)
	 */
	public void onClickSyncStart(View v)
	{
		if(mTopPanelView != null)
		{
			LinearLayout panelTop = (LinearLayout) findViewById(R.id.panel_top);
			panelTop.removeView(mTopPanelView);
			mTopPanelView = null;
		}

		// �i���_�C�A���O��\��
		setupSyncProgressDialog();

		//�������������w��
		mHandler.sendEmptyMessage(MessageFlags.KIND_NONE|MessageFlags.OP_ONLINE_INIT|MessageFlags.STATE_START);
	}

	/**
	 *
	 * �w�����e�ɏ]���A�K�؂ȃX���b�h���N������n���h��
	 *
	 */
	Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			Log.d(TAG, "START");

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

			/*
			 * �����X���b�h�N��
			 */
			if(state == MessageFlags.STATE_START)
			{
				mAdapter.setLoadingState(true);

				switch(operation)
				{
					// �����������̊J�n
					case MessageFlags.OP_ONLINE_INIT:
						setupSyncProgressDialog();
						task = new MiscInitialOperationTask(getMyActivity(), this);
						runTask(task);
						break;

					// ���������J�n
					case MessageFlags.OP_ONLINE_SYNC:
						setupSyncProgressDialog();
						task = new SyncChromeLiteBookmarksTask(getMyActivity(), this);
						runTask(task);
						break;

					// �^�O���X�g���[�h�J�n
					case MessageFlags.OP_ONLINE_QUERY:
						task = new TagLoadAllTask(true, getMyActivity(), this);
						runTask(task);
						break;

					// �^�O�V�K�ǉ��̊J�n
					case MessageFlags.OP_ONLINE_CREATE:
						info = msg.getData();
						TagItem newTag = (TagItem) info.getSerializable(BundleKeys.NEW_TAG);
						task = new TagAddTask(newTag, getMyActivity(), this);
						runTask(task);
						break;

					// �^�O���O�ύX�̊J�n
					case MessageFlags.OP_ONLINE_UPDATE:
						info = msg.getData();
						TagItem updOldTag = (TagItem) info.getSerializable(BundleKeys.OLD_TAG);
						TagItem updNewTag = (TagItem) info.getSerializable(BundleKeys.NEW_TAG);
						task = new TagRenameTask(updOldTag, updNewTag, getMyActivity(), this);
						runTask(task);
						break;

					// �^�O�폜�̊J�n
					case MessageFlags.OP_ONLINE_DELETE:
						info = msg.getData();
						TagItem deleteTag = (TagItem) info.getSerializable(BundleKeys.DELETE_TAG);
						task = new TagDeleteTask(deleteTag, getMyActivity(), this);
						runTask(task);
						break;

					// ���ёւ��̊J�n
					case MessageFlags.OP_ONLINE_SORT:
						task = new TagSortReorderTask(getMyActivity(), this);
						runTask(task);
						break;
				}
			}
			/*
			 * ��������
			 */
			else if(state == MessageFlags.STATE_COMPLETE)
			{
				mAdapter.setLoadingState(false);

				switch(operation)
				{
					// �����������I��
					case MessageFlags.OP_ONLINE_INIT:
						post(mUIWorker_InitDone);
						break;

					// ������������
					case MessageFlags.OP_ONLINE_SYNC:
						post(mUIWorker_SyncDone);
						break;

					// �^�O���X�g���[�h����
					case MessageFlags.OP_ONLINE_QUERY:
						info = msg.getData();
						@SuppressWarnings("unchecked") ArrayList<TagItem> tags =
							(ArrayList<TagItem>) info.getSerializable(BundleKeys.ALL_TAG);
						runAsReloadListView(tags);
						break;

					// �^�O�V�K�ǉ��̊���
					case MessageFlags.OP_ONLINE_CREATE:
						post(mUIWorker_Added);
						break;

					// �^�O���O�ύX�̊���
					case MessageFlags.OP_ONLINE_UPDATE:
						post(mUIWorker_Updated);
						break;

					// �^�O�폜�̊���
					case MessageFlags.OP_ONLINE_DELETE:
						post(mUIWorker_Deleted);
						break;

					// ���ёւ��̊���
					case MessageFlags.OP_ONLINE_SORT:
						post(mUIWorker_Sorted);
						break;
				}
			}
			/*
			 * �G���[�I��
			 */
			else if(state == MessageFlags.STATE_FAILED)
			{
				mAdapter.setLoadingState(false);
				post(mUIWorker_UnexpectedError);
			}
			/*
			 * �������f
			 */
			else if(state == MessageFlags.STATE_CANCELED)
			{
				mAdapter.setLoadingState(false);
				post(mUIWorker_OperationCanceled);
			}
		}

		/**
		 * �^�X�N�����s����B
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
	 * �X���b�h���A���UI�X�V����
	 *
	 */
	/** ���X�g�ĕ`�� */
	private void runAsReloadListView(final ArrayList<TagItem> tags)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mAdapter.removeAll();
				mAdapter.addAll(tags);
				mAdapter.notifyDataSetChanged();
			}
		});
	}
	/** �������������� */
	Runnable mUIWorker_InitDone = new Runnable()
	{
		@Override
		public void run()
		{
			// �������w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_NONE | MessageFlags.OP_ONLINE_SYNC | MessageFlags.STATE_START);
		}
	};
	/** �������� */
	Runnable mUIWorker_SyncDone = new Runnable()
	{
		@Override
		public void run()
		{
			if(mProgressDialog != null && mProgressDialog.isShowing())
			{
				mProgressDialog.dismiss();
			}
			mProgressDialog = null;

			Toast.makeText(getMyActivity(), R.string.msg_normal_default_syncdone, Toast.LENGTH_SHORT).show();
			// �^�O���X�g�̃��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** �V�K�ǉ����� */
	Runnable mUIWorker_Added = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_add, Toast.LENGTH_SHORT).show();
			// �^�O���X�g�̃��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** �ύX���� */
	Runnable mUIWorker_Updated = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_updated, Toast.LENGTH_SHORT).show();
			// �^�O���X�g�̃��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** �폜���� */
	Runnable mUIWorker_Deleted = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_deleted, Toast.LENGTH_SHORT).show();
			// �^�O���X�g�̃��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** ���ёւ����� */
	Runnable mUIWorker_Sorted = new Runnable()
	{
		@Override
		public void run()
		{
			// �^�O���X�g�̃��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** �\�����Ȃ��G���[������ */
	Runnable mUIWorker_UnexpectedError = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_unexpectederror, Toast.LENGTH_LONG).show();
		}
	};
	/** �������f */
	Runnable mUIWorker_OperationCanceled = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_operation_canceled, Toast.LENGTH_LONG).show();
		}
	};

	/*
	 *
	 * ���X�g�A�C�e���I����
	 *
	 */

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		Log.d(TAG, "START");

		TagItem item = (TagItem) mAdapter.getItem(position);

		Intent intent = new Intent(getMyActivity(), BookmarkItemsListActivity.class);
		intent.putExtra(BundleKeys.SELECTED_TAG, item);
		Procedure.startActivityForResult(getMyActivity(), intent, 0);
	}

	/*
	 *
	 * ���X�g�A�C�e���̒��������j���[
	 *
	 */

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		Log.d(TAG, "START");

		if(mAdapter.nowLoading())
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_busy, Toast.LENGTH_LONG).show();
			return;
		}

		menu.add(0, R.string.caption_menu_tags_rename, 0, getString(R.string.caption_menu_tags_rename));
		menu.add(0, R.string.caption_menu_tags_delete, 0, getString(R.string.caption_menu_tags_delete));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		final AdapterContextMenuInfo menuinfo = (AdapterContextMenuInfo) item.getMenuInfo();
		final TagItem selectedTag = (TagItem) mAdapter.getItem(menuinfo.position);

		if(mAdapter.nowLoading())
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_busy, Toast.LENGTH_LONG).show();
			return true;
		}

		switch(item.getItemId())
		{
			//
			// �^�O�̖��O�ύX
			//
			case R.string.caption_menu_tags_rename:
				AlertDialog dialog = Procedure.createSimpleInputDialog(
					getMyActivity(),
					getString(R.string.caption_title_inputtagname),
					selectedTag.getTag(),
					new IDialogOnClickListener()
					{
						@Override
						public void onClick(IDialogOnClickListener.DialogEventArgs e)
						{
							String inputTagName = (String) e.what;
							if(StringUtils.isBlank(inputTagName))
							{
								Toast.makeText(getMyActivity(), R.string.msg_error_text_is_null_or_empty, Toast.LENGTH_SHORT).show();
								return;
							}
							TagItem newTag = new TagItem(inputTagName);
							Bundle updInfo = new Bundle();
							updInfo.putSerializable(BundleKeys.OLD_TAG, selectedTag);
							updInfo.putSerializable(BundleKeys.NEW_TAG, newTag);

							Message msg = Message.obtain();
							msg.what = MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_UPDATE|MessageFlags.STATE_START;
							msg.setData(updInfo);
							mHandler.sendMessage(msg);
						}
					});
				dialog.show();
				break;

			//
			// �^�O�̍폜.
			//
			case R.string.caption_menu_tags_delete:
				// �����ރ^�O�͏����Ȃ�
				if(StringUtils.equals(selectedTag.getTag(), HamDBKeys.NO_TAGGED))
				{
					Toast.makeText(getMyActivity(), R.string.msg_error_can_not_delete_tag, Toast.LENGTH_SHORT).show();
					break;
				}
				Bundle delInfo = new Bundle();
				delInfo.putSerializable(BundleKeys.DELETE_TAG, selectedTag);
				Message msg = Message.obtain();
				msg.what = MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_DELETE|MessageFlags.STATE_START;
				msg.setData(delInfo);
				mHandler.sendMessage(msg);
				break;

			default:
				throw new UnsupportedOperationException("Unknown context menu id. -->"+item.getItemId());
		}
		return true;
	}

	/*
	 *
	 * ���j���[�{�^�������ɂ�郁�j���[
	 *
	 */

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		Log.d(TAG, "START");

		super.onPrepareOptionsMenu(menu);
		menu.clear();
		getMenuInflater().inflate(R.menu.menu_tags, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Log.d(TAG, "START");

		super.onOptionsItemSelected(item);

		if(mAdapter.nowLoading())
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_busy, Toast.LENGTH_LONG).show();
			return true;
		}

		switch(item.getItemId())
		{
			//
			// �^�O�ǉ�
			//
			case R.id.menu_add:
				AlertDialog dialog = Procedure.createSimpleInputDialog(
					getMyActivity(),
					getString(R.string.caption_title_inputtagname),
					null,
					new IDialogOnClickListener()
					{
						@Override
						public void onClick(IDialogOnClickListener.DialogEventArgs e)
						{
							String inputTagName = (String) e.what;
							if(StringUtils.isBlank(inputTagName))
							{
								Toast.makeText(getMyActivity(), R.string.msg_error_text_is_null_or_empty, Toast.LENGTH_SHORT).show();
								return;
							}

							TagItem newTag = new TagItem(inputTagName);
							Bundle info = new Bundle();
							info.putSerializable(BundleKeys.NEW_TAG, newTag);

							Message msg = Message.obtain();
							msg.what = MessageFlags.KIND_TAG|MessageFlags.OP_ONLINE_CREATE|MessageFlags.STATE_START;
							msg.setData(info);
							mHandler.sendMessage(msg);
						}
					});
				dialog.show();
				break;

			//
			// �\�����̕ύX
			//
			case R.id.menu_sort:

				closeDialog();
				final ArrayAdapter<SimpleDialogItem> adapter =
					new ArrayAdapter<SimpleDialogItem>(
							getMyActivity(),
							R.layout.parts_simplechooserdialog_listitem,
							android.R.id.text1);
				adapter.add(new SimpleDialogItem(TagItemAdapter.SortOrder.BY_TITLE_ASC, getString(R.string.caption_sortorder_title_asc)));
				adapter.add(new SimpleDialogItem(TagItemAdapter.SortOrder.BY_TITLE_DESC, getString(R.string.caption_sortorder_title_desc)));

				mChooserDialog = Procedure.createSimpleChooserDialog(getMyActivity(), adapter,
				new IDialogOnClickListener()
				{
					@Override
					public void onClick(IDialogOnClickListener.DialogEventArgs e)
					{
						SimpleDialogItem selected = adapter.getItem(e.which);
						switch(selected.getId())
						{
							case TagItemAdapter.SortOrder.BY_TITLE_ASC:
							case TagItemAdapter.SortOrder.BY_TITLE_DESC:
								setPreferences(PrefKeys.SORTORDER_TAG, selected.getId());
								mHandler.sendEmptyMessage(
									MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_SORT | MessageFlags.STATE_START);
								break;

							default:
								throw new UnsupportedOperationException("Unknown sort order. -->"+selected.getId());
						}
					}
				});
				mChooserDialog.show();
				break;

			//
			// ����
			//
			case R.id.menu_sync:

				closeDialog();
				mChooserDialog = Procedure.createSimpleYesNoDialog(
					null,
					getString(R.string.msg_normal_sync_continue),
					getString(R.string.caption_button_continue),
					getString(R.string.caption_button_discard),
					getMyActivity(),
					new IDialogOnClickListener()
					{
						@Override
						public void onClick(DialogEventArgs e)
						{
							mHandler.sendEmptyMessage(MessageFlags.KIND_NONE|MessageFlags.OP_ONLINE_SYNC|MessageFlags.STATE_START);
						}
					},
					new IDialogOnClickListener()
					{
						@Override
						public void onClick(DialogEventArgs e){ }
					});
				mChooserDialog.show();
				break;

			//
			// ����`�̃��j���[
			//
			default:
				throw new UnsupportedOperationException("Unsupported menu. -->"+item.getItemId());
		}

		return true;
	}

	/*
	 *
	 * ���̑�
	 *
	 */

	/*
	 * (�� Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch(resultCode)
		{
			case Activity.RESULT_CANCELED:
				finish();
				break;
		}
	}

	/**
	 * �_�C�A���O���J���Ă��������B
	 */
	private void closeDialog()
	{
		if(mChooserDialog != null && mChooserDialog.isShowing())
		{
			mChooserDialog.dismiss();
		}
		mChooserDialog = null;

		if(mProgressDialog != null && mProgressDialog.isShowing())
		{
			mProgressDialog.dismiss();
		}
		mProgressDialog = null;
	}

	/**
	 * �����i���_�C�A���O���\�z����B
	 */
	private void setupSyncProgressDialog()
	{
		if(mProgressDialog == null)
		{
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setMessage(getString(R.string.msg_normal_sync_progressing));
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.show();	//���V�т���ɂ͂�����ɌĂԕK�v����

			// ���V��
			int resId_progress_number = 0;
			int resId_progress_percent = 0;
			try
			{
				Class<?> resourceIdClass = ProgressDialog.class.getClassLoader().loadClass("com.android.internal.R$id");
				Field R_id_progress_numberField = resourceIdClass.getDeclaredField("progress_number");
				Field R_id_progress_percentField = resourceIdClass.getDeclaredField("progress_percent");
				resId_progress_number = R_id_progress_numberField.getInt(null);
				resId_progress_percent = R_id_progress_percentField.getInt(null);

				View progressNumberView = (View) mProgressDialog.findViewById(resId_progress_number);
				View progressPercentView = (View) mProgressDialog.findViewById(resId_progress_percent);
				progressNumberView.setVisibility(View.INVISIBLE);
				progressPercentView.setVisibility(View.INVISIBLE);
			}
			catch (Throwable e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}
}
