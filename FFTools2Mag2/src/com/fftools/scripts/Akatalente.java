package com.fftools.scripts;

import java.util.ArrayList;

import magellan.library.Building;
import magellan.library.rules.SkillType;

import com.fftools.pools.akademie.AkademieManager;
import com.fftools.pools.akademie.AkademiePool;
import com.fftools.pools.akademie.AkademieTalent;

/**
 * Entscheidet, dass die Akademie, deren Besitzer diese Einheit ist
 * sich automatisch Nutzer aus dem Pool sucht
 * 
 * @author Fiete
 *
 */

public class Akatalente extends Script{
	
	
	private static final int Durchlauf = 662;
	
	private AkademiePool AP=null;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Akatalente() {
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
		// 	gibt es eigentlich eine Akademie zum verwalten?
		Building b = this.scriptUnit.getUnit().getModifiedBuilding();		
		if (b!=null){
			// ah, wir sind in einem gebäude! eine akademie?
			if (b.getBuildingType().getName().equals("Akademie")){
				if (b.getOwnerUnit().equals(this.scriptUnit.getUnit())){
				    // ok wir sind besitzer einer akademie! dann melden wir das dem mächtigen AkaManager
					AkademieManager AM = this.getOverlord().getAkademieManager();
					AkademiePool _AP = AM.addAkademie(b,this);
					if (_AP == null){
						// Problem, Pool bereits vorhanden
						addComment("!!! Akademiepool konnte nicht angelegt werden, Besitzerwechsel??");
						doNotConfirmOrders();
					} else {
						// kein Problem
						addComment("AkaTalente: die Aka wurde erfolgreich dem AkaManger bekannt gemacht.");
						this.AP = _AP;
					}
				}
			}else{
				// naja, die hütte ist kein gebäude..
				this.keinVerwalter();
			}
			
		}
		else{
			this.keinVerwalter();
		}	
		
		// Gibt es überhaupt Argumente?
		if (super.getArgCount() > 0 && this.AP!=null) {
            // gleich die übergebenen Talente parsen
			ArrayList<AkademieTalent> akademieTalente = new ArrayList<AkademieTalent>();
			for (String s:this.getArguments()){
				// s hat die Form TalentName:Anzahl
				String[] ss = s.split(":");
				if (ss.length!=2){
					this.addComment("!!! Fehler. Argument " + s + " hat nicht die Form Talent:Anzahl, es fehlt das :");
				} else {
					String talentName = ss[0];
					String talentAnzahl = ss[1];
					SkillType sK = null;
					// SkillType festStellen
					if (talentName.equalsIgnoreCase("draig")||talentName.equalsIgnoreCase("illaun")||talentName.equalsIgnoreCase("tybied")||talentName.equalsIgnoreCase("gwyrrd")||talentName.equalsIgnoreCase("cerddor")){
						// !! Tada MagieTalent
						// entsprechenden SkillType generieren!
						sK=this.scriptUnit.getScriptMain().gd_ScriptMain.rules.getSkillType(talentName.toLowerCase(), true);
					} else {
						// keine Magiename, also muss Talent vorhanden sein
						sK = this.gd_Script.rules.getSkillType(talentName);
					}
					if (sK==null){
						this.addComment("!!! Akatalente: Talent nicht erkannt: " + talentName);
					} else {
						// wir haben ein gültiges Talent...haben wir auch ne Anzahl
						int Anz = Integer.parseInt(talentAnzahl);
						if (Anz<=0 || Anz>25){
							this.addComment("!!! Akatalente: Anzahl nicht erkannt in: " + s + " (" + talentAnzahl + ")");
						} else {
							// hier stimmt Alles
							AkademieTalent AT = new AkademieTalent(sK,Anz);
							akademieTalente.add(AT);
						}
					}
				}
			}
			
			this.addComment("Akatalente: " + akademieTalente.size() + " Talentangaben gelesen");
			this.AP.setAkademieTalente(akademieTalente);
		}	
				
		
	 }
	
	/*
	 * Wenn Einheit kein Akademieverwalter ist!
	 *
	 */
	
	 private void keinVerwalter(){
    	this.addComment("Unbestätigt, da Einheit keine Akademie verwaltet!");
		this.scriptUnit.doNotConfirmOrders();
	 }
	
	
}