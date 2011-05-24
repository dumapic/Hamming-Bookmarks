/*
 * Copyright(C) 2011 - Ham4Droid
 * hamsterdb libraries for Android(TM) platform.
 *
 * @author DUMAPIC
 */
package jp.androiddevelopers.ham4droid;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jp.androdev.debkit.util.Log;
import de.crupp.hamsterdb.Const;
import de.crupp.hamsterdb.Cursor;
import de.crupp.hamsterdb.Database;
import de.crupp.hamsterdb.DatabaseException;
import de.crupp.hamsterdb.Transaction;

/**
 * The wrapper class of hamsterdb Cursor class({@link Cursor}) for Android(TM).
 * A part of javadoc is copied from {@link Cursor} class source code and modfied by (@)dumapick.
 *
 * NOTE: Cursor must be closed prior to Transaction abort/commit.
 *
 * @version 1.0.0
 */
public final class Ham4DroidCursor
{
	private static final String TAG = "#ham4droid";

	private de.crupp.hamsterdb.Cursor mBaseCursor;

	/**
	 * <p>
     * Creates a new Cursor.
     * This method wraps the native ham_cursor_create function.
     * </p>
     * <p>
     * Creates a new Database Cursor.
     * Cursors can be used to traverse the Database from start to end or vice versa.
     * Cursors can also be used to insert, delete or search Database items.
	 * <p>
     *
     * @param db 	the Database wrapper object.
     * @param txn 	an optional Transaction object.
     * @throws DatabaseException hamsterdb native exception.
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#ga37c8ab3ca9b6005de5cb2c48659fa67f">C documentation</a>
	 * @see Ham4DroidCursor#Ham4DroidCursor(Database, Transaction)
	 */
	public static Ham4DroidCursor createCursor(Ham4DroidDatabase db, Transaction txn) throws DatabaseException
	{
		return new Ham4DroidCursor(db.getHamsterDb(), txn);
	}

	/**
	 * <p>
     * Creates a new Cursor.
     * </p>
     *
     * @param db 	the Database wrapper object.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see Ham4DroidCursor#Ham4DroidCursor(Database)
	 */
	public static Ham4DroidCursor createCursor(Ham4DroidDatabase db) throws DatabaseException
	{
		return new Ham4DroidCursor(db.getHamsterDb());
	}

    /**
     * Constructor - creates a new Cursor of a Database
     *
     * @param db the Database object.
     * @throws DatabaseException hamsterdb native exception.
     *
     * @see Cursor#create(Database)
     */
	private Ham4DroidCursor(Database db) throws DatabaseException
	{
		mBaseCursor = new Cursor(db);
	}

    /**
     * Constructor - creates a new Cursor of a Database
     *
     * @param db the Database object.
     * @param txn the Transaction object.
     * @throws DatabaseException hamsterdb native exception.
     *
     * @see Cursor#Cursor(Database, Transaction)
     */
	private Ham4DroidCursor(Database db, Transaction txn) throws DatabaseException
	{
		mBaseCursor = new Cursor(db, txn);
	}

