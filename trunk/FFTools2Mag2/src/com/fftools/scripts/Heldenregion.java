package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Faction;
import magellan.library.rules.SkillType;

import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;




public class Heldenregion extends MatPoolScript{
	
	
	private static final int Durchlauf = 10;
	
	private static final int defaultSilverPrio = 200;
	
	// parameter partei
	private String factionNumber = null;
	// partei als faction...
	private Faction faction = null;
	
	// prio
	private int prio = 0;
	// nur eingheiten mit diesme lernplan
	private ArrayList<String> LernplanListe=null;
	// nach diesem Talent sortieren
	private ArrayList<String> TalentListe=null;
	// der skill zu dem gew�hlten talent
	private ArrayList<SkillType> talentSkillTypeListe = null;
	// die geplanten helden und der entsprechende MatPool Silber Request
	private Hashtable<ScriptUnit,MatPoolRequest> designierteHelden = null;
	
	
	/**
	 * @return the lernplanListe
	 */
	public ArrayList<String> getLernplanListe() {
		return LernplanListe;
	}


	/**
	 * @return the talentSkillTypeListe
	 */
	public ArrayList<SkillType> getTalentSkillTypeListe() {
		return talentSkillTypeListe;
	}


	/**
	 * @return the faction
	 */
	public Faction getFaction() {
		return faction;
	}


	/**
	 * @return the prio
	 */
	public int getPrio() {
		return prio;
	}

	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Heldenregion() {
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
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// Faction MUSS angegeben sein
		this.factionNumber = OP.getOptionString("partei");
		if (this.factionNumber.length()<1){
			this.addComment("F�r ein Aufruf von Heldenregion fehlt der Partei-Parameter");
			outText.addOutLine("!!! F�r ein Aufruf von Heldenregion fehlt der Partei-Parameter: " + this.unitDesc(), true);
			return;
		}
		
		// existiert eine Partei dazu?
		boolean factionExists = false;
		for (Faction actFaction : this.gd_Script.getFactions()){
			if (actFaction.getID().toString().equalsIgnoreCase(this.factionNumber)){
				factionExists=true;
				this.faction = actFaction;
				break;
			}
		}
		
		if (!factionExists){
			this.addComment("Heldenregion: Partei " + this.factionNumber + " ist unbekannt.");
			outText.addOutLine("!!! Heldenregion: Partei " + this.factionNumber + " ist unbekannt. "+ this.unitDesc(), true);
			return;
		}
		
		this.prio = OP.getOptionInt("prio", Integer.MIN_VALUE);
		if (this.prio<Integer.MAX_VALUE){
			// irgeneine Prio angegeben
			if (this.prio<1 || this.prio> 10000){
				this.addComment("F�r ein Aufruf von Heldenregion ung�ltige Prio: " + this.prio);
				outText.addOutLine("!!! F�r ein Aufruf von Heldenregion ung�ltige Prio: " + this.prio + " (" + this.unitDesc() + ")", true);
				return;
			}
		} else {
			this.addComment("HeldenRegion: benutze f�r " + this.factionNumber + " DEFAULT Silber Prio (" + Heldenregion.defaultSilverPrio + ")");
			this.prio = Heldenregion.defaultSilverPrio;
		}
		
		this.LernplanListe = OP.getOptionStringList("Lernplan");
		if (this.LernplanListe.size()==0){
			this.addComment("F�r ein Aufruf von Heldenregion fehlender Lernplan. Partei: " + this.factionNumber);
			outText.addOutLine("!!! F�r ein Aufruf von Heldenregion fehlender Lernplan. Partei: " + this.factionNumber + " (" + this.unitDesc() + ")", true);
			return;
		}
		
		this.TalentListe = OP.getOptionStringList("Talent");
		// Talent Muss angegeben werden
		if (this.TalentListe.size()==0){
			this.addComment("F�r ein Aufruf von Heldenregion fehlendes Talent. Partei: " + this.factionNumber);
			outText.addOutLine("!!! F�r ein Aufruf von Heldenregion fehlendes Talent: " + this.factionNumber + " (" + this.unitDesc() + ")", true);
			return;
		}
		for (Iterator<String> iter = this.TalentListe.iterator();iter.hasNext();){
			String actTalent = (String)iter.next();
			// Talent pr�fen
			SkillType actSkillType = this.gd_Script.rules.getSkillType(actTalent, false);
			if (actSkillType==null){
				this.addComment("F�r ein Aufruf von Heldenregion ung�ltiges Talent: " + actTalent);
				outText.addOutLine("!!! F�r ein Aufruf von Heldenregion ung�ltiges Talent: " + actTalent + " (" + this.unitDesc() + ")", true);
				return;
			} 
			
			if (this.talentSkillTypeListe==null){
				this.talentSkillTypeListe = new ArrayList<SkillType>();
			}
			this.talentSkillTypeListe.add(actSkillType);
		}
		// Parsen fertig
		boolean ret;
		ret = this.getOverlord().getHeldenRegionsManager().addHeldenregion(this);
		if (!ret){
			// nicht akzeptiert
			this.addComment("Der Eintrag als Heldenregion scheiterte f�r Partei: " + this.factionNumber);
			outText.addOutLine("!!! Der Eintrag als Heldenregion scheiterte f�r Partei: " + this.factionNumber + " (" + this.unitDesc() + ")", true);
		} 
		
		// fertig
	}
	
