package com.fftools.tool1;
/**
 * 
 */


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.Faction;
import magellan.library.GameData;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.UnitID;
import magellan.library.io.GameDataReader;
import magellan.library.io.cr.CRWriter;
import magellan.library.io.file.FileType;
import magellan.library.io.file.FileTypeFactory;
import magellan.library.utils.Encoding;
import magellan.library.utils.PropertiesHelper;
import magellan.library.utils.Resources;
import magellan.library.utils.VersionInfo;

import com.fftools.OutTextClass;
import com.fftools.utils.FFToolsTags;
import com.fftools.utils.FileCopy;



/**
 * @author Fiete
 *
 */
public class Tool1_Main {
	
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static File settingsDir = null;
	
	// der monopolreport
	private static GameData gd = null;
	private static Regions r = null;
	private static Settings s = null;
	private static User u = null;
	
	private static String versionDate = "21.04.2009";
	
	/**
	 * @param args
	 */
    // ////////////////////
    // START & END Code //
    // ////////////////////
    public static void main(String args[]) {
    	
    	// sets thre output file
    	outText.setFile("FFTools_log_Tool1_" + FileCopy.getDateSDay());
    	
    	outText.addOutLine(FileCopy.getDateS() + " start Tool 1, version " + versionDate);
    	
    	// neu 20090421: Parameter das Megallen2 Dir
    	if (args.length != 1) {
	      System.out.println("Syntax:");
	      System.out.println("java -jar FFToolsTool1.jar <magellan_dir>");
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
    	String version = VersionInfo.getVersion(settingsDir);
    	outText.addOutLine("Magellan Version: " + version);
    	// wenn temp  file da : abbruch
    	File checkFile = null;
    	
		outText.addOutLine("checking temp.cr");
		checkFile = new File("temp.cr");
		if (checkFile.exists()){
			outText.addOutLine("temp.cr found. Aborting.");
			return;
		}	
    	
        try {
        	// settings einlesen
        	s = new Settings("toolsettings.config");
        	s.sayStatus();
        	
        	// das Region object anlegen
        	// r = new Regions(gd);
        	// alle userverzeichnisse durchsuchen
        	for (Iterator<User> iter = s.users.iterator();iter.hasNext();){
        		u = (User)iter.next();
        		// selections einlesen...vorher wird gecleart
        		// int i = r.readVerz(s.getDirectory("selections") + File.separator + u.getName() + File.separator);
        		// outText.addOutLine("Regions in selections read: " + i);
        		// in zuege liegt / liegen die CRs mit den Befehlen 
        		String verz = s.getDirectory("zuege") + File.separator + u.getName() + File.separator;
        		// Verzeichnis bekannt..jetzt abarbeiten
        		workVerz(verz);
        	}
        	
        } catch (Throwable exc) { // any fatal error
            outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt            
            System.exit(1);
        }
        outText.addOutLine("OK\n");
    }

    private static void workVerz(String verz){
    	// bekommt ein Userverzeichnis übergeben, die Settings sind global
    	File file = new File(verz);
    	File[] files = file.listFiles();
    	if (files!=null) {
    		if (files.length>0) {
		    	for (int i = 0; i < files.length; i++) {
		    		File actF = files[i];
		    		if (actF.getName().endsWith(".xt1.cr") && actF.isFile()) {
		    			// ein CR ist im Verzeichnis gefunden worden...
		    			outText.addOutLine("work on file: " + actF);
		    			workFile(actF);
		    			break;
		    		} else if (!actF.getName().endsWith(".cr") && actF.isFile()){
		    			// kein skipping mehr im log
		    			// outText.addOutLine("skipping: " + actF);
		    		}
		 		}
    		} else {
    			// outText.addOutLine("dir empty: " + verz);
    		}
    	} else {
    		// outText.addOutLine("no dir found: " + verz);
    	}
    }

    
    private static void workFile(File f){
    	// f = ein CR mit den Befehlen für eine oder mehrere Regionen
    	// in r immer noch die bereits geladenen Selektion(s) der gültigen Regionen
    	// in s immer noch die Settings
    	// trotzdem checken....
    	if (s==null){
    		outText.addOutLine("Settings s are null: " + f.getName());
    		return;
    	}
    	
    	// das MailLog anlegen
    	LogMail log = new LogMail(s);
    	// in u eiegentlich noch der user ?!
    	log.setSubject("MonopolServer: Befehlseingang von " + u.getName());
    	
    	// den abgelegten CR mit den Befehlen laden...
    	GameData myGD = loadFile(f);
    	if (myGD==null) {
    		outText.addOutLine("workFile produces no GameData: " + f.getName());
    		log.cancel("workFile produces no GameData: " + f.getName());
    		return;
    	}
    	
    	r = new Regions(myGD);
    	if (r==null){
    		outText.addOutLine("Region is null / no valid region(s) defined: " + f.getName());
    		log.cancel("Region is null / no valid region(s) defined: " + f.getName());
    		return;
    	}
     	int i = r.readVerz(s.getDirectory("selections") + File.separator + u.getName() + File.separator);
		outText.addOutLine("Regions in selections read: " + i);
		if (i==0) {
			outText.addOutLine("- no selections - ne regions to send -");
			log.cancel("- no selections - ne regions to send -");
    		return;
		}
		log.addLog("Total Regions in File: " + r.getOverallRegionCount() + ", selected: " + i);
    	// some checks
    	
		
		
		
    	// seemes to be OK
    	
    	// hier fehlt noch order check: kein Stirb!
    	// TODO: checks!
    	
    	// in myGD die validen Region selectieren (unnötig..bekommt der CRwriter)  	
    	// myGD.setSelectedRegionCoordinates(r.getSelectedRegions());
    	
    	// diese in temp exportieren
    	// damit nur diese mit dem monopol.cr gemerged werden
    	File tempFile = null;
    	try {
    		outText.addOutLine("preparing temp.cr");
    		// log.addLog("preparing temp.cr");
    		tempFile = new File("temp.cr");
    		// Filetype organisieren..brauchen wir zum init des CRWriters
    		// log.addLog("getting filetype");
    		FileType filetype = FileTypeFactory.singleton().createFileType(tempFile, false);
    		// log.addLog("setCreateBackup");
    		filetype.setCreateBackup(false);
    		// log.addLog("new CRWriter");
    		// String s2 = FileType.DEFAULT_ENCODING;
    		String s2 = Encoding.ISO.toString();
    		
    		// log.addLog("with lokale: " + s2);
    		CRWriter crw = new CRWriter(null,filetype,s2);
    		// alle anderen Values des CRw auf default
    		crw.setRegions(r.getSelectedRegions().values());
    		crw.setServerConformance(false);
    		// temp.cr schreiben
    		myGD.setEncoding(Encoding.ISO.toString());
    		crw.writeSynchronously(myGD);
    		// Thread t = crw.writeAsynchronously(myGD);
    		// while (t.isAlive()){}
    		crw.close();
    		outText.addOutLine("wrote temp.cr");
    		// log.addLog("wrote temp.cr");
    	} catch (IOException e) {
    		outText.addOutLine("IOException writing the temp.cr: " + f.getAbsolutePath());
    		log.cancel("IOException writing the temp.cr: " + f.getAbsolutePath() + "\n" + e.getMessage());
    		return;
    	}
    	
    	// eigentliche Datei umbenennen
    	String oldName = f.getAbsolutePath();
    	String newName = oldName + "_proc" + FileCopy.getDateS();     	
    	f.renameTo(new File(newName));
    	
    	// Aus dem temp.cr die Befehlsdateien erstellen...dazu diesen
    	// wieder als myGD einlesen...
    	
    	// auch zum Mergen unseren temp.cr nutzen
    	// tempFile dürfte nicht null sein (s.o.)
    	myGD = loadFile(tempFile);
    	outText.addOutLine("temp.cr loaded for generating orders and merging");

    	// a check
    	
    	// Marcs EWG Bug...copy a unit from a foreign unit into a region you have right to send in...
		// and the unit will be sent to the server...
		// so we try to check this here...lets load gd here 
		gd = readMonopol();
    	outText.addOutLine("read monopol.cr for checking UnitRegionStatus");
    	long startT = System.currentTimeMillis();
    	long unitCounter = 0;
    	boolean checkOK = true;
    	
    	for (Iterator<UnitID> iter = myGD.units().keySet().iterator();iter.hasNext();){
    		// im Keyset müssten unit IDs sein...
    		UnitID myGDUnitID = (UnitID)iter.next();
    		Unit myGDUnit = myGD.getUnit(myGDUnitID);
    		
    		// nur die weiter checken, die uns gehören
    		String actFactionName = myGDUnit.getFaction().getID().toString();
	    	if (s.isKnownFaction(actFactionName)) {
	    		
	    		Unit gdUnit = gd.getUnit(myGDUnitID);
	    		// Tempunit sausschliessen und Spione
	    		if (!myGD.tempUnits().containsKey(myGDUnitID) && myGDUnit.getCombatStatus()!=-1) {
	    			// ist keine TempUnit!
	    			unitCounter++;
	    			if (gdUnit == null){
	    				// Einheit nicht im Report...Problem!
	    				outText.addOutLine("Unit not in monopol data: " + myGDUnit.toString(true));
	    				log.addLog("Unit not in monopol data: " + myGDUnit.toString(true));
	    				checkOK = false;
	    				break;
	    			}
	    			// check ob RegionenIDs gleich sind...
	    			if (!myGDUnit.getRegion().getID().equals(gdUnit.getRegion().getID())) {
	    				outText.addOutLine("Unit not in the same region! " + myGDUnit.toString(true));
	    				checkOK = false;
	    				log.addLog("Unit not in the same region! " + myGDUnit.toString(true));
	    				break;
	    			}
	    		}
	    		if (myGDUnit.getCombatStatus()==-1){
	    			// Spion gefunden (?!)
	    			outText.addOutLine("Spion erkannt:" + myGDUnit.toString(true));
	    			log.insertAtBeginn("Spion erkannt:" + myGDUnit.toString(true));
	    		}
    		}
    	}
    	
    	if (!checkOK){
    		outText.addOutLine("UnitRegionStatus is not OK - aborting");
    		log.cancel("UnitRegionStatus is not OK - aborting");
    		return;
    	}
    	
    	long endT = System.currentTimeMillis();
    	outText.addOutLine("Check UnitRegionStatus took " + (endT-startT) + "ms. Checked " + unitCounter + " units. Nothing to worry about.");
    	    	
    	
    	// Tags
    	// aus den Tags Orders machen in myGD
    	FFToolsTags.AllTags2Orders(myGD);
    	log.addLog("All Tags 2 Orders finished.");
    	
    	ArrayList<Region> unconfirmedRegionList = new ArrayList<Region>();
    	
    	
    	// nun Befehlsdateien erstellen
    	int factionCounter = 0;
    	int selectedOverallUnits = 0;
    	int selectedOverallUnitsConfirmed = 0;
    	for (Iterator<myFaction> iter = s.factions.iterator();iter.hasNext();){
    		myFaction myF = (myFaction)iter.next();
    		if (myF.isSendOrders()){
	    		Faction actGDFaction = findFaction(myF.getName());
	    		int counterFactionUnits = -1;
	    		int counterFactionUnitsConfirmed = -1;
	    		int selectedCounterFactionUnits = -1;
	    		int selectedCounterFactionUnitsConfirmed = -1;
	    		if (actGDFaction != null) {
	    			counterFactionUnits = countUnits(actGDFaction,gd,false);
	    			counterFactionUnitsConfirmed =  countUnits(actGDFaction,gd,true);
	    			selectedCounterFactionUnits = countUnits(actGDFaction,myGD,false);
	    			selectedCounterFactionUnitsConfirmed =  countUnits(actGDFaction,myGD,true);
	    			selectedOverallUnits+=selectedCounterFactionUnits;
	    			selectedOverallUnitsConfirmed += selectedCounterFactionUnitsConfirmed;
	    			unconfirmedRegionList = addUnconfirmedRegionsOfFaction(actGDFaction,myGD,unconfirmedRegionList);
	    		}
	    		outText.addOutLine("checking Faction for orders:" + myF.getName() + " (known units:" + counterFactionUnits + "," + counterFactionUnitsConfirmed + " confirmed)");
	    		log.addLog("checking Faction for orders:" + myF.getName() + " (known units:" + counterFactionUnits + "," + counterFactionUnitsConfirmed + " confirmed)");
	    		outText.addOutLine("in Selection:" + myF.getName() + " (known units:" + selectedCounterFactionUnits + "," + selectedCounterFactionUnitsConfirmed + " confirmed)");
	    		log.addLog("in Selection:" + myF.getName() + " (known units:" + selectedCounterFactionUnits + "," + selectedCounterFactionUnitsConfirmed + " confirmed)");
	    		if (myF.makeToSend(myGD, s, u,log)) {
	    			factionCounter++;
	    		}
    		} else {
    			outText.addOutLine("skipping faction (do not send orders!): " + myF.getName());
	    		log.addLog("skipping faction (do not send orders!): " + myF.getName());
    		}
    	}
    	
    	log.insertAtBeginn("Status: " + selectedOverallUnitsConfirmed + " von " + selectedOverallUnits + " bestätigt.");
    
    	if (factionCounter==0){
    		outText.addOutLine("- no faction has orders to send - aborting.");
    		log.cancel("- no faction has orders to send - aborting.");
    		// nothing to do...
    		return;
    	}
    	
    	// backup des monopol.cr anlegen
    	// bzw FileCopy
    	// Zeitstring generieren...
  	  	String dateS = FileCopy.getDateS();
    	try {
    		FileCopy.copy(s.getDirectory("monopolreport"), s.getDirectory("monopolreport") + dateS);
    		
    	} catch (IOException e) {
    		outText.addOutLine("IOException saving the monopol.cr: " + s.getDirectory("monopolreport"));
    		log.cancel("IOException saving the monopol.cr: " + s.getDirectory("monopolreport"));
    		return;
    	}
    	outText.addOutLine("monopol.cr saved: " + s.getDirectory("monopolreport") + dateS);
    	log.addLog("monopol.cr saved: " + s.getDirectory("monopolreport") + dateS);
    	
    	
    	if (unconfirmedRegionList.size()>0){
    		log.addLog("Regions with possible units still needing orders:");
    		for (Region r:unconfirmedRegionList){
    			log.addLog(r.toString());
    		}
    	}
    	
        log.overAndOut();
    	
    }
    
    public static GameData loadFile(File f){
    	// laden
    	String fileName = f.getAbsolutePath();
    	GameData data = null;
    	
    	outText.addOutLine("trying to load: " + f.getName());
    	try {
    		FileType filetype = FileTypeFactory.singleton().createFileType(new File(fileName), true);
    		filetype.setCreateBackup(false);
            data = new GameDataReader(null).readGameData(filetype);
            outText.addOutLine("loaded with encoding: " + filetype.getEncoding());
        } catch (FileTypeFactory.NoValidEntryException e) {
        	outText.addOutLine("Fehler beim Laden des Reports");
        	outText.addOutLine(e.toString());
            System.exit(1);
            return null;
        } catch (Exception exc) {
            // here we also catch RuntimeExceptions on purpose!
            // } catch (IOException exc) {
        	outText.addOutLine("Schwerer Fehler beim Laden des Reports");
        	outText.addOutLine(exc.toString());
            System.exit(1);
            return null;
        }
        outText.addOutLine(f.getName() + " loaded with " + data.regions().size() + " Regions");
        return data;
    }
    
    private static GameData readMonopol(){
    	
    	GameData data = null;
    	String fileName = s.getDirectory("monopolreport");
    	outText.addOutLine("trying to load as monopolreport: " + fileName);
    	try {
    		FileType filetype = FileTypeFactory.singleton().createFileType(new File(fileName), true);
            data = new GameDataReader(null).readGameData(filetype);
            outText.addOutLine("loaded with encoding: " + filetype.getEncoding());
        } catch (FileTypeFactory.NoValidEntryException e) {
        	outText.addOutLine("Fehler beim Laden des Reports");
        	outText.addOutLine(e.toString());
            System.exit(1);
            return null;
        } catch (Exception exc) {
            // here we also catch RuntimeExceptions on purpose!
            // } catch (IOException exc) {
        	outText.addOutLine("Schwerer Fehler beim Laden des Reports");
        	outText.addOutLine(exc.toString());
            System.exit(1);
            return null;
        }
        outText.addOutLine(fileName + " loaded (monopolreport) with " + data.regions().size() + " Regions");
        return data;
    }
   
    
    private static int countUnits(Faction actFact, GameData data, boolean confirmedOnly){
    	int erg = 0;
    	if (actFact==null){return 0;}
    	if (data==null) {return 0;}
    	
    	for (Iterator<Unit> iter = data.units().values().iterator();iter.hasNext();){
    		Unit u = (Unit) iter.next();
    		if (u.getFaction().getID().equals(actFact.getID())){
    			if (confirmedOnly){
    				if (u.isOrdersConfirmed()){
    					erg++;
    				}
    			} else {
    				erg++;
    			}
    		}
    	}
    	return  erg;
    }
    
    private static Faction findFaction(String actFactionName) {
    	Faction erg = null;
    	for (Iterator<Faction> iter = gd.factions().values().iterator();iter.hasNext();){
    		Faction F = (Faction)iter.next();
    		if (F.getID().toString().equalsIgnoreCase(actFactionName)){
    			return F;
    		}
    	}		
    	return erg;
    }
    
    /**
     * Adds the regions with unconfirmed units of that faction to that list 
     * @param actFact
     * @param data
     * @param regionList
     * @return
     */
    private static ArrayList<Region> addUnconfirmedRegionsOfFaction(Faction actFact, GameData data, ArrayList<Region> regionList){
    	if (actFact==null){return regionList;}
    	if (data==null) {return regionList;}
    	
    	for (Iterator<Unit> iter = data.units().values().iterator();iter.hasNext();){
    		Unit u = (Unit) iter.next();
    		if (u.getFaction().getID().equals(actFact.getID()) && !u.isOrdersConfirmed()){
    			Region r = u.getRegion();
    			if (!regionList.contains(r)){
    				regionList.add(r);
    			}
    		}
    	}
    	
    	
    	return regionList;
    }
    
}
