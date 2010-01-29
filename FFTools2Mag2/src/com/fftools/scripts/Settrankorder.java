package com.fftools.scripts;

import com.fftools.pools.alchemist.AlchemistManager;



/**
 * Simples Script zum Setzen der Trankorder
 * alles wensentliche passiert in <code>AlchemistTrank</code>
 * @author Fiete
 *
 */
public class Settrankorder extends Script{
	
	
	private static final int Durchlauf = 0;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Settrankorder() {
		super.setRunAt(Durchlauf);
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
		
		if (scriptDurchlauf==Durchlauf){
			this.scriptStart();
		}
	}
	
	/**
	 * here we go
	 *
	 */
	private void scriptStart(){
		AlchemistManager AM = this.getOverlord().getAlchemistManager();
		AM.addTrankOrder(this.scriptUnit,this.getArguments());
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
	
	/**
	 * wenn beim start im client regionen selected sind, dann nur die
	 * scripts ausf�hren,die hier true liefern, alle anderen nicht.
	 * @return
	 */
	public boolean runIfNotInSelectedRegions(){
		return true;
	}
	
}
