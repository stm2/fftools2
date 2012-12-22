package com.fftools.pools.akademie;

import java.util.ArrayList;
import java.util.Collections;

import magellan.library.Building;
import magellan.library.Unit;

import com.fftools.ReportSettings;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.scripts.Akademie;


public class AkademiePool {

	  
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	public static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private Building akademieBuilding;
	private Akademie verwalterScript;
	
	private ArrayList<AusbildungsRelation> relevantRelations;
	private ArrayList<AkademieTalent> akademieTalente;
	
	/**
	 * Konstruktor - setzt das Geb�ude
	 *
	 */
	
	public AkademiePool(Building akademie, Akademie AT){
		this.akademieBuilding = akademie;
		this.verwalterScript = AT;
    }
	
	
	/**
     * Hier rennt der Pool. 
     */
	
	public void runPool(){
		
		this.verwalterScript.addComment("Akademie Pool start f�r: " + this.akademieBuilding.getID().toString() + ")");
		
		// die Liste der relevanten ARs besorgen
		this.relevantRelations = reportSettings.getScriptMain().getOverlord().getAkademieManager().getRelevantAR(this.akademieBuilding.getRegion());
		if (this.relevantRelations==null || this.relevantRelations.isEmpty()){
			this.verwalterScript.addComment("Akademieverwaltung: keine relevanten Ausbildungsteilnehmer mehr in der Region");
			return;
		}
		
		this.verwalterScript.addComment("Debug: Anzahl relavanter Lernfixer (alle Talente):" + this.relevantRelations.size(),false);
		
		if (this.akademieTalente==null || this.akademieTalente.isEmpty()){
			this.akademieTalente = verwalterScript.getOverlord().getAkademieManager().getDefaultTalentList();
		} else {
			// die defaults kurzerhand anf�gen, wenn noch nicht vorhanden
			for (AkademieTalent AT:verwalterScript.getOverlord().getAkademieManager().getDefaultTalentList()){
				if (!this.akademieTalente.contains(AT)){
					this.akademieTalente.add(AT);
				}
			}
				
		}
		
		if (this.akademieTalente==null || this.akademieTalente.isEmpty()){
			this.verwalterScript.addComment("Akademieverwaltung: keine relevanten Talente bekannt - nanu?");
			return;
		}
		
		// OK, wir haben units und wir habe Talente...
		// wieviele Pl�tze haben wir zu vergeben?
		int verfPl�tze = 25;
		for (Unit u:this.akademieBuilding.modifiedUnits()){
			// diese Unit wird nur dann von den verf�gbaren abgezogen
			// wenn sie nicht Teil der relevantenARs ist
			boolean isRelevant = false;
			for (AusbildungsRelation AR:this.relevantRelations){
				if (AR.getScriptUnit().getUnit().equals(u)){
					// bingo
					isRelevant=true;
					break;
				}
			}
			if (!isRelevant){
				verfPl�tze-=u.getModifiedPersons();
				this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + "- ignoriere Einheit " + u.toString() + " (kein Lernfix...)");
			} else {
				// ist relevant
				this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + " - Insasse wird gepoolt " + u.toString());
			}
		}
		this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": verf�gbare Pl�tze:" + verfPl�tze);
		if (verfPl�tze<=0){
			this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": abbruch, nix zu tun und voll...");
			return;
		}
		
		int �bergabePl�tze = 0;
		
		// alle Talente durchgehen
		for (AkademieTalent AT:this.akademieTalente){
			// die relevanten Sortieren, nach dem aktuellen SkillType...
			// Submenge der relevanten Bilden, mit dem richtigen
			// Skilltype und der maximalen Anzahl
			int �bernommene�bergabepl�tze = �bergabePl�tze;
			�bergabePl�tze += AT.getAnzahl();
			this.verwalterScript.addComment("Debug " + this.akademieBuilding.getID().toString() + ": poole Talent:" + AT.getSkillType().toString() + " definiertes Limit: " + �bergabePl�tze + ", davon " + �bernommene�bergabepl�tze + " bereits �bernommen.",false);
			ArrayList<AusbildungsRelation> actRel = new ArrayList<AusbildungsRelation>();
			for (AusbildungsRelation AR:this.relevantRelations){
				if (AR.getAkademieFromAM()==null && AR.getOrderedSkillType().equals(AT.getSkillType())){
					if (!AR.isSchueler()){
						actRel.add(AR);
					}
					
				}
			}
			
			this.verwalterScript.addComment("Debug " + this.akademieBuilding.getID().toString() + ": Anzahl der reduzierten relevanten Lernfixer: " + actRel.size(),false);
			
			// fertige Liste...>0 ?
			if (actRel.isEmpty()){
				// n�chstes Talent
				continue;
			}
			// Liste sortieren
			Collections.sort(actRel, new AusbildungsRelationComparator(this.akademieBuilding));
			// stehen jetzt aber falsch rum
			Collections.reverse(actRel);
			// Liste abarbeiten
			
			
			
			for (AusbildungsRelation AR:actRel){
				boolean passtNoch = false;
				// passen wir hier noch rein?
				if (AR.getScriptUnit().getUnit().getModifiedPersons()<=verfPl�tze && AR.getScriptUnit().getUnit().getModifiedPersons()<=�bergabePl�tze){
					// richtiger Type und passige Einheit
					// kein Lehrer, dessen Sch�ler nicht auch reinpassen...
					passtNoch = true;
					int checkInt=0; 
					if (AR.isTeacher() ){
						checkInt = AR.getOrdererdSch�leranzahl();
						// Einen Sch�ler holen
						AusbildungsRelation einSchueler = AR.getPooledRelation().get(0);
						// Anzahl der Lehrer feststellen
						checkInt += einSchueler.getAnzahlPooledPersons();
						if (checkInt>verfPl�tze || checkInt>�bergabePl�tze){
							passtNoch=false;
						}
					}
				}
				if (passtNoch){
					// Geb�ude setzen
					AR.setAkademieFromAM(this.akademieBuilding);
					// verf reduzieren
					verfPl�tze -= AR.getSchuelerPlaetze();
					�bergabePl�tze -= AR.getSchuelerPlaetze();
					
					// info
					this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": IN f�r " + AT.getSkillType().toString() + ": " + AR.getScriptUnit().getUnit().toString(true) + " (" + verfPl�tze + " verbleibend)" );
					// wenn Lehrer, auch die Sch�ler mit rein
					if (AR.isTeacher()){
						for (AusbildungsRelation schueler : AR.getPooledRelation()){
							schueler.setAkademieFromAM(this.akademieBuilding);
							verfPl�tze -= schueler.getSchuelerPlaetze();
							�bergabePl�tze -= schueler.getSchuelerPlaetze();
							this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": mit Sch�ler f�r " + AT.getSkillType().toString() + ": " + schueler.getScriptUnit().getUnit().toString(true) + " (" + verfPl�tze + " verbleibend)");
						}
						// die weiteren Lehrer auch noch mitnehmen
						// einen Sch�ler holen
						AusbildungsRelation einSchueler = AR.getPooledRelation().get(0);
						if (einSchueler.getPooledRelation().isEmpty()){
							this.verwalterScript.addComment("Debug: sch�ler liefert keine Lehrer-Liste",false);
						}
						// alle Lehrer setzen
						for (AusbildungsRelation einLehrer : einSchueler.getPooledRelation()){
							if (!AR.equals(einLehrer)){
								einLehrer.setAkademieFromAM(this.akademieBuilding);
								verfPl�tze -= einLehrer.getSchuelerPlaetze();
								�bergabePl�tze -= einLehrer.getSchuelerPlaetze();
								this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": weiterer Lehrer f�r " + AT.getSkillType().toString() + ": " + einLehrer.getScriptUnit().getUnit().toString(true) + " (" + verfPl�tze + " verbleibend)");
							} else {
								this.verwalterScript.addComment("Debug: (bereits eingez�hlter Lehrer ignoriert)",false);
							}
						}
						
					}
				}
				// was passiert, wenn verfPl = 0 ?!
				if (�bergabePl�tze<=0){
					this.verwalterScript.addComment("Debug: definiertes Limit erreicht.",false);
					// fertig
					break;
				}
				// was passiert, wenn verfPl = 0 ?!
				if (verfPl�tze<=0){
					// fertig
					break;
				}
			}
			if (verfPl�tze<=0){
				// fertig
				break;
			}
		}
		this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + " final Statement: noch " + verfPl�tze + " Pl�tze frei.");
	}
	
	/**
	 * schmeisst aus dieser Akademie alle raus, die nicht bleiben d�rfen
	 */
	public void leaveAkademie(){
		for (Unit u:this.akademieBuilding.modifiedUnits()){
			// relevant = gepoolt und darf bleiben
			boolean isRelevant = false;
			for (AusbildungsRelation AR:this.relevantRelations){
				if (AR.getScriptUnit().getUnit().equals(u)){
					// bingo
					if (AR.getAkademieFromAM()!=null && AR.getAkademieFromAM().equals(this.akademieBuilding)){
						isRelevant=true;
					}
					if (isRelevant){
						AR.getScriptUnit().addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": Einheit darf in " + this.akademieBuilding.toString() + " bleiben.");
					} else {
						AR.getScriptUnit().addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": Einheit muss " + this.akademieBuilding.toString() + " verlassen.");
						if (AR.getAkademieFromAM()==null){
							AR.getScriptUnit().addOrder("verlassen ; Akapool",true);
						}
					}
					break;
				}
			}
			
		}
		
	}
	

	/**
	 * @return the akademieBuilding
	 */
	public Building getAkademieBuilding() {
		return akademieBuilding;
	}


	/**
	 * @return the verwalterScript
	 */
	public Akademie getVerwalterScript() {
		return verwalterScript;
	}


	/**
	 * @param akademieTalente the akademieTalente to set
	 */
	public void setAkademieTalente(ArrayList<AkademieTalent> akademieTalente) {
		this.akademieTalente = akademieTalente;
	}
	
	
	
}
