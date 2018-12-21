package com.test;

import java.io.FileNotFoundException;
import java.util.List;

import com.util.Dao;

public class Test {
	
	public static void main(String[] args) throws FileNotFoundException {
		
		HelloDao mapper = Dao.getMapper(HelloDao.class);
		
		List<Users> list = mapper.getAll();
		
		list.forEach(System.out::println);
		
	}
	
}