	/**
	 * f�gt eine als held vorgesehen unit und ihren silberrequest
	 * hinzu
	 * @param u
	 * @param Silberbedarf
	 */
	public void addHeld(ScriptUnit u, int Silberbedarf){
		Lernfix lernfix = (Lernfix)u.getScript(Lernfix.class);
		
		MatPoolRequest mpr = new MatPoolRequest(lernfix,u.getUnit().getModifiedPersons() * Silberbedarf,"Silber",this.prio,"Bef�rderung");
		if (this.designierteHelden==null){
			this.designierteHelden = new Hashtable<ScriptUnit, MatPoolRequest>();
		}
		this.designierteHelden.put(u, mpr);
		lernfix.addMatPoolRequest(mpr);
		lernfix.addComment("Bef�rderung geplant f�r " + mpr.getOriginalGefordert() + " Silber");
		this.addComment("Bef�rderung geplant (" + this.faction + ") f�r: " + u.getUnit().toString(true) + " (" + u.getUnit().getModifiedPersons() + " pers)");
	}
	
	/**
	 * �berpr�ft den bef�rderungsstatus und gibt anzahl
	 * der vermutlich erfolgreichen bef�rderungen zur�ck
	 * @return
	 */
	public int checkHelden(){
		int erg = 0;
		if (this.designierteHelden==null || this.designierteHelden.size()==0){
			return 0;
		}
		
		for (Iterator<ScriptUnit> iter=this.designierteHelden.keySet().iterator();iter.hasNext();){
			ScriptUnit actScriptUnit = (ScriptUnit)iter.next();
			MatPoolRequest mpr = this.designierteHelden.get(actScriptUnit);
			Lernfix lernfix = (Lernfix)actScriptUnit.getScript(Lernfix.class);
			if (mpr.getBearbeitet()<mpr.getOriginalGefordert()){
				// nicht ausreichend silber
				lernfix.addComment("Bef�rderung nicht m�glich: " + mpr.getBearbeitet() + "/" + mpr.getOriginalGefordert() + " Silber (Prio:" + mpr.getPrio() + ")");
				lernfix.removeMatPoolRequest(mpr);
			} else {
				// ausreichend silber
				lernfix.addComment("Bef�rderung geplant: " + mpr.getBearbeitet() + "/" + mpr.getOriginalGefordert() + " Silber (Prio:" + mpr.getPrio() + ")");
				lernfix.addOrder("BEF�RDERUNG",true);
				erg+=actScriptUnit.getUnit().getModifiedPersons();
			}
		}
		
		
		return erg;
	}
	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		// darf mehrfach vorkommen, gilt ja f�r die region
		return true;
	}
	
}
