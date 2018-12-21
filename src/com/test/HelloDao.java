package com.test;

import com.util.Dao;
import com.util.SQL;

public interface HelloDao extends Dao<Users> {
	
	@SQL("select * from users")
	Hello[] getLike();
	
//	@SQL("select * from users where name like concat('%', #{0.account}, '%')")
//	Hello[] GG(Users user);
	
}