	/**
	 * <p>
	 * Moves the Cursor to the direction specified in the flags.
	 * This method wraps the native ham_cursor_move function.
	 * </p>
	 * Note:
	 * <ul>
	 * <li>returning find result. (Modified by (@)dumapick)
	 * </ul>
	 *
	 * @param flags
	 * 	the direction for the move.
	 *	If no direction is specified, the Cursor will remain on the current position.
	 *	Possible flags are:
	 *  <ul>
	 *  <li>{@link Const#HAM_CURSOR_FIRST}
	 *  	positions the Cursor on the first item in the Database.
	 *
	 *	<li>{@link Const#HAM_CURSOR_LAST}
	 *		positions the Cursor on the last item in the Database.
	 *
	 *	<li>{@link Const#HAM_CURSOR_NEXT}
	 *		positions the Cursor on the next item in the Database;
	 *		if the Cursor does not point to any item,
	 *		the function behaves as if direction was <code>Const.HAM_CURSOR_FIRST</code>.
	 *
	 *	<li>{@link Const#HAM_CURSOR_PREVIOUS}
	 *		positions the Cursor on the previous item in the Database;
	 *		if the Cursor does not point to any item,
	 *		the function behaves as if direction was <code>Const.HAM_CURSOR_LAST</code>.
	 *
	 *	<li>{@link Const#HAM_SKIP_DUPLICATES}
	 *		skip duplicate keys of the current key.
	 *		Not allowed in combination with <code>Const.HAM_ONLY_DUPLICATES</code>.
	 *
	 *	<li>{@link Const#HAM_ONLY_DUPLICATES}
	 *		only move through duplicate keys of the current key.
	 *		Not allowed in combination with <code>Const.HAM_SKIP_DUPLICATES</code>.
	 *  </ul>
	 * @return if reached out of record, return false. otherwise true.
	 * @throws DatabaseException native hamsterdb error.
	 *
	 * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#gabed22a217e561c77850928292409d8b8">C documentation</a>
	 * @see Cursor#move(int)
	 */
	public boolean moveAs(int flags) throws DatabaseException
	{
		try
		{
			mBaseCursor.move(flags);
			return true;
		}
		catch (DatabaseException e)
		{
			// reached end of the database?
			if(e.getErrno() == Const.HAM_KEY_NOT_FOUND)
			{
				return false;
			}
			throw e;
		}
    }

    /**
     * <p>
     * Moves the Cursor to the first Database element.
     * </p>
	 * Note:
	 * <ul>
	 * <li>returning find result. (Modified by (@)dumapick)
	 * </ul>
     *
     * @return if reached out of record, return false. otherwise true.
     * @throws DatabaseException native hamsterdb error.
     *
     * @see Ham4DroidCursor#moveAs(int)
     * @see Cursor#moveFirst()
     */
	public boolean moveToFirst() throws DatabaseException
	{
		return this.moveAs(Const.HAM_CURSOR_FIRST);
	}

    /**
     * <p>
     * Moves the Cursor to the last Database element.
     * </p>
	 * Note:
	 * <ul>
	 * <li>returning find result. (Modified by (@)dumapick)
	 * </ul>
     *
     * @return if reached out of record, return false. otherwise true.
     * @throws DatabaseException native hamsterdb error.
     *
     * @see Ham4DroidCursor#moveAs(int)
     * @see Cursor#moveLast()
     */
	public boolean moveToLast() throws DatabaseException
	{
		return this.moveAs(Const.HAM_CURSOR_LAST);
	}

    /**
     * <p>
     * Moves the Cursor to the next Database element.
     * </p>
	 * Note:
	 * <ul>
	 * <li>returning find result. (Modified by (@)dumapick)
	 * </ul>
     *
     * @param flags
     * 	addtional flags. see also {@link Ham4DroidCursor#moveAs(int)}.
     * 	HAM_CURSOR_NEXT is already set.
     * @return if reached out of record, return false. otherwise true.
     * @throws DatabaseException native hamsterdb error.
     *
     * @see Ham4DroidCursor#moveAs(int)
     * @see Cursor#moveNext(int)
     */
    public boolean moveToNext(int flags) throws DatabaseException
	{
    	return this.moveAs(Const.HAM_CURSOR_NEXT | flags);
	}

	/**
     * <p>
     * Moves the Cursor to the next Database element.
     * </p>
	 * Note:
	 * <ul>
	 * <li>returning find result. (Modified by (@)dumapick)
	 * </ul>
	 *
	 * @return if reached out of record, return false. otherwise true.
	 * @throws DatabaseException native hamsterdb error.
	 *
     * @see Ham4DroidCursor#moveAs(int)
     * @see Cursor#moveNext(int)
	 */
	public boolean moveToNext() throws DatabaseException
	{
		return this.moveAs(Const.HAM_CURSOR_NEXT);
	}

