package com.fftools.scripts;

import magellan.library.rules.ItemType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;




public class Trankeffekt extends MatPoolScript{
	
	
	private static final int Durchlauf_vorMP = 2;
	private static final int Durchlauf_nachMP = 162;
	
	private int[] runsAt = {Durchlauf_vorMP,Durchlauf_nachMP};
	
	private int requestPrio = 20;
	
	private int vorratsRunden = 2;
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
		// wieviel Trank braucht die einheit pro runde?
		double usagePerRound = (double)this.scriptUnit.getUnit().getModifiedPersons() / 10;
		// wieviel trank auf Vorrat
		
		int userVorrat = OP.getOptionInt("vorrat",-1);
		if (userVorrat>0){
			this.vorratsRunden = userVorrat;
		}
		
		
		int usageNeeded = (int)Math.ceil(usagePerRound * (double)this.vorratsRunden);
		
		// priorität
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
		if (effects<this.scriptUnit.getUnit().getModifiedPersons()){
//			 Trank benutzen
			for (int i = 0;i<Math.ceil((double)this.scriptUnit.getUnit().getModifiedPersons()/10);i++){
				this.addOrder("BENUTZEN " +  this.trank,false);
			}
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
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
	
}
