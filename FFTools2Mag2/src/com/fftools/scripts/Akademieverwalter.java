package com.fftools.scripts;

import com.fftools.pools.akademie.relations.*;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.pools.akademie.*;

/**
 * Erzeugt ScriptAkademie und meldet diese an AkademiePool
 * 
 * @author Marc
 *
 */

public class Akademieverwalter extends Script{
	
	
	private static final int Durchlauf = 84;
	
	private ScriptAkademie scriptAkademie = null;
	private int schuelerPlaetze=25;
	private AkademiePool AP=null;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Akademieverwalter() {
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
	
	private void scriptStart(){
			
		
		// Gibt es überhaupt Argumente?
				if (super.getArgCount() > 0) {
                    
					// feines util, erst jetzt entdeckt! warum steht es nicht direkt in script? egal..
					FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
					OP.addOptionList(this.getArguments());
					
					// wenn Externe innerhalb der dimension einer akademie ist, dann halten wir auch plätze frei
					int externe = OP.getOptionInt("Gaeste", -1);
					if (externe>0&&externe<=25){
						this.schuelerPlaetze=25-externe;
					}else{
						this.scriptUnit.addComment("Anzahl der Gäste in Akademie unzulässig");
					}
				}	
				
		// gibt es eigentlich eine Akademie zum verwalten?
				
				if (this.scriptUnit.getUnit().getModifiedBuilding()!=null){
					// ah, wir sind in einem gebäude! eine akademie?
					if (this.scriptUnit.getUnit().getModifiedBuilding().getBuildingType().getName().equals("Akademie")){
						if (this.scriptUnit.getUnit().getModifiedBuilding().getOwnerUnit().equals(this.scriptUnit.getUnit())){
						    // ok wir sind besitzer einer akademie! dann melden wir das dem mächtigen AkaPool!
							this.scriptAkademie= new ScriptAkademie(this.scriptUnit.getUnit().getModifiedBuilding(), this.scriptUnit, this.schuelerPlaetze );
						    this.AP = this.scriptUnit.getScriptMain().getOverlord().getAkademieManager().getAkademiePool(this.scriptUnit);
                            this.AP.addScriptAkademie(this.scriptAkademie);
                            this.scriptUnit.addComment("AkademiePool lässt Platz für " + (25-this.schuelerPlaetze) + " Gäste in " +this.scriptAkademie.getAkademie().getName() + " (" + this.scriptAkademie.getAkademie().getID().toString()+ ")");
						}
					}else{
						// naja, die hütte ist kein gebäude..
						this.keinVerwalter();
					}
					
				}
				else{
					this.keinVerwalter();
				}
					
					
	 }
	
	/*
	 * Wenn Einheit kein Akademieverwalter ist!
	 *
	 */
	
	 private void keinVerwalter(){
    	this.addComment("Unbestätigt, da Einheit keine Akademie verwaltet!");
		this.scriptUnit.doNotConfirmOrders();
		// darf auf keinen fall lernen, fa sonst 26 schueler in akademie stehen!
		this.scriptUnit.addOrder("Arbeiten", true, false); 
	 }
	
	
}