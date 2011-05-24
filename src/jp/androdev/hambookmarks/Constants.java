/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks;

import java.lang.reflect.Field;
import java.util.Hashtable;


/**
 * Constants class.
 *
 * @version 1.0.0
 */
public final class Constants
{
	public static class Path
	{
		public static final String WEBPAGE_CAPTURE_SERVICE_PREFIX = "http://img.simpleapi.net/small/";
		public static final String WEBPAGE_CAPTURE_THUMBNAILFILE_EXT = ".capture";
	}

	public static class HamDBKeys
	{
		public static final String BOOKMARKS_ROOT = "bookmarks root";
		public static final String TAGS_ROOT = "tags root";
		public static final String TAG_REF = "tag ref";
		public static final String NO_TAGGED = "no tagged";
	}

	public static class PrefKeys
	{
		public static final String SORTORDER_BOOKMARKITEM = "BookmarkItemAdapter.SortOrder";
		public static final String SORTORDER_TAG = "TagItemAdapter.SortOrder";
	}

	public static class BundleKeys
	{
		public static final String ALL_TAG = "all tags";
		public static final String SELECTED_TAG = "selected tag";
		public static final String OLD_TAG = "old tag";
		public static final String NEW_TAG = "new tag";
		public static final String DELETE_TAG = "delete tag";

		public static final String ALL_BOOKMARK = "all bookmark";
		public static final String SELECTED_BOOKMARK = "selected bookmark";
		public static final String OLD_BOOKMARK = "old bookmark";
		public static final String NEW_BOOKMARK = "new bookmark";
		public static final String DELETE_BOOKMARK = "delete bookmark";
	}

	public static class MessageFlags
	{
		public static final int KIND_TAG				= 0x01000000;	//1000000000000000000000000	右に24シフトで値取得
		public static final int KIND_BOOKMARKITEM		= 0x02000000;
		public static final int KIND_NONE				= 0x09000000;

		public static final int STATE_START			= 0x00010000;	//10000000000000000	右に16シフトで値取得
		public static final int STATE_COMPLETE		= 0x00050000;
		public static final int STATE_FAILED 			= 0x00060000;
		public static final int STATE_CANCELED		= 0x00070000;

		public static final int OP_ONLINE_CREATE		= 0x00000001;
		public static final int OP_ONLINE_DELETE		= 0x00000002;
		public static final int OP_ONLINE_UPDATE		= 0x00000003;
		public static final int OP_OFFLINE_UPDATE		= 0x00000004;
		public static final int OP_ONLINE_QUERY 		= 0x00000005;
		public static final int OP_ONLINE_INIT		= 0x00000006;
		public static final int OP_ONLINE_SYNC		= 0x00000007;
		public static final int OP_ONLINE_SORT		= 0x00000008;
		public static final int OP_ONLINE_MOVE		= 0x00000009;

		public static final int test = (KIND_BOOKMARKITEM >> 24);

		public static int getKind(int flags)
		{
			int shifted = flags >> 24;
			return (shifted << 24);
		}

		public static int getState(int flags)
		{
			int f = flags - getKind(flags);
			int shifted = f >> 16;
			return (shifted << 16);
		}

		public static int getOperation(int flags)
		{
			int kind = getKind(flags);
			int state = getState(flags);
			return (flags - kind - state);
		}

		public static String getFlagName(int aFlag)
		{
			Field f = MESSAGEFLAGS_FIELDNAMEMAPPING.get(aFlag);
			if(f != null)
			{
				return f.getName();
			}
			return "Unknown flags. -->"+String.valueOf(aFlag);
		}
	}

	private static final Hashtable<Integer, Field> MESSAGEFLAGS_FIELDNAMEMAPPING;

	static
	{
		MESSAGEFLAGS_FIELDNAMEMAPPING = new Hashtable<Integer, Field>();
		Field[] constants = MessageFlags.class.getDeclaredFields();

		try
		{
			for(Field f : constants)
			{
				Integer staticValue = (Integer) f.get(null);
				MESSAGEFLAGS_FIELDNAMEMAPPING.put(staticValue, f);
			}
		}
		catch (Throwable e)
		{
			throw new IllegalAccessError("Can't get MessageFlags class fields.");
		}
	}
}
