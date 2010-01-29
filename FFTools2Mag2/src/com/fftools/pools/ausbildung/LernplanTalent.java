package com.fftools.pools.ausbildung;

import magellan.library.Skill;
import magellan.library.rules.SkillType;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.utils.FFToolsOptionParser;


/**
 * Bestandteil eines Lernplanes
 * enth�lt das Talent mit Zielen und Restriktionen
 * 
 * @author Fiete
 *
 */
public class LernplanTalent {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	/**
	 * Konstanten zur Abbildung der Art des Zieltalents
	 */
	public static int Talent_unset = Integer.MIN_VALUE+1;
	public static int Talent_max = Integer.MIN_VALUE+2;
	
	/**
	 * Der SkillType dieses LernplanTalentes
	 */
	private SkillType skillType = null;
	
	/**
	 * nur merken...vorerst
	 */
	// private String talentZielString = null;
	
	
	/**
	 * Lernverlauf: der SkillType, von diesem skillType abh�ngig ist
	 * (eine Restriktion existiert)
	 */
	private SkillType lernVerlaufSkillType = null;
	

	/**
	 * gibt die Restriktion relativ zu skillType an
	 * 
	 * Talent=X Lernverlauf=Ausdauer%+6  bedeutet
	 * X wird nur dann gelernt, wenn gilt: X <= Ausdauer + 6.
	 * 
	 * Talent=Ausdauer%max Lernverlauf=Hiebwaffen%-2
	 * Ausdauer wird nur dann gelernt, wenn gilt: Ausdauer <= Hiebwaffen -  2 
	 * 
	 */
	private int lernVerlaufRestriktion = Talent_unset;
	
	/**
	 * Gesetztes Ziel diese Talentes.
	 * Talent_unset -> nichts gesetzt
	 * Talent_max -> soll Maximal gelernt werden
	 * sonst: harte Talentgrenze, bis zu der gelernt werden soll
	 */
	private int talentZiel = Talent_unset;
	
	/**
	 * wie der Name schon sagt..
	 */
	private boolean kannLehrerSein = true;

	
	/**
	 * Konstruktor
	 * parst gleichzeitig das Lernziel (Stufe bzw max)
	 * @param skillType
	 */
	public LernplanTalent(ScriptUnit u, SkillType skillType,String _talentZiel){
		this.skillType = skillType;
		// this.talentZielString = _talentZiel;
		// Versuchen wir doch gleich, talentZiel zu bestimmen
		if (_talentZiel==null || _talentZiel.length()==0){
			// keine Angabe
			outText.addOutLine("!!! LernplanTalent:Lernziel nicht angegeben! " + u.unitDesc() , true);
			u.addComment("!!! LernplanTalent:Lernziel nicht angegeben!");
			u.doNotConfirmOrders();
			return;
		}
		
		// Parsen
		boolean talentZielOK=true;
		try {
			Integer i = Integer.valueOf(_talentZiel);
			int newTalentZiel = i.intValue();
			if (newTalentZiel>-50 && newTalentZiel<50){
				this.talentZiel = newTalentZiel;
			} else {
				talentZielOK = false;
			}
		} catch (NumberFormatException e){
			// es k�nnte auch noch Max sein
			if (_talentZiel.equalsIgnoreCase("max")){
				// alles fein
				this.talentZiel = LernplanTalent.Talent_max;
			} else {
				// wirklich ein Problem
				talentZielOK = false;
			}
		}
		if (!talentZielOK){
			// Problem beim erkennen des LernZieles
			outText.addOutLine("!!! LernplanTalent:Lernziel nicht erkannt! " + u.unitDesc() , true);
			u.addComment("!!! LernplanTalent:Lernziel nicht erkannt!");
			u.doNotConfirmOrders();
			return;
		}
	}
	
	
	
	/**
	 * 
	 * setzt parameter und Talente dieses Lernplantalentes nach den 
	 * angaben der Orderzeile (in Optionparser)
	 * 
	 * wir schleppen u mit f�r eventuelle parsefehler ->anzeigen, wo sie
	 * auftreten
	 * 
	 * @param u
	 * @param OP
	 */
	public void parseOptionLine(ScriptUnit u, FFToolsOptionParser OP){
		// ZielTalentStufe herausfinden -> bereits im Konstruktor
		// hier nur noch tats�chlich zus�tzliche Angaben bearbeiten
		
		// Lernverlauf
		// Gibt die Abh�ngigkeit zu einem anderen Talent an
		// nur wenn diese erf�llt ist, kann "unser" Talent gelernt werden
		// optionaler Parameter
		this.parseLernverlauf(u, OP);
		
		// kein Lehrereinsatz
		this.kannLehrerSein = OP.getOptionBoolean("Lehrer", true);
		
	}
	
