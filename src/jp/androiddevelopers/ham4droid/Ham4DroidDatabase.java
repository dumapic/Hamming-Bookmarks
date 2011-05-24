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
import de.crupp.hamsterdb.Parameter;
import de.crupp.hamsterdb.Transaction;

/**
 * The wrapper class of hamsterdb Database class({@link Database}) for Android(TM).
 * A part of javadoc/source code is copied from {@link Database} source code and modfied by (@)dumapick.
 *
 * @version 1.0.0
 */
public final class Ham4DroidDatabase
{
	private static final String TAG = "#ham4droid";

	private Database mBaseDB;
	private int mLatestOpenDbFlags;	//we needs ham_get_flags (ham_db_t *db)!!
	private boolean mIsOpen;

	/**
	 * Default constructor.
	 */
	public Ham4DroidDatabase()
	{
		mBaseDB = new Database();
		mLatestOpenDbFlags = 0;
		mIsOpen = false;
	}

	/**
	 * Get hamsterdb.
	 * @return the {@link Database} object.
	 */
	public Database getHamsterDb()
	{
		return mBaseDB;
	}

    /**
     * <p>
     * Creates a hamsterdb database.
     * This method wraps the native ham_create_ex function.
     * </p>
     * Note:
     * <ul>
     * <li>File acces mode is 0664.
     * <li>returning status code. (Modified by (@)dumapick)
     * <li><font color="red">This function using java reflection.</font>
     * </ul>
     *
     * @param filename
     * 	The filename(Should be FullPath) of the Database file.
     * 	If the file already exists, it is overwritten.
     * 	Can be null if you create an In-Memory Database(seealso <code>Const#HAM_IN_MEMORY_DB</code>).
     *
     * @param flags
     * 	Optional flags for this operation, combined with bitwise OR. Default value is zero.<br/>
     * 	Possible flags are:
     *	<ul>
     *	<li>{@link Const#HAM_WRITE_THROUGH} :
     *		Immediately write modified pages to the disk.
     *		This slows down all Database operations,
     *		but may save the Database integrity in case of a system crash.
     *
     *	<li>{@link Const#HAM_USE_BTREE} :
     *		Use a B+Tree for the index structure.
     *		Currently enabled by default,
     *		but future releases of hamsterdb will offer additional index structures, i.e. hash tables.
     *
     *	<li>{@link Const#HAM_DISABLE_VAR_KEYLEN} :
     *		Do not allow the use of variable length keys.
     *      Inserting a key, which is larger than the B+Tree index key size,
     *      returns <code>Const.HAM_INV_KEYSIZE</code>.
     *
     *	<li>{@link Const#HAM_IN_MEMORY_DB} :
     *		Creates an In-Memory Database. No file will be created,
     *      and the Database contents are lost after the Database is closed.
     *      The <code>filename</code> parameter can be null.
     *      Do <b>NOT</b> use in combination with <code>Const.HAM_CACHE_STRICT</code> and
     *      do <b>NOT</b> specify a cache size other than 0.
     *
     *	<li>{@link Const#HAM_RECORD_NUMBER} :
     *		Creates an "auto-increment" Database.
     *		HamDBKeys in Record Number Databases are automatically assigned an incrementing 64bit value.
     *
     *	<li>{@link Const#HAM_ENABLE_DUPLICATES} :
     *		Enable duplicate keys for this Database.
     *		By default, duplicate keys are disabled.
     *
     *	<li>{@link Const#HAM_SORT_DUPLICATES} :
     *		Sort duplicate keys for this Database.
     *		Only allowed in combination with <code>Const.HAM_ENABLE_DUPLICATES</code>.
     *		A compare function can be set with <code>ham_set_duplicate_compare_func</code>.
     *		This flag is not persistent.
     *
     *	<li>{@link Const#HAM_DISABLE_MMAP} :
     *		Do not use memory mapped files for I/O.
     *		By default, hamsterdb checks if it can use mmap, since mmap is faster than read/write.
     *		For performance reasons, this flag should not be used.
     *
     *	<li>{@link Const#HAM_CACHE_STRICT} :
     *		Do not allow the cache to grow larger than the size specified with <code>Const.HAM_PARAM_CACHESIZE</code>.
     *		If a Database operation needs to resize the cache,
     *		it will fail and return <code>Const.HAM_CACHE_FULL</code>.
     *		If the flag is not set, the cache is allowed to allocate more pages than the maximum cache size,
     *		but only if it's necessary and only for a short time.
     *
     *	<li>{@link Const#HAM_CACHE_UNLIMITED} :
     *		Do not limit the cache. Nearly as fast as an In-Memory Database.
     *		Not allowed in combination with HAM_CACHE_STRICT or a limited cache size.
     *
     *	<li>{@link Const#HAM_DISABLE_FREELIST_FLUSH} :
     *		This flag is deprecated.
     *
     *	<li>{@link Const#HAM_LOCK_EXCLUSIVE} :
     *		Place an exclusive lock on the file.
     *		Only one process may hold an exclusive lock for a given file at a given time.
     *		Deprecated - this is now the default
     *
     *	<li>{@link Const#HAM_ENABLE_RECOVERY} :
     *		Enables logging/recovery for this Database.
     *		Not allowed in combination with <code>Const.HAM_IN_MEMORY_DB</code>,
     *		<code>Const.HAM_DISABLE_FREELIST_FLUSH</code> and <code>Const.HAM_WRITE_THROUGH</code>.
     *
     *	<li>{@link Const#HAM_ENABLE_TRANSACTIONS} :
     *		Enables Transactions for this Database.
     *		[Remark] Transactions were introduced in hamsterdb 1.0.4,
     *		but with certain limitations (which will be removed in later version).
     *		Please read the README file and the Release Notes for details.
     *		This flag imples HAM_ENABLE_RECOVERY.
     *	</ul>
     *
     * @param params
     * 	An array of <code>Parameter</code> structures.
     * 	The following parameters are available:
     *	<ul>
     *  <li>{@link Const#HAM_DEFAULT_CACHESIZE}:
     *  	The size of the Database cache, in bytes.
     *  	The default size is defined in src/config.h as HAM_DEFAULT_CACHESIZE - usually 2MB .
     *  <li>{@link Const#HAM_PARAM_PAGESIZE}:
     *  	The size of a file page, in bytes.
     *  	It is recommended not to change the default size.
     *  	The default size depends on hardware and operating system.
     *  	Page sizes must be 1024 or a multiple of 2048.
     *  <li>{@link Const#HAM_PARAM_KEYSIZE}:
     *  	The size of the keys in the B+Tree index. The default size is 21 bytes.
     *  <li>{@link Const#HAM_PARAM_DATA_ACCESS_MODE}:
     *  	Gives a hint regarding data access patterns.
     *  	The default setting optimizes hamsterdb for random read/write access (HAM_DAM_RANDOM_WRITE).
     *  	Use HAM_DAM_SEQUENTIAL_INSERT for sequential inserts (this is automatically set for record number Databases).
     *  	For more information about available DAM (Data Access Mode) flags,
     *  	see hamsterdb Data Access Mode Codes. The DAM is not persistent.
     *  </ul>
     *
     * @return status code from native. see below:
     * <ul>
     * <li>{@link Const#HAM_SUCCESS} upon success.
     * <li>{@link Const#HAM_DATABASE_ALREADY_OPEN} if db is already in use.
     * </ul>
     *
     * @throws DatabaseException hamsterdb native exception. see below:
     * <ul>
     * <li>HAM_INV_PARAMETER - if the db pointer is NULL or an invalid combination of flags was specified.
     * <li>HAM_IO_ERROR - if the file could not be opened or reading/writing failed.
     * <li>HAM_INV_FILE_VERSION - if the Database version is not compatible with the library version.
     * <li>HAM_OUT_OF_MEMORY - if memory could not be allocated.
     * <li>HAM_INV_PAGESIZE - if pagesize is not 1024 or a multiple of 2048.
     * <li>HAM_INV_KEYSIZE - if keysize is too large (at least 4 keys must fit in a page).
     * <li>HAM_WOULD_BLOCK - if another process has locked the file.
     * </ul>
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__database.html#ga24245a31681e5987a0d71c04ff953ea3">C documentation</a>
     * @see {@link Database#create(String, int, int, Parameter[])}
     */
	public synchronized int createDatabase(final String filename, int flags, Parameter[] params) throws DatabaseException
	{
		long handle = mBaseDB.getHandle();

		// make sure that the parameters don't have a NULL-element
		if (params!=null)
		{
			for (int i = 0; i < params.length; ++i)
			{
                if (params[i] == null)
                	throw new NullPointerException("params["+i+"] argument is null.");
			}
        }

		if (handle == 0)
		{
			handle = (Long) Ham4DroidUtils.Reflect.invokeMethod(mBaseDB, sJNI_ham_new, new Object[]{});
			setHandleFieldValue(mBaseDB, handle);
		}

        //int status = ham_create_ex(handle, filename, flags, 0664, params);
		int status = (Integer) Ham4DroidUtils.Reflect.invokeMethod(mBaseDB,
			sJNI_ham_create_ex, new Object[]{ handle, filename, flags, 0664, params});

		mIsOpen = false;

        switch(status)
        {
        	case Const.HAM_SUCCESS:
        		mLatestOpenDbFlags = flags;
        		mIsOpen = true;
        		Log.i(TAG, "hamsterdb created. ("+filename+")");
        		return status;

        	case Const.HAM_DATABASE_ALREADY_OPEN:
        		mIsOpen = true;
        		Log.i(TAG, "hamsterdb already opened. ("+filename+")");
        		return status;

        	default:
        		Log.e(TAG, "ERROR: hamsterdb native error. (status: "+Ham4DroidUtils.getConstantsName(status)+")");
        		throw new DatabaseException(status);
        }
	}

