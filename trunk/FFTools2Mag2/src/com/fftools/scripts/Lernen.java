package com.fftools.scripts;

import magellan.library.Building;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.io.cr.CRParser;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsUnits;

public class Lernen extends MatPoolScript{
	public static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private static final int Vorlauf = 6;
	private static final int Nachlauf =155; //Nach letzten Matpool?
	
	private int[] runners = {Vorlauf,Nachlauf};
	
	
	private Building akademie= null;
	private MatPoolRequest request = null;
	private int SilberPrio=550;
	private int anforderungsRunden=4;
    private double prioParameterB=-0.5;
	private double prioParameterC=0.0;	
	private double prioParameterD=100.0;	
	private double prioParameterA=SilberPrio-prioParameterD;
	
	Skill actSkill = null;
	int actLernTalent = 0;
	
	// Parameterloser constructor
	public Lernen() {
		super.setRunAt(this.runners);
     
	}
	
	public void runScript(int scriptDurchlauf){
		boolean mayConfirm = true;
		
		if (scriptDurchlauf==Vorlauf){
			
//			 DEbug
			if (this.scriptUnit.getUnitNumber().equalsIgnoreCase("8ejl")){
				int ii22 = 0;
				ii22++;
			}
			
//			akademie feststellen falls einheit darin ist oder betritt.
	    	Building gebaeude = this.scriptUnit.getUnit().getModifiedBuilding();
	    	if (gebaeude != null){
	    		if ((this.scriptUnit.getScriptMain().gd_ScriptMain.rules.getBuildingType("Akademie").equals(gebaeude.getBuildingType()))&&(gebaeude.getSize()>=25)){
	    			this.akademie = gebaeude;
	    		}
	      	}
		// hier code fuer Lernen
		// addOutLine("....start Lernen mit " + super.getArgCount() + " Argumenten");
		
		// erstmal ganz Einfach: Das erste Argument weiter lernen und bestaetigen
		if (super.getArgCount()<1) {
			super.addComment("Das Talent fehlt beim Aufruf von Lernen!",true);
			// nicht bestaetigen!
			// wo speichern wir das?
			// in der scriptunit
			super.addComment("Unit wurde durch Lernen NICHT bestaetigt", true);
			super.scriptUnit.doNotConfirmOrders();
		} else {
			String talent = getArgAt(0);
			talent = talent.substring(0, 1).toUpperCase() + talent.substring(1).toLowerCase();

			SkillType skillType = super.gd_Script.rules.getSkillType(talent);
			
			if (skillType==null){
				// wow, kein Lerntalent
				super.addComment("Kein Lerntalent! -> NICHT bestaetigt!", true);
				super.scriptUnit.doNotConfirmOrders();
				addOutLine("!!! ungueltiges Lerntalent bei " + this.unitDesc());
			} else {
				// Alles OK...Lerntalent erkannt
				// checken..haben wir nen max Talentstufe ?
				int maxTalent = 100;
				if (super.getArgCount()> 1) {
					String maxTalentS = getArgAt(1);
					int newMaxTalent = -1;
					try {
						newMaxTalent = Integer.parseInt(maxTalentS);
					} catch (NumberFormatException e){
						// wird unten behandelt..
					}
					if (newMaxTalent>0 && newMaxTalent<100) {
						maxTalent = newMaxTalent;
					} else {
						addOutLine("!!! ungueltiges LernMaxtalent bei " + this.unitDesc());
						mayConfirm = false;
						super.addComment("Fehler bei maxTalentStufe", true);
						super.scriptUnit.doNotConfirmOrders();
					}
				}
				// int actLernTalent = super.scriptUnit.getUnit().getSkill(skillType).getLevel();
				Unit actUnit = super.scriptUnit.getUnit();
				actSkill = actUnit.getModifiedSkill(skillType);
				// kein skill da, dann legen wir einen an..
				if (actSkill ==null){
					actSkill = new Skill(skillType,0,0,super.getUnit().getModifiedPersons(),true);
				    // Kannmann ja mal eintragen in unit
					super.getUnit().addSkill(actSkill);
				}
				if (actSkill!=null){
					// actLernTalent = actSkill.getModifiedLevel(actUnit,true);
					actLernTalent = FFToolsUnits.getModifiedSkillLevel(actSkill, actUnit, false);
				}
				this.scriptUnit.putTag(CRParser.TAGGABLE_STRING3, "Schüler - Skript");
				this.scriptUnit.putTag(CRParser.TAGGABLE_STRING4, talent);
				super.addOrder("LERNEN " + talent, true);
				
				// Koste das Lernen was?
				if(this.getLernKosten(actSkill)>0){
				    // Silber anfordern!
					this.workLernkosten();
				}
				if (actLernTalent >= maxTalent) { 
					super.addComment("Unit hat maxTalentStufe erreicht.", true);
					super.scriptUnit.doNotConfirmOrders();
					mayConfirm = false;
				}
				if (mayConfirm){
					super.addComment("Lernen ok", true);
				}
			}
		}
		}
		
		// Nachlaufprüft ob Silber erhalten wurde
		if (scriptDurchlauf==Nachlauf){
			
			if (request!=null){
				if (request.getBearbeitet()<this.getLernKosten(actSkill)){
				// int a =  this.getLernKosten(actSkill);
				// int b = request.getBearbeitet();
				super.scriptUnit.doNotConfirmOrders();
				super.addComment("Lernkosten ungedeckt: " +request.getBearbeitet()+" von " +this.getLernKosten(actSkill)+ " Silber erhalten" , true);
				}
			}
			
		}
		
		
		
		
	}

	
	
