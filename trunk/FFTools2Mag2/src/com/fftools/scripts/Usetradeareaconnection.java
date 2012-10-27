package com.fftools.scripts;

import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeAreaConnector;
import com.fftools.utils.FFToolsOptionParser;



/**
 * 
 * Definiert eine Handelsbeziehung zwischen 2 TradeAreas anhand 2er Units
 * 
 * @author Fiete
 *
 */

public class Usetradeareaconnection extends TradeAreaScript{
	
	

	private static final int Durchlauf_vorMP1 = 104;
	
	private static final int default_request_prio=50;
	
	
	private int[] runners = {Durchlauf_vorMP1};
	
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Usetradeareaconnection() {
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
		
		
		
		
	}
	
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// Pflichtfelder
		// TAC-Name
		String Name = OP.getOptionString("name");
		if (Name.length()<2){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC: Name fehlt!!! -> unbestaetigt");
			return;
		}
		// gibts den namen
		TradeAreaConnector myTAC = super.getTradeAreaHandler().getTAC(Name);
		if (myTAC==null){
			this.addComment("!useTAC gescheitert: so einen TAC gibt es nicht: " + Name);
			this.doNotConfirmOrders();
			return;
		}
		if (!myTAC.isValid()){
			this.addComment("!useTAC gescheitert: der TAC ist ungültig : " + Name);
			this.doNotConfirmOrders();
			return;
		}
		
		// Ware
		String Ware = OP.getOptionString("Ware");
		if (Ware.length()<2){
			this.addComment("!useTAC gescheitert: Ware nicht erkannt : " + Ware);
			this.doNotConfirmOrders();
			return;
		}
		
		// Summe
		int Summe = OP.getOptionInt("Summe",-1);
		if (Summe<1){
			Summe = OP.getOptionInt("Menge",-1);
			if (Summe<1){
				this.addComment("!useTAC gescheitert: Summe nicht erkannt : " + Summe);
				this.doNotConfirmOrders();
				return;
			}
		}
		
		// prio 
		int prio = OP.getOptionInt("Prio",-1);
		if (prio<1){
			prio = default_request_prio;
			this.addComment("useTAC: benutze für " + Ware + " auf " + Name + " Defaultprio: " + prio);
		} else {
			this.addComment("useTAC: benutze für " + Ware + " auf " + Name + " gesetzte Prio: " + prio);
		}
		
		TradeArea myTA = this.getTradeArea();
		// Ergänzen
		myTAC.addUsage(myTA, Ware, Summe, prio);
		
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
