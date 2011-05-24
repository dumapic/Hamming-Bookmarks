/*
 * Copyright(C) 2011 - DUMAPIC androdev.debkit libraries.
 *
 * @author DUMAPIC
 */
package jp.androdev.debkit.util;

import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.text.TextUtils.StringSplitter;

/**
 * Implementation class of Android logger.
 */
public final class Log
{
	/**
	 * Log level constants.
	 */
	public static final class Level
	{
		public static final int VERBOSE 	= 0x00000001;
		public static final int DEBUG 	= 0x00000002;
		public static final int INFO 		= 0x00000004;
		public static final int WARN 		= 0x00000008;
		public static final int ERROR 	= 0x00000010;

		public static final int DISABLED 	= 0x00000000;
		public static final int ALL 		= VERBOSE | DEBUG | INFO | WARN | ERROR;
	}

	/**
	 * Current enabled log level.
	 *
	 * @see {@link Level}
	 */
	private static int TARGET_LOG_LEVEL = Level.DISABLED;
	/** metadata name for debugging log level */
	private static final String METANAME_LOGLEVEL_DEBUG = "loglevel.debug";
	/** metadata name for released log level */
	private static final String METANAME_LOGLEVEL_RELEASE = "loglevel.release";

	/**
	 * Direct set log level.
	 */
	public static void setLogLevel(int level)
	{
		Log.TARGET_LOG_LEVEL = level;
	}

	/**
	 * Set log level with application metadata in AndroidManifest.xml.
	 */
	public static void setLogLevel(ApplicationInfo appInfo)
	{
		if(appInfo == null || appInfo.metaData == null)
		{
			System.err.println("Can not set up log level. So the logger is automatically disabled(null argument).");
			Log.TARGET_LOG_LEVEL = Log.Level.DISABLED;
			return;
		}

		boolean isDebuggable = ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE);
		String logLevelMeta = null;
		Log.TARGET_LOG_LEVEL = Log.Level.DISABLED;

		// If debug mode...
		if(isDebuggable)
		{
			if(appInfo.metaData.containsKey(METANAME_LOGLEVEL_DEBUG))
			{
				logLevelMeta = appInfo.metaData.getString(METANAME_LOGLEVEL_DEBUG);
			}
		}
		// release mode
		else
		{
			if(appInfo.metaData.containsKey(METANAME_LOGLEVEL_RELEASE))
			{
				logLevelMeta = appInfo.metaData.getString(METANAME_LOGLEVEL_RELEASE);
			}
		}

		if(logLevelMeta == null || logLevelMeta.trim().length() == 0)
		{
			Log.TARGET_LOG_LEVEL = Log.Level.DISABLED;
			return;
		}

		StringSplitter splitter = new TextUtils.SimpleStringSplitter('|');
		splitter.setString(logLevelMeta);
		for(String each : splitter)
		{
			String lv = (each != null ? each.trim() : "");
			if("VERBOSE".equalsIgnoreCase(lv))
			{
				Log.TARGET_LOG_LEVEL |= Log.Level.VERBOSE;
			}
			else if("DEBUG".equalsIgnoreCase(lv))
			{
				Log.TARGET_LOG_LEVEL |= Log.Level.DEBUG;
			}
			else if("INFO".equalsIgnoreCase(lv))
			{
				Log.TARGET_LOG_LEVEL |= Log.Level.INFO;
			}
			else if("WARN".equalsIgnoreCase(lv))
			{
				Log.TARGET_LOG_LEVEL |= Log.Level.WARN;
			}
			else if("ERROR".equalsIgnoreCase(lv))
			{
				Log.TARGET_LOG_LEVEL |= Log.Level.ERROR;
			}
		}