	/**
     * <p>
     * Creates a hamsterdb database.
     * </p>
     * Note:
     * <ul>
     * <li>File acces mode is 0664.
     * <li>Paramters is not specified.
     * <li>returning status code. (Modified by (@)dumapick)
     * <li><font color="red">This function using java reflection.</font>
     * </ul>
	 * @param filename The filename(Should be FullPath) of the Database file.
	 * @param flags Optional flags for this operation, combined with bitwise OR.
     * @return status code from native.
     * @throws DatabaseException hamsterdb native exception.
     *
     * @see Ham4DroidDatabase#createDatabase(String, int, Parameter[])
	 */
	public int createDatabase(final String filename, int flags) throws DatabaseException
	{
		return this.createDatabase(filename, flags, null);
	}

    /**
     * <p>
     * Opens an existing Database.
     * This method wraps the native <code>ham_open_ex</code> function.
     * </p>
     * Note:
     * <ul>
     * <li>returning status code. (Modified by (@)dumapick)
     * <li><font color="red">This function using java reflection.</font>
     * </ul>
     *
     * @param filename
     * 	The filename(Should be FullPath) of the Database file.
     * @param flags
     * 	Optional flags for this operation, combined with bitwise OR. Default value is zero.<br/>
     * 	Possible flags are:
     *	<ul>
     *  <li>{@link Const#HAM_READ_ONLY} :
     *		Opens the file for reading only.
     *		Operations which need write access (i.e. <code>Database.insert</code>) will return
     *		<code>Const.HAM_DB_READ_ONLY</code>.
     *
     *	<li>{@link Const#HAM_WRITE_THROUGH} :
     *		Immediately write modified pages to the disk.
     *		This slows down all Database operations,
     *		but could save the Database integrity in case of a system crash.
     *
     *	<li>{@link Const#HAM_DISABLE_VAR_KEYLEN} :
     *		Do not allow the use of variable length keys.
     *		Inserting a key, which is larger than the B+Tree index key size,
     *		returns <code>Const.HAM_INV_KEYSIZE</code>.
     *
     *	<li>{@link Const#HAM_DISABLE_MMAP} :
     *		Do not use memory mapped files for I/O.
     *		By default, hamsterdb checks if it can use mmap, since mmap is faster than read/write.
     *		For performance reasons, this flag should not be used.
     *
     *  <li>{@link Const#HAM_CACHE_STRICT} :
     *		Do not allow the cache to grow larger than the cache size.
     *		If a Database operation needs to resize the cache,
     *		it will fail and return <code>Const.HAM_CACHE_FULL</code>.
     *		If the flag is not set, the cache is allowed to allocate more pages than the maximum cache size,
     *		but only if it's necessary and only for a short time.
     *
     *	<li>{@link Const#HAM_CACHE_UNLIMITED} :
     *		Do not limit the cache. Nearly as fast as an In-Memory Database.
     *		Not allowed in combination with HAM_CACHE_STRICT or a limited cache size.
     *
     *	<li>{@link Const#HAM_DISABLE_FREELIST_FLUSH} :
     *		This flag is deprecated.
     *
     *	<li>{@link Const#HAM_LOCK_EXCLUSIVE} :
     *		Place an exclusive lock on the file.
     *		Only one process may hold an exclusive lock for a given file at a given time.
     *		Deprecated - this is now the default
     *
     *	<li>{@link Const.HAM_ENABLE_RECOVERY} :
     *		Enables logging/recovery for this Database.
     *		Will return <code>Const.HAM_NEED_RECOVERY</code>,
     *		if the Database is in an inconsistent state.
     *		Not allowed in combination with <code>Const.HAM_IN_MEMORY_DB</code>, <code>Const.HAM_DISABLE_FREELIST_FLUSH</code> and
     *		<code>Const.HAM_WRITE_THROUGH</code>.
     *
     *	<li>{@link Const#HAM_AUTO_RECOVERY} :
     *		Automatically recover the Database, if necessary.
     *		This flag implies <code>Const.HAM_ENABLE_RECOVERY</code>.
     *
     *	<li>{@link Const#HAM_SORT_DUPLICATES} :
     *		Sort duplicate keys for this Database.
     *		Only allowed if the Database was created with the flag HAM_ENABLE_DUPLICATES.
     *		A compare function can be set with ham_set_duplicate_compare_func. This flag is not persistent.
     *
     * 	<li>{@link Const#HAM_ENABLE_DUPLICATES} :
     * 		Is't support? Sorry, it's unknown..
     *
     *	<li>{@link Const#HAM_ENABLE_TRANSACTIONS} :
     *		Enables Transactions for this Database.
     *		[Remark] Transactions were introduced in hamsterdb 1.0.4,
     *		but with certain limitations (which will be removed in later version).
     *		Please read the README file and the Release Notes for details.
     *		This flag imples HAM_ENABLE_RECOVERY.
     *	</ul>
     *
     * @param params
     * 	An array of <code>Parameter</code> structures. The following parameters are available:
     *	<ul>
     *  <li>{@link Const#HAM_PARAM_CACHESIZE}:
     *  	The size of the Database cache, in bytes.
     *  	The default size is defined in src/config.h as HAM_DEFAULT_CACHESIZE - usually 2MB.
     *  <li>{@link Const#HAM_PARAM_DATA_ACCESS_MODE}:
     *  	 Gives a hint regarding data access patterns.
     *  	The default setting optimizes hamsterdb for random read/write access (HAM_DAM_RANDOM_WRITE).
     *  	Use HAM_DAM_SEQUENTIAL_INSERT for sequential inserts (this is automatically set for record number Databases).
     *  	Data Access Mode hints can be set for individual Databases,
     *  	too (see also ham_create_ex) but are applied globally to all Databases within a single Environment.
     *  	 For more information about available DAM (Data Access Mode) flags,
     *  	see hamsterdb Data Access Mode Codes. The DAM is not persistent.
     *  </ul>
     *
     * @return status code from native. see below:
     * <ul>
     * <li>{@link Const#HAM_SUCCESS} upon success.
     * <li>{@link Const#HAM_DATABASE_ALREADY_OPEN} if db is already in use.
     * </ul>
     *
     * @throws DatabaseException hamsterdb native exception. see below:
     * <ul>
     * <li>HAM_INV_PARAMETER - if the db pointer is NULL or an invalid combination of flags was specified.
     * <li>HAM_FILE_NOT_FOUND - if the file does not exist.
     * <li>HAM_IO_ERROR - if the file could not be opened or reading failed.
     * <li>HAM_INV_FILE_VERSION  - if the Database version is not compatible with the library version.
     * <li>HAM_OUT_OF_MEMORY - if memory could not be allocated.
     * <li>HAM_WOULD_BLOCK - if another process has locked the file.
     * <li>HAM_NEED_RECOVERY - if the Database is in an inconsistent state.
     * <li>HAM_LOG_INV_FILE_HEADER - if the logfile is corrupt.
     * </ul>
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__database.html#ga7b1ae31472d71ae215249295457d23f9">C documentation</a>
     * @see {@link Database#open(String, int, Parameter[])}
     */
	public synchronized int openDatabase(final String filename, final int flags, final Parameter[] params) throws DatabaseException
	{

		long handle = mBaseDB.getHandle();

		// make sure that the parameters don't have a NULL-element
		if (params!=null)
		{
			for (int i = 0; i < params.length; ++i)
			{
                if (params[i] == null)
                	throw new NullPointerException("params["+i+"] argument is null.");
			}
        }

		if (handle == 0)
		{
			//handle = ham_new();
			handle = (Long) Ham4DroidUtils.Reflect.invokeMethod(mBaseDB, sJNI_ham_new, new Object[]{});
			setHandleFieldValue(mBaseDB, handle);
		}

        //int status = ham_open_ex(handle, filename, flags, params);
		int status = (Integer) Ham4DroidUtils.Reflect.invokeMethod(mBaseDB,
			sJNI_ham_open_ex, new Object[]{ handle, filename, flags, params });

		mIsOpen = false;

        switch(status)
        {
        	case Const.HAM_SUCCESS:
        		mLatestOpenDbFlags = flags;
        		mIsOpen = true;
        		Log.i(TAG, "hamsterdb opened. ("+filename+")");
        		return status;

        	case Const.HAM_DATABASE_ALREADY_OPEN:
        		mIsOpen = true;
        		Log.i(TAG, "hamsterdb already opened. ("+filename+")");
        		return status;

        	default:
        		Log.e(TAG, "ERROR: hamsterdb native error. (status: "+Ham4DroidUtils.getConstantsName(status)+")");
        		throw new DatabaseException(status);
        }
	}

