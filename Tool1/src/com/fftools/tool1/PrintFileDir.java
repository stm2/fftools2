package com.fftools.tool1;

import java.io.File;

import com.fftools.OutTextClass;


public class PrintFileDir {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private File myDir = null;
	
	public PrintFileDir(File _myDir) {
		this.myDir = _myDir;
	}
	
	public void printIt(int level) {
		if (this.myDir == null) {
			outText.addOutLine(printLevel(level) + "PrintFileDir: null, nothing to print");
			return;
		}
		File[] files = this.myDir.listFiles();
		if (files==null){
			outText.addOutLine(printLevel(level) +  this.myDir + " does not exists.");
			return;
		}
		if (files.length==0){
			outText.addOutLine(printLevel(level) +  this.myDir + " is empty.");
			return;
		}
		
		for (int i = 0; i < files.length; i++) {
    		File actF = files[i];
    		if (actF.isDirectory()) {
    			int newLevel = level + 1;
    			outText.addOutLine(printLevel(level) + "DIR:" + actF.getPath());
    			PrintFileDir newPFD = new PrintFileDir(actF);
    			newPFD.printIt(newLevel);
    		}
    		if (actF.isFile()) {
    			outText.addOutLine(printLevel(level) + actF.getPath());
    		}
		}
		
	}
	
	private String printLevel(int level) {
		String erg = "";
		for (int i = 0;i<level;i++){
			erg = erg + ".";
		}
		return erg;
	}
	
}