    /**
     * <p>
     * Moves the Cursor to the previous Database element.
     * </p>
	 * Note:
	 * <ul>
	 * <li>returning find result. (Modified by (@)dumapick)
	 * </ul>
     *
     * @param flags
     * 	addtional flags. see also {@link Ham4DroidCursor#moveAs(int)}.
     * 	HAM_CURSOR_PREVIOUS is already set.
     * @return if reached out of record, return false. otherwise true.
     * @throws DatabaseException native hamsterdb error.
     *
     * @see Ham4DroidCursor#moveAs(int)
     * @see Cursor#movePrevious(int)
     */
	public boolean moveToPrevious(int flags) throws DatabaseException
	{
	    return this.moveAs(Const.HAM_CURSOR_PREVIOUS | flags);
	}

    /**
     * <p>
     * Moves the Cursor to the previous Database element.
     * </p>
	 * Note:
	 * <ul>
	 * <li>returning find result. (Modified by (@)dumapick)
	 * </ul>
     *
     * @return if reached out of record, return false. otherwise true.
     * @throws DatabaseException native hamsterdb error.
     *
     * @see Ham4DroidCursor#moveAs(int)
     * @see Cursor#movePrevious(int)
     */
    public boolean moveToPrevious() throws DatabaseException
    {
        return this.moveAs(Const.HAM_CURSOR_PREVIOUS);
    }