	private void parseLernverlauf(ScriptUnit u, FFToolsOptionParser OP){
		String lernverlauf = OP.getOptionString("Lernverlauf");
		// check1: �berhaupt eine angabe
		if (lernverlauf.length()==0){
			// vermutlich keine Angabe
			// kein Fehler, da optionaler Parameter
			return;
		}
		
		// check2: Seperator benutzt
		if (!(lernverlauf.indexOf(LernplanHandler.lernplanDetailSeparator)>0)){
			// kein seperator drinne...dat ist ne Fehlermeldung wert
			outText.addOutLine("!!! LernplanTalent:Lernverlauf unkorrekt! " + u.unitDesc() + ":" + lernverlauf , true);
			u.addComment("!!! LernplanTalent:Lernverlauf unkorrekt!!");
			u.doNotConfirmOrders();
			return;
		}
		
		// splitten und Talen checken und Restriktion checken
		// splitten
		String[] paare = lernverlauf.split(LernplanHandler.lernplanDetailSeparator);
		String talentName = paare[0];
		String restriktionString = paare[1];
		// talentName
		SkillType actSkillType = u.getScriptMain().gd_ScriptMain.rules.getSkillType(talentName);
		if (actSkillType==null){
			// Talent nicht erkannt -> Fehler
			outText.addOutLine("!!! LernplanTalent:Lernverlauf Talent nicht erkannt: (" + talentName + ") " + u.unitDesc() , true);
			u.addComment("LernplanTalent:Lernverlauf Talent nicht erkannt: (" + talentName + ") ");
			u.doNotConfirmOrders();
			return;
		}
		
		this.lernVerlaufSkillType = actSkillType;
		
		// Restriktion...muss sinvoller int sein
		boolean isOK = true;
		// aus der Restriktion eventuelles +rausholen
		restriktionString = restriktionString.replace("+", "");
		try {
			Integer i = Integer.parseInt(restriktionString);
			int actI = i.intValue();
			if (actI>-20 && actI<20){
				this.lernVerlaufRestriktion = actI;
			} else {
				// nicht gut, ausserhalb des bereiches
				isOK = false;
			}
		} catch (NumberFormatException e){
			isOK = false;
		}
		if (!isOK){
			// Die Ristriktioin wurde nich erkannt oder ist ausserhalb 
			// des sinnvollen bereiches -> Fehlermeldung
			outText.addOutLine("!!! LernplanTalent:Lernverlauf TalentAbh�ngigkeit nicht erkannt: (" + restriktionString + ") " + u.unitDesc() , true);
			u.addComment("LernplanTalent:Lernverlauf TalentAbh�ngigkeit nicht erkannt: (" + restriktionString + ") ");
			u.doNotConfirmOrders();
			return;
		}
		
	}
	
