package com.test;

import java.sql.Date;

public class Hello {
	
	private Double id;
	
	private String name;
	
	private Date dat;

	public Double getId() {
		return id;
	}

	public void setId(Double id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDat() {
		return dat;
	}

	public void setDat(Date dat) {
		this.dat = dat;
	}

	@Override
	public String toString() {
		return "Hello [id=" + id + ", name=" + name + ", dat=" + dat + "]";
	}
	
}