    /**
     *
     * <p>
     * Searches a key and points the Cursor to this key.
     * This method wraps the native ham_cursor_find function.
     * <b><font color="red">Also, this function using java reflection.</font></b>
     * </p>
	 * Note:
	 * <ul>
	 * <li>returning find result. (Modified by (@)dumapick)
	 * </ul>
     * <p>
     * Searches a key and points the Cursor to this key.
     * Searches for an item in the Database and points the Cursor to this item.
     * If the item could not be found, the Cursor is not modified.
     * </p>
     * <p>
     * Note that ham_cursor_find can not search for duplicate keys.
     * If key has multiple duplicates, only the first duplicate is returned.
     * When specifying HAM_DIRECT_ACCESS, the data pointer will point directly to the record that is stored in hamsterdb;
     * the data can be modified, but the pointer must not be reallocated of freed.
     * The flag HAM_DIRECT_ACCESS is only allowed in In-Memory Databases.
     * </p>
     * <p>
     * When either or both {@link Const#HAM_FIND_LT_MATCH} and/or {@link HAM_FIND_GT_MATCH} have been specified as flags,
     * the key structure will be overwritten when an approximate match was found:
     * the key and record structures will then point at the located key (and record).
     * In this case the caller should ensure key points at a structure which must adhere to the same restrictions and
     * conditions as specified for ham_cursor_move(...,HAM_CURSOR_*): key->data will point to temporary data upon return.
     * This pointer will be invalidated by subsequent hamsterdb API calls.
     * See <code>HAM_KEY_USER_ALLOC</code> on how to change this behaviour.
     * </p>
     * <p>
     * Further note that the key structure must be non-const at all times as its internal flag bits may be written to.
     * This is done for your benefit, as you may pass the returned key structure to ham_key_get_approximate_match_type()
     * to retrieve additional info about the precise nature of the returned key:
     * the sign value produced by ham_key_get_approximate_match_type() tells you which kind of match
     * (equal, less than, greater than) occurred.
     * This is very useful to discern between the various possible successful answers produced by the combinations of
     * {@link Const#HAM_FIND_LT_MATCH}, {@link Const#HAM_FIND_GT_MATCH} and/or {@link Const#HAM_FIND_EXACT_MATCH}.
     * </p>
     * <p>
     * <b>Remark</b><br/>
     * For Approximate Matching the returned match will either match the key exactly or is either the first key available above or below the given key
     * when an exact match could not be found; 'find' does NOT spend any effort,
     * in the sense of determining which of both is the 'nearest' to the given key,
     * when both a key above and a key below the one given exist; 'find' will simply return the first of both found.
     * As such, this flag is the simplest possible combination of the combined {@link Const#HAM_FIND_LEQ_MATCH} and
     * {@link HAM_FIND_GEQ_MATCH} flags.
     * </p>
     * <p>
     * Note:<br/>
     * Note that these flags may be bitwise OR-ed to form functional combinations.
     * {@link Const#HAM_FIND_LEQ_MATCH}, {@link Const#HAM_FIND_GEQ_MATCH} and {@link Const#HAM_FIND_NEAR_MATCH} are
     * themselves shorthands created using the bitwise OR operation like this:
     * <li>HAM_FIND_LEQ_MATCH == (HAM_FIND_LT_MATCH | HAM_FIND_EXACT_MATCH)
     * <li>HAM_FIND_GEQ_MATCH == (HAM_FIND_GT_MATCH | HAM_FIND_EXACT_MATCH)
     * <li>HAM_FIND_NEAR_MATCH == (HAM_FIND_LT_MATCH | HAM_FIND_GT_MATCH | HAM_FIND_EXACT_MATCH)
     * The remaining bit-combination (HAM_FIND_LT_MATCH | HAM_FIND_GT_MATCH) has no shorthand,
     * but it will function as expected nevertheless: finding only 'neighbouring' records for the given key.
     * </p>
     *
     * @param key
     * 	the key to search for. If this pointer is not NULL, the key of the new item is returned. <br/>
     * 	Note that key->data will point to temporary data.
     * 	This pointer will be invalidated by subsequent hamsterdb API calls.
     * 	See HAM_KEY_USER_ALLOC on how to change this behaviour.
     *
     * @param flags
     *	Optional flags for searching, which can be combined with bitwise OR. Possible flags are:
     * 	<ul>
     * 	<li>{@link Const#HAM_FIND_EXACT_MATCH}(default):
     * 		If the key exists, the cursor is adjusted to reference the record.
     * 		Otherwise, an error is returned. <br/>
     * 		Note that for backwards compatibility the value zero (0) can specified as an alternative
     * 		when this option is not mixed with any of the others in this list.
     * 	<li>{@link Const#HAM_FIND_LT_MATCH}(Cursor 'find' flag 'Less Than'):
     * 		the cursor is moved to point at the last record which' key is less than the specified key.
     * 		When such a record cannot be located, an error is returned.
     * 	<li>{@link Const#HAM_FIND_GT_MATCH}(Cursor 'find' flag 'Greater Than'):
     * 		the cursor is moved to point at the first record which' key is larger than the specified key.
     * 		When such a record cannot be located, an error is returned.
     * 	<li>{@link HAM_FIND_LEQ_MATCH}(Cursor 'find' flag 'Less or EQual'):
     * 		the cursor is moved to point at the record which' key matches the specified key and
     * 		when such a record is not available the cursor is moved to point at the last record which' key is less than the specified key.
     * 		When such a record cannot be located, an error is returned.
     * 	<li>{@link Const#HAM_FIND_GEQ_MATCH}(Cursor 'find' flag 'Greater or Equal'):
     * 		the cursor is moved to point at the record which' key matches the specified key and
     * 		when such a record is not available the cursor is moved to point at the first record which' key is larger than the specified key.
     * 		When such a record cannot be located, an error is returned.
     * 	<li>{@link Const#HAM_FIND_NEAR_MATCH}(Cursor 'find' flag 'Any Near Or Equal'):
     * 		the cursor is moved to point at the record which' key matches the specified key and
     * 		when such a record is not available the cursor is moved to point at either the last record which' key is less than the specified key or the first record which' key is larger than the specified key,
     * 		whichever of these records is located first. When such records cannot be located, an error is returned.
     * 	<li>{@link Const#HAM_DIRECT_ACCESS}(Only for In-Memory Databases!):
     * 		Returns a direct pointer to the data blob stored by the hamsterdb engine.
     * 		This pointer must not be resized or freed, but the data in this memory can be modified.
     *  </ul>
     *
     * @return Moved staus.
     * 	If status code is {@link Const#HAM_SUCCESS}, return true. otherwise return false. See below:
     * <ul>
     * <li>{@link Const#HAM_SUCCESS}:
     * 		upon success.
     * 		Mind the remarks about the key flags being adjusted and the useful invocation of ham_key_get_approximate_match_type() afterwards.
     * <li>{@link Const#HAM_KEY_NOT_FOUND}:
     * 		if no suitable key (record) exists.
     * </ul>
     *
     * @throws DatabaseException native hamsterdb error. see below:
     * <ul>
     * <li>{@link Const#HAM_INV_PARAMETER}:
     * 		if db, key or record is NULL.
     * <li>{@link Const#HAM_CURSOR_IS_NIL}:
     * 		if the Cursor does not point to an item.
     * <li>{@link Const#HAM_INV_PARAMETER}:
     * 		if HAM_DIRECT_ACCESS is specified, but the Database is not an In-Memory Database.
     * </ul>
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#gabb460b8b80d67f574388519576b2c251">C documentation</a>
     * @see Cursor#find(byte[])
     * @see Ham4DroidCursor#find(int)
     */
    public boolean find(Object[] key, int flags) throws DatabaseException
    {
		if (key == null)
			throw new NullPointerException();

		byte[] keydata = Ham4DroidUtils.marshallKeys(key);
		Database mdb = getDatabaseFieldValue(mBaseCursor);
		long handle = mBaseCursor.getHandle();
        int status;

        synchronized(mdb)
        {
            status = (Integer) Ham4DroidUtils.Reflect.invokeMethod(mBaseCursor,
            	sJNI_ham_cursor_find, new Object[]{ handle, keydata, flags });
        }

        switch(status)
        {
        	case Const.HAM_SUCCESS:
        		return true;
        	case Const.HAM_KEY_NOT_FOUND:
        		return false;
        	default:
        		Log.e(TAG, "ERROR: hamsterdb native error. (status: "+Ham4DroidUtils.getConstantsName(status)+")");
        		throw new DatabaseException(status);
        }
    }

