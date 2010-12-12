package com.fftools.tool1;

public class User {
	private String name = null;
	private String pass = null;
	
	public void setName(String _name){
		this.name = _name;
	}
	
	public String getName() {
		return this.name;
	}

	/**
	 * @return the pass
	 */
	public String getPass() {
		return pass;
	}

	/**
	 * @param pass the pass to set
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}
	
}
