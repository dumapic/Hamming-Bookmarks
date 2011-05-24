/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.context;

import java.util.ArrayList;

import jp.androdev.debkit.lazyimageloader.LazyLoadImageView;
import jp.androdev.debkit.util.Log;
import jp.androdev.hambookmarks.Constants.BundleKeys;
import jp.androdev.hambookmarks.Constants.MessageFlags;
import jp.androdev.hambookmarks.Constants.Path;
import jp.androdev.hambookmarks.Constants.PrefKeys;
import jp.androdev.hambookmarks.IDialogOnClickListener;
import jp.androdev.hambookmarks.Procedure;
import jp.androdev.hambookmarks.R;
import jp.androdev.hambookmarks.data.BookmarkItem;
import jp.androdev.hambookmarks.data.BookmarkItemAdapter;
import jp.androdev.hambookmarks.data.SimpleDialogItem;
import jp.androdev.hambookmarks.data.TagItem;
import jp.androdev.hambookmarks.dialog.ChooserDialog;
import jp.androdev.hambookmarks.dialog.ChooserDialogItem;
import jp.androdev.hambookmarks.task.BaseSequentialTask;
import jp.androdev.hambookmarks.task.BookmarkItemAddTask;
import jp.androdev.hambookmarks.task.BookmarkItemDeleteTask;
import jp.androdev.hambookmarks.task.BookmarkItemLoadByTagTask;
import jp.androdev.hambookmarks.task.BookmarkItemMoveTask;
import jp.androdev.hambookmarks.task.BookmarkItemSortReorderTask;
import jp.androdev.hambookmarks.task.BookmarkItemUpdateTask;
import jp.androdev.hambookmarks.task.TagLoadAllTask;

import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
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

/**
 * Activity for list up the bookmarks.
 *
 * @version 1.0.0
 */
public class BookmarkItemsListActivity extends MyBaseActivity implements AdapterView.OnItemClickListener
{
	private Intent mReceivedIntent;
	private TagItem mParentTag;

	private BookmarkItemAdapter mAdapter;
	private ListView mListView;
	private View mTopPanelView;
	private AlertDialog mChooserDialog;

	private BookmarkItem mMoveItem;