    /**
     * <p>
     * Searches a key and points the Cursor to this key.
     * This method wraps the native ham_cursor_find function.
     * </p>
     * <p>
     * Searches for an item in the Database and points the Cursor to this item.
     * If the item could not be found, the Cursor is not modified.
     * If the key has multiple duplicates, the Cursor is positioned on the first duplicate.
	 * </p>
	 * Note:
	 * <ul>
	 * <li>returning find result. (Modified by (@)dumapick)
	 * </ul>
	 *
     * @param key the key to search for.
     * @return if record not found, return false. otherwise true.
     * @throws DatabaseException native hamsterdb error. see below:
     * <ul>
     * <li>{@link Const#HAM_INV_PARAMETER}:
     * 		if db, key or record is NULL.
     * <li>{@link Const#HAM_CURSOR_IS_NIL}:
     * 		if the Cursor does not point to an item.
     * <li>{@link Const#HAM_INV_PARAMETER}:
     * 		if HAM_DIRECT_ACCESS is specified, but the Database is not an In-Memory Database.
     * </ul>
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#gabb460b8b80d67f574388519576b2c251">C documentation</a>
     * @see Cursor#find(byte[])
     * @see Ham4DroidCursor#find(Object[], int)
     */
    public boolean find(Object[] key) throws DatabaseException
    {
    	byte[] keydata = Ham4DroidUtils.marshallKeys(key);
    	try
		{
			mBaseCursor.find(keydata);
			return true;
		}
		catch (DatabaseException e)
		{
			if(e.getErrno() == Const.HAM_KEY_NOT_FOUND)
			{
				return false;
			}
			throw e;
		}
    }

    /**
     * <p>
     * Retrieves the Key of the current item.
     * This method wraps the native ham_cursor_move function.
     * </p>
     * <p>
     * Returns the key of the current Database item.
     * Throws <code>Const.HAM_CURSOR_IS_NIL</code> if the Cursor does not point to any item.
	 * </p>
     * @return the key of the current item.
     * @throws DatabaseException native hamsterdb error.
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#gabed22a217e561c77850928292409d8b8">C documentation</a>
     * @see Cursor#getKey()
     */
	public Object[] getKey() throws DatabaseException
	{
		byte[] ret = mBaseCursor.getKey();
		Object[] result = Ham4DroidUtils.unmarshallKeys(ret);
		return result;
	}