		android.util.Log.i("HamBookmarks", "LogLevel:"+Log.TARGET_LOG_LEVEL);
	}

	/**
	 * Check loggable.
	 *
	 * @param level value of {@link Level}
	 * @return If it's loggable, return true.
	 */
	public static boolean isLoggable(int level)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED)
			return false;

		if(TARGET_LOG_LEVEL == Level.ALL)
			return true;

		if(level != Level.VERBOSE
		&& level != Level.DEBUG
		&& level != Level.INFO
		&& level != Level.WARN
		&& level != Level.ERROR)
			throw new IllegalArgumentException("Illegal argument(level). -->"+level);

		if(TARGET_LOG_LEVEL == level)
			return true;

		if((TARGET_LOG_LEVEL & level) == level)
			return true;

		return false;
	}


	/**
	 *
	 * Output logs.
	 *
	 * @param level
	 * 	Log level. ({@link Level} constants.)
	 * @param stackTrace
	 * 	Stacktrace where the log call occurs.
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 * @param e
	 * 	Throwable object.
	 */
	private static void outputLog(int level, StackTraceElement stackTrace, String tag, String msg, Throwable e)
	{
		String classFullName = stackTrace.getClassName();
		String classSimpleName = classFullName.substring(classFullName.lastIndexOf('.') + 1);
		String methodName = stackTrace.getMethodName();

		String output = new StringBuilder()
			.append("[ ")
			.append(classSimpleName)
			.append("#")
			.append(methodName)
			.append("() ] ")
			.append(msg)
			.toString();

		switch(level)
		{
			case Level.VERBOSE:
				if(e == null)
				{
					android.util.Log.v(tag, output);
				}
				else
				{
					android.util.Log.v(tag, output, e);
				}
				break;
			case Level.DEBUG:
				if(e == null)
				{
					android.util.Log.d(tag, output);
				}
				else
				{
					android.util.Log.d(tag, output, e);
				}
				break;
			case Level.INFO:
				if(e == null)
				{
					android.util.Log.i(tag, output);
				}
				else
				{
					android.util.Log.i(tag, output, e);
				}
				break;
			case Level.WARN:
				if(e == null)
				{
					android.util.Log.w(tag, output);
				}
				else
				{
					android.util.Log.w(tag, output, e);
				}
				break;
			case Level.ERROR:
				if(e == null)
				{
					android.util.Log.e(tag, output);
				}
				else
				{
					android.util.Log.e(tag, output, e);
				}
				break;
		}
	}

	/**
	 * Output VERBOSE level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#v(String, String)
	 */
	public static void v(String tag, String msg)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.VERBOSE) != Level.VERBOSE) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.VERBOSE, stackTrace, tag, msg, null);
	}

	/**
	 * Output VERBOSE level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#v(String, String, Throwable)
	 */
	public static void v(String tag, String msg, Throwable e)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.VERBOSE) != Level.VERBOSE) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.VERBOSE, stackTrace, tag, msg, e);
	}

	/**
	 * Output DEBUG level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#d(String, String)
	 */
	public static void d(String tag, String msg)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.DEBUG) != Level.DEBUG) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.DEBUG, stackTrace, tag, msg, null);
	}

	/**
	 * Output DEBUG level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#d(String, String, Throwable)
	 */
	public static void d(String tag, String msg, Throwable e)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.DEBUG) != Level.DEBUG) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.DEBUG, stackTrace, tag, msg, e);
	}

	/**
	 * Output INFO level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#i(String, String)
	 */
	public static void i(String tag, String msg)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.INFO) != Level.INFO) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.INFO, stackTrace, tag, msg, null);
	}

	/**
	 * Output INFO level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#i(String, String, Throwable)
	 */
	public static void i(String tag, String msg, Throwable e)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.INFO) != Level.INFO) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.INFO, stackTrace, tag, msg, e);
	}

	/**
	 * Output WARN level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#w(String, String)
	 */
	public static void w(String tag, String msg)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.WARN) != Level.WARN) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.WARN, stackTrace, tag, msg, null);
	}

	/**
	 * Output WARN level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#w(String, String, Throwable)
	 */
	public static void w(String tag, String msg, Throwable e)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.WARN) != Level.WARN) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.WARN, stackTrace, tag, msg, e);
	}

	/**
	 * Output ERROR level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#e(String, String)
	 */
	public static void e(String tag, String msg)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.ERROR) != Level.ERROR) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.ERROR, stackTrace, tag, msg, null);
	}

	/**
	 * Output ERROR level log with Latest class name and method name.
	 *
	 * @param tag
	 * 	Used to identify the source of a log message.
	 * 	It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 * 	The message you would like logged.
	 *
	 * @see android.util.Log#e(String, String, Throwable)
	 */
	public static void e(String tag, String msg, Throwable e)
	{
		if(TARGET_LOG_LEVEL == Level.DISABLED) return;
		if((TARGET_LOG_LEVEL & Level.ERROR) != Level.ERROR) return;

		StackTraceElement stackTrace = new Throwable().getStackTrace()[1];
		outputLog(Level.ERROR, stackTrace, tag, msg, e);
	}
}
