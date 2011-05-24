/*
 * Copyright(C) 2011 - Ham4Droid
 * hamsterdb libraries for Android(TM) platform.
 *
 * @author DUMAPIC
 */
package jp.androiddevelopers.ham4droid;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import android.os.Parcel;

/**
 * Ham4Droid Utilities.
 *
 * @version 1.0.0
 */
public final class Ham4DroidUtils
{
	private static final Hashtable<Integer, Field> HAM_CONSTANTS_FIELDNAMEMAPPING;
	private static final int PARCEL_VALUE_NULL = 0;
	private static final int PARCEL_VALUE_NOT_NULL = 1;

	/**
	 * Get constants({@link de.crupp.hamsterdb.Const}) field name.
	 *
	 * @param value constants value.
	 * @return constants name
	 */
	public static String getConstantsName(int value)
	{
		Field f = HAM_CONSTANTS_FIELDNAMEMAPPING.get(value);
		return f.getName();
	}

	/**
	 * <p>
	 * Convert database item keys to raw byte array data.<br/>
	 * This function uses <code>adnroid.os.Parcel</code>.
	 * </p>
	 *
	 * @param keys variable keys on hamsterdb.
	 * @return raw byte data.
	 * @throws NullPointerException When keys is null/zero length.
	 *
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public static byte[] marshallKeys(final Object[] keys)
	{
		Parcel p = Parcel.obtain();

		if(keys == null || keys.length == 0)
			throw new NullPointerException("keys argument is null or length zero.");

		p.writeArray(keys);
		byte[] result = p.marshall();
		p.recycle();
		return result;
	}

	/**
	 * <p>
	 * Convert database item keys to raw byte array data.<br/>
	 * This function uses <code>adnroid.os.Parcel</code>.
	 * </p>
	 *
	 * @param keys variable keys on hamsterdb.
	 * @return raw byte data.
	 * @throws NullPointerException When keys is null/zero length.
	 *
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public static Object[] unmarshallKeys(final byte[] bytedata)
	{
		if(bytedata == null || bytedata.length == 0)
			throw new NullPointerException("key bytes is null or length zero.");

		Parcel p = Parcel.obtain();
		p.unmarshall(bytedata, 0, bytedata.length);
		p.setDataPosition(0);

		Object[] result = p.readArray(Ham4DroidUtils.class.getClassLoader());
		p.recycle();
		return result;
	}

	/**
	 * <p>
	 * Convert a database record to raw byte array data.<br/>
	 * This function uses <code>adnroid.os.Parcel</code>.
	 * </p>
	 *
	 * @param aValue a record data on hamsterdb.
	 * @return raw byte data.
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public static byte[] marshallValue(final Object aValue)
	{
		Parcel p = Parcel.obtain();

		if(aValue == null)
		{
			p.writeInt(PARCEL_VALUE_NULL);
		}
		else
		{
			p.writeInt(PARCEL_VALUE_NOT_NULL);
			p.writeValue(aValue);
		}

		byte[] result = p.marshall();
		p.recycle();
		return result;
	}

	/**
	 * <p>
	 * Convert raw byte data to database record value.<br/>
	 * This function uses <code>adnroid.os.Parcel</code>.
	 * </p>
	 *
	 * @param bytedata the raw byte data.
	 * @return hamsterdb record.
	 * @see {@link android.os.Parcel#writeValue(Object)}
	 */
	public static Object unmarshallValue(final byte[] bytedata)
	{
		if(bytedata == null || bytedata.length == 0)
			return null;

		Parcel p = Parcel.obtain();
		p.unmarshall(bytedata, 0, bytedata.length);
		p.setDataPosition(0);

		Object result;

		int dataType = p.readInt();
		switch(dataType)
		{
			case PARCEL_VALUE_NULL:
				result = null;
				break;
			case PARCEL_VALUE_NOT_NULL:
				result = p.readValue(Ham4DroidUtils.class.getClassLoader());
				break;
			default:
				throw new UnknownError("Unknown parcel value type. (dataType:"+dataType+")");
		}

		p.recycle();
		return result;
	}

