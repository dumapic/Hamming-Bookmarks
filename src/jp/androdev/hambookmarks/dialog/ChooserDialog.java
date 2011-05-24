/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks.dialog;

import java.util.ArrayList;
import java.util.Collection;

import jp.androdev.hambookmarks.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * �I��p�_�C�A���O
 *
 * @version 1.0.0
 */
public final class ChooserDialog<T extends ChooserDialogItem> extends AlertDialog
{
	/**
	 *
	 * �I���_�C�A���O�ł�OK�n�{�^�������C�x���g���X�i�[
	 *
	 */
	public interface OnSelectionListener<T>
	{
		/**
		 *
		 * OK�n�{�^�������C�x���g
		 *
		 * @param selection �_�C�A���O�őI�������G���e�B�e�B�̃��X�g
		 */
		public void onSelected(final ArrayList<T> selection);
	}

	private final View mDialogView;
	private final ListView mListView;

	/**
	 * �R���X�g���N�^
	 *
	 * @param title �^�C�g��
	 * @param activity �A�N�e�B�r�e�B
	 * @param choiceMode �I�����[�h�i{@link ListView#CHOICE_MODE_SINGLE} Or {@link ListView#CHOICE_MODE_MULTIPLE}�j
	 * @param items �I�����X�g
	 * @param callback OK�{�^���������̃��X�i
	 */
	public ChooserDialog(
		final String title,
		final Activity activity,
		final int choiceMode,
		final Collection<T> items,
		final OnSelectionListener<T> callback)
	{
		super(activity);

		int dialogLayout = 0;
		switch(choiceMode)
		{
			case ListView.CHOICE_MODE_SINGLE:
				dialogLayout = R.layout.chooserdialog_singlechoice;
				break;
			case ListView.CHOICE_MODE_MULTIPLE:
				dialogLayout = R.layout.chooserdialog_multiplechoice;
				break;
			default:
				throw new IllegalArgumentException("Illegal choiceMode. -->"+choiceMode);
		}

		LayoutInflater factory = LayoutInflater.from(activity.getBaseContext());
		mDialogView = factory.inflate(dialogLayout, null);
		mListView = (ListView) mDialogView.findViewById(R.id.chooserdialog_listview);
		setView(mDialogView);
		setOwnerActivity(activity);

		ChooserDialogAdapter<T> adapter = new ChooserDialogAdapter<T>(activity.getBaseContext(), choiceMode);
		adapter.addAll(items);
		mListView.setAdapter(adapter);

		if(title != null && title.length() > 0)
		{
			setTitle(title);
		}

		setInverseBackgroundForced(true);

		//�����I����Ԃ𔽉f
		int i = 0;
		for(T item : adapter)
		{
			mListView.setItemChecked(i, item.getSelected());
			++i;
		}

		//
		// OK�n�{�^���C�x���g
		//
		DialogInterface.OnClickListener buttonOkListener = null;
		switch(choiceMode)
		{
			case ListView.CHOICE_MODE_SINGLE:
				buttonOkListener = new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						ArrayList<T> result = new ArrayList<T>();
						int checkedPos = mListView.getCheckedItemPosition();
						ListAdapter adapter = mListView.getAdapter();

						@SuppressWarnings("unchecked") T chosen =
							(checkedPos != ListView.INVALID_POSITION) ? (T) adapter.getItem(checkedPos) : null;

						if(chosen != null && callback != null)
						{
							result.add(chosen);
							callback.onSelected(result);
						}
					}
				};
				break;

			case ListView.CHOICE_MODE_MULTIPLE:
				buttonOkListener = new DialogInterface.OnClickListener()
				{
					@SuppressWarnings("unchecked")
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						final ArrayList<T> result = new ArrayList<T>();
						final SparseBooleanArray chosenFlags = mListView.getCheckedItemPositions();
						final ListAdapter adapter = mListView.getAdapter();

						final int listItemsCount = adapter.getCount();
						for(int i = 0; i < listItemsCount; ++i)
						{
							if(chosenFlags.get(i) == true)
							{
								result.add((T)adapter.getItem(i));
							}
						}
						if(callback != null)
						{
							callback.onSelected(result);
						}
					}
				};
				break;
		}
		setButton(Dialog.BUTTON_POSITIVE, activity.getString(R.string.caption_button_ok), buttonOkListener);

		//
		// �L�����Z���n�{�^��
		//
		DialogInterface.OnClickListener buttonCancelListener = null;
		switch(choiceMode)
		{
			case ListView.CHOICE_MODE_SINGLE:
			case ListView.CHOICE_MODE_MULTIPLE:
				buttonCancelListener = new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				};
				break;
		}
		setButton(Dialog.BUTTON_NEGATIVE, activity.getString(R.string.caption_button_cancel), buttonCancelListener);
	}
}