	private void workLernkosten(){
		
		//	eventuell veränderte VorratsRundenanzahl
		int newVorratsRunden=reportSettings.getOptionInt("Ausbildung_LernSilberVorratsRunden", this.region());
    	if (newVorratsRunden>-1 && newVorratsRunden<=10){
    		this.anforderungsRunden = newVorratsRunden;
    	}
		
		
		int userSilberPrio = reportSettings.getOptionInt("Ausbildung_SilberPrio", this.region());
		if (userSilberPrio>0){
			this.SilberPrio = userSilberPrio;
		}
		this.setPrioParameter(this.prioParameterA  ,this.prioParameterB,this.prioParameterC, this.prioParameterD);
		// runtergezählt damit der letzte request der aktuelle ist, dessen auszahlung im nachlauf geprüft werden kann.
		// FF: cleverly!
		if (this.anforderungsRunden>0){
			for (int n=this.anforderungsRunden;n>0;n--){
				request = new MatPoolRequest(this,this.getLernKosten(actSkill), "Silber",this.getPrio(n) ,"Lernen (in " + n + ")");	
				this.addMatPoolRequest(request);
			}
		}
		super.addComment("Lernkosten: " + this.getLernKosten(actSkill), true);
	}
	

		/**
		 * Gibt Lerkosten für teures Talent zurück.. eressea.rules gibt da nix her...
		*/
	 
	public int getLernKosten(Skill _skill){
		
		if (_skill!=null){
			if (_skill.getName().equals("Alchemie")){
			 
				if (this.akademie==null){
					return super.getUnit().getModifiedPersons()*200;
				}else{
					return super.getUnit().getModifiedPersons()*400;	
				}
			}
			
	        if (_skill.getName().equals("Taktik")){
	        	if (this.akademie==null){
					return super.getUnit().getModifiedPersons()*200;
				}else{
					return super.getUnit().getModifiedPersons()*400;	
				}
			}
			
	        
	        if (_skill.getName().equals("Kräuterkunde")){
	        	if (this.akademie==null){
					return super.getUnit().getModifiedPersons()*200;
				}else{
					return super.getUnit().getModifiedPersons()*400;	
				}
			}
			
	        if (_skill.getName().equals("Magie")){
	        	return FFToolsUnits.calcMagieLernKosten(this.getUnit(), this.gd_Script);
			}
	        
	       
		  if (akademie!=null){
			  return super.getUnit().getModifiedPersons() *50;
		  }
	      
		  return 0;
		}
	return 0;	
		
	}
	 
	
	
	
}