	/*
	 * (�� Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent)
	{
		Log.d(TAG, "START");
		super.onNewIntent(intent);

		mReceivedIntent = intent;
	}

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

		setResult(Activity.RESULT_OK);

		// �e�^�O���擾�E�ޔ�
		if(mReceivedIntent == null)
			mReceivedIntent = getIntent();
		mParentTag = (TagItem) mReceivedIntent.getSerializableExtra(BundleKeys.SELECTED_TAG);

		// �^�C�g���ݒ�
		TextView txtActionTitle = (TextView) findViewById(R.id.txt_actionbar_title);
		txtActionTitle.setText("/"+mParentTag.getTagCaption());

		// �A�_�v�^�E���X�g�r���[�ݒ�
		mListView = (ListView) findViewById(R.id.list_main);
		mListView.setOnItemClickListener(this);
		registerForContextMenu(mListView);
		mAdapter = new BookmarkItemAdapter(getBaseContext());
		mListView.setAdapter(mAdapter);

		// �߂�{�^��
		ImageButton btnBack = (ImageButton) findViewById(R.id.btn_prev);
		btnBack.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//���̉�ʂ����N���[�Y
				finish();
			}
		});
		// ����{�^��
		ImageButton btnClose = (ImageButton) findViewById(R.id.btn_close);
		btnClose.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// �ďo������������ŃN���[�Y
				// @see jp.androdev.hambookmarks.context.TagsListActivity#onActivityResult(int, int, Intent)
				setResult(Activity.RESULT_CANCELED);
				finish();
			}
		});

		// �^�O�ɕR�Â��u�b�N�}�[�N���X�g�̃��[�h���w��
		mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM |MessageFlags.OP_ONLINE_QUERY |MessageFlags.STATE_START);
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
	 * �w�����e�ɏ]���A�K�؂ȃX���b�h���N������n���h��
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

			switch(kind)
			{
				case MessageFlags.KIND_BOOKMARKITEM:
					handleBookmarkItemsMessage(msg, operation, state);
					break;

				case MessageFlags.KIND_TAG:
					handleBookmarkTagsMessage(msg, operation, state);
					break;

				default:
					throw new UnsupportedOperationException("Unknown message(kind).");
			}
		}

		/**
		 * �u�b�N�}�[�N�A�C�e���Ɋւ��鏈���̃n���h�����O
		 */
		private void handleBookmarkItemsMessage(Message msg, int operation, int state)
		{
			Bundle info;
			BaseSequentialTask task;
			BookmarkItem oldItem;
			BookmarkItem newItem;

			/*
			 * �����X���b�h�N��
			 */
			if(state == MessageFlags.STATE_START)
			{
				mAdapter.setLoadingState(true);

				switch(operation)
				{
					// �u�b�N�}�[�N���X�g�̃��[�h�J�n
					case MessageFlags.OP_ONLINE_QUERY:
						task = new BookmarkItemLoadByTagTask(mParentTag, getMyActivity(), this);
						runTask(task);
						break;

					// �u�b�N�}�[�N�̐V�K�ǉ��J�n
					case MessageFlags.OP_ONLINE_CREATE:
						info = msg.getData();
						newItem = (BookmarkItem) info.getSerializable(BundleKeys.NEW_BOOKMARK);
						TagItem tag = (TagItem) info.getSerializable(BundleKeys.SELECTED_TAG);
						task = new BookmarkItemAddTask(tag, newItem, getMyActivity(), this);
						runTask(task);
						break;

					// �I���u�b�N�}�[�N�̍X�V�J�n�i�I�����C���X�V�j
					case MessageFlags.OP_ONLINE_UPDATE:
						info = msg.getData();
						oldItem = (BookmarkItem) info.getSerializable(BundleKeys.OLD_BOOKMARK);
						newItem = (BookmarkItem) info.getSerializable(BundleKeys.NEW_BOOKMARK);
						task = new BookmarkItemUpdateTask(oldItem, newItem, false, getMyActivity(), this);
						runTask(task);
						break;

					// �I���u�b�N�}�[�N�̍X�V�J�n�i�o�b�N�O���E���h�X�V�j
					case MessageFlags.OP_OFFLINE_UPDATE:
						// �o�b�N�O���E���h�X�V�̏ꍇ�̓A�_�v�^�����b�N����K�v�������A
						// ���I�����Ƀ��b�Z�[�W�͓������Ȃ��̂ŕ֋X��A�_�v�^�̃��b�N�͊O���B
						mAdapter.setLoadingState(false);
						// �^�X�N���s
						info = msg.getData();
						oldItem = (BookmarkItem) info.getSerializable(BundleKeys.OLD_BOOKMARK);
						newItem = (BookmarkItem) info.getSerializable(BundleKeys.NEW_BOOKMARK);
						task = new BookmarkItemUpdateTask(oldItem, newItem, true, getMyActivity(), this);
						runTask(task);
						break;

					// �I���u�b�N�}�[�N�̍폜�J�n
					case MessageFlags.OP_ONLINE_DELETE:
						info = msg.getData();
						BookmarkItem delItem = (BookmarkItem) info.getSerializable(BundleKeys.DELETE_BOOKMARK);
						task = new BookmarkItemDeleteTask(delItem, getMyActivity(), this);
						runTask(task);
						break;

					// �u�b�N�}�[�N�̕��ёւ�
					case MessageFlags.OP_ONLINE_SORT:
						task = new BookmarkItemSortReorderTask(getMyActivity(), this);
						runTask(task);
						break;

					// �u�b�N�}�[�N�̈ړ�
					case MessageFlags.OP_ONLINE_MOVE:
						info = msg.getData();
						BookmarkItem moveItem = (BookmarkItem) info.getSerializable(BundleKeys.SELECTED_BOOKMARK);
						TagItem destTag = (TagItem) info.getSerializable(BundleKeys.SELECTED_TAG);
						task = new BookmarkItemMoveTask(moveItem, destTag, getMyActivity(), this);
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
					// �u�b�N�}�[�N���X�g�̃��[�h����
					case MessageFlags.OP_ONLINE_QUERY:
						info = msg.getData();
						@SuppressWarnings("unchecked") ArrayList<BookmarkItem> items =
							(ArrayList<BookmarkItem>) info.getSerializable(BundleKeys.ALL_BOOKMARK);
						runAsReloadListView(this, items);
						break;

					// �u�b�N�}�[�N�̒ǉ�����
					case MessageFlags.OP_ONLINE_CREATE:
						post(mUIWorker_Added);
						break;

					// �I���u�b�N�}�[�N�̍X�V�����i�I�����C���X�V�̏ꍇ�̂݁j
					case MessageFlags.OP_ONLINE_UPDATE:
						post(mUIWorker_Updated);
						break;

					// �I���u�b�N�}�[�N�̍폜����
					case MessageFlags.OP_ONLINE_DELETE:
						post(mUIWorker_Deleted);
						break;

					// �u�b�N�}�[�N�\�[�g����
					case MessageFlags.OP_ONLINE_SORT:
						post(mUIWorker_Sorted);
						break;

					// �u�b�N�}�[�N�ړ�����
					case MessageFlags.OP_ONLINE_MOVE:
						post(mUIWorker_Moved);
						break;
				}
			}
			/*
			 * �������s
			 */
			else if(state == MessageFlags.STATE_FAILED)
			{
				mAdapter.setLoadingState(false);

				switch(operation)
				{
					case MessageFlags.OP_ONLINE_QUERY:
						post(mUIWorker_LoadFailed);
						break;

					case MessageFlags.OP_ONLINE_UPDATE:
					case MessageFlags.OP_ONLINE_DELETE:
						post(mUIWorker_UnexpectedError);
						break;
				}
			}
			/*
			 * �������~
			 */
			else if(state == MessageFlags.STATE_CANCELED)
			{
				mAdapter.setLoadingState(false);

				post(mUIWorker_OperationCanceled);
			}
		}

