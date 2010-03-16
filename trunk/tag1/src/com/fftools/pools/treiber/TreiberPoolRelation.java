package com.fftools.pools.treiber;

import magellan.library.rules.SkillType;

import com.fftools.ScriptUnit;

/**
 * Klasse die Auskunft �ber die Treiberqualit�ten einer ScriptUnit gibt
 * Wird ben�tigt falls sp�ter im Pool sortiert werden soll und stellt in diesem Fall 
 * dann Comparable bereit. 
 * 
 * @author Marc
 *
 */

public class TreiberPoolRelation implements Comparable<TreiberPoolRelation> {
    
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	// Welche ScriptUnit bietet sich als Treiber an?
	private ScriptUnit scriptUnit=null;
	
	// Welcher TreiberPool ist f�r die Relation zust�ndig?
	private TreiberPool treiberPool;
	
	// Daten zur Einheit selbst, die f�r das Unterhalten wichtig sind
	
	private int talentStufe =0;
	private int personenZahl =0;
	private int verdienst=0;
	private int proKopfVerdienst=0; 
	private int doTreiben = 250000;
	
	
	public TreiberPoolRelation(ScriptUnit _su, TreiberPool _cp){
		scriptUnit = _su;
		treiberPool = _cp;
		// FF 20070413 ge�ndert auf modified persons
		personenZahl = scriptUnit.getUnit().getModifiedPersons();
		
		// Skilltype zu "Treiben" besorgen
		SkillType treibenSkillType =  treiberPool.treiberPoolManager.scriptMain.gd_ScriptMain.rules.getSkillType("Steuereintreiben");
		
		// kann die Einheit treiben? Falls nicht kommt null zur�ck.
		if (scriptUnit.getUnit().getSkill(treibenSkillType) != null){
            // ja, dann kann man das talent abgreifen. 		
			// sonst bleibt es eben bei Stufe 0.
			talentStufe = scriptUnit.getUnit().getSkill(treibenSkillType).getLevel();
        }	
		
		verdienst=personenZahl*talentStufe*20;
		proKopfVerdienst=talentStufe*20;
			
	};


	// ein paar fixe methoden f�r den TreiberPool
	
	public ScriptUnit getSkriptUnit(){
		return scriptUnit;
	};
	
	public TreiberPool getTreiberPool(){
		return treiberPool;
	};
	
	public int getTalentStufe(){
		return talentStufe;
	};
		
	public int getPersonenZahl(){
		return personenZahl;
	};
	
	public int getVerdienst(){
		return verdienst;
	};
	
	public int getProKopfVerdienst(){
		return proKopfVerdienst;
	};
	
		
	/**
	 * 
	 * Gibt nach dem Poollauf zur�ck welchen Betrag die Einheit treiben soll.
	 * Ist der Betrag negativ soll alternative zu Treiben von Script gew�hlt werden. 
	 */
	
	public int getDoTreiben(){
		return doTreiben;
	}
	
	/**
	 * Pool setzt den Betrag den die Einheit treiben soll
	 *
	 */
    public void setDoTreiben(int _unt){
		doTreiben = _unt;
	}
	
    
   
			
	/**
	 * Sortierbarkeit in Array und ArrayList
	 
	 */
	
	public int compareTo(TreiberPoolRelation cpr){
		// Wei� jetzt nicht ob negativ oder positiv gr��er hei�t...???
		int Differenz = (cpr.talentStufe - this.talentStufe);
		if (Differenz==0){
			Differenz = (cpr.personenZahl - this.personenZahl);
		}
		return Differenz;
	}


	public void setPersonenZahl(int personenZahl) {
		this.personenZahl = personenZahl;
	}
}
