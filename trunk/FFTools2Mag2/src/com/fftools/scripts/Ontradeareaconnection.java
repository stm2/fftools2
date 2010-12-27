package com.fftools.scripts;

import java.text.DecimalFormat;

import com.fftools.trade.TradeAreaConnector;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.scripts.Setkapa;



/**
 * 
 * Ordnet die aktuelle unit einer TAC zu
 * 
 * @author Fiete
 *
 */

public class Ontradeareaconnection extends TradeAreaScript{
	
	

	private static final int Durchlauf_vorMP1 = 15;
	private static final int Durchlauf_vorMP1_2 = 16;
	
	
	private int[] runners = {Durchlauf_vorMP1,Durchlauf_vorMP1_2};
	
	private TradeAreaConnector myTAC = null;
	private int kapa = -1;
	private double anteil = -1;
	
	public double getAnteil() {
		return anteil;
	}


	public int getKapa() {
		return kapa;
	}


	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Ontradeareaconnection() {
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
		
		if (scriptDurchlauf==Durchlauf_vorMP1_2){
			this.scriptStart2();
		}
		
		
	}
	
	/**
	 * checken und beim TAH registrieren
	 */
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"OnTradeAreaConnection");
		
		// Pflichtfelder
		// Name des TAC
		String Name = OP.getOptionString("name");
		if (Name.length()<2){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC: Name fehlt!!! -> unbestaetigt");
			return;
		}
		
		// TAC suchen...
		TradeAreaConnector actTAC = super.getTradeAreaHandler().getTAC(Name);
		if (actTAC==null || !actTAC.isValid()){
			this.doNotConfirmOrders();
			this.addComment("!!! ein TAC " + Name + " konnte nicht gefunden werden! -> nicht best�tigt");
			return;
		}
		this.myTAC = actTAC;
		
		
		// checken, ob diese Unit auch ein saetKapa hat
		Object o = this.scriptUnit.getScript(Setkapa.class);
		if (o==null){
			this.doNotConfirmOrders();
			this.addComment("!!! onTradeAreaConnection verlangt zwingend eine setKapa-Angabe -> unbest�tigt");
			return;
		}
		Setkapa sk = (Setkapa)o;
		this.kapa = sk.getKapa();
		this.addComment("onTAC: �bernommene Kapazit�t: " + this.kapa + " GE");
		
		// beim TAC vermerken
		this.myTAC.addMover(this);
		
		
	}
	
	/**
	 * alle Mover sind registriert, eigenen Anteil ausrechnen
	 * Position pr�fen und requests ausl�sen
	 */
	private void scriptStart2(){
		if (this.kapa<=0){
			this.addComment("!!!onTAC:keine kapa");
			return;
		}
		if (this.myTAC==null || !this.myTAC.isValid()){
			this.addComment("!!!onTAC:kein TAC");
			return;
		}
		
		int allSumme = myTAC.getOverallMoverKapa();
		this.addComment(myTAC.getMoverInfo());
		int allWeight = myTAC.getNeededGE();
		this.addComment("GE insgesammt ben�tigt: " + allWeight + " (" + myTAC.getWeightInfo() + ")");
		
		// Anteil
		this.anteil = (double)this.kapa / (double)allSumme;
		DecimalFormat df = new DecimalFormat("0.00");
		this.addComment("TAC: Anteil der Unit nach Kapa:" + df.format(this.anteil * 100) + "%");
	
		
		// Rausfinden, wo wir sind
		
		if (this.getUnit().getRegion().equals(myTAC.getSU1().getUnit().getRegion())){
			// wir sind bei SCU1 und werden danach nach TA2 fahren
			this.myTAC.processMoverRequests(2, this);
			return;
		}
		
		if (this.getUnit().getRegion().equals(myTAC.getSU2().getUnit().getRegion())){
			// wir sind bei SCU2 und werden danach nach TA1 fahren
			this.myTAC.processMoverRequests(1, this);
			return;
		}
		
		// wir sind auf hoher See....
		// GibNix aktivieren?
		this.scriptUnit.setGibNix(true);
		this.addComment("TAC-unit unterwegs: GibNix aktiviert");
		
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
