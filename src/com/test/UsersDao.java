package com.test;

import com.util.Dao;
import com.util.SQL;

public interface UsersDao extends Dao<Users> {

	@SQL("select * from Users where account=? and password=?")
	Users get(String account, String password);
	
}