		/**
		 * �^�O�Ɋւ��鏈���̃n���h�����O
		 */
		private void handleBookmarkTagsMessage(Message msg, int operation, int state)
		{
			final Bundle info;
			final BaseSequentialTask task;

			/*
			 * �����X���b�h�N��
			 */
			if(state == MessageFlags.STATE_START)
			{
				mAdapter.setLoadingState(true);

				switch(operation)
				{
					// �S�^�O�̃s�b�N�A�b�v�i���X�g�擾�j
					case MessageFlags.OP_ONLINE_QUERY:
						task = new TagLoadAllTask(false, getMyActivity(), this);
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
					// �S�^�O�̃s�b�N�A�b�v�i���X�g�擾�j����
					case MessageFlags.OP_ONLINE_QUERY:
						info = msg.getData();
						@SuppressWarnings("unchecked") final ArrayList<TagItem> tagList =
							(ArrayList<TagItem>) info.getSerializable(BundleKeys.ALL_TAG);
						// �^�O�I���_�C�A���O�\��
						runAsShowTagChooserAndSendMessage(tagList);
						break;
				}
			}
			/*
			 * �������s
			 */
			else if(state == MessageFlags.STATE_FAILED)
			{
				mAdapter.setLoadingState(false);
				post(mUIWorker_UnexpectedError);
			}
			/*
			 * �������~
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
	private void runAsReloadListView(final Handler handler, final ArrayList<BookmarkItem> items)
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				if(items.size() > 0)
				{
					// ��A�C�e�����b�Z�[�W�͔�\��
					if(mTopPanelView != null)
					{
						LinearLayout panelTop = (LinearLayout) findViewById(R.id.panel_top);
						panelTop.removeView(mTopPanelView);
						mTopPanelView = null;
					}
				}
				else
				{
					// ��A�C�e�����b�Z�[�W��\��
					mTopPanelView = inflater.inflate(R.layout.parts_main_paneltop_emptybookmark, null);
					LinearLayout panelTop = (LinearLayout) findViewById(R.id.panel_top);
					panelTop.addView(mTopPanelView);
				}

				mAdapter.removeAll();
				mAdapter.addAll(items);
				mAdapter.notifyDataSetChanged();
			}
		});
	}
	/** �ړ���̃^�O��I�����邽�߂̃_�C�A���O�\�� */
	private void runAsShowTagChooserAndSendMessage(final ArrayList<TagItem> tags)
	{
		Log.d(TAG, "START");

		//�_�C�A���O�I�����A�C�e�����\�z
		final ArrayList<ChooserDialogItem> chooserList = new ArrayList<ChooserDialogItem>();
		int i = 0;
		for(TagItem tag : tags)
		{
			chooserList.add(new ChooserDialogItem(i, tag.getTagCaption(), tag, false));
			++i;
		}

		// �I�����X�g�\��
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				closeDialog();

				mChooserDialog = new ChooserDialog<ChooserDialogItem>(
					getString(R.string.caption_title_choose_tags),
					getMyActivity(),
					ListView.CHOICE_MODE_SINGLE,
					chooserList,
					new ChooserDialog.OnSelectionListener<ChooserDialogItem>()
					{
						@Override
						public void onSelected(final ArrayList<ChooserDialogItem> selection)
						{
							if(selection.size() == 0)
								return;

							TagItem selectedTag = (TagItem) selection.get(0).getValue();

							Bundle info = new Bundle();
							info.putSerializable(BundleKeys.SELECTED_TAG, selectedTag);
							info.putSerializable(BundleKeys.SELECTED_BOOKMARK, mMoveItem);	//�����ōŏ��ɑޔ����Ă������A�C�e�����Z�b�g

							Message msg = Message.obtain();
							msg.what = MessageFlags.KIND_BOOKMARKITEM |MessageFlags.OP_ONLINE_MOVE | MessageFlags.STATE_START;
							msg.setData(info);
							mHandler.sendMessage(msg);
						}
					});
				mChooserDialog.show();
			}
		});
	}
	/** �V�K�ǉ����� */
	Runnable mUIWorker_Added = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_add, Toast.LENGTH_SHORT).show();
			// ���X�g�ă��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** �ύX���� */
	Runnable mUIWorker_Updated = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_updated, Toast.LENGTH_SHORT).show();
			// ���X�g�ă��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** �폜���� */
	Runnable mUIWorker_Deleted = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_deleted, Toast.LENGTH_SHORT).show();
			// ���X�g�ă��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** ���ёւ����� */
	Runnable mUIWorker_Sorted = new Runnable()
	{
		@Override
		public void run()
		{
			// ���X�g�ă��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** �ړ����� */
	Runnable mUIWorker_Moved = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_moved, Toast.LENGTH_SHORT).show();
			// ���X�g�ă��[�h���w��
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** �u�b�N�}�[�N���X�g�̃��[�h���s */
	Runnable mUIWorker_LoadFailed = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_loaditems_failed, Toast.LENGTH_LONG).show();
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

		closeDialog();

		final BookmarkItem selectedBookmark = (BookmarkItem) mAdapter.getItem(position);
		final ArrayAdapter<SimpleDialogItem> adapter = new ArrayAdapter<SimpleDialogItem>(
				getMyActivity(), R.layout.parts_simplechooserdialog_listitem, android.R.id.text1);

		adapter.add(new SimpleDialogItem(R.string.caption_menu_items_browse, getString(R.string.caption_menu_items_browse)));
		adapter.add(new SimpleDialogItem(R.string.caption_menu_items_share, getString(R.string.caption_menu_items_share)));
		adapter.add(new SimpleDialogItem(R.string.caption_menu_items_copy, getString(R.string.caption_menu_items_copy)));

		mChooserDialog = Procedure.createSimpleChooserDialog(getMyActivity(), adapter,
		new IDialogOnClickListener()
		{
			@Override
			public void onClick(IDialogOnClickListener.DialogEventArgs e)
			{
				SimpleDialogItem selected = adapter.getItem(e.which);
				Intent intent;
				switch(selected.getId())
				{
					/*
					 * URL���u���E�Y
					 */
					case R.string.caption_menu_items_browse:

						intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(selectedBookmark.getUrl()));
						Procedure.startActivity(getMyActivity(), intent);

						// �ŏI�A�N�Z�X�����X�V�iDB���f�j
						BookmarkItem updItem = selectedBookmark.deepCopy();
						updItem.setLastAccessed(System.currentTimeMillis(), getString(R.string.caption_label_lastaccessed));

						Bundle updInfo = new Bundle();
						updInfo.putSerializable(BundleKeys.OLD_BOOKMARK, updItem);
						updInfo.putSerializable(BundleKeys.NEW_BOOKMARK, updItem);

						Message msg = Message.obtain();
						msg.what = MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_OFFLINE_UPDATE | MessageFlags.STATE_START;
						msg.setData(updInfo);
						mHandler.sendMessage(msg);
						break;

					/*
					 * URL�����L
					 */
					case R.string.caption_menu_items_share:

						intent = new Intent(Intent.ACTION_SEND);
						intent.addCategory(Intent.CATEGORY_DEFAULT);
						intent.setType("text/plain");
						intent.putExtra(Intent.EXTRA_TITLE, selectedBookmark.getTitle());
						intent.putExtra(Intent.EXTRA_TEXT, selectedBookmark.getUrl());
						Procedure.startActivity(getMyActivity(), intent);
						break;

					/*
					 * URL���N���b�v�{�[�h�ɃR�s�[
					 */
					case R.string.caption_menu_items_copy:

						ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
						cm.setText(selectedBookmark.getUrl());
						Toast.makeText(getMyActivity(), R.string.msg_normal_copy_to_clipboard, Toast.LENGTH_SHORT).show();
						break;

					default:
						throw new UnsupportedOperationException("Unknown sort order. -->"+selected.getId());
				}
			}
		});
		mChooserDialog.show();
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

		menu.add(0, R.string.caption_menu_items_update_title, 0, getString(R.string.caption_menu_items_update_title));
		menu.add(0, R.string.caption_menu_items_update_thumbnail, 0, getString(R.string.caption_menu_items_update_thumbnail));
		menu.add(0, R.string.caption_menu_items_delete, 0, getString(R.string.caption_menu_items_delete));
		menu.add(0, R.string.caption_menu_items_move,   0, getString(R.string.caption_menu_items_move));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		final AdapterContextMenuInfo menuinfo = (AdapterContextMenuInfo) item.getMenuInfo();
		final View srcView = menuinfo.targetView;
		final BookmarkItem bookmarkItem = (BookmarkItem) mAdapter.getItem(menuinfo.position);
		Message msg;

		if(mAdapter.nowLoading())
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_busy, Toast.LENGTH_LONG).show();
			return true;
		}

		Bundle info;

		switch(item.getItemId())
		{
			//
			// �u�b�N�}�[�N�̃^�C�g���X�V
			//
			case R.string.caption_menu_items_update_title:
				AlertDialog dialog = Procedure.createSimpleInputDialog(
					getMyActivity(),
					getString(R.string.caption_title_inputbookmarktitle),
					bookmarkItem.getTitle(),
					new IDialogOnClickListener()
					{
						@Override
						public void onClick(IDialogOnClickListener.DialogEventArgs e)
						{
							String inputTitle = (String) e.what;
							if(StringUtils.isBlank(inputTitle))
							{
								Toast.makeText(getMyActivity(), R.string.msg_error_text_is_null_or_empty, Toast.LENGTH_SHORT).show();
								return;
							}
							BookmarkItem newItem = bookmarkItem.deepCopy();
							newItem.setTitle(inputTitle);

							Bundle updInfo = new Bundle();
							updInfo.putSerializable(BundleKeys.OLD_BOOKMARK, bookmarkItem);
							updInfo.putSerializable(BundleKeys.NEW_BOOKMARK, newItem);

							Message msg = Message.obtain();
							msg.what = MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_UPDATE | MessageFlags.STATE_START;
							msg.setData(updInfo);
							mHandler.sendMessage(msg);
						}
					});
				dialog.show();
				break;

			//
			// �u�b�N�}�[�N�̃T���l�C���X�V
			//
			case R.string.caption_menu_items_update_thumbnail:
				if(!Procedure.isNetworkConnected(this))
				{
					Toast.makeText(this, R.string.msg_error_network_unavailable, Toast.LENGTH_SHORT).show();
					break;
				}
				LazyLoadImageView imgView = (LazyLoadImageView) srcView.findViewById(R.id.img_thumbnail);
				imgView.loadImage(Path.WEBPAGE_CAPTURE_SERVICE_PREFIX+bookmarkItem.getUrl(), true);
				break;

			//
			// �u�b�N�}�[�N�̍폜
			//
			case R.string.caption_menu_items_delete:
				info = new Bundle();
				info.putSerializable(BundleKeys.DELETE_BOOKMARK, bookmarkItem);

				msg = Message.obtain();
				msg.what = MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_DELETE | MessageFlags.STATE_START;
				msg.setData(info);
				mHandler.sendMessage(msg);
				break;

			//
			// �u�b�N�}�[�N�̈ړ�
			// �E�܂��ŏ��Ƀ^�O���X�g�̒��o���J�n�B
			// �E��q�����̂��ߑI�������u�b�N�}�[�N�̓����o�[�ϐ��ɑޔ����Ă����B
			//
			case R.string.caption_menu_items_move:
				mMoveItem = bookmarkItem;

				msg = Message.obtain();
				msg.what = MessageFlags.KIND_TAG | MessageFlags.OP_ONLINE_QUERY | MessageFlags.STATE_START;
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
		getMenuInflater().inflate(R.menu.menu_bookmarkitems, menu);

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
			// �\�����̕ύX
			//
			case R.id.menu_sort:

				closeDialog();

				final ArrayAdapter<SimpleDialogItem> adapter =
					new ArrayAdapter<SimpleDialogItem>(
						getMyActivity(),
						R.layout.parts_simplechooserdialog_listitem,
						android.R.id.text1);
				adapter.add(new SimpleDialogItem(BookmarkItemAdapter.SortOrder.BY_TITLE_ASC, getString(R.string.caption_sortorder_title_asc)));
				adapter.add(new SimpleDialogItem(BookmarkItemAdapter.SortOrder.BY_URL_ASC, getString(R.string.caption_sortorder_url_asc)));
				adapter.add(new SimpleDialogItem(BookmarkItemAdapter.SortOrder.BY_CREATED_DESC, getString(R.string.caption_sortorder_created_desc)));
				adapter.add(new SimpleDialogItem(BookmarkItemAdapter.SortOrder.BY_LASTACCESSED_DESC, getString(R.string.caption_sortorder_lastaccessed_desc)));

				mChooserDialog = Procedure.createSimpleChooserDialog(getMyActivity(), adapter,
				new IDialogOnClickListener()
				{
					@Override
					public void onClick(IDialogOnClickListener.DialogEventArgs e)
					{
						SimpleDialogItem selected = adapter.getItem(e.which);
						setPreferences(PrefKeys.SORTORDER_BOOKMARKITEM, selected.getId());
						mHandler.sendEmptyMessage(
							MessageFlags.KIND_BOOKMARKITEM | MessageFlags.OP_ONLINE_SORT | MessageFlags.STATE_START);
					}
				});
				mChooserDialog.show();
				break;

			//
			// �u�b�N�}�[�N�ǉ�
			//
			case R.id.menu_add:
				Intent intent = new Intent(getMyActivity(), AddBookmarkActivity.class);
				intent.putExtra(BundleKeys.SELECTED_TAG, mParentTag);
				Procedure.startActivity(getMyActivity(), intent);
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

	/**
	 * �J���Ă���_�C�A���O�����B
	 */
	private void closeDialog()
	{
		if(mChooserDialog != null && mChooserDialog.isShowing())
		{
			mChooserDialog.dismiss();
		}
		mChooserDialog = null;
	}
}