	/**
     * <p>
     * Opens an existing Database.
     * </p>
     * Note:
     * <ul>
     * <li>Parameters not specified.
     * <li>returning status code. (Modified by (@)dumapick)
     * <li><font color="red">This function using java reflection.</font>
     * </ul>
	 * @param filename The filename(Should be FullPath) of the Database file.
	 * @param flags Optional flags for this operation, combined with bitwise OR.
	 * @return status code from native.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see {@link Ham4DroidDatabase#openDatabase(String, int, Parameter[])}
	 */
	public int openDatabase(final String filename, final int flags) throws DatabaseException
	{
		return this.openDatabase(filename, flags, null);
	}

    /**
     * <p>
     * Closes the Database.
     * This function flushes the Database and then closes the file handle.
     * This method wraps the native <code>ham_close</code> function.
     * </p>
     * Note:
     * <ul>
     * <li>returning status code. (Modified by (@)dumapick)
     * <li><font color="red">This function using java reflection.</font>
     * </ul>
     *
     * @param flags Optional flags for closing the Database. Possible values are:
     * <ul>
     * <li>{@link Const#HAM_AUTO_CLEANUP}:
     * 		Automatically closes all open Cursors.
     * 		This flag already have been set(This means that this flag not required).
     * <li>{@link Const#HAM_TXN_AUTO_COMMIT}:
     * 		Automatically commit all open Transactions.
     * <li>{@link Const#HAM_TXN_AUTO_ABORT}:
     * 		Automatically abort all open Transactions; this is the default behaviour.
     * </ul>
     *
     * @return return status code. see below:
     * <ul>
     * <li>{@link Const#HAM_SUCCESS}:
     * 		Already closed or Closing database was successful.
     * <li>{@link Const#HAM_INV_PARAMETER}:
     * 		if db is NULL.
     * <li>{@link Const#HAM_CURSOR_STILL_OPEN}
     * 		if not all Cursors of this Database were closed, and HAM_AUTO_CLEANUP was not specified.
     * </ul>
     *
     * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__Database__cfg__parameters.html#gac0e1e492c2b36e2ae0e87d0c0ff6e04e">C documentation</a>
     * @see {@link Database#close(int)}
     */
	public synchronized int closeDatabase(int flags)
	{
		long handle = mBaseDB.getHandle();

        if (handle == 0)
        {
        	Log.i(TAG, "this hamsterdb already closed.");
        	return Const.HAM_SUCCESS;
        }

		//int closeStatus = ham_close(handle, flags | Const.HAM_AUTO_CLEANUP);
		//ham_delete(handle);
        int status = (Integer) Ham4DroidUtils.Reflect.invokeMethod(mBaseDB,
        	sJNI_ham_close, new Object[]{ handle, flags | Const.HAM_AUTO_CLEANUP });
        Ham4DroidUtils.Reflect.invokeMethod(mBaseDB, sJNI_ham_delete, new Object[]{ handle });
		setHandleFieldValue(mBaseDB, 0L);

		switch(status)
		{
        	case Const.HAM_SUCCESS:
        		mIsOpen = false;
        		Log.i(TAG, "hamsterdb closed.");
        		break;

        	case Const.HAM_INV_PARAMETER:
        		mIsOpen = false;
        		Log.e(TAG, "hamsterdb is null.");
        		break;

        	case Const.HAM_CURSOR_STILL_OPEN:
        		Log.e(TAG, "hamsterdb cursor is still opened.");
        		break;

        	default:
        		Log.e(TAG, "ERROR: hamsterdb native error. (status: "+Ham4DroidUtils.getConstantsName(status)+")");
        }

		return status;
	}

