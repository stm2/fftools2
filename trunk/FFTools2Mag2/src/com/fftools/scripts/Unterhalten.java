package com.fftools.scripts;

import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.pools.circus.CircusPool;
import com.fftools.pools.circus.CircusPoolRelation;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

/**
 * 
 * Erweiterte Version 2 des Unterhalters
 * die jetzt auch die anderen Unterhalter 
 * der Region im Auge hat.
 * @author Marc
 *
 */

public class Unterhalten extends TransportScript{
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf_vorCircusPool = 5;
	private int Durchlauf_nachCircusPool = 7;
	
	private int[] runners = {Durchlauf_vorCircusPool,Durchlauf_nachCircusPool};
	
	// Ab welchem Talenwert f�r Unterhaltung soll Silber gemacht werden?
	// Default Runde 502 auf 2
	// mit setScripterOption minUnterhalterTalent=X gesetzt werden
		
	private int mindestTalent=2;
	private CircusPool circusPool = null;
	private int limit;
	// private static final OutTextClass outText = OutTextClass.getInstance();
	// Wird historisch von CircusPoolManager angelegt und mu� dann m�hsam �ber CP.getcpr geholt werden
	private CircusPoolRelation circusPoolRelation = null;
	
	private final int defaultMindestAuslastung = 90;
	
	private int mindestAuslastung = defaultMindestAuslastung; 
	private Skill skill=null;
	
	private boolean confirmIfunemployed = false;
	
	// wird per parameter gesetzt
	private boolean automode = false;
	
	// WENN in AutoMode UND soll in andere Region, dann hier das Ziel
	private Region targetRegion = null;
	private GotoInfo gotoInfo = null;
	
	private int finalOrderedUnterhaltung = 0;
	
