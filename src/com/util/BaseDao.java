package com.util;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseDao<M> extends SqlUtil implements Dao<M> {
	
	private static final Pattern pattern = Pattern.compile(".+<(.*)>");
	
	@SuppressWarnings("unchecked")
	public BaseDao() {
		try {
			Matcher matcher = pattern.matcher(this.getClass().getGenericSuperclass().getTypeName());
			if( matcher.find() ) {
				clz = (Class<M>) Class.forName(matcher.group(1));
			}
			this.table = clz.getSimpleName();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public BaseDao(Class<M> clz) {
		this.clz = clz;
		this.table = clz.getSimpleName();
	}
	
	public BaseDao(Class<M> clz, Connection con) throws SQLException {
		super(con);
		this.clz = clz;
		this.table = clz.getSimpleName();
	}
	
	private Class<M> clz;
	
	private final String table; 
	
	@Override
	public void setAutoCommit(boolean autoCommit) { 
		try {
			super.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void rollback() {
		try {
			super.rollback();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void commit() {
		try {
			super.commit();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Connection getConn() {
		return super.getConn();
	}
	
	@Override
	public M get(long id) {
		return executeSingleQuery(clz, "select * from ${0} where id=?", table, id);
	}
	
	@Override
	public List<M> getAll() {
		return executeQuery(clz, "select * from ${0}", table);
	}
	
	@Override
	public List<M> getAll(long start, long len) {
		return executeQuery(clz, "select * from ${0} limit ?,?", table, start, len);
	}
	
	@Override
	public long getCount() {
		return executeQueryNumber("select count(*) from ${0}", table).longValue();
	}
	
	@Override
	public int delete(long id) {
		return executeUpdate("delete from ${0} where id=?", table, id);
	}
	
	@Override
	public int add(M t) {
		StringBuilder sql = new StringBuilder("insert into ${0}(");
		List<Object> params = filterMethod(t, (item) -> sql.append(item.getName().substring(3) + ","));
		sql.setLength(sql.length() - 1);
		sql.append(") values(");
		params.forEach((item) -> sql.append("?,"));
		sql.setLength(sql.length() - 1);
		sql.append(")");
		params.add(0, table);
		return executeUpdate(sql.toString(), params.toArray());
	}
	
	@Override
	public int update(long id, M t) {
		StringBuilder r = new StringBuilder();
		List<Object> params = filterMethod(t, (item) -> r.append(item.getName().substring(3) + "=?,") );
		r.setLength(r.length() - 1);
		params.add(0, table);
		params.add(id);
		return executeUpdate("update ${0} set " + r.toString() + " where id=?", params.toArray());
	}
	
	public int update(M t) {
		try {
			Method method = t.getClass().getMethod("getId");
			return update(((Number) method.invoke(t)).longValue(), t);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	private List<Object> filterMethod(M t, Consumer<Method> consumer) {
		ArrayList<Object> params = new ArrayList<>();
		Map<String, String> allColumns;
		try {
			allColumns = getAllColumns(table);
			allColumns.keySet().forEach((key) -> {
				Method method = findMethod(t, "get" + key);
				if( method != null ) {
					consumer.accept(method);
					try {
						params.add(method.invoke(t));
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}
			});
		} catch (SQLException e1) {
			throw new RuntimeException(e1);
		}
		return params;
	}
	
	private static Method findMethod(Object m, String methodName) {
		Optional<Method> findFirst = Arrays.stream(m.getClass().getMethods()).filter((item) -> {
			return item.getName().equalsIgnoreCase("getId") == false && item.getName().equalsIgnoreCase(methodName);
		}).findFirst();
		return findFirst.isPresent() ? findFirst.get() : null;
	}
	
	@Override
	protected int executeUpdate(String sql, Object... params) {
		try {
			return super.executeUpdate(sql, params);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected <T> List<T> executeQuery(Class<T> bean, String sql, Object... params) {
		try {
			return super.executeQuery(bean, sql, params);
		} catch (ReflectiveOperationException | SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected Number executeQueryNumber(String sql, Object... params) {
		try {
			return super.executeQueryNumber(sql, params);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected <T> T executeSingleQuery(Class<T> bean, String sql, Object... params) {
		try {
			return super.executeSingleQuery(bean, sql, params);
		} catch (SQLException | ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
}
