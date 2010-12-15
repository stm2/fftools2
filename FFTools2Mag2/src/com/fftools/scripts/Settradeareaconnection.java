package com.fftools.scripts;

import com.fftools.ScriptUnit;
import com.fftools.utils.FFToolsOptionParser;



/**
 * 
 * Definiert eine Handelsbeziehung zwischen 2 TradeAreas anhand 2er Units
 * 
 * @author Fiete
 *
 */

public class Settradeareaconnection extends TradeAreaScript{
	
	

	private static final int Durchlauf_vorMP1 = 15;
	private static final int Durchlauf_nachMP1 = 38;
	
	
	private int[] runners = {Durchlauf_vorMP1,Durchlauf_nachMP1};
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Settradeareaconnection() {
		super.setRunAt(this.runners);
	}
	
	
	/**
	 * Eigentliche Prozedur
	 * runScript von Script.java MUSS/SOLLTE ueberschrieben werden
	 * Durchlauf kommt von ScriptMain
	 * 
	 * in Script steckt die ScriptUnit
	 * in der ScriptUnit steckt die Unit
	 * mit addOutLine jederzeit Textausgabe ans Fenster
	 * mit addComment Kommentare...siehe Script.java
	 */
	
	public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf_vorMP1){
			this.scriptStart();
		}
		
		if (scriptDurchlauf==Durchlauf_nachMP1){
			this.scriptStart_nachMP1();
		}
		
		
	}
	
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"SetTradeAreaConnection");
		
		// Pflichtfelder
		// Zielunit
		String targetUnitName = OP.getOptionString("ziel");
		if (targetUnitName.length()==0){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC: ziel fehlt! -> Unbestaetigt!!");
			return;
		}
		
		ScriptUnit scu = this.scriptUnit.getScriptMain().getScriptUnit(targetUnitName);
		if (scu==null){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC: ziel nicht gefunden! -> Unbestaetigt!!");
			return;
		}
		String Name = OP.getOptionString("name");
		if (Name.length()<2){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC: Name fehlt!!! -> unbestaetigt");
			return;
		}
		
		String erg = super.getTradeAreaHandler().addTradeAreaConnector(this.scriptUnit,scu, Name);
		if (erg!=""){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC gescheitert: " + erg);
			return;
		} else {
			this.addComment("setTAC erfolgreich");
		}
	}
	
	
	private void scriptStart_nachMP1(){
		
	}
	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}

}