	// Konstruktor
	public Unterhalten() {
		super.setRunAt(this.runners);
	}
	
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf_vorCircusPool){
			this.vorCircusPool();
		}
        

		if (scriptDurchlauf==Durchlauf_nachCircusPool){
			this.nachCircusPool();
		}
		
	}
	
	
	private void vorCircusPool(){
	
	// Erster Lauf mit direktem Lernen ODER Pool-Anmeldung
		
	
		int reportMinLevel = reportSettings.getOptionInt("minUnterhalterTalent",region());
		if (reportMinLevel>0){
			this.mindestTalent = reportMinLevel;
		}
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Unterhalten");
		int unitMinLevel = OP.getOptionInt("minTalent", -1);
		if (unitMinLevel>this.mindestTalent){
			this.mindestTalent = unitMinLevel;
		}
		unitMinLevel = OP.getOptionInt("mindestTalent", -1);
		if (unitMinLevel>this.mindestTalent){
			this.mindestTalent = unitMinLevel;
		}
		
		if (OP.getOptionString("mode").equalsIgnoreCase("auto")){
			this.automode = true;
		}
		
		
		// FF: eventuell hier setting f�r die Region ansetzen....falls n�tig
		
		// skillType holen!
		SkillType skillType = super.gd_Script.rules.getSkillType("Unterhaltung");
		
		// Kann die Einheit das Talent Unterhalten?
		// skill = super.scriptUnit.getUnit().getSkill(skillType);
		skill = super.scriptUnit.getUnit().getModifiedSkill(skillType);
		if (skill!= null) {
			
			// Einheit kann unterhalten ABER lohnt es sich? MindestTalent pr�fen!
			if (skill.getLevel() >= this.mindestTalent){
				
				// Hurra! Ein Kandidat f�r den CircusPool! Aber welcher ist zust�ndig?
				// Registrieren l�uft gleich mit durch Manager
				circusPool = super.scriptUnit.getScriptMain().getOverlord().getCircusPoolManager().getCircusPool(this);
							
				// Relation gleich mal referenzieren f�r sp�ter.
				circusPoolRelation = circusPool.getCircusPoolRelation(super.scriptUnit);
				// M�chte die Einheit die Gesamt-Unterhaltung in der Region beschr�nken?
				// Dann sollten wir ein Argument finden!
				// FF Neu..per Optionen...
				
				
				// entweder wird der geparste wert genommen, oder regionsmaximum
				limit = OP.getOptionInt("limit",super.scriptUnit.getUnit().getRegion().maxEntertain());
				
				// wenn wir schon den OP haben..gleich alles
				// gibts den nen reportSetting zur Mindestauslastung?
				int reportMindestAuslastung = reportSettings.getOptionInt("UnterhaltenMindestAuslastung", this.region());
				if (reportMindestAuslastung!=ReportSettings.KEY_NOT_FOUND){
					this.mindestAuslastung = reportMindestAuslastung;
				}
				
				// haben wir vielleicht noch einen direkten Parameter in den Optionen?
				this.mindestAuslastung = OP.getOptionInt("mindestAuslastung", this.mindestAuslastung);
				
				// Gibt es eine scripterOption f�r diese Region zum Limit ?
				int settingsLimit = reportSettings.getOptionInt("UnterhaltenLimit", this.region()); 
				if (settingsLimit>=0){
					circusPool.setUnterhaltungslimit(settingsLimit);
				}
				
				
				// Wurde das Regionslimit bereits durch eine andere Einheit ver�ndert?
										
				if (circusPool.getRegionMaxUnterhaltung()!= circusPool.getUnterhaltungslimit()){
				     super.addComment("Warnung: Mehrere Einheiten setzen Unterhalungslimits f�r " + super.scriptUnit.getUnit().getRegion().getName());
				}     
				// ist das neue Limit strenger und positv?
				if ((limit < circusPool.getUnterhaltungslimit())&&(limit > 0 )){
				      // neues Limit gilt!
				      circusPool.setUnterhaltungslimit(limit);
				}
				// Best�tigen, wenn wegen �berz�hliger Unterhaltereinheit eigentlich arbeitslos?
				this.confirmIfunemployed = OP.getOptionBoolean("confirmUnemployed",false);
				if (this.confirmIfunemployed){
					this.addComment("Unterhalten: Einheit wird auf Benutzerwunsch nicht unbest�tigt bleiben.");
				}
			} else {
				// zu schlecht => lernen
				this.lerneUnterhaltung("Mindesttalentwert " + mindestTalent + " unterschritten");
			}
			
		} else {
			// Einheit kann garnicht Unterhalten!
			this.lerneUnterhaltung("Mindesttalentwert " + mindestTalent + " unterschritten");
		}	
	}	
	
	/**
	 * Zweiter Lauf nach dem runCircusPool
	 */
	
	private void nachCircusPool(){
	
		
		// Nimmt diese Einheit an einem Pool teil?
		if (circusPool != null){
		
			// Diverse Ausgaben 
		    
			super.addComment("Erwartetes Gesamteinkommen in " + super.scriptUnit.getUnit().getRegion().getName() +": " + circusPool.getRegionsVerdienst() + " Silber");
			if (circusPool.getRemainingUnterhalt() > 0){
				super.addComment("M�glicher Mehrverdienst: " +circusPool.getRemainingUnterhalt() + " Silber" );
			
			}
			if (circusPool.getUnterhaltungslimit() != circusPool.getRegionMaxUnterhaltung()){
				super.addComment("Unterhaltungslimit von " + circusPool.getUnterhaltungslimit() +" Silber wirksam!");
				
			}
		
			// Nun Befehle setzen!
			
			// M�glicher Verdienst gr��er als das was Pool erlaubt?
			
			// FF: ?!? m�gliches Risiko: ist sicher, das Relation != null ?
			
			if (circusPoolRelation.getVerdienst() > circusPoolRelation.getDoUnterhaltung()){
				if (!automode){
					// Negativ w�re ein �berz�hliger Unterhalter!
					if (circusPoolRelation.getDoUnterhaltung() < 0 ){
						this.lerneUnterhaltung("Warnung: �berz�hlige Unterhalter Einheit!");
						if (!this.confirmIfunemployed){
							super.scriptUnit.doNotConfirmOrders();
						}
					} else{
						
						// postiv aber nicht ausgelastet!
						super.addComment("Warnung: Einheit ist NICHT ausgelastet!");
						super.addComment("" + Math.round((circusPoolRelation.getVerdienst()-circusPoolRelation.getDoUnterhaltung())/circusPoolRelation.getProKopfVerdienst()) + " Unterhalter �berfl�ssig");		
						super.addOrder("UNTERHALTEN " + circusPoolRelation.getDoUnterhaltung(), true);			
						this.setFinalOrderedUnterhaltung(circusPoolRelation.getDoUnterhaltung());
						double auslastung = ((double)circusPoolRelation.getDoUnterhaltung()/(double)circusPoolRelation.getVerdienst());
						
						// unter 90% auslastung unbest�tigt. 
						if ( auslastung < ((double)this.mindestAuslastung/100)){
							if (!this.confirmIfunemployed){
								super.scriptUnit.doNotConfirmOrders();
							}
						}
						
						// FF: unter 100% angabe
						if ( auslastung < 1){
							this.addComment("(geplante Unterhaltung war hier: " + circusPoolRelation.getDoUnterhaltung());
							this.addComment("Auslastung: " + (int)Math.floor(auslastung*100) + "%, unbest�tigt unter " + this.mindestAuslastung + "%");	
						}				
						
					}
				} else {
					// wir haben einen Unterhalter in Automode
					double auslastung = ((double)circusPoolRelation.getDoUnterhaltung()/(double)circusPoolRelation.getVerdienst());
					// FF: unter 100% angabe					
					if ( auslastung < 1){
						this.addComment("(geplante Unterhaltung war hier: " + circusPoolRelation.getDoUnterhaltung() + ")");
						this.addComment("Auslastung: " + (int)Math.floor(auslastung*100) + "%, kein Unterhalten unter unter " + this.mindestAuslastung + "%");	
					}
					if (this.targetRegion==null){
						// keine Zielregion...also lernen...oder trotzdem unterhalten?
						// unter X% auslastung unbest�tigt. 
						if ( auslastung < ((double)this.mindestAuslastung/100)){
							
							addComment("Automode->Einheit lernt");
							int reittalent=this.scriptUnit.getSkillLevel("Reiten");
							if (reittalent>0){
								super.lerneTalent("Unterhaltung", true);
							} else {
								// neu, wir lernen auf T1 Reiten
								this.addComment("-> wir lernen prophylaktisch Reiten T1");
								this.lerneTalent("Reiten",false);
							}
						} else {
							super.addComment("Warnung: Einheit ist NICHT ausgelastet!");
							super.addComment("" + Math.round((circusPoolRelation.getVerdienst()-circusPoolRelation.getDoUnterhaltung())/circusPoolRelation.getProKopfVerdienst()) + " Unterhalter �berfl�ssig");		
							super.addOrder("UNTERHALTEN " + circusPoolRelation.getDoUnterhaltung(), true);
							this.setFinalOrderedUnterhaltung(circusPoolRelation.getDoUnterhaltung());
						}

					} else {
						// wir haben doch tats�chlich ne Zielregion...
						
						int reittalent=this.scriptUnit.getSkillLevel("Reiten");
						if (reittalent>0){
							gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.region().getCoordinate(),targetRegion.getCoordinate(), true,"Unterhalten");
							addComment("dieser Region NEU als Unterhalter zugeordnet: " + targetRegion.toString());
							addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
							// Pferde requesten...
							MatPoolRequest MPR = new MatPoolRequest(this,this.scriptUnit.getUnit().getModifiedPersons(), "Pferd", 20, "Unterhalter unterwegs" );
							this.addMatPoolRequest(MPR);
						} else {
							// neu, wir lernen auf T1 Reiten
							gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.region().getCoordinate(),targetRegion.getCoordinate(), false,"Unterhalten");
							this.addComment("-> wir lernen aber erstmal Reiten T1");
							addComment("dieser Region NEU als Unterhalter zugeordnet: " + targetRegion.toString());
							addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
							this.lerneTalent("Reiten",false);
						}
					}
				}
	
			}
			else {
				super.addOrder("UNTERHALTEN " + circusPoolRelation.getDoUnterhaltung(), true);
				this.setFinalOrderedUnterhaltung(circusPoolRelation.getDoUnterhaltung());
			}
		}
	}
	  
		
   	/**
	 * Wenn die Einheit zu schlecht ist oder Unterhaltung nicht 
	 * kennt.
	 * 20070303 Lernpool ist angebunden
	 * ToDO umstellen auf lernPoolScript
	 */
	
	
	private void lerneUnterhaltung(String Meldung){
		this.addComment(Meldung);
		this.lerneTalent("Unterhaltung", true);
	}


	public boolean isAutomode() {
		return automode;
	}


	public void setAutomode(boolean automode) {
		this.automode = automode;
	}


	public GotoInfo getGotoInfo() {
		return gotoInfo;
	}


	/**
	 * @return the targetRegion
	 */
	public Region getTargetRegion() {
		return targetRegion;
	}


	/**
	 * @param targetRegion the targetRegion to set
	 */
	public void setTargetRegion(Region targetRegion) {
		this.targetRegion = targetRegion;
	}
			
	
	public boolean isUnterMindestAuslastung(){
		boolean erg = false;
		if (circusPoolRelation!=null){
			double auslastung = ((double)circusPoolRelation.getDoUnterhaltung()/(double)circusPoolRelation.getVerdienst());
			if ( auslastung < ((double)this.mindestAuslastung/100)){
				erg = true;
			}
		}
		
		return erg;
	}


	public int getFinalOrderedUnterhaltung() {
		return finalOrderedUnterhaltung;
	}


	public void setFinalOrderedUnterhaltung(int finalOrderedUnterhaltung) {
		this.finalOrderedUnterhaltung = finalOrderedUnterhaltung;
	}
	
}
