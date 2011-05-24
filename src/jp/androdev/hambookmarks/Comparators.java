/*
 * Copyright(C) 2011 - Hamming Bookmarks
 * The sample demo app for hamsterdb.
 *
 * @author DUMAPIC
 */
package jp.androdev.hambookmarks;

import java.util.Comparator;

import jp.androdev.hambookmarks.data.BookmarkItem;
import jp.androdev.hambookmarks.data.BookmarkItemAdapter;
import jp.androdev.hambookmarks.data.TagItem;
import jp.androdev.hambookmarks.data.TagItemAdapter;

/**
 * Comparators grouping class.
 *
 * @version 1.0.0
 */
public final class Comparators
{
	/**
	 * For TagItem comparators.
	 */
	public static class ForTags
	{
		/**
		 *  Get comparator for TagItem object.
		 *
		 *  @param order {@link TagItemAdapter.SortOrder} value.
		 */
		public static Comparator<TagItem> getComparatorByOrder(int order)
		{
			switch(order)
			{
				case TagItemAdapter.SortOrder.BY_TITLE_ASC:
					return SORT_BY_TITLE_ASC;
				case TagItemAdapter.SortOrder.BY_TITLE_DESC:
					return SORT_BY_TITLE_DESC;
				default:
					throw new IllegalArgumentException("Illegal sort order. -->"+order);
			}
		}

		/** Sort asc by TagItem title. */
		public static final Comparator<TagItem> SORT_BY_TITLE_ASC = new Comparator<TagItem>()
		{
			@Override
			public int compare(TagItem obj1, TagItem obj2)
			{
				if(obj1.getTag().compareTo(obj2.getTag()) < 0)
				{
					return -1;
				}
				else if(obj1.getTag().compareTo(obj2.getTag()) > 0)
				{
					return 1;
				}
				return 0;
			}
		};

		/** Sort desc by TagItem title. */
		public static final Comparator<TagItem> SORT_BY_TITLE_DESC = new Comparator<TagItem>()
		{
			@Override
			public int compare(TagItem obj1, TagItem obj2)
			{
				if(obj1.getTag().compareTo(obj2.getTag()) < 0)
				{
					return 1;
				}
				else if(obj1.getTag().compareTo(obj2.getTag()) > 0)
				{
					return -1;
				}
				return 0;
			}
		};
	}

	/**
	 * For BookmarkItem comparators.
	 */
	public static class ForBookmarkItems
	{
		/**
		 *  Get comparator for BookmarkItem object.
		 *
		 *  @param order {@link BookmarkItemAdapter.SortOrder} value.
		 */
		public static Comparator<BookmarkItem> getComparatorByOrder(int order)
		{
			switch(order)
			{
				case BookmarkItemAdapter.SortOrder.BY_TITLE_ASC:
					return SORT_BY_TITLE_ASC;
				case BookmarkItemAdapter.SortOrder.BY_URL_ASC:
					return SORT_BY_URL_ASC;
				case BookmarkItemAdapter.SortOrder.BY_CREATED_DESC:
					return SORT_BY_CREATED_DESC;
				case BookmarkItemAdapter.SortOrder.BY_LASTACCESSED_DESC:
					return SORT_BY_LASTACCESSED_DESC;
				default:
					throw new IllegalArgumentException("Illegal sort order. -->"+order);
			}
		}

		/** Sort asc by BookmarkItem URL. */
		public static final Comparator<BookmarkItem> SORT_BY_URL_ASC = new Comparator<BookmarkItem>()
		{
			@Override
			public int compare(BookmarkItem obj1, BookmarkItem obj2)
			{
				if(obj1.getUrl().compareTo(obj2.getUrl()) < 0)
				{
					return -1;
				}
				else if(obj1.getUrl().compareTo(obj2.getUrl()) > 0)
				{
					return 1;
				}
				return 0;
			}
		};

		/** Sort asc by URL title */
		public static final Comparator<BookmarkItem> SORT_BY_TITLE_ASC = new Comparator<BookmarkItem>()
		{
			@Override
			public int compare(BookmarkItem obj1, BookmarkItem obj2)
			{
				if(obj1.getTitle().compareTo(obj2.getTitle()) < 0)
				{
					return -1;
				}
				else if(obj1.getTitle().compareTo(obj2.getTitle()) > 0)
				{
					return 1;
				}
				return 0;
			}
		};

		/** Sort desc by created date. */
		public static final Comparator<BookmarkItem> SORT_BY_CREATED_DESC = new Comparator<BookmarkItem>()
		{
			@Override
			public int compare(BookmarkItem obj1, BookmarkItem obj2)
			{
				if(obj1.getCreated() < obj2.getCreated())
				{
					return 1;
				}
				else if(obj1.getCreated() > obj2.getCreated())
				{
					return -1;
				}
				return 0;
			}
		};

		/** Sort desc by last acessed date. */
		public static final Comparator<BookmarkItem> SORT_BY_LASTACCESSED_DESC = new Comparator<BookmarkItem>()
		{
			@Override
			public int compare(BookmarkItem obj1, BookmarkItem obj2)
			{
				if(obj1.getLastAccessed() < obj2.getLastAccessed())
				{
					return 1;
				}
				else if(obj1.getLastAccessed() > obj2.getLastAccessed())
				{
					return -1;
				}
				return 0;
			}
		};
	}

	/**
	 * For string value comparators.
	 */
	public static class ForString
	{
		/** Sort asc by string value. */
		public static final Comparator<String> SORT_BY_ASC = new Comparator<String>()
		{
			@Override
			public int compare(String obj1, String obj2)
			{
				if(obj1.compareTo(obj2) < 0)
				{
					return -1;
				}
				else if(obj1.compareTo(obj2) > 0)
				{
					return 1;
				}
				return 0;
			}
		};

		/** Sort desc by string value. */
		public static final Comparator<String> SORT_BY_DESC = new Comparator<String>()
		{
			@Override
			public int compare(String obj1, String obj2)
			{
				if(obj1.compareTo(obj2) < 0)
				{
					return 1;
				}
				else if(obj1.compareTo(obj2) > 0)
				{
					return -1;
				}
				return 0;
			}
		};
	}
}
