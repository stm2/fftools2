package com.fftools.SelectionInfo;

import java.io.File;
import java.util.Iterator;

import magellan.library.GameData;
import magellan.library.utils.PropertiesHelper;
import magellan.library.utils.Resources;

import com.fftools.OutTextClass;
import com.fftools.tool1.Regions;
import com.fftools.tool1.Settings;
import com.fftools.tool1.Tool1_Main;
import com.fftools.tool1.User;
import com.fftools.utils.FileCopy;

public class SI_main {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static File settingsDir = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
//		 sets the output file
    	outText.setFile("FFTools_SI_log_" + FileCopy.getDateSDay());
    	
    	outText.addOutLine(FileCopy.getDateS() + " start Selection Info");
    	
    	
    	// neu 20090421: Parameter das Megallen2 Dir
    	if (args.length != 1) {
	      System.out.println("Syntax:");
	      System.out.println("java -jar SelectionInfo.jar <magellan_dir>");
	      System.out.println("  magellan_dir  - the directory that contains the magellan settings (rules and resources).");
	      outText.addOutLine("no mag dir given - aborting");
	      System.exit(1);
	    }
    	
    	
    	settingsDir = new File(args[0]);
        if (!settingsDir.isDirectory()) {
          System.out.println("<magellan_dir> must be a directory.");
          outText.addOutLine("<magellan_dir> must be a directory.");
          System.exit(1);
        }
        
        PropertiesHelper.setSettingsDirectory(settingsDir);
        Resources.getInstance().initialize(settingsDir, "");
    	
    	
		
		// settings einlesen
    	Settings s = new Settings("toolsettings.config");
    	outText.addOutLine(FileCopy.getDateS() + " settings gelesen");
		// GameData = monopol.cr
       //    	 den abgelegten CR mit den Befehlen laden...
    	File f = new File("../scratch/monopol.cr");
    	outText.addOutLine(FileCopy.getDateS() + " lese monopol.cr \n");
    	GameData myGD = Tool1_Main.loadFile(f);
    	
    	
    	for (Iterator<User> iter = s.users.iterator();iter.hasNext();){
    		User u = (User)iter.next();
    		Regions r = new Regions(myGD);
    		int i = r.readVerz(s.getDirectory("selections") + File.separator + u.getName() + File.separator);
    		if (i>0){
    			// es sind selektionen vor Ort
    			// diese temporär speichern
    			outText.addOutLine("Sichere Selektionen fuer: " + u.getName());
    			File tempF = new File("../selections/_work/selektion_" + myGD.getDate().getDate() + "_" + FileCopy.getDateSDay()+"_" + u.getName() + ".sel");
    			int selWrote = r.saveSelection(tempF);
    			outText.addOutLine("Regionen geschrieben: " + selWrote);
    		}
    	}
    	outText.addOutLine(FileCopy.getDateS() + " end Selection Info");
	}

}
