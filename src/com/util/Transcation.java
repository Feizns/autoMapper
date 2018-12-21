package com.util;

import java.sql.Connection;

public interface Transcation {
	
	/**
	 * 设置自动提交方式，仅相对与当前 Dao
	 * @param autoCommit
	 */
	void setAutoCommit(boolean autoCommit);
	
	/**
	 * 事务提交
	 */
	void commit();
	
	/**
	 * 事务回滚
	 */
	void rollback();
	
	/**
	 * 获取正在使用的数据库连接
	 * @return
	 */
	Connection getConn();
	
}
