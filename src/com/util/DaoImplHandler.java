package com.util;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DaoImplHandler implements InvocationHandler {
	
	private BaseDao<?> baseDao;
	
	/**
	 * 要实现的接口类型
	 */
	private Class<?> ori;
	
	/**
	 * 对应实体类类型
	 */
	private Class<?> entity;
	
	private static final Pattern pattern = Pattern.compile(".+<(.*)>");
	
	/**
	 * 一些基本的类型
	 */
	public static final List<Class<?>> types = Arrays.asList(String.class, 
			Byte.class,
			Short.class, 
			Integer.class, 
			Long.class, 
			Float.class,
			Double.class, 
			Boolean.class, 
			Character.class,
			String.class);
	
	public static final Map<Class<? extends Number>, Function<Number, Number>> Primitive_TYPES = new HashMap<>();
	
	static {
		Primitive_TYPES.put(byte.class, Number::byteValue);
		Primitive_TYPES.put(short.class, Number::shortValue);
		Primitive_TYPES.put(int.class, Number::intValue);
		Primitive_TYPES.put(long.class, Number::longValue);
		Primitive_TYPES.put(float.class, Number::floatValue);
		Primitive_TYPES.put(double.class, Number::doubleValue);
	}
	
	public DaoImplHandler(Class<?> ori) {
		Class<?> clz = null;
		try {
			this.ori = ori;
			Matcher matcher = pattern.matcher(ori.getGenericInterfaces()[0].getTypeName());
			if( matcher.find() ) {
				clz = Class.forName(matcher.group(1));
				entity = clz;
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		baseDao = new BaseDao<>(entity);
	}
	
	public DaoImplHandler(Class<?> ori, Connection con) {
		Class<?> clz = null;
		try {
			this.ori = ori;
			Matcher matcher = pattern.matcher(ori.getGenericInterfaces()[0].getTypeName());
			if( matcher.find() ) {
				clz = Class.forName(matcher.group(1));
				entity = clz;
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		baseDao = new BaseDao<>(entity);
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if( method.getDeclaringClass() == Dao.class ) {
			return method.invoke(baseDao, args);
		} else if( method.getDeclaringClass() == ori || method.getDeclaringClass() == Transcation.class ) {
			SQL annotation = method.getAnnotation(SQL.class);
			String sql = annotation.value();
			
			if( sql.isEmpty() || sql.length() < 6 ) {
				throw new IllegalArgumentException("Please check your sql statement !!!");
			}
			
			if( args != null ) {
				
				Object[] newArgs = new Object[args.length];
				System.arraycopy(args, 0, newArgs, 0, newArgs.length);
				
				Matcher matcher = plachholder.matcher(sql);
				int newArgsIndex = 0;
				while( matcher.find() ) {
					String name = matcher.group(1);
					int contains = name.indexOf(".");
					if( contains != -1 ) {
						String exepress = name.substring(contains + 1);
						newArgs[newArgsIndex] = findProperty(exepress, args[Integer.valueOf(name.substring(0, contains))]);
					} else {
						newArgs[newArgsIndex] = args[Integer.valueOf(name)];
					}
					sql = sql.replace("#{" + name + "}", "?");
					newArgsIndex++;
				}
				
				args = newArgs;
			}
			
			if( "select".equalsIgnoreCase(sql.trim().substring(0, 6)) ) {
				if( List.class.isAssignableFrom(method.getReturnType()) ) {
					return baseDao.executeQuery(entity, sql, args);
				} else if( method.getReturnType().isArray() ) {
					List<?> list = baseDao.executeQuery(entity, sql, args);
					if( list != null ) {
						Object instance = Array.newInstance(entity, list.size());
						for (int i = 0; i < list.size(); i++) {
							Array.set(instance, i, list.get(i));
						}
						return instance;
					}
					return null;
				} else if( method.getReturnType().isPrimitive() || types.contains(method.getReturnType()) ) {
					Object result = baseDao.executeSingleQuery(sql, args);
					Object numberType = TypeUtil.numberConvertTo(method.getReturnType(), result);
					if( numberType != null ) {
						return numberType;
					}
					return result;
				} else {
					return baseDao.executeSingleQuery(method.getReturnType(), sql, args);
				}
			} else {
				Class<?> returnType = method.getReturnType();
				if( returnType == Boolean.class || returnType == boolean.class ) {
					return baseDao.executeUpdate(sql, args) > 0;
				} else {
					return TypeUtil.numberConvertTo(returnType, baseDao.executeUpdate(sql, args));
				}
			}
		}
		
		return null;
	}
	
	private static final Pattern plachholder = Pattern.compile("#\\{(\\d+(\\.[0-9A-Za-z_$]+)*)\\}");
	
	//address.name
	private static Object findProperty(String express, Object obj) throws ReflectiveOperationException {
		Object temp = obj;
		String errorMsg = express + " : error.";
		StringBuilder r = new StringBuilder(express);
		int start = 0;
		int end = r.indexOf(".", start);
		
		while( end != -1 ) {
			String name = r.substring(start, end);
			temp = checkAndInvoke("get" + name, temp, errorMsg);
			start = end + 1;
			end = r.indexOf(".", start);
		}
		
		temp = checkAndInvoke("get" + r.substring(start), temp, errorMsg);
		
		return temp;
	}
	
	private static Object checkAndInvoke(String name, Object obj, String errorMsg) throws ReflectiveOperationException {
		Method method = getIgnoreCaseMethod(obj, name);
		if( method == null || method.getParameterCount() > 0 ) {
			throw new IllegalArgumentException(errorMsg + " : error.");
		}
		return method.invoke(obj);
	}
	
	private static Method getIgnoreCaseMethod(Object obj, String name) {
		Optional<Method> result = Arrays.stream(obj.getClass().getMethods()).
				filter((item) -> item.getName().equalsIgnoreCase(name)).findFirst();
		if( result.isPresent() ) {
			return result.get();
		}
		return null;
	}
	
}
