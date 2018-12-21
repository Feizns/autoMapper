package com.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TypeUtil {
	
	private TypeUtil(){}
	
	public static final Map<Class<? extends Number>, Function<Number, Number>> Number_Types = new HashMap<>();
	
	static {
		Number_Types.put(byte.class, Number::byteValue);
		Number_Types.put(short.class, Number::shortValue);
		Number_Types.put(int.class, Number::intValue);
		Number_Types.put(long.class, Number::longValue);
		Number_Types.put(float.class, Number::floatValue);
		Number_Types.put(double.class, Number::doubleValue);
		Number_Types.put(Byte.class, Number::byteValue);
		Number_Types.put(Short.class, Number::shortValue);
		Number_Types.put(Integer.class, Number::intValue);
		Number_Types.put(Long.class, Number::longValue);
		Number_Types.put(Float.class, Number::floatValue);
		Number_Types.put(Double.class, Number::doubleValue);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T numberConvertTo(Class<T> tar, Object data) {
		Function<Number, Number> function = Number_Types.get(tar);
		if( function != null ) {
			return (T) function.apply((Number) data);
		}
		return null;
	}
	
	public static void main(String[] args) {
		Object obj = 21;
		Byte f = numberConvertTo(Byte.class, obj);
		System.out.println(f.getClass());
	}
	
}