	/**
	 * Reflection utilities for Ham4Droid.
	 */
	public static class Reflect
	{
		/**
		 * Get the accessible method.
		 */
		public static Method getAccessibleMethod(
			final Class<?> target,
			final String methodName,
			final Class<?>...methodArgTypes)
		{
			Class<?> clazz = target;
			Method foundMethod = null;
			while(clazz != null)
			{
				try
				{
					foundMethod = clazz.getDeclaredMethod(methodName, methodArgTypes);
					break;
				}
				catch (NoSuchMethodException e)
				{
					clazz = clazz.getSuperclass();
					foundMethod = null;
				}
			}

			if(foundMethod == null)
			{
				throw new IllegalStateException("'"+methodName+"' method is not found in "+target.getSimpleName());
			}

			try
			{
				final Method result = foundMethod;
				if(!result.isAccessible())
				{
					AccessController.doPrivileged(new PrivilegedAction<Object>()
					{
						@Override
						public Object run()
						{
							result.setAccessible(true);
							return null;
						}
					});
				}

				return result;
			}
			catch (Throwable e)
			{
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		/**
		 * Invoke instance method.
		 */
		public static Object invokeMethod(final Object targetInstance, final Method method, final Object[] methodArgValues)
		{
			Object result = null;
			try
			{
				if(method.getReturnType() != void.class)
				{
					result = method.invoke(targetInstance, methodArgValues);
				}
				else
				{
					method.invoke(targetInstance, methodArgValues);
					result = null;
				}
			}
			catch (Throwable e)
			{
				throw new IllegalStateException(e.getMessage(), e);
			}
			return result;
		}

		/**
		 * Get the accessible field.
		 */
		public static Field getAccessibleField(final Class<?> target, final String fieldName)
		{
			Class<?> clazz = target;
			Field foundField = null;
			while(clazz != null)
			{
				try
				{
					foundField = clazz.getDeclaredField(fieldName);
					break;
				}
				catch (NoSuchFieldException e)
				{
					clazz = clazz.getSuperclass();
					foundField = null;
				}
			}

			if(foundField == null)
			{
				throw new IllegalStateException("'"+fieldName+"' field is not found in "+target.getSimpleName());
			}

			try
			{
				final Field result = foundField;
				if(!result.isAccessible())
				{
					AccessController.doPrivileged(new PrivilegedAction<Object>()
					{
						@Override
						public Object run()
						{
							result.setAccessible(true);
							return null;
						}
					});
				}

				return result;
			}
			catch (Throwable e)
			{
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		/**
		 * Get the instance field value.
		 */
		public static Object getFieldValue(final Object targetInstance, final Field field)
		{
			Object result = null;
			try
			{
				result = field.get(targetInstance);
			}
			catch (Throwable e)
			{
				throw new IllegalStateException(e.getMessage(), e);
			}
			return result;
		}

		/**
		 * Set the instance field value.
		 */
		public static void setFieldValue(final Object targetInstance, final Field field, final Object value)
		{
			try
			{
				field.set(targetInstance, value);
			}
			catch (Throwable e)
			{
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}

	static
	{
		HAM_CONSTANTS_FIELDNAMEMAPPING = new Hashtable<Integer, Field>();
		Field[] constants = de.crupp.hamsterdb.Const.class.getDeclaredFields();

		try
		{
			for(Field f : constants)
			{
				Integer staticValue = (Integer) f.get(null);
				HAM_CONSTANTS_FIELDNAMEMAPPING.put(staticValue, f);
			}
		}
		catch (Throwable e)
		{
			throw new IllegalAccessError("Can't get hamsterdb constants(Const class) fields.");
		}
	}
}