	/**
	 * liegt die Einheit unter den Restriktionen dieses Talentes?
	 * @param u
	 * @return
	 */
	public boolean istUnterAnforderungen(ScriptUnit u){
		// ZielTalentstufe noch nicht erreicht
		// wenn diese noch nicht gesetzt ist...false rausgeben
		if (this.talentZiel==LernplanTalent.Talent_unset){
			return false;
		}
		// wenn es max gesetzt ist..sind wir immer drunter
		if (this.talentZiel==LernplanTalent.Talent_max){
			return true;
		}
		// jetzt m�ssen wir vergleichen
		Skill actSkill = u.getUnit().getModifiedSkill(this.skillType);
		if (actSkill==null){
			// einheit kann das noch gar nicht....also m�ssen wir definitiv was tun
			return true;
		}
		if (actSkill.getLevel()<this.talentZiel){
			// skill ist kleiner als sollskill
			return true;
		}

		return false;
	}
	
	
	/**
	 * wo m�glich, wird AusBildRel erg�nzt
	 * @param u
	 * @param AB
	 */
	public void appendAusbildungsRelation(ScriptUnit u,AusbildungsRelation AB){
		// Kann man unser Talent noch Lernen
		Skill actLernSkill =u.getUnit().getModifiedSkill(this.skillType);
		
		// Wird geraucht falls actLernSkill = Magie
		SkillType magieType =null;
		Skill magieSkill =null;
		
		if (this.istUnterAnforderungen(u)){
			// gibt es restriktionen ?
			// boolean OK = true;
			boolean OK = this.isInRestriktionen(u);
			if (OK){
				// erg�nzen
				if (actLernSkill==null){
					// neuen skill anlegen, der Ausbildungspool mag keinen skill=null mapeintr�ge
										
					actLernSkill = new Skill(this.skillType,0,0,u.getUnit().getModifiedPersons(),true);
				}
				
				// Wen skilltype magie ist, dann schieben wir der Ausbildungsrelation fiktive Magieskills unter die einen r�ckschluss auf
				// das magiegebiet erlauben. damit kann der ausbildungspool dann magier gleicher Gebiete verketten ohne patzer.
				
				if (this.skillType.equals(u.getScriptMain().gd_ScriptMain.rules.getSkillType("Magie"))){
					// ist magiegebiet bekannt?
					if (u.getUnit().getFaction().getSpellSchool()!=null){
                        magieType =u.getScriptMain().gd_ScriptMain.rules.getSkillType(u.getUnit().getFaction().getSpellSchool(), true); 
                        magieSkill = new Skill (magieType,0, this.getActUnitLevel(u)  ,u.getUnit().getModifiedPersons(),true);
                        AB.addStudyRequest(magieType, magieSkill);
                        
					}else{
						u.doNotConfirmOrders();
						u.addComment("Lernfix: Lernplan fordert Magie, es ist aber kein Magiegebiet gesetzt!");
					}
					
				}else{
				    // ist NICHT magie
					AB.addStudyRequest(this.skillType, actLernSkill);
				}
			}
		}
		
		// darf die Unit noch Lehren
		if (this.kannLehrerSein){
			if (actLernSkill!=null && actLernSkill.getLevel()>1){
				if (!this.skillType.equals(u.getScriptMain().gd_ScriptMain.rules.getSkillType("Magie"))){
				    // KEIN MAGIER
					AB.addTeachOffer(this.skillType, actLernSkill);
				}
				if (magieSkill!=null){
					AB.addTeachOffer(magieType, magieSkill);
				}
			}
		}
	}

	/**
	 * �berpr�ft, ob ein Skill die angegebenen VerlaufsRestriktionen erf�llt
	 * @param u
	 * @param actLernSkill
	 * @return
	 */
	public boolean isInRestriktionen(ScriptUnit u){
		boolean OK = true;
		Skill actLernSkill =u.getUnit().getModifiedSkill(this.skillType);
		if (this.lernVerlaufSkillType!=null && this.lernVerlaufRestriktion!=LernplanTalent.Talent_unset){
			// OK, Restriktion liegt vor
			// Talent besorgen
			Skill actSkill = u.getUnit().getModifiedSkill(this.lernVerlaufSkillType);
			int actUnitVerlaufSkillLevel = 0;
			if (actSkill!=null){
				actUnitVerlaufSkillLevel=actSkill.getLevel();
			}
			// aktuelles Lerntalent
			int actUnitTalentSkillLevel = 0;
			if (actLernSkill!=null){
				actUnitTalentSkillLevel = actLernSkill.getLevel();
			}
			
			// eigentliche restriktion
			if (!(actUnitTalentSkillLevel <= actUnitVerlaufSkillLevel + this.lernVerlaufRestriktion)){
				OK=false;
			}
		}
		return OK;
	}
	

	/**
	 * @return the skillType
	 */
	public SkillType getSkillType() {
		return skillType;
	}
	
	/**
	 * liegfert die Stufe des Talentes, die die unit gerade hat
	 * @return
	 */
	public int getActUnitLevel(ScriptUnit u){
		if (this.skillType==null){
			return 0;
		}
		Skill actUnitSkill = u.getUnit().getModifiedSkill(this.skillType);
		if (actUnitSkill==null){
			return 0;
		}
		
		return actUnitSkill.getLevel();
	}
}
