package com.fftools.scripts;

import java.util.ArrayList;

import magellan.library.Skill;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.pools.treiber.TreiberPool;
import com.fftools.pools.treiber.TreiberPoolRelation;
import com.fftools.utils.FFToolsOptionParser;

/**
 * 
 * Treiber
 * die jetzt auch die anderen Treiber 
 * der Region im Auge hat.
 * @author Fiete
 *
 */

public class Treiben extends TransportScript{
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf_VorMatPool = 15;
	private int Durchlauf_vorTreiberPool = 25;
	private int Durchlauf_nachTreiberPool = 35;
	
	private int[] runners = {Durchlauf_VorMatPool,Durchlauf_vorTreiberPool, Durchlauf_nachTreiberPool};
	
	
	private final String[] talentNamen = {"Hiebwaffen","Stangenwaffen","Bogenschie�en","Armbrustschie�en","Katapultbedienung"};
	
	private int WaffenPrio = 400;
	
	// Ab welchem Talenwert f�r Steuereinrteiben soll Silber gemacht werden?
	// Default Runde 502 auf 2
	// mit setScripterOption minTreiberTalent=X gesetzt werden
		
	private int mindestTalent=2;
	private TreiberPool treiberPool = null;
	private int limit;
	// private static final OutTextClass outText = OutTextClass.getInstance();
	// Wird historisch von TreiberPoolManager angelegt und mu� dann m�hsam �ber CP.getcpr geholt werden
	private TreiberPoolRelation treiberPoolRelation = null;
	
	private final int defaultMindestAuslastung = 90;
	
	private int mindestAuslastung = defaultMindestAuslastung; 
	private Skill skill=null;
	
	
	private boolean noWeapons = false;
	
	private boolean confirmIfunemployed = false;
	
	private ArrayList<MatPoolRequest> requests = new ArrayList<MatPoolRequest>();
	
