package com.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.util.SqlUtilCommons.*;
/**
 * @author Feizns <br/>
 * @date 2018年9月18日 <br/>
 * @description 托管一个 Connection， 以方便更好的使用 JDBC 相关内容
 */
public abstract class SqlUtil {
	
	private static final String DEFAULT_RESOURCE_PROPERTIES = "/db.properties";
	
	private static final Properties Properties = new Properties(); 
	
	private static final String get(SqlUtilCommons com) {
		for (Entry<Object, Object> item : Properties.entrySet()) {
			if( item.getKey().toString().equalsIgnoreCase( name(com) ) ) {
				return item.getValue().toString();
			}
		}
		return null;
	}
	
	static {
		try {
			Properties.load(SqlUtil.class.getResourceAsStream(DEFAULT_RESOURCE_PROPERTIES));
			Class.forName(get(DRIVER));
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public SqlUtil() {
		try {
			setConn(DriverManager.getConnection(get(URL), get(USERNAME), get(PASSWORD)));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public SqlUtil(Connection con) throws SQLException {
		setConn(con);
	}
	
	/**
	 * 在传入的SQL语句中替换 ${}的一些模式 
	 * @see manageSql(String sql, Object... params);
	 */
	private static final Pattern PATTERN = Pattern.compile("\\$\\{(\\d+)\\}");
	
	/**
	 * for replaceAll ${?}
	 */
	private static final NOTCOMPILE NOT = NOTCOMPILE.NOTCOMPILE;
	
	private static enum NOTCOMPILE { NOTCOMPILE }
	
	private Connection con;
	
	private DatabaseMetaData dbmd;
	
	private PreparedStatement ps;
	
	private String lastExecuteSql;	//最近一次执行的SQL
	
	protected void startTransaction() throws SQLException { con.setAutoCommit(false); }
	
	protected void setAutoCommit(boolean autoCommit) throws SQLException { con.setAutoCommit(autoCommit); }
	
	protected void rollback() throws SQLException { con.rollback(); }
	
	protected void commit() throws SQLException { con.commit(); }
	
	protected void close() throws Exception { con.close(); }
	
	protected String[] getAllTables() throws SQLException {
		List<String> r = new ArrayList<>();
		try (ResultSet rs = dbmd.getTables(null, null, null, new String[]{ name(TABLE) }); ) {
			while ( rs.next() ) { 
				r.add( rs.getString( name(TABLE_NAME) ) ); 
			}
		}
		return r.toArray( new String[r.size()] );
	}
	
	protected Map<String, String> getAllColumns(String tableName) throws SQLException {
		Map<String, String> r = new LinkedHashMap<>();
		try ( ResultSet rs = dbmd.getColumns(null, null, tableName, null) ) {
			while ( rs.next() ) {
				r.put( rs.getString( name(COLUMN_NAME) ), rs.getString( name(TYPE_NAME) ) );
			}
		}
		return r;
	}
	
	protected ResultSet executeQuery(String sql, Object... params) throws SQLException {
		return setParameter( createPS(sql, params), params ).executeQuery();
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T executeSingleQuery(String sql, Object... params) throws SQLException {
		ResultSet rs = setParameter( createPS(sql, params) , params).executeQuery();
		return (T) (rs.next() ? rs.getObject(1) : null);
	}
	
	protected Number executeQueryNumber(String sql, Object... params) throws SQLException {
		return (Number) executeSingleQuery(sql, params);
	}
	 
	protected int executeUpdate(String sql, Object... params) throws SQLException {
		return setParameter( createPS(sql, params), params ).executeUpdate();
	}
	
	protected void setConn(Connection conn) throws SQLException {
		if( con != null && con.isClosed() == false ){
			con.close();
		}
		con = conn;
		dbmd = con.getMetaData();
	}
	
	protected Connection getConn() {
		return con;
	}
	
	/**
	 * 
	 * @param bean
	 * @param sql
	 * @param params
	 * @return
	 * @throws ReflectiveOperationException
	 * @throws SQLException
	 */
	protected <T> List<T> executeQuery(Class<T> bean, String sql, Object... params) throws ReflectiveOperationException, SQLException {
		List<T> result = new ArrayList<>();
		
		ResultSet rs = executeQuery(sql, params);
		ResultSetMetaData colInfo = rs.getMetaData();
		while( rs.next() ) {
			T newInstance = bean.newInstance();
			for (int i = 0; i < colInfo.getColumnCount(); i++) {
				String col = colInfo.getColumnLabel(i + 1);
				
				if( col.contains("_") ) {
					col = col.replace("_", "");
				}
				
				Object object = rs.getObject(col);
				for (Method item : bean.getMethods()) {
					if( item.getName().equalsIgnoreCase("set" + col) ||
							item.getName().equalsIgnoreCase("set" + col.replace("_", ""))) {
						
						if( item.getParameterCount() == 1 ) {
							Class<?> para = item.getParameterTypes()[0];
							Object numberData = TypeUtil.numberConvertTo(para, object);
							if( numberData != null ) {
								object = numberData;
							}
							item.invoke(newInstance, object);
							break;
						}
					}
				}
			}
				
			result.add(newInstance);
		}
		
		return result.size() == 0 ? null : result;
	}
	
	/**
	 * 
	 * @param bean
	 * @param sql
	 * @param params
	 * @return
	 * @throws SQLException
	 * @throws ReflectiveOperationException
	 */
	protected <T> T executeSingleQuery(Class<T> bean, String sql, Object... params) throws SQLException, ReflectiveOperationException {
		List<T> r = executeQuery(bean, sql, params);
		return r == null ? null : r.get(0);
	}
	
	/**
	 * 判断是否复用原来的 PreparedStatement.
	 * @param sql
	 * @return 
	 * @throws SQLException
	 */
	private PreparedStatement createPS(String sql, Object[] params) throws SQLException {
		System.err.println("SQL: " + sql + " ==> params:" + Arrays.toString(params));
		sql = manageSql(sql, params);
		return sql.equalsIgnoreCase(lastExecuteSql) ? ps : ( ps = con.prepareStatement(sql) );
	}
	
	/**
	 * 为PreparedStatement 处理真正要设置的参数
	 * @param ps
	 * @param params
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement setParameter(PreparedStatement ps, Object... params) throws SQLException {
		
		if( params == null ) {
			return ps;
		}
		
		List<Object> list = new ArrayList<>();
		for (Object item : params) {
			if( item != NOT )
				list.add(item);
		}
		
		params = list.size() == params.length ? params : list.toArray(new Object[list.size()]);
		
		for (int i = 0; i < params.length; i++) {
			ps.setObject(i + 1, params[i]);
		}
		
		return ps;
	}
	
	/**
	 * 不适用预编译的方式替换SQL语句中的一些内容
	 * @param sql
	 * @param params
	 * @return
	 */
	private static String manageSql(String sql, Object... params) {
		Matcher mat = PATTERN.matcher(sql);
		HashMap<String, Integer> parIndex = new HashMap<>();  
		while( mat.find() ) {
			parIndex.put( mat.group(), Integer.valueOf(mat.group(1)) );
		}
		
		for (Map.Entry<String, Integer> entry : parIndex.entrySet()) {
			int val = entry.getValue();
			sql = sql.replace(entry.getKey(), params[val].toString());
			params[val] = NOT;
		}
		
		return sql;
	}
	
}