    /**
     * <p>
     * Closes the Database.
     * </p>
     * Note:
     * <ul>
     * <li>flags not specified. Force use default flag(by zero).
     * <li>returning status code. (Modified by (@)dumapick)
     * <li><font color="red">This function using java reflection.</font>
     * </ul>
     * @return return status code.
     * @see {@link Ham4DroidDatabase#closeDatabase(int)}
     */
	public int closeDatabase()
	{
		return closeDatabase(0);
	}

    /**
     * Flushes the Database.
     * This method wraps the native ham_flush function.
     * <p>
	 * This function flushes the Database cache and writes the whole file to disk.
	 * If this Database was opened in an Environment,
	 * all other Databases of this Environment are flushed as well.
     * </p>
     * <p>
     * Since In-Memory Databases do not have a file on disk, the
     * function will have no effect and will return successfully({@link Const#HAM_SUCCESS}).
     * </p>
     * Note:
     * <ul>
     * <li>returning status code. (Modified by (@)dumapick)
     * <li><font color="red">This function using java reflection.</font>
     * </ul>
     *
     * @return return status code.
     *
	 * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__Database__cfg__parameters.html#gad4db2642d606577e23808ed8e35b7d30">C documentation</a>
	 * @see {@link Database#flush()}
     */
	public synchronized int flush()
	{
		long handle = mBaseDB.getHandle();

        int status = (Integer) Ham4DroidUtils.Reflect.invokeMethod(mBaseDB, sJNI_ham_flush, new Object[]{ handle, 0 });

        switch(status)
        {
        	case Const.HAM_SUCCESS:
        		Log.i(TAG, "hamsterdb is flushed.");
        		break;
        	default:
        		Log.e(TAG, "ERROR: hamsterdb native error. (status: "+Ham4DroidUtils.getConstantsName(status)+")");
        }

        return status;
	}

