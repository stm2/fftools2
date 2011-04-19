package com.fftools.scripts;

import com.fftools.utils.FFToolsOptionParser;


/**
 * 
 * St�sst den automatischen Burgenbau in dem TA an
 * @author Fiete
 *
 */

public class Burgenbau extends MatPoolScript{
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf1 = 8;
	
	private int[] runners = {Durchlauf1};
	
	/**
	 * wieviel Burgenauftr�ge werden automatisch vergeben ?
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
		
		
		this.getBauManager().addBurgenbau(this);
	}	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}



}
