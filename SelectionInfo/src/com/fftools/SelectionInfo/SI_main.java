package com.fftools.SelectionInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.CoordinateID;
import magellan.library.GameData;
import magellan.library.Region;
import magellan.library.Unit;
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
	
	private static Hashtable<CoordinateID,Region> importantRegions = new Hashtable<CoordinateID, Region>();
	private static Hashtable<CoordinateID,Region> allSelectedRegions = new Hashtable<CoordinateID, Region>();
	
	private static Hashtable<String,Hashtable<CoordinateID,Region>> selFiles = new Hashtable<String, Hashtable<CoordinateID,Region>>();
	
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
    			if (!u.getName().equalsIgnoreCase("monopol")){
	    			allSelectedRegions.putAll(r.getSelectedRegions());
	    			selFiles.putAll(r.getSelFiles());
    			}
    		}
    	}
    	
    	
    	outText.addOutLine(FileCopy.getDateS() + " starta Analysis");
    	checkImportantRegionsInSelections(s,myGD);
    	checkOverlappingSelections(s,myGD);
    	outText.addOutLine(FileCopy.getDateS() + " end Selection Info");
	}
	
	private static void checkImportantRegionsInSelections(Settings s, GameData gd){
		int anzImportant = fillImportantRegions(s, gd);
		// kompletter check
		String lastOuttextFile =outText.getActFileName();
		outText.setFile("../selections/_work/analysis.txt");
		outText.addOutLine("Starte Analyse der wichtigen Regionen: ");
		outText.addOutLine("Anzahl wichtiger gefundener Regionen: " + anzImportant);
		outText.addOutLine("Anzahl der Regionen in Spielerselektionen: " + allSelectedRegions.size());
		
		for (CoordinateID c:importantRegions.keySet()){
			Region r = allSelectedRegions.get(c);
			if (r==null){
				// Problem erkannt
				r = gd.getRegion(c);
				if (r==null){
					// kompletter humbug
					outText.addOutLine("Unselektierte Koordinaten ohne Region in Gamedata ?!: " + c.toString(",", true));
				} else {
					outText.addOutLine("Unselektierte Koordinaten: " + c.toString(",", true) + ": " + r.toString());
				}
			}
		}
		outText.addOutLine("Ende Analysis");
		outText.setFile(lastOuttextFile);
		
	}
	
	
	
	
	/**
	 * Baut die Liste wichtiger Regionen, die Parteien enthalten, von denen 
	 * der Server das Passwort kennt und über die Befehle eingeschickt werden können
	 * @param s
	 * @param gd
	 */
	private static int fillImportantRegions(Settings s, GameData gd){
		int erg=0;
		importantRegions.clear();
		// kompletter durchlauf
		for (Region r:gd.regions().values()){
			if (r.units()!=null && r.units().size()>0){
				for (Unit u:r.units()){
					String actFactionName = u.getFaction().getID().toString();
			    	if (s.isFactionWithPassword(actFactionName)) {
			    		// wichtige region
			    		importantRegions.put(r.getCoordinate(), r);
			    		erg++;
			    		break;
			    	}
				}
			}
		}
		return erg;
	}
	
	private static void checkOverlappingSelections(Settings s, GameData gd){
		
		// kompletter check
		String lastOuttextFile =outText.getActFileName();
		outText.setFile("../selections/_work/analysis.txt");
		outText.addOutLine("Starte Analyse der Selektionen: ");
		for (String actFileName:selFiles.keySet()){
			Hashtable<CoordinateID,Region> ActSel = selFiles.get(actFileName);
			if (ActSel!=null){
				outText.addOutLine("Selektion " + actFileName + " mit " + ActSel.size() + " Regionen");
			} else {
				outText.addOutLine("Selektion " + actFileName + " ohne Regionen");
			}
		}
		outText.addOutLine("Starte Komplettsuche ueber " + gd.regions().size() + " Regionen.");
		// Alle Regionen durchlaufen, dürfen niemals mehr als 1x gefunden werden
		ArrayList<String> ergList = new ArrayList<String>();
		for (Region r:gd.regions().values()){
			int cnt = 0;
			ergList.clear();
			CoordinateID actC = r.getCoordinate();
			for (String actFileName:selFiles.keySet()){
				Hashtable<CoordinateID,Region> ActSel = selFiles.get(actFileName);
				if (ActSel!=null){
					if (ActSel.get(actC)!=null){
						cnt++;
						ergList.add(actFileName);
					}
				}
			}
			if (cnt>1){
				// Treffer
				outText.addOutLine("Mehrfach gefundene Koordinaten: " + actC.toString(",", true) + " " + r.toString());
				for (String dd:ergList){
					outText.addOutLine("  gefunden in: " + dd);
				}
			}
		}
		
		outText.addOutLine("Ende Analyse der Selektionen");
		outText.setFile(lastOuttextFile);
		
	}
	
	
	
}
