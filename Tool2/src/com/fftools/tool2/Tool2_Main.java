package com.fftools.tool2;

import java.io.File;
import java.text.NumberFormat;
import java.util.Random;

import com.fftools.OutTextClass;
import com.fftools.tool1.Settings;
import com.fftools.utils.FileCopy;


public class Tool2_Main {

	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final NumberFormat weightNumberFormat = NumberFormat.getNumberInstance();
	
	
	private static Settings s = null;
	private static Random ra = new Random();

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// sets thre output file
    	outText.setFile("FFTools_log_Tool2_" + FileCopy.getDateSDay());
		
		
		outText.addOutLine(FileCopy.getDateS() + " start Tool2");
		try {
			s = new Settings("toolsettings.config");
	    	s.sayStatus();
	    	// alle userverzeichnisse durchsuchen
        	//for (Iterator iter = s.users.iterator();iter.hasNext();){
        	//	u = (User)iter.next();
        		// selections einlesen...vorher wird gecleart
        		// int i = r.readVerz(s.getDirectory("selections") + File.separator + u.getName() + File.separator);
        		// outText.addOutLine("Regions in selections read: " + i);
        		// in zuege liegt / liegen die CRs mit den Befehlen 
        		// String verz = s.getDirectory("data") + File.separator + u.getName() + File.separator;
        		String verz = s.getDirectory("data") + File.separator;
        		// Verzeichnis bekannt..jetzt abarbeiten
        		workVerz(verz);
        //	}
	    	
	    	
	    	
		} catch (Throwable exc) { // any fatal error
            outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt            
            System.exit(1);
        }
        outText.addOutLine(FileCopy.getDateS() + " OK\n");
    	
	}

	private static void workVerz(String verz){
    	// bekommt ein Userverzeichnis übergeben, die Settings sind global
    	File file = new File(verz);
    	File[] files = file.listFiles();
    	if (files!=null) {
    		if (files.length>0) {
		    	for (int i = 0; i < files.length; i++) {
		    		File actF = files[i];
		    		if (actF.getName().endsWith(".sendME") && actF.isFile()) {
		    			// ein toSend ist im Verzeichnis gefunden worden...
		    			outText.addOutLine("work on file: " + actF);
		    			workFile(actF);
		    		} else if (!actF.getName().endsWith(".toSend") && actF.isFile()){
		    			outText.addOutLine("skipping: " + actF);
		    		}
		 		}
    		} else {
    			outText.addOutLine("dir empty: " + verz);
    		}
    	} else {
    		outText.addOutLine("no dir found: " + verz);
    	}
    }
	private static void workFile(File f){
		/**
		 * 1. umbenennen
		 * 2. thread starten
		 */
		String oldName = f.getPath();
    	String newName = oldName + "_proc" + FileCopy.getDateS();
    	File newFile = new File(newName);
    	f.renameTo(newFile);
    	outText.addOutLine("renamed to: " + newName);
    	
    	
    	SendFileMail sfm = new SendFileMail(s);
    	sfm.setFile(newFile);
    	
    	String minWaitMinutesString = s.getDirectory("minWaitMinutes");
    	String maxWaitMinutesString = s.getDirectory("maxWaitMinutes");
    	int minMin = Integer.parseInt(minWaitMinutesString);
    	int maxMin = Integer.parseInt(maxWaitMinutesString);
    	
    	
    	int i = ra.nextInt((maxMin - minMin) * 60 * 1000) + (minMin * 60 * 1000);
    	String iS = weightNumberFormat.format(new Float(i/60000F));
    	//int i = 2000;
    	sfm.setWaitTime(i);
    	
    	Thread t = new Thread(sfm);
    	t.start();

    	
    	
    	outText.addOutLine(FileCopy.getDateS() + " Thread running. Wait time: " + i + " ms (" + iS + "min), Name:" + newFile.getPath());
    }
	
}
