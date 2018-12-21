package com.util;

public enum SqlUtilCommons {
	
	URL,
	DRIVER,
	USERNAME,
	PASSWORD,
	TABLE_NAME,
	COLUMN_NAME,
	TYPE_NAME,
	TABLE;
	
	static String name(SqlUtilCommons com) {
		return com.name();
	}
	
}
