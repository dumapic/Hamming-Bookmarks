/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks;

import android.content.DialogInterface;


/**
 * Dialog onClick listener.
 *
 * @version 1.0.0
 */
public interface IDialogOnClickListener
{
	/**
	 * Callback method.
	 * @param e event object.
	 */
	public void onClick(DialogEventArgs e);

	/**
	 * Event object.
	 */
	public static class DialogEventArgs
	{
		public final DialogInterface dialog;
		public final int which;
		public final Object what;

		/**
		 * Constructor.
		 */
		public DialogEventArgs(DialogInterface dialog, int which, Object what)
		{
			this.dialog = dialog;
			this.which = which;
			this.what = what;
		}
	}
}
