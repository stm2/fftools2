package com.fftools.scripts;

import magellan.library.Region;
import magellan.library.RegionResource;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

public class Eisen extends MatPoolScript{
	
	
	
	int Durchlauf_1 = 20;
	

	
	private int[] runners = {Durchlauf_1};
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Eisen() {
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
		if (scriptDurchlauf==this.Durchlauf_1){
			this.start();
		}

	}
	
	
	
	/**
	 * eigentlich ganz einfach: nur so lange eisen machen,
	 * bis die Talentstufe nicht mehr ausreicht
	 */
	private void start(){
		// Eigene Talentstufe ermitteln
		int skillLevel = 0;
		SkillType skillType = this.gd_Script.rules.getSkillType("Bergbau", false);
		if (skillType!=null){
			Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
			if (skill!=null){
				skillLevel = skill.getLevel();
			} else {
				this.addComment("!!! unit has no skill Bergbau!");
			}
		} else {
			this.addComment("!!! can not get SkillType Bergbau!");
		}
		if (skillLevel>0){
			// Regionslevel beziehen
			Region R = this.scriptUnit.getUnit().getRegion();
			ItemType IT = this.gd_Script.rules.getItemType("Eisen");
			RegionResource RR = R.getResource(IT);
			if (RR.getSkillLevel()<=skillLevel) {
				// weiter machen
				this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
				this.addOrder("machen Eisen ;(script Eisen)", true);
			} else {
				this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen NICHT weiter ab, ich kann ja nur T" + skillLevel);
				this.addOrder("Lernen Bergbau", true);
				this.doNotConfirmOrders();
			}
		} else {
			this.addOrder("Lernen Bergbau", true);
			this.doNotConfirmOrders();
		}
		
	}

}