    /**
     * <p>
     * Retrieves the Record of the current item.
     * This method wraps the native ham_cursor_move function.
     * </p>
     * <p>
     * Returns the record of the current Database item.
     * Throws <code>Const.HAM_CURSOR_IS_NIL</code> if the Cursor does not point to any item.
	 * </p>
     * @return the record of the current item.
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#gabed22a217e561c77850928292409d8b8">C documentation</a>
     * @see Cursor#getRecord()
     */
	public Object getRecord() throws DatabaseException
	{
		byte[] ret = mBaseCursor.getRecord();
		Object result = Ham4DroidUtils.unmarshallValue(ret);
		return result;
	}

    /**
     * <p>
     * Overwrites the current Record.
     * This method wraps the native ham_cursor_overwrite function.
     * </p>
     * <p>
     * This function overwrites the record of the current item.
	 * </p>
     * @param record the new Record of the item.
     * @throws DatabaseException native hamsterdb error.
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#gae9da6fd465aff00e6b0076ba8f203287">C documentation</a>
     * @see Cursor#overwrite(byte[])
     */
    public void update(Object record) throws DatabaseException
    {
    	byte[] bytedata = Ham4DroidUtils.marshallValue(record);
    	mBaseCursor.overwrite(bytedata);
    }

    /**
     * <p>
     * Inserts a Database item and points the Cursor to the inserted item.
     * This method wraps the native ham_cursor_insert function.
     * </p>
     * <p>
     * This function inserts a key/record pair as a new Database item.
     * If the key already exists in the Database, error <code>Const.HAM_DUPLICATE_KEY</code> is thrown.
     * </p>
     * <ul>
     * <li>If you wish to overwrite an existing entry specify the flag <code>Const.HAM_OVERWRITE</code>.
     * <li>If you wish to insert a duplicate key specify the flag <code>Const.HAM_DUPLICATE</code>.
     * 		(Note that the Database has to be created with <code>Const.HAM_ENABLE_DUPLICATES</code>, in order to use duplicate keys.)
     * </ul>
     * <p>
     * After inserting, the Cursor will point to the new item.
     * If inserting the item failed, the Cursor is not modified.
	 * </p>
	 *
     * @param key
     * 	the key of the new item.
     * @param
     * 	record the record of the new item.
     * @param flags
     * 	optional flags for inserting the item, combined with bitwise OR.
     * 	Possible flags are:
     *  <ul>
     *  <li>{@link Const#HAM_OVERWRITE}
     *  	If the key already exists, the record is overwritten. Otherwise, the key is inserted.
     *
     *  <li>{@link Const#HAM_DUPLICATE}
     *  	If the key already exists, a duplicate key is inserted.
     *  	The key is inserted after the already existing duplicates.
     *  	Same as <code>Const.HAM_DUPLICATE_INSERT_LAST</code>.
     *
     *  <li>{@link Const#HAM_DUPLICATE_INSERT_BEFORE}
     *  	If the key already exists, a duplicate key is inserted before the duplicate pointed to by this Cursor.
     *
     *  <li>{@link Const#HAM_DUPLICATE_INSERT_AFTER}
     *  	If the key already exists, a duplicate key is inserted after the duplicate pointed to by this Cursor.
     *
     *  <li>{@link Const#HAM_DUPLICATE_INSERT_FIRST}
     *  	If the key already exists, a duplicate key is inserted as the first duplicate of the current key.
     *
     *  <li>{@link Const#HAM_DUPLICATE_INSERT_LAST}
     *  	If the key already exists, a duplicate key is inserted as the last duplicate of the current key.
     *	</ul>
     * @throws DatabaseException native hamsterdb error.
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#ga4e2861736763d02a2a04f01ee787c37a">C documentation</a>
     * @see Cursor#insert(byte[], byte[], int)
     */
	public void insert(Object[] key, Object record, int flags) throws DatabaseException
	{
		byte[] keydata = Ham4DroidUtils.marshallKeys(key);
		byte[] recorddata = Ham4DroidUtils.marshallValue(record);
		mBaseCursor.insert(keydata, recorddata, flags);
    }

