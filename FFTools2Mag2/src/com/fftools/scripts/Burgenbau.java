package com.fftools.scripts;

import magellan.library.CoordinateID;

import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;


/**
 * 
 * Stösst den automatischen Burgenbau in dem TA an
 * @author Fiete
 *
 */

public class Burgenbau extends MatPoolScript{
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf1 = 8;
	
	private int[] runners = {Durchlauf1};
	
	/**
	 * wieviel Burgenaufträge werden automatisch vergeben ?
	 */
	private int Anzahl = 3;
	
	
	public int getAnzahl() {
		return Anzahl;
	}


	/**
	 * Mit welcher Prio fordert der wichtigste Bauauftrag an?
	 */
	private int Prio = 200;
	
	
	public int getPrio() {
		return Prio;
	}


	// Konstruktor
	public Burgenbau() {
		super.setRunAt(this.runners);
	}
	
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf1){
			this.run1();
		}
        

		
	}
	
	
	private void run1(){
		
		super.addVersionInfo();
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		
		
		this.Anzahl = OP.getOptionInt("Anzahl", 3);
		this.Prio = OP.getOptionInt("Prio", 200);
		
		// home
		String homeString=OP.getOptionString("home");
		if (homeString.length()>2){
			CoordinateID actDest = null;
			if (homeString.indexOf(',') > 0) {
				actDest = CoordinateID.parse(homeString,",");
			} else {
			// Keine Koordinaten, also Region in Koordinaten konvertieren
				actDest = FFToolsRegions.getRegionCoordFromName(this.gd_Script, homeString);
			}
			if (actDest!=null){
				this.getBauManager().setCentralHomeDest(actDest, this.scriptUnit);
			} else {
				this.addComment("!!! HOME Angabe nicht erkannt!");
				this.doNotConfirmOrders();
			}
		}
		
		
		this.getBauManager().addBurgenbau(this);
	}	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}



}
