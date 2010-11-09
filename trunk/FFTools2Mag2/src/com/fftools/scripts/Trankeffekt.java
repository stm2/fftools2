package com.fftools.scripts;

import java.text.NumberFormat;

import magellan.library.rules.ItemType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;




public class Trankeffekt extends MatPoolScript{
	
	
	private static final int Durchlauf_vorMP = 2;
	private static final int Durchlauf_nachMP = 162;
	
	private int[] runsAt = {Durchlauf_vorMP,Durchlauf_nachMP};
	
	private int requestPrio = 20;
	
	private int vorratsRunden = 2;
	
	private int otherPersons = 0;
	
	private int personenWirkung=10;
	
	private String trank = null;
	
	private ItemType itemType=null;
	
	private MatPoolRequest myMPR = null;

	private boolean notSuccessACK = false;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Trankeffekt() {
		super.setRunAt(this.runsAt);
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
		switch (scriptDurchlauf){
		
		case Durchlauf_vorMP:this.vorMatPool(scriptDurchlauf);break;
	
		case Durchlauf_nachMP:this.nachMatPool(scriptDurchlauf);break;
		}
	}
	
	public void vorMatPool(int scriptDurchlauf){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		
		String trankName = OP.getOptionString("Trank");
		if (trankName.length()<2) {
			this.addComment("Trankeffekt: Trank nicht angegeben.");
			outText.addOutLine("!!!Trankeffekt: Trank nicht angegeben. " + this.unitDesc());
			return;
		}
		this.itemType = this.gd_Script.rules.getItemType(trankName);
		if (this.itemType==null){
			this.addComment("Trankeffekt: Trank nicht erkannt.");
			outText.addOutLine("!!!Trankeffekt: Trank nicht erkannt. " + this.unitDesc());
			return;
		}
		this.trank = trankName;
		
		// wieviel trank auf Vorrat
		
		int userVorrat = OP.getOptionInt("vorrat",-1);
		if (userVorrat>0){
			this.vorratsRunden = userVorrat;
		}
		
		int userOtherPersons = OP.getOptionInt("other", -1);
		if (userOtherPersons>0){
			this.otherPersons = userOtherPersons;
		}
		
		int persZahl = this.scriptUnit.getUnit().getModifiedPersons()+this.otherPersons;
		
		if (this.trank.equalsIgnoreCase("Bauernblut")){
			this.personenWirkung = 100;
		}
		
		
		// wieviel Trank braucht die einheit pro runde?
		double usagePerRound = (double)(persZahl)/ this.personenWirkung;
		
		int usageNeeded = (int)Math.ceil(usagePerRound * (double)this.vorratsRunden);

		NumberFormat NF = NumberFormat.getInstance();
		NF.setMaximumFractionDigits(0);
		NF.setMinimumFractionDigits(0);
		NF.setMinimumIntegerDigits(1);
		NF.setMaximumIntegerDigits(5);
		
		
		this.addComment("Vorrat " + this.trank + ": daher ben�tigt: " + usageNeeded + " Tr�nke (mit Prio=" + this.requestPrio + ")");
		this.addComment("Vorrat: " + this.trank + " pro Runde ben�tigt:" + usagePerRound + " (bei " + persZahl + " ber�cksichtigten Personen)");
		
		
		// priorit�t
		int userPrio = OP.getOptionInt("Prio",-1);
		if (userPrio>0){
			this.requestPrio=userPrio;
		}
		
		
		// notSuccessACK
		this.notSuccessACK = OP.getOptionBoolean("notSuccessACK", this.notSuccessACK);
		
		
		// entsprechenden request loswerden
		this.myMPR = new MatPoolRequest(this,usageNeeded,this.trank,this.requestPrio,"TrankEffekt");
		this.addMatPoolRequest(this.myMPR);
		// Effekt feststellen.
		int effects = this.scriptUnit.getEffekte(this.trank);
		if (effects<persZahl){
//			 Trank benutzen
			this.addOrder("BENUTZEN " +  NF.format(Math.ceil((double)persZahl/this.personenWirkung)) + " " + this.trank,false);
			
		} else {
			this.addComment("Trank " + this.trank + " ausreichend.");
		}
		
	}
	
	public void nachMatPool(int scriptDurchlauf){
		if (this.myMPR!=null && this.myMPR.getOriginalGefordert()>0){
			if (this.myMPR.getBearbeitet()<this.myMPR.getOriginalGefordert()){
				this.addComment("Trank vorrat nicht ausreichend!. (" +  this.trank + ")");
				if (!this.notSuccessACK) {
					this.scriptUnit.doNotConfirmOrders();
				}
			}
		}
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
	
}