	// Konstruktor
	public Treiben() {
		super.setRunAt(this.runners);
	}
	
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf_vorTreiberPool){
			this.vorTreiberPool();
		}
		
		if (scriptDurchlauf==Durchlauf_VorMatPool){
			this.vorMatPool();
		}

		if (scriptDurchlauf==Durchlauf_nachTreiberPool){
			this.nachTreiberPool();
		}
		
	}
	
	
	private void vorTreiberPool(){
	
	// Erster Lauf mit direktem Lernen ODER Pool-Anmeldung
		
	
		int reportMinLevel = reportSettings.getOptionInt("minTreiberTalent",region());
		if (reportMinLevel>0){
			this.mindestTalent = reportMinLevel;
		}
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Treiben");
		int unitMinLevel = OP.getOptionInt("minTalent", -1);
		if (unitMinLevel>this.mindestTalent){
			this.mindestTalent=unitMinLevel;
		}
		unitMinLevel = OP.getOptionInt("mindestTalent", -1);
		if (unitMinLevel>this.mindestTalent){
			this.mindestTalent=unitMinLevel;
		}
		
		
		// FF: eventuell hier setting f�r die Region ansetzen....falls n�tig
		
		// skillType holen!
		SkillType skillType = super.gd_Script.rules.getSkillType("Steuereintreiben");
		
		// Kann die Einheit das Talent Treiben?
		skill = super.scriptUnit.getUnit().getSkill(skillType);
		if (skill!= null) {
			
			// Einheit kann Treiben ABER lohnt es sich? MindestTalent pr�fen!
			if (skill.getLevel() >= mindestTalent){
				
				// Waffentest
				int waffenanzahl = 0;
				if (this.requests.size()>0){
					for (MatPoolRequest MPR : this.requests){
						waffenanzahl+=MPR.getBearbeitet();
					}
				}
				
				if (waffenanzahl>this.scriptUnit.getUnit().getModifiedPersons()){
					waffenanzahl = this.scriptUnit.getUnit().getModifiedPersons();
					this.addComment("!!! Zu viele Waffen beim Treiber?!");
					this.doNotConfirmOrders();
				}
				
				treiberPoolRelation.setPersonenZahl(waffenanzahl);
				if (waffenanzahl<=0){
					this.addComment("Keine Waffen f�r Treiber gefunden.");
					this.addOrder("LERNEN Steuereintreiben", true);
					this.doNotConfirmOrders();
					this.noWeapons=true;
				}
				
				
				// M�chte die Einheit die Gesamt-Treibmenge in der Region beschr�nken?
				// Dann sollten wir ein Argument finden!
				// FF Neu..per Optionen...
				
				
				// entweder wird der geparste wert genommen, oder regionsmaximum
				limit = OP.getOptionInt("limit",1000000);
				
				// wenn wir schon den OP haben..gleich alles
				// gibts den nen reportSetting zur Mindestauslastung?
				int reportMindestAuslastung = reportSettings.getOptionInt("TreiberMindestAuslastung", this.region());
				if (reportMindestAuslastung!=ReportSettings.KEY_NOT_FOUND){
					this.mindestAuslastung = reportMindestAuslastung;
				}
				
				// haben wir vielleicht noch einen direkten Parameter in den Optionen?
				this.mindestAuslastung = OP.getOptionInt("mindestAuslastung", this.mindestAuslastung);
				
				// Wurde das Regionslimit bereits durch eine andere Einheit ver�ndert?
										
				if (treiberPool.getRegionMaxTreiben()!= treiberPool.getUnterhaltungslimit()){
				     super.addComment("Warnung: Mehrere Einheiten setzen Unterhalungslimits f�r " + super.scriptUnit.getUnit().getRegion().getName());
				}     
				// ist das neue Limit strenger und positv?
				if ((limit < treiberPool.getUnterhaltungslimit())&&(limit > 0 )){
				      // neues Limit gilt!
				      treiberPool.setTreibenLimit(limit);
				}
				// Best�tigen, wenn wegen �berz�hliger Treibereinheit eigentlich arbeitslos?
				this.confirmIfunemployed = OP.getOptionBoolean("confirmUnemployed",false);
				if (this.confirmIfunemployed){
					this.addComment("Treiben: Einheit wird auf Benutzerwunsch nicht unbest�tigt bleiben.");
				}
				
				// mode
				if (OP.getOptionString("mode").equalsIgnoreCase("kepler") || 
						(reportSettings.getOptionString("TreiberMode",this.region())!=null && 
								reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("Kepler"))){
					if (treiberPool!=null){
						treiberPool.setKeplerMode(true);
						treiberPool.keplerRegionMaxTreiben();
					}
				}
				if (OP.getOptionString("mode").equalsIgnoreCase("kepler2") || 
						(reportSettings.getOptionString("TreiberMode",this.region())!=null && 
								reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("Kepler2"))){
					if (treiberPool!=null){
						treiberPool.setKeplerMode(true);
						treiberPool.maxTreibsilberFreigabe();
					}
				}
				
			} else {
				// zu schlecht => lernen
				this.lerneTalent("Mindesttalentwert " + mindestTalent + " unterschritten");
			}
			
		} else {
			// Einheit kann garnicht Unterhalten!
			this.lerneTalent("Mindesttalentwert " + mindestTalent + " unterschritten (Keine F�higkeit gefunden)");
		}	
	}	
	
	/**
	 * Nur check Anzahl Personen.....
	 * nach Waffen
	 */
	private void vorMatPool(){
		
        //		 Hurra! Ein Kandidat f�r den CircusPool! Aber welcher ist zust�ndig?
		// Registrieren l�uft gleich mit durch Manager
		treiberPool = super.scriptUnit.getScriptMain().getOverlord().getTreiberPoolManager().getTreiberPool(super.scriptUnit);
					
		// Relation gleich mal referenzieren f�r sp�ter.
		treiberPoolRelation = treiberPool.getTreiberPoolRelation(super.scriptUnit);
		
		// Entweder f�r alle Waffentalente waffen anfordern oder nur f�r das Argument(Itemgroup oder Item)
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Treiben");
		
		if (OP.getOptionInt("WaffenPrio", -1)>0){
			this.WaffenPrio = OP.getOptionInt("WaffenPrio", this.WaffenPrio);
			this.addComment("Waffenprio gesetzt auf: " + this.WaffenPrio);
		}
		int newFaktor = OP.getOptionInt("Faktor_Silberbestand_Region", -1);
		if (newFaktor>0){
			this.treiberPool.setFaktor_silberbestand_region(newFaktor);
			this.addComment("Silberstandsfaktor gesetzt auf: " + newFaktor);
		}
		
		String comment = "Treiberbewaffnung";
		if (OP.getOptionString("Waffe").length()>0){
			// Mit Waffen-Argument
			// einfach �bergeben
			MatPoolRequest MPR = new MatPoolRequest(this,this.scriptUnit.getUnit().getModifiedPersons(),OP.getOptionString("Waffe"),this.WaffenPrio,comment);
			this.addMatPoolRequest(MPR);
			this.requests.add(MPR);
		} else {
			// Kein Waffen-Argument
			// Liste bauen
			boolean didSomething = false;
			for (int i = 0;i<this.talentNamen.length;i++){
				String actName = this.talentNamen[i];
				SkillType actSkillType = this.gd_Script.rules.getSkillType(actName);
				if (actSkillType!=null){
					Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
					if (actSkill!=null && actSkill.getLevel()>0){
						// Was gefunden
						// Zum Talent das passendste Ger�t definieren
						String materialName = actSkillType.getName();
						String matNameNeu="nix";
						if (materialName.equalsIgnoreCase("Hiebwaffen")) {
							matNameNeu = "Schwert";
						} else if(materialName.equalsIgnoreCase("Stangenwaffen")){
							matNameNeu = "Speer";
						} else if(materialName.equalsIgnoreCase("Bogenschie�en")){
							matNameNeu = "Bogen";
						} else if(materialName.equalsIgnoreCase("Armbrustschie�en")){
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
					}
				}
			}
			if (!didSomething){
				this.addComment("Treiben: keine Waffenanforderung! - kein Waffentalent?!");
				this.doNotConfirmOrders();
			}
		}
	}
	
	
	/**
	 * Zweiter Lauf nach dem runCircusPool
	 */
	
	private void nachTreiberPool(){
	
		
		// Nimmt diese Einheit an einem Pool teil?
		if (treiberPool != null){
		
			// Diverse Ausgaben 
		    
			super.addComment("Erwartetes Gesamteinkommen(Treiben) in " + super.scriptUnit.getUnit().getRegion().getName() +": " + treiberPool.getRegionsVerdienst() + " Silber");
			if (treiberPool.getRemainingTreiben() > 0){
				super.addComment("M�glicher Mehrverdienst: " +treiberPool.getRemainingTreiben() + " Silber" );
			
			}
			if (treiberPool.getUnterhaltungslimit() != treiberPool.getRegionMaxTreiben()){
				super.addComment("Treiben-limit von " + treiberPool.getUnterhaltungslimit() +" Silber wirksam!");
				
			}
		    
			
			if (treiberPool.isSilberknapp()){
				super.addComment("Treiben: in der Region wurde die MindestSilbermenge unterschritten");
			}
			
			if (treiberPool.isKeplerMode()){
				super.addComment("Kepler-Modus erkannt:");
				treiberPool.addKeplerInfo(this.scriptUnit);
			}
			
			
			if (!this.noWeapons){
			
				// Nun Befehle setzen!
				
				// M�glicher Verdienst gr��er als das was Pool erlaubt?
				
				// FF: ?!? m�gliches Risiko: ist sicher, das Relation != null ?
				
				if (treiberPoolRelation.getVerdienst() > treiberPoolRelation.getDoTreiben()){
					
					// Negativ w�re ein �berz�hliger Unterhalter!
					if (treiberPoolRelation.getDoTreiben() < 0 ){
						this.lerneTalent("Warnung: �berz�hlige Treiber Einheit!");
						if (!this.confirmIfunemployed){
							super.scriptUnit.doNotConfirmOrders();
						}
					} else{
						
						// postiv aber nicht ausgelastet!
						super.addComment("Warnung: Einheit ist NICHT ausgelastet!");
						super.addComment("" + Math.round((treiberPoolRelation.getVerdienst()-treiberPoolRelation.getDoTreiben())/treiberPoolRelation.getProKopfVerdienst()) + " Treiber �berfl�ssig");		
						super.addOrder("TREIBEN " + treiberPoolRelation.getDoTreiben(), true);			
						
						double auslastung = ((double)treiberPoolRelation.getDoTreiben()/(double)treiberPoolRelation.getVerdienst());
						
						// unter 90% auslastung unbest�tigt. 
						if ( auslastung < ((double)this.mindestAuslastung/100)){
							if (!this.confirmIfunemployed){
								super.scriptUnit.doNotConfirmOrders();
							}
						}
						
						// FF: unter 100% angabe
						if ( auslastung < 1){
							this.addComment("Auslastung: " + (int)Math.floor(auslastung*100) + "%, unbest�tigt unter " + this.mindestAuslastung + "%");	
						}				
						
					}
		
				} else {
				 super.addOrder("TREIBEN " + treiberPoolRelation.getDoTreiben(), true);
				}
			}
		}	
	}
	  
		
   	/**
	 * Wenn die Einheit zu schlecht ist oder Steuereintreiben nicht 
	 * kennt.
	 * 20070303 Lernpool ist angebunden
	 * ToDO umstellen auf lernPoolScript
	 */
	
	
	private void lerneTalent(String Meldung){
		this.addComment(Meldung);
		this.lerneTalent("Steuereintreiben", true);
	}
			
		
}
