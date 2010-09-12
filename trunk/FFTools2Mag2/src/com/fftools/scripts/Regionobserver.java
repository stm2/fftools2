package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.rules.SkillType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;

/**
 * 
 * Job "RegionObserver": Watches region for monsters and other unfriendlies
 * Call: // script RegionObserver [EmbassyUnits=<UnitID>[,<UnitID>]]
 * - Learns according to plan "RegionObserver" (minimum defense, primarily observation)
 * - Guards region unless enemies are present, then he yells and flees combat
 * - Required Items: Cheap "talent-friendly" weapon
 * - Optional Items: AdwS 
 * 
 * @author Torsten
 *
 */

public class Regionobserver extends MatPoolScript{

	// Durchlauf vor Lernfix, wg. Lernplan
	private static final int Durchlauf = 5;

	// Keine Bewachung ohne Waffen...
	private final String[] talentNamen = {"Hiebwaffen","Stangenwaffen","Bogenschießen","Armbrustschießen","Katapultbedienung"};
	// ... und erst werden die Wächter versorgt, dann die Steuereintreiber
	private int WaffenPrio = 350;
	// Ausrüstungs-Requests gehen über den Pool
	private ArrayList<MatPoolRequest> requests = new ArrayList<MatPoolRequest>();

	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Regionobserver() {
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
		
		// 1. Lernen nach Plan...
		this.scriptUnit.findScriptClass("Lernfix", "Lernplan=RegionObserver");

		// 2. Geeignete Waffe anfordern (abgekupfert von "Treiben")
		String comment = "RegionObserver-Waffen";
		boolean didSomething = false;
		for (int i = 0;i<this.talentNamen.length;i++){
			String actName = this.talentNamen[i];
			SkillType actSkillType = this.gd_Script.rules.getSkillType(actName);
			if (actSkillType!=null){
				Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
				if (actSkill!=null && actSkill.getLevel()>0){
					// Was gefunden? --> Preiswerte Waffe anfordern
					String materialName = actSkillType.getName();
					String matNameNeu="nix";
					if (materialName.equalsIgnoreCase("Hiebwaffen")) {
						matNameNeu = "Schwert";
					} else if(materialName.equalsIgnoreCase("Stangenwaffen")){
						matNameNeu = "Speer";
					} else if(materialName.equalsIgnoreCase("Bogenschießen")){
						matNameNeu = "Bogen";
					} else if(materialName.equalsIgnoreCase("Armbrustschießen")){
						matNameNeu = "Armbrust";
					} else if (materialName.equalsIgnoreCase("Katapultbedienung")){
						matNameNeu="Katapult";
					} 
					if (matNameNeu!="nix"){
						// Bestellen
						MatPoolRequest MPR = new MatPoolRequest(this,this.scriptUnit.getUnit().getModifiedPersons(),matNameNeu,this.WaffenPrio,comment);
						this.addMatPoolRequest(MPR);
						didSomething=true;
						this.requests.add(MPR);
					}
					// Amulett bestellen, aber mit niedriger Prio
					MatPoolRequest MPR = new MatPoolRequest(this,1,"Amulett des wahren Sehens",30,"RegionObserver-Amulett");
					this.addMatPoolRequest(MPR);
				}
			}
		}
		if (!didSomething){
			this.addComment("Keine Waffenanforderung - kein Talent vorhanden?");
			this.doNotConfirmOrders();
		}
		
		// 3. Bewachen, falls keine Gegner in der Region sind, sonst Alarm 
		
		// In dieser Region erlaubte Botschafter aus Einheit-Optionen auslesen
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		String[] embassyUnits = null;
		String eU = OP.getOptionString("EmbassyUnits");
		embassyUnits = eU.split(",");
		Arrays.sort(embassyUnits);
		
		// Parteien, denen vertraut wird, aus den Report-Optionen holen
		String[] trustedFactions = null;
		String tF = reportSettings.getOptionString("TrustedFactions", this.region());
		if (tF==null) tF = "";
		trustedFactions = tF.split(",");
		Arrays.sort(trustedFactions);

		// Check all units in region
		int alertStatus=0;
		for (Iterator<Unit> iter = this.region().units().iterator();iter.hasNext();){
			Unit actU = (Unit)iter.next();
			String actF = actU.getFaction().getID().toString().toLowerCase();
			// Do we trust this faction?
			if (Arrays.binarySearch(trustedFactions, actF)< 0) {
				// No, but maybe we trust this unit?
				if (Arrays.binarySearch(embassyUnits, actU.getID().toString()) < 0) {
				// Uh-oh, suspicious unit found!
					this.addComment("WARNUNG: Einheit " + actU.toString(true) + " wird nicht vertraut!");
					alertStatus=1;
					this.doNotConfirmOrders();
				}
			}
		}
		// Alle gefundenen Einheiten vertrauenswürdig? Falls ja, bewachen und 
		if (alertStatus==0) {
			// Falls die Einheit noch nicht bewacht, setze Befehle
			if (this.scriptUnit.getUnit().getGuard()==0) {
				this.scriptUnit.getUnit().addOrders("BEWACHEN", false);
				this.scriptUnit.getUnit().addOrders("KÄMPFE NICHT", false);
			}
		// Nicht alle Einheiten vertrauenswürdig? Volle Deckung...
		} else {
			if (this.scriptUnit.getUnit().getGuard()==1) {
				this.scriptUnit.getUnit().addOrders("BEWACHEN NICHT", false);
				this.scriptUnit.getUnit().addOrders("KÄMPFE FLIEHE", false);
			}
		}
		
		// 4. Further Tasks...
		// TODO oben: Lernplan für "Volks-Waffentalent" ermöglichen (Halbling=Armbrust,
		//		Troll=Hiebwaffen, Insekt=Stangenwaffen, usw.
	
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
}