    /**
     * <p>
     * Inserts a Database item.
     * This method wraps the native <code>ham_insert</code> function.
     * </p>
     * <p>
     * This function inserts a key/record pair as a new Database item.
     * If the key already exists in the Database, error code {@link Const#HAM_DUPLICATE_KEY} is thrown.
     * <ul>
     * <li>If you wish to overwrite an existing entry specify the flag <code>Const.HAM_OVERWRITE</code>.
     * <li>If you wish to insert a duplicate key specify the flag <code>Const.HAM_DUPLICATE</code>.
     * (Note that the Database has to be created with <code>Const.HAM_ENABLE_DUPLICATES</code> in order to use duplicate keys.)<br/>
     * The duplicate key is inserted after all other duplicate keys (see <code>Const.HAM_DUPLICATE_INSERT_LAST</code>).
     * </ul>
     * </p>
     * @param txn
     * 	the (optional) Transaction. Null is specified if it doesn't use it.
     * @param key
     * 	the key of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the key is null, <code>NullPointerException</code> is thrown.
     * @param record
     * 	the record of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the record is null, <code>NullPointerException</code> is thrown.
     * @param flags
     * 	Default value is Zero. This argument is optional flags for inserting. <br/>
     * 	Possible flags are:
     *	<ul>
     *  <li>Zero(0):
     *  	If the key already exists in the Database, error code <code>Const.HAM_DUPLICATE_KEY</code> is thrown.
     *  	Otherwise, the key is inserted.
     *  <li>{@link Const#HAM_OVERWRITE}:
     *  	If the key already exists, the record is overwritten.
     *  	Otherwise, the key is inserted.
     *  <li>{@link Const#HAM_DUPLICATE}:
     *  	If the key already exists, a duplicate key is inserted.
     *  	The key is inserted before the already existing duplicates.
     *  </ul>
     * @throws DatabaseException hamsterdb native exception. see below:
     * <ul>
     * <li>HAM_INV_PARAMETER - if db, key or record is NULL.
     * <li>HAM_INV_PARAMETER - if the Database is a Record Number Database and the key is invalid (see above).
     * <li>HAM_INV_PARAMETER - if HAM_PARTIAL was specified AND duplicate sorting is enabled (HAM_SORT_DUPLICATES).
     * <li>HAM_INV_PARAMETER - if the flags HAM_OVERWRITE and HAM_DUPLICATE were specified, or if HAM_DUPLICATE was specified, but the Database was not created with flag HAM_ENABLE_DUPLICATES.
     * <li>HAM_INV_PARAMETER - if HAM_PARTIAL is specified and record->partial_offset+record->partial_size exceeds the record->size.
     * <li>HAM_DB_READ_ONLY - if you tried to insert a key in a read-only Database.
     * <li>HAM_INV_KEYSIZE - if the key size is larger than the keysize parameter specified for ham_create_ex and variable key sizes are disabled (see HAM_DISABLE_VAR_KEYLEN) OR if the keysize parameter specified for ham_create_ex is smaller than 8.
     * </ul>
     *
	 * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__Database__cfg__parameters.html#ga5bb99ca3c41f069db310123253c1c1fb">C documentation</a>
	 * @see {@link Database#insert(Transaction, byte[], byte[], int)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
     */
	public void rawInsert(final Transaction txn, final Object[] key, final Object record, final int flags) throws DatabaseException
	{
		byte[] key2 = Ham4DroidUtils.marshallKeys(key);
		byte[] record2 = Ham4DroidUtils.marshallValue(record);
		mBaseDB.insert(txn, key2, record2, flags);
	}