    /**
     * Inserts a Database item and points the Cursor to the inserted item.
     * <p>
     * Note: flags option is {@link Const#HAM_OVERWRITE}.
     * </p>
     * @param key
     * 	the key of the new item.
     * @param
     * 	record the record of the new item.
     * @throws DatabaseException native hamsterdb error.
     *
     * @see Ham4DroidCursor#insert(byte[], byte[], int)
     */
	public void insertOrUpdate(Object[] key, Object record) throws DatabaseException
	{
		this.insert(key, record, Const.HAM_OVERWRITE);
	}

    /**
     * Inserts a Database item and points the Cursor to the inserted item.
     * <p>
     * Note: flags option is {@link Const#HAM_DUPLICATE}.
     * </p>
     * @param key
     * 	the key of the new item.
     * @param
     * 	record the record of the new item.
     * @throws DatabaseException native hamsterdb error.
     *
     * @see Ham4DroidCursor#insert(byte[], byte[], int)
     */
	public void insertOrDuplicates(Object[] key, Object record) throws DatabaseException
	{
		this.insert(key, record, Const.HAM_DUPLICATE);
	}

    /**
     * <p>
     * Erases the current key.
     * This method wraps the native ham_cursor_erase function.
     * </p>
     * <p>
     * Erases a key from the Database.
     * If the erase was successfull, the Cursor is invalidated, and does no longer point to any item.
     * In case of an error, the Cursor is not modified.
     * </p>
     * <p>
     * If the Database was opened with the flag <code>Const.HAM_ENABLE_DUPLICATES</code>,
     * this function erases only the duplicate item to which the Cursor refers.
	 * </p>
	 * @throws DatabaseException native hamsterdb error.
	 *
	 * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#gaf7f093b157f1d98df93fb7358e677cac">C documentation</a>
	 * @see Cursor#erase()
     */
	public void erase() throws DatabaseException
	{
		mBaseCursor.erase();
	}

    /**
     * <p>
     * Returns the number of duplicate keys.
     * This method wraps the native ham_cursor_get_duplicate_count function.
     * </p>
     * <p>
     * Returns the number of duplicate keys of the item to which the Cursor currently refers.<br/>
     * Returns 1 if the key has no duplicates.
	 * </p>
     * @throws DatabaseException native hamsterdb error.
     * @return the number of duplicate keys.
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#ga4f17e3304e9d5cbe30a7798bf719cfc6">C documentation</a>
     * @see Cursor#getDuplicateCount()
     */
    public int getDuplicateCount() throws DatabaseException
    {
    	return mBaseCursor.getDuplicateCount();
    }

    /**
     * <p>
     * Closes the Cursor.
     * This method wraps the native ham_cursor_close function.
     * </p>
     * <p>
     * Closes this Cursor and frees allocated memory.
     * All Cursors should be closed before closing the Database.
	 * </p>
	 *
	 * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__cursor.html#ga260674c75512d6fe2c10b86e82aabf2b">C documentation</a>
	 * @see Cursor#close()
     */
    public void close()
    {
    	try
		{
			mBaseCursor.close();
		}
		catch (DatabaseException e)
		{
			Log.e(TAG, "ERROR: hamsterdb native error. (status: "+Ham4DroidUtils.getConstantsName(e.getErrno())+")");
		}
    }

	/*
	 * private native methods / private fields on Cursor class.
	 */
	/** private Database m_db; */
	private static Field sDatabaseField =
		Ham4DroidUtils.Reflect.getAccessibleField(Cursor.class, "m_db");

    /** private native int ham_cursor_find(long handle, byte[] key, int flags) */
	private static Method sJNI_ham_cursor_find =
		Ham4DroidUtils.Reflect.getAccessibleMethod(Cursor.class, "ham_cursor_find",
			new Class[]{ long.class, byte[].class, int.class });

	/**
	 * Get 'm_db' field value.
	 *
	 * @param c Cursor object.
	 */
	private static Database getDatabaseFieldValue(final Cursor c)
	{
		return (Database) Ham4DroidUtils.Reflect.getFieldValue(c, sDatabaseField);
	}
}
