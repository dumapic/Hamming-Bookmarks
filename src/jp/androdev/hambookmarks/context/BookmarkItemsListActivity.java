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
	 * (非 Javadoc)
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
	 * (非 Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "START");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setResult(Activity.RESULT_OK);

		// 親タグを取得・退避
		if(mReceivedIntent == null)
			mReceivedIntent = getIntent();
		mParentTag = (TagItem) mReceivedIntent.getSerializableExtra(BundleKeys.SELECTED_TAG);

		// タイトル設定
		TextView txtActionTitle = (TextView) findViewById(R.id.txt_actionbar_title);
		txtActionTitle.setText("/"+mParentTag.getTagCaption());

		// アダプタ・リストビュー設定
		mListView = (ListView) findViewById(R.id.list_main);
		mListView.setOnItemClickListener(this);
		registerForContextMenu(mListView);
		mAdapter = new BookmarkItemAdapter(getBaseContext());
		mListView.setAdapter(mAdapter);

		// 戻るボタン
		ImageButton btnBack = (ImageButton) findViewById(R.id.btn_prev);
		btnBack.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//この画面だけクローズ
				finish();
			}
		});
		// 閉じるボタン
		ImageButton btnClose = (ImageButton) findViewById(R.id.btn_close);
		btnClose.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// 呼出元も巻き込んでクローズ
				// @see jp.androdev.hambookmarks.context.TagsListActivity#onActivityResult(int, int, Intent)
				setResult(Activity.RESULT_CANCELED);
				finish();
			}
		});

		// タグに紐づくブックマークリストのロードを指示
		mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM |MessageFlags.OP_ONLINE_QUERY |MessageFlags.STATE_START);
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

		closeDialog();
	}

	/**
	 * 指示内容に従い、適切なスレッドを起動するハンドラ
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
		 * ブックマークアイテムに関する処理のハンドリング
		 */
		private void handleBookmarkItemsMessage(Message msg, int operation, int state)
		{
			Bundle info;
			BaseSequentialTask task;
			BookmarkItem oldItem;
			BookmarkItem newItem;

			/*
			 * 処理スレッド起動
			 */
			if(state == MessageFlags.STATE_START)
			{
				mAdapter.setLoadingState(true);

				switch(operation)
				{
					// ブックマークリストのロード開始
					case MessageFlags.OP_ONLINE_QUERY:
						task = new BookmarkItemLoadByTagTask(mParentTag, getMyActivity(), this);
						runTask(task);
						break;

					// ブックマークの新規追加開始
					case MessageFlags.OP_ONLINE_CREATE:
						info = msg.getData();
						newItem = (BookmarkItem) info.getSerializable(BundleKeys.NEW_BOOKMARK);
						TagItem tag = (TagItem) info.getSerializable(BundleKeys.SELECTED_TAG);
						task = new BookmarkItemAddTask(tag, newItem, getMyActivity(), this);
						runTask(task);
						break;

					// 選択ブックマークの更新開始（オンライン更新）
					case MessageFlags.OP_ONLINE_UPDATE:
						info = msg.getData();
						oldItem = (BookmarkItem) info.getSerializable(BundleKeys.OLD_BOOKMARK);
						newItem = (BookmarkItem) info.getSerializable(BundleKeys.NEW_BOOKMARK);
						task = new BookmarkItemUpdateTask(oldItem, newItem, false, getMyActivity(), this);
						runTask(task);
						break;

					// 選択ブックマークの更新開始（バックグラウンド更新）
					case MessageFlags.OP_OFFLINE_UPDATE:
						// バックグラウンド更新の場合はアダプタをロックする必要が無く、
						// かつ終了時にメッセージは投げられないので便宜上アダプタのロックは外す。
						mAdapter.setLoadingState(false);
						// タスク実行
						info = msg.getData();
						oldItem = (BookmarkItem) info.getSerializable(BundleKeys.OLD_BOOKMARK);
						newItem = (BookmarkItem) info.getSerializable(BundleKeys.NEW_BOOKMARK);
						task = new BookmarkItemUpdateTask(oldItem, newItem, true, getMyActivity(), this);
						runTask(task);
						break;

					// 選択ブックマークの削除開始
					case MessageFlags.OP_ONLINE_DELETE:
						info = msg.getData();
						BookmarkItem delItem = (BookmarkItem) info.getSerializable(BundleKeys.DELETE_BOOKMARK);
						task = new BookmarkItemDeleteTask(delItem, getMyActivity(), this);
						runTask(task);
						break;

					// ブックマークの並び替え
					case MessageFlags.OP_ONLINE_SORT:
						task = new BookmarkItemSortReorderTask(getMyActivity(), this);
						runTask(task);
						break;

					// ブックマークの移動
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
			 * 処理完了
			 */
			else if(state == MessageFlags.STATE_COMPLETE)
			{
				mAdapter.setLoadingState(false);

				switch(operation)
				{
					// ブックマークリストのロード完了
					case MessageFlags.OP_ONLINE_QUERY:
						info = msg.getData();
						@SuppressWarnings("unchecked") ArrayList<BookmarkItem> items =
							(ArrayList<BookmarkItem>) info.getSerializable(BundleKeys.ALL_BOOKMARK);
						runAsReloadListView(this, items);
						break;

					// ブックマークの追加完了
					case MessageFlags.OP_ONLINE_CREATE:
						post(mUIWorker_Added);
						break;

					// 選択ブックマークの更新完了（オンライン更新の場合のみ）
					case MessageFlags.OP_ONLINE_UPDATE:
						post(mUIWorker_Updated);
						break;

					// 選択ブックマークの削除完了
					case MessageFlags.OP_ONLINE_DELETE:
						post(mUIWorker_Deleted);
						break;

					// ブックマークソート完了
					case MessageFlags.OP_ONLINE_SORT:
						post(mUIWorker_Sorted);
						break;

					// ブックマーク移動完了
					case MessageFlags.OP_ONLINE_MOVE:
						post(mUIWorker_Moved);
						break;
				}
			}
			/*
			 * 処理失敗
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
			 * 処理中止
			 */
			else if(state == MessageFlags.STATE_CANCELED)
			{
				mAdapter.setLoadingState(false);

				post(mUIWorker_OperationCanceled);
			}
		}

		/**
		 * タグに関する処理のハンドリング
		 */
		private void handleBookmarkTagsMessage(Message msg, int operation, int state)
		{
			final Bundle info;
			final BaseSequentialTask task;

			/*
			 * 処理スレッド起動
			 */
			if(state == MessageFlags.STATE_START)
			{
				mAdapter.setLoadingState(true);

				switch(operation)
				{
					// 全タグのピックアップ（リスト取得）
					case MessageFlags.OP_ONLINE_QUERY:
						task = new TagLoadAllTask(false, getMyActivity(), this);
						runTask(task);
						break;
				}
			}
			/*
			 * 処理完了
			 */
			else if(state == MessageFlags.STATE_COMPLETE)
			{
				mAdapter.setLoadingState(false);

				switch(operation)
				{
					// 全タグのピックアップ（リスト取得）完了
					case MessageFlags.OP_ONLINE_QUERY:
						info = msg.getData();
						@SuppressWarnings("unchecked") final ArrayList<TagItem> tagList =
							(ArrayList<TagItem>) info.getSerializable(BundleKeys.ALL_TAG);
						// タグ選択ダイアログ表示
						runAsShowTagChooserAndSendMessage(tagList);
						break;
				}
			}
			/*
			 * 処理失敗
			 */
			else if(state == MessageFlags.STATE_FAILED)
			{
				mAdapter.setLoadingState(false);
				post(mUIWorker_UnexpectedError);
			}
			/*
			 * 処理中止
			 */
			else if(state == MessageFlags.STATE_CANCELED)
			{
				mAdapter.setLoadingState(false);
				post(mUIWorker_OperationCanceled);
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
	/** リスト再描画 */
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
					// 空アイテムメッセージは非表示
					if(mTopPanelView != null)
					{
						LinearLayout panelTop = (LinearLayout) findViewById(R.id.panel_top);
						panelTop.removeView(mTopPanelView);
						mTopPanelView = null;
					}
				}
				else
				{
					// 空アイテムメッセージを表示
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
	/** 移動先のタグを選択するためのダイアログ表示 */
	private void runAsShowTagChooserAndSendMessage(final ArrayList<TagItem> tags)
	{
		Log.d(TAG, "START");

		//ダイアログ選択肢アイテムを構築
		final ArrayList<ChooserDialogItem> chooserList = new ArrayList<ChooserDialogItem>();
		int i = 0;
		for(TagItem tag : tags)
		{
			chooserList.add(new ChooserDialogItem(i, tag.getTagCaption(), tag, false));
			++i;
		}

		// 選択リスト表示
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
							info.putSerializable(BundleKeys.SELECTED_BOOKMARK, mMoveItem);	//ここで最初に退避しておいたアイテムをセット

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
	/** 新規追加完了 */
	Runnable mUIWorker_Added = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_add, Toast.LENGTH_SHORT).show();
			// リスト再ロードを指示
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** 変更完了 */
	Runnable mUIWorker_Updated = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_updated, Toast.LENGTH_SHORT).show();
			// リスト再ロードを指示
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** 削除完了 */
	Runnable mUIWorker_Deleted = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_deleted, Toast.LENGTH_SHORT).show();
			// リスト再ロードを指示
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** 並び替え完了 */
	Runnable mUIWorker_Sorted = new Runnable()
	{
		@Override
		public void run()
		{
			// リスト再ロードを指示
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** 移動完了 */
	Runnable mUIWorker_Moved = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_normal_default_moved, Toast.LENGTH_SHORT).show();
			// リスト再ロードを指示
			mHandler.sendEmptyMessage(MessageFlags.KIND_BOOKMARKITEM|MessageFlags.OP_ONLINE_QUERY|MessageFlags.STATE_START);
		}
	};
	/** ブックマークリストのロード失敗 */
	Runnable mUIWorker_LoadFailed = new Runnable()
	{
		@Override
		public void run()
		{
			Toast.makeText(getMyActivity(), R.string.msg_error_loaditems_failed, Toast.LENGTH_LONG).show();
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


	/*
	 *
	 * リストアイテム選択時
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
					 * URLをブラウズ
					 */
					case R.string.caption_menu_items_browse:

						intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(selectedBookmark.getUrl()));
						Procedure.startActivity(getMyActivity(), intent);

						// 最終アクセス日を更新（DB反映）
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
					 * URLを共有
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
					 * URLをクリップボードにコピー
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
	 * リストアイテムの長押しメニュー
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
			// ブックマークのタイトル更新
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
			// ブックマークのサムネイル更新
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
			// ブックマークの削除
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
			// ブックマークの移動
			// ・まず最初にタグリストの抽出を開始。
			// ・後述処理のため選択したブックマークはメンバー変数に退避しておく。
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
	 * メニューボタン押下によるメニュー
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
			// 表示順の変更
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
			// ブックマーク追加
			//
			case R.id.menu_add:
				Intent intent = new Intent(getMyActivity(), AddBookmarkActivity.class);
				intent.putExtra(BundleKeys.SELECTED_TAG, mParentTag);
				Procedure.startActivity(getMyActivity(), intent);
				break;

			//
			// 未定義のメニュー
			//
			default:
				throw new UnsupportedOperationException("Unsupported menu. -->"+item.getItemId());
		}

		return true;
	}

	/*
	 *
	 * その他
	 *
	 */

	/**
	 * 開いているダイアログを閉じる。
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