	/**
     * <p>
     * Inserts a Database item.
     * </p>
     * Note:
     * <ul>
     * <li>Transaction not specified.
     * </ul>
     *
     * @param key
     * 	the key of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the key is null, <code>NullPointerException</code> is thrown.
     *  And if the key already exists in the Database, error code <code>Const.HAM_DUPLICATE_KEY</code> is thrown.
     * @param record
     * 	the record of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the record is null, <code>NullPointerException</code> is thrown.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see {@link Ham4DroidDatabase#rawInsert(Transaction, Object, Object, int)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public void insert(final Object[] key, final Object record) throws DatabaseException
	{
		this.rawInsert(null, key, record, 0);
	}

	/**
     * <p>
     * Inserts a Database item.
     * </p>
     *
     * @param txn
     * 	the (optional) Transaction. Null is specified if it doesn't use it.
     * @param key
     * 	the key of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the key is null, <code>NullPointerException</code> is thrown.
     *  And if the key already exists in the Database, error code <code>Const.HAM_DUPLICATE_KEY</code> is thrown.
     * @param record
     * 	the record of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the record is null, <code>NullPointerException</code> is thrown.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see {@link Ham4DroidDatabase#rawInsert(Transaction, Object, Object, int)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public void insert(final Transaction txn, final Object[] key, final Object record) throws DatabaseException
	{
		this.rawInsert(txn, key, record, 0);
	}

	/**
     * <p>
     * Inserts a Database item.
     * </p>
     * Note:
     * <ul>
     * <li>Transaction not specified.
     * <li>If you use this function, the Database has to be created with <code>Const.HAM_ENABLE_DUPLICATES</code>
     * in order to use duplicate keys.<br/>
     * </ul>
     *
     * @param key
     * 	the key of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the key is null, <code>NullPointerException</code> is thrown.
     *  If the key already exists in the Database, The duplicate key is inserted after all other duplicate keys.
     * @param record
     * 	the record of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the record is null, <code>NullPointerException</code> is thrown.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see {@link Ham4DroidDatabase#rawInsert(Transaction, Object, Object, int)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public void insertOrDuplicates(final Object[] key, final Object record) throws DatabaseException
	{
		this.rawInsert(null, key, record, Const.HAM_DUPLICATE);
	}

	/**
     * <p>
     * Inserts a Database item.
     * </p>
     * Note:
     * <ul>
     * <li>If you use this function, the Database has to be created with <code>Const.HAM_ENABLE_DUPLICATES</code>
     * in order to use duplicate keys.<br/>
     * </ul>
     *
     * @param txn
     * 	the (optional) Transaction. Null is specified if it doesn't use it.
     * @param key
     * 	the key of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the key is null, <code>NullPointerException</code> is thrown.
     *  If the key already exists in the Database, The duplicate key is inserted after all other duplicate keys.
     * @param record
     * 	the record of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the record is null, <code>NullPointerException</code> is thrown.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see {@link Ham4DroidDatabase#rawInsert(Transaction, Object, Object, int)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public void insertOrDuplicates(final Transaction txn, final Object[] key, final Object record) throws DatabaseException
	{
		this.rawInsert(txn, key, record, Const.HAM_DUPLICATE);
	}

	/**
     * <p>
     * Inserts a Database item.
     * </p>
     * Note:
     * <ul>
     * <li>Transaction not specified.
     * </ul>
     *
     * @param key
     * 	the key of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the key is null, <code>NullPointerException</code> is thrown.
     *  If the key already exists in the Database, it will be overwrite an existing entry.
     * @param record
     * 	the record of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the record is null, <code>NullPointerException</code> is thrown.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see {@link Ham4DroidDatabase#rawInsert(Transaction, Object, Object, int)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public void insertOrUpdate(final Object[] key, final Object record) throws DatabaseException
	{
		this.rawInsert(null, key, record, Const.HAM_OVERWRITE);
	}

	/**
     * <p>
     * Inserts a Database item.
     * </p>
     *
     * @param txn
     * 	the (optional) Transaction. Null is specified if it doesn't use it.
     * @param key
     * 	the key of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the key is null, <code>NullPointerException</code> is thrown.
     *  If the key already exists in the Database, it will be overwrite an existing entry.
     * @param record
     * 	the record of the new item. This value must be Parcerable/Primitive/Serializable.
     * 	If the record is null, <code>NullPointerException</code> is thrown.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see {@link Ham4DroidDatabase#rawInsert(Transaction, Object, Object, int)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public void insertOrUpdate(final Transaction txn, final Object key[], final Object record) throws DatabaseException
	{
		this.rawInsert(txn, key, record, Const.HAM_OVERWRITE);
	}

	/**
	 *
	 * Update a Database item.
     * Note:
     * <ul>
     * <li>Transaction not specified.
     * <li>This function is bad performance.
     * </ul>
	 *
     * @param key
     * 	the key of the update item. This value must be Parcerable/Primitive/Serializable.
     * 	If the key is null, <code>NullPointerException</code> is thrown.
     *  If the key not exists in the Database, <code>IllegalArgumentException</code> is thrown.
     * @param record
     * 	the record of the update item. This value must be Parcerable/Primitive/Serializable.
     * 	If the record is null, <code>NullPointerException</code> is thrown.
	 * @throws DatabaseException hamsterdb native exception.
	 * @throws IllegalArgumentException When entry not found.
	 *
	 * @see {@link Ham4DroidDatabase#rawInsert(Transaction, Object, Object, int)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public void update(final Object[] key, final Object record) throws DatabaseException
	{
		Object found = this.find(key);
		if(found != null)
		{
			this.rawInsert(null, key, record, Const.HAM_OVERWRITE);
		}
		throw new IllegalArgumentException("Not found key-values entry.");
	}

	/**
	 *
	 * Update a Database item.
     * Note:
     * <ul>
     * <li>This function is bad performance.
     * </ul>
	 *
     * @param txn
     * 	the (optional) Transaction. Null is specified if it doesn't use it.
     * @param key
     * 	the key of the update item. This value must be Parcerable/Primitive/Serializable.
     * 	If the key is null, <code>NullPointerException</code> is thrown.
     *  If the key not exists in the Database, <code>IllegalArgumentException</code> is thrown.
     * @param record
     * 	the record of the update item. This value must be Parcerable/Primitive/Serializable.
     * 	If the record is null, <code>NullPointerException</code> is thrown.
	 * @throws DatabaseException hamsterdb native exception.
	 * @throws IllegalArgumentException When entry not found.
	 *
	 * @see {@link Ham4DroidDatabase#rawInsert(Transaction, Object, Object, int)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public void update(final Transaction txn, final Object[] key, final Object record) throws DatabaseException
	{
		Object found = this.find(key);
		if(found != null)
		{
			this.rawInsert(txn, key, record, Const.HAM_OVERWRITE);
		}
		throw new IllegalArgumentException("Not found key-values entry.");
	}

    /**
     * <p>
     * Searches an item in the Database, returns the record.
     * This method wraps the native <code>ham_find</code> function.
     * </p>
     * <p>
     * This method wraps the native ham_find function.
     * This function searches the Database for a key.
     * If the key is found, the method will return the record of this item.
     * </p>
     * <code>Database.find</code> can not search for duplicate keys.
     * If the key has multiple duplicates, only the first duplicate is returned.
	 * </p>
	 *
     * @param txn the (optional) Transaction. Null is specified if it doesn't use it.
     * @param key the key of the item. This value must be Parcerable/Primitive/Serializable.
     * @return the record of the item.
     * @throws DatabaseException hamsterdb native exception. see below:
     * <ul>
     * <li>HAM_INV_PARAMETER - if db, key or record is NULL.
     * <li>HAM_INV_PARAMETER - if HAM_DIRECT_ACCESS is specified, but the Database is not an In-Memory Database.
     * <li>HAM_KEY_NOT_FOUND - if the key does not exist
     * </ul>
     *
     * @see More information: <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__Database__cfg__parameters.html#ga1385b79dab227fda11bbc80ceb929233">C documentation</a>
     * @see {@link Database#find(Transaction, byte[])}
     * @see {@link android.os.Parcel#writeValue(Object)}
     */
	public Object find(final Transaction txn, final Object[] key) throws DatabaseException
	{
		byte[] key2 = Ham4DroidUtils.marshallKeys(key);
		byte[] record = null;
		try
		{
			record = mBaseDB.find(txn, key2);
		}
		catch (DatabaseException e)
		{
			if(e.getErrno() != Const.HAM_KEY_NOT_FOUND)
			{
				Log.e(TAG, "ERROR: hamsterdb native error. (status: "+Ham4DroidUtils.getConstantsName(e.getErrno())+")");
				throw e;
			}
		}

		return Ham4DroidUtils.unmarshallValue(record);
	}

	/**
     * <p>
     * Searches an item in the Database, returns the record.
     * </p>
	 *
	 * @param key the key of the item. This value must be Parcerable/Primitive/Serializable.
	 * @return the record of the item.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see {@link Ham4DroidDatabase#find(Transaction, Object)}
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public Object find(final Object[] key) throws DatabaseException
	{
		return this.find(null, key);
	}

    /**
     * <p>
     * Erases a Database item.
     * This method wraps the native <code>ham_erase</code> function.
     * </p>
     * <p>
     * This function erases a Database item.
     * If the item with the specified key does not exist, <code>Const.HAM_KEY_NOT_FOUND</code> is thrown.
     * </p>
     * <p>
     * Note that this method can not erase a single duplicate key.
     * If the key has multiple duplicates, all duplicates of this key will be erased.
     * Use <code>Cursor.erase</code> to erase a specific duplicate key.
     * </p>
     *
     * @param txn the (optional) Transaction. Null is specified if it doesn't use it.
     * @param key the key of the item. This value must be Parcerable/Primitive/Serializable.
     * @throws DatabaseException hamsterdb native exception.
     *
     * @see More information: <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__Database__cfg__parameters.html#ga79acbb3f8c06f28b089b9d86cae707db">C documentation</a>
     * @see {@link Cursor#erase}
     * @see {@link android.os.Parcel#writeValue(Object)}
     */
	public void erase(final Transaction txn, final Object[] key) throws DatabaseException
	{
		byte[] key2 = Ham4DroidUtils.marshallKeys(key);
		mBaseDB.erase(txn, key2);
	}

	/**
	 * <p>
	 * This function erases a Database item.
	 * </p>
	 *
	 * @param key the key of the item. This value must be Parcerable/Primitive/Serializable.
	 * @throws DatabaseException hamsterdb native exception.
	 * @see {@link Ham4DroidDatabase#erase(Transaction, Object)}
	 */
	public void erase(final Object[] key) throws DatabaseException
	{
		this.erase(null, key);
	}

    /**
     * <p>
     * Begins a new Transaction.<br/>
     * Note that it is not possible to create multiple Transactions in parallel.<br/>
     * This limitation will be removed in further versions of hamsterdb.<br/>
     * This method wraps the native ham_txn_begin function.
     * </p>
	 *
	 * @param flags Optional flags for beginning the Transaction, combined with bitwise OR. Possible flags are:
	 * <ul>
	 * <li>{@link Const#HAM_TXN_READ_ONLY}:
	 * 		This Transaction is read-only and will not modify the Database.
	 * </ul>
	 *
	 * @return the {@link Transaction} object.
	 * @throws DatabaseException hamsterdb native exception.
	 *
	 * @see <a href="http://hamsterdb.com/public/scripts/html_www/group__ham__txn.html#ga680a26a4ed8fea77a8cafc53d2850055">C documentation</a>
	 * @see {@link Database#begin(int)}
     */
	public Transaction begin(int flags) throws DatabaseException
	{
		return mBaseDB.begin(flags);
    }

    /**
     * Begins a new Transaction.
     *
     * @see {@link Ham4DroidDatabase#begin(int)}
     */
    public Transaction begin() throws DatabaseException
    {
        return this.begin(0);
    }

	/**
	 * Get createDatabase/openDatabase flags.
	 * This function is custom method.
	 *
	 * @return database flags.
	 */
	public int getOpenOrCreateDbFlags()
	{
		return mLatestOpenDbFlags;
	}

	/**
	 * Get the database is opened.
	 * @return If database is opened, return true. otherwise return false.
	 */
	public boolean isOpened()
	{
		return mIsOpen;
	}

	/*
	 * private native methods / private fields on Database class.
	 */

	/** private long m_handle */
	private static Field sHandleField =
		Ham4DroidUtils.Reflect.getAccessibleField(Database.class, "m_handle");

	/** private native long ham_new() */
	private static Method sJNI_ham_new =
		Ham4DroidUtils.Reflect.getAccessibleMethod(Database.class, "ham_new", new Class[]{});
	/** private native void ham_delete(long); */
	private static Method sJNI_ham_delete =
		Ham4DroidUtils.Reflect.getAccessibleMethod(Database.class, "ham_delete", new Class[]{ long.class });

	/** private native int ham_create_ex(long, String, int, int, Parameter[]) */
	private static Method sJNI_ham_create_ex =
		Ham4DroidUtils.Reflect.getAccessibleMethod(Database.class, "ham_create_ex",
		new Class[]{ long.class, String.class, int.class, int.class, Parameter[].class });
	/** private native int ham_open_ex(long, String, int, Parameter[]) */
	private static Method sJNI_ham_open_ex =
		Ham4DroidUtils.Reflect.getAccessibleMethod(Database.class, "ham_open_ex",
		new Class[]{ long.class, String.class, int.class, Parameter[].class });
	/** private native long ham_close(long, int) */
	private static Method sJNI_ham_close =
		Ham4DroidUtils.Reflect.getAccessibleMethod(Database.class, "ham_close", new Class[]{ long.class, int.class });

	/** private native int ham_flush(long, int); */
	private static Method sJNI_ham_flush =
		Ham4DroidUtils.Reflect.getAccessibleMethod(Database.class, "ham_flush", new Class[]{ long.class, int.class });

	/**
	 * Set 'm_handle' field value.
	 *
	 * @param db the Database object.
	 * @param handle new m_handle value.
	 */
	private static void setHandleFieldValue(final Database db, final long handle)
	{
		Ham4DroidUtils.Reflect.setFieldValue(db, sHandleField, handle);
	}
}
