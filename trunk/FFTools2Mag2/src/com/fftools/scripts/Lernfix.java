package com.fftools.scripts;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import magellan.library.Skill;
import magellan.library.StringID;
import magellan.library.rules.SkillType;

import com.fftools.pools.ausbildung.AusbildungsPool;
import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
/**
 * AusbildungsRelationemitter, das zentrale Lernscript im Monopol
 * 
 * @author Marc
 *
 */

public class Lernfix extends MatPoolScript{
	
	
	private static final int Durchlauf = 62; // ge�ndert von 2 auf 5, damit Pferde davor passt
	private AusbildungsPool ausbildungsPool=null;
	
	private String LernplanName = null;
	
	/**
	 * @return the lernplanName
	 */
	public String getLernplanName() {
		return LernplanName;
	}


	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Lernfix() {
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
		
		
		// Pool holen, man das ist umst�ndlich....
		ausbildungsPool = super.scriptUnit.getScriptMain().getOverlord().getAusbildungsManager().getAusbildungsPool(super.scriptUnit);
		// reation abschicken... 
		// FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Lernfix");
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		String talentName = OP.getOptionString("Talent");
		if (talentName.length()>1){
			// wir haben Talent=XXX angabe in der scriot anweiseiung
			
			// checken, ob wir auch eine Ziellernstufe dabei haben...
			int talentZiel = OP.getOptionInt("Ziel",0);
			if (talentZiel>0){
				// OK, wir haben ziellevel...
				// levelcheck
				if (!FFToolsGameData.talentUnderLevel(this.getUnit(), this.gd_Script, talentName, talentZiel)){
					// nicht mehr lernfixen...
					// soll user entscheiden..zumindest nicht best�tigen
					// und Kommentar setzen
					this.scriptUnit.doNotConfirmOrders();
					this.addComment("Talentwert nicht unter Ziellevel (" + talentName + ": " + talentZiel + ")");
				}
			}
			
			// vorerst nur das angegebene Talent zum Lehren und Lernen setzen.
			if (this.erzeugeSingleSkillList(talentName).size()>0){
				// nur, wenn SkillIste durch TalentName gef�llt....
				AusbildungsRelation AR = new AusbildungsRelation(this.scriptUnit, this.erzeugeSingleSkillList(talentName), this.erzeugeSingleSkillList(talentName));
				
				// Einschub Gratistalent
				if (OP.getOptionString("Gratistalent").length()>2){
					this.addGratisTalent(AR,OP.getOptionString("Gratistalent"));
				}
				
				AR.informScriptUnit();
				ausbildungsPool.addAusbildungsRelation(AR);
			}
			return;
		} 
		
		this.LernplanName = OP.getOptionString("Lernplan");
		if (this.LernplanName.length()>0){
			AusbildungsRelation AR = super.getOverlord().getLernplanHandler().getAusbildungsrelation(this.scriptUnit, this.LernplanName);
			if (AR!=null){
				AR.informScriptUnit();
				ausbildungsPool.addAusbildungsRelation(AR);
				if (AR.getActLernplanLevel()!=Lernplan.level_unset){
					// this.scriptUnit.ordersHaveChanged();
					// this.scriptUnit.setUnitOrders_adjusted(true);
				}
			} else {
				// keine AR -> Lernplan beendet ?!
				this.addComment("Lernplan liefert keine Aufgabe mehr");
				this.scriptUnit.doNotConfirmOrders();
				// default erg�nzen - keine Ahnung, was, eventuell kan
				// die einheit ja nix..
				this.addOrder("Lernen Ausdauer", true);
			}
			return;
		}
		
		
		// wir haben keine Hauptanwqeisung, also default
		if (this.scriptUnit.getUnit().getSkills()!=null){
			ausbildungsPool.addAusbildungsRelation(new AusbildungsRelation(this.scriptUnit, this.erzeugeSkillList(this.scriptUnit.getUnit().getSkillMap()), this.erzeugeSkillList(this.scriptUnit.getUnit().getSkillMap()) ));
		}
		
		
	
	}

	
	/**
	 * Setzt die SkillMap aus Magellan in eine ScriptSkillListe um, die die Ausbildungsrelation schluckt.
	 * @param _m
	 * @return
	 */
      
	  private HashMap<SkillType,Skill> erzeugeSkillList(Map<StringID,Skill> _m){
		  HashMap<SkillType,Skill> liste = new HashMap<SkillType,Skill>();
			for(Iterator<Skill> iter = _m.values().iterator();iter.hasNext();){
				Skill skill =( Skill) iter.next();
				liste.put(skill.getSkillType(), skill);	
			}
			return liste;
	  }


	  
	  /**
	   * erzeugt eine SkillListe mit nur einem Eintrag, welcher durch einen Talentnamen
	   * angegeben wird, oder eine Leere Liste, falls ein Schreibfehler aufgetreten ist 
	   * bzw die Unit diesen Skill nicht besitzt.
	   * @param talentName
	   * @return
	   */
	  private HashMap<SkillType,Skill> erzeugeSingleSkillList(String talentName){
		  HashMap<SkillType,Skill> liste = new HashMap<SkillType,Skill>();
		  SkillType actSkillType = this.gd_Script.rules.getSkillType(talentName);
		  SkillType magieType = null;
		  Skill magieSkill = null;
		  
		  if (actSkillType==null){
			  // Screibweise falsch?!
			  addComment("!!!Lernfix: Talent nicht erkannt");
			  doNotConfirmOrders();
			  outText.addOutLine("!!!Lernfix Talent nicht erkannt: " + this.unitDesc());
		  } else {
			  Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
			  if (actSkill==null){
				  // neuen Skill erzeugen
				  actSkill = new Skill(actSkillType,0,0,this.scriptUnit.getUnit().getModifiedPersons(),true);
			  }
			  // Wenns ein Magier ist m�ssen wir fiktive Skills unterschieben...
			  if (actSkill.getSkillType().equals(this.scriptUnit.getScriptMain().gd_ScriptMain.rules.getSkillType("Magie"))){
				  // OK ist ein Magier...
				  
				  magieType = this.scriptUnit.getScriptMain().gd_ScriptMain.rules.getSkillType(this.scriptUnit.getUnit().getFaction().getSpellSchool(), true); 
                  magieSkill = new Skill (magieType,0, actSkill.getLevel()  ,this.scriptUnit.getUnit().getModifiedPersons(),true);
                  // magische skills einf�gen 		  
                  liste.put(magieType, magieSkill);  
			  }
			  
			  else {
				  // nicht magische skills einf�gen
				 liste.put(actSkillType, actSkill);  
			  }
			  
			  
		  }
		  return liste;
	  }


	  private void addGratisTalent(AusbildungsRelation AR,String Name){
		  SkillType actSkillType = this.gd_Script.rules.getSkillType(Name);
		  if (actSkillType==null){
			  // Screibweise falsch?!
			  addComment("!!!Lernfix: GratisTalent nicht erkannt");
			  outText.addOutLine("!!!Lernfix GratisTalent nicht erkannt: " + this.unitDesc());
		  } else {
			  Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
			  if (actSkill==null){
				  // neuen Skill erzeugen
				  actSkill = new Skill(actSkillType,0,0,this.scriptUnit.getUnit().getModifiedPersons(),true);
			  }
			  AR.setDefaultGratisTalent(actSkill);
		  }
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
