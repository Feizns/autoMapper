package com.util;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface Dao<T> extends Transcation {
	
	/**
	 * 获取表中所有数据
	 * @return
	 */
	List<T> getAll();
	
	/**
	 * 分页获取数据
	 * @param start
	 * @param len
	 * @return
	 */
	List<T> getAll(long start, long len);
	
	/**
	 * 根据id获取指定数据
	 * @param id
	 * @return
	 */
	T get(long id);
	
	/**
	 * 添加指定数据
	 * @param d
	 * @return
	 */
	int add(T d);
	
	/**
	 * 根据Id修改数据
	 * @param id
	 * @param d
	 * @return
	 */
	int update(long id, T d);
	
	/**
	 * 根据Id修改实体类
	 * @param d
	 * @return
	 */
	int update(T d);
	
	/**
	 * 根据Id删除指定数据
	 * @param id
	 * @return
	 */
	int delete(long id);
	
	/**
	 * 获取表中拥有的数据量
	 * @return
	 */
	long getCount();
	
	@SuppressWarnings("unchecked")
	static <R> R getMapper(Class<R> clz) {
		return (R) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{clz}, new DaoImplHandler(clz));
	}
	
	@SuppressWarnings("unchecked")
	static <R> R getMapper(Class<R> clz, Connection conn) {
		return (R) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{clz}, new DaoImplHandler(clz, conn));
	}
	
	@SuppressWarnings("unchecked")
	static <R> R getMapper(Class<R> clz, Dao<?> dao) {
		return (R) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{clz}, new DaoImplHandler(clz, dao.getConn()));
	}

	static Map<Class<?>, Dao<?>> getMapper(Class<?>... clz) {
		Map<Class<?>, Dao<?>> result = new LinkedHashMap<>();
		if( clz == null || clz.length == 0 ) {
			return null;
		}

		Dao<?> first = (Dao<?>) getMapper(clz[0]);
		result.put(clz[0], first);
		for (int i = 1; i < clz.length; i++) {
			result.put(clz[i], (Dao<?>) getMapper(clz[i], first));
		}
		return result;
	}
	
}
