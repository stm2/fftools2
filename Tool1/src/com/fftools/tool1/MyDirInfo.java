package com.fftools.tool1;

import java.io.File;

import com.fftools.OutTextClass;


public class MyDirInfo {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private String actDir = null;
	
	public MyDirInfo(){
		this(null);
	}
	
	public MyDirInfo(String newDirectory){
		if (newDirectory!=null) {
			this.setActDir(newDirectory);
		}
	}
	
	
	public void dirInfo(){
		if (this.getActDir()==null) {
			outText.addOutLine("MyDirInfo: cannot create DirInfo...Dir = null");
			return;
		}
		File nF = new File(this.actDir);
		PrintFileDir pFD = new PrintFileDir(nF);
		pFD.printIt(1);
	}
	
	
	
	
	/**
	 * @return the actDir
	 */
	public String getActDir() {
		return actDir;
	}

	/**
	 * @param actDir the actDir to set
	 */
	public void setActDir(String actDir) {
		if (actDir!=null) {
			this.actDir = actDir;
			outText.addOutLine("MyDirInfo: set Dir to:" + this.actDir);
		} else {
			this.actDir = null;
			outText.addOutLine("MyDirInfo: set Dir to null");
		}
	}
	
	
}
