package com.fftools.scripts;

import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Border;
import magellan.library.Building;
import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.rules.BuildingType;
import magellan.library.rules.SkillType;
import magellan.library.utils.Direction;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;

/**
 * 
 * Dat Bauscript - für Bauarbeiter, und solche, die nur matreial für einen Bau anfordern sollen
 * @author Fiete
 *
 */

public class Bauen extends MatPoolScript{
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf_vorMatPool = 10;
	private int Durchlauf_nachMatPool = 100;
	
	private int[] runners = {Durchlauf_vorMatPool,Durchlauf_nachMatPool};
	
	private boolean parseOK = false;
	
	/**
	 * zum besseren Verständnis
	 */
	public static final int STRASSE=1;
	public static final int BURG=2;
	public static final int BUILDING=3;
	
	/**
	 * Typ dieses Scriptes
	 */
	private int actTyp = Bauen.BUILDING;
	
	/**
	 * wenn Buildung, dann hier der Type
	 */
	private BuildingType buildingType = null;
	
	/**
	 * wenn Building oder Burg optionale Zielnummer
	 */
	private String buildungNummer = "";
	
	/**
	 * aktuelle Grösse des (grössten) passenden Objektes in der Region
	 */
	private int actSize = 0;
	
	/**
	 * bei bedarf gwünschte Grösse des Buildings / der Burg
	 */
	private int targetSize = 0;
	
	/**
	 * bei Strasse, Richtung
	 */
	private Direction dir = null;
	
	/**
	 * Ort der MPRs, Items sind die rawMaterials für den Burgentyp
	 */
	private Hashtable<Item,MatPoolRequest> mprTable = null;
	
	/**
	 * wenn dieses Bauscript an der Reihe ist, dann mit diesem Befehl
	 */
	private String bauBefehl = "";
	
	/**
	 * wenn Typ grössenbegrenzt, mehrere Gebäude pro Region möglich, z.B. Akademie
	 * dann hier anzahl der gebäude pro Region
	 * paremeter: anzahl=1
	 */
	private int numberOfBuildings = 1;
	
	private String lernTalent = "Burgenbau";
	
	
	/**
	 * nix mehr zu tun!
	 */
	private boolean fertig=false;
	
	private int prioSilber = 650;
	private int prioSteine = 500;
	private int prioEisen = 650;
	private int prioHolz = 650;
	
	private String steinSpec = "";
	
	private MatPoolRequest steinRequest = null;
	
	private int minAuslastung = 75;
	
	
	// Konstruktor
	public Bauen() {
		super.setRunAt(this.runners);
	}
	
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf_vorMatPool){
			this.vorMatPool();
		}
        

		if (scriptDurchlauf==Durchlauf_nachMatPool){
			this.nachMatPool();
		}
		
	}
	
	
	private void vorMatPool(){
		
		super.addVersionInfo();
		
		// eintragen
		this.getBauManager().addBauScript(this);
		
		this.parseOK = false;
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		
		this.actTyp = Bauen.BUILDING;
		
		// Typbestimmung
		if (OP.getOptionString("Typ").equalsIgnoreCase("Strasse") || OP.getOptionString("Type").equalsIgnoreCase("Strasse")
				|| OP.getOptionString("Typ").equalsIgnoreCase("Road")
				|| OP.getOptionString("Type").equalsIgnoreCase("Road")){
			this.actTyp = Bauen.STRASSE;
		}
		
		if (OP.getOptionString("Typ").equalsIgnoreCase("Burg") || OP.getOptionString("Type").equalsIgnoreCase("Burg")
				|| OP.getOptionString("Typ").equalsIgnoreCase("Castle")
				|| OP.getOptionString("Type").equalsIgnoreCase("Castle")){
			this.actTyp = Bauen.BURG;
		}
		
		if (this.actTyp == Bauen.BUILDING){
			// Typ muss GebäudeType enthalten
			String s = OP.getOptionString("Typ");
			if (s.length()<2){
				s = OP.getOptionString("Type");
			}
			this.buildingType = this.gd_Script.rules.getBuildingType(s,false);
			if (this.buildingType==null){
				// Abbruch
				this.addComment("Bauen: unbekanntes Gebäude: " + s);
				this.doNotConfirmOrders();
				return;
			}
			
			// wenn keine maxGrösse angegeben im Type wird grösse erwartet
			if (this.buildingType.getMaxSize()>0){
				// Grösse ist vorgegeben
				this.targetSize = this.buildingType.getMaxSize();
			} else {
				// Grösse MUSS angegeben werden
				int i = OP.getOptionInt("ziel",0);
				if (i==0){
					i = OP.getOptionInt("size",0);
				}
				if (i>0) {
					// alles OK
					this.targetSize=i;
				} else {
					// nix ist OK
					this.addComment("Bauen: bei " + this.buildingType.getName() + " MUSS eine Zielgrösse angegeben werden! (ziel=X)");
					this.doNotConfirmOrders();
					return;
				}
			}
			// Anzahl vomBuildings
			this.numberOfBuildings=OP.getOptionInt("anzahl", 1);
		}
		if (this.actTyp == Bauen.BURG){
			// Grösse MUSS angegeben sein.
			int i = OP.getOptionInt("ziel",0);
			if (i==0){
				i = OP.getOptionInt("size",0);
			}
			if (i>0) {
				// alles OK
				this.targetSize=i;
			} else {
				// nix ist OK
				this.addComment("Bauen: beim Burgenbau MUSS eine Zielgrösse angegeben werden! (ziel=X)");
				this.doNotConfirmOrders();
				return;
			}
		}
		
		if (this.actTyp== Bauen.STRASSE){
			// Richtung MUSS angegeben sein.
			String s = OP.getOptionString("Richtung");
			if (s.length()<1){
				s = OP.getOptionString("Ziel");
			}
			try {
				this.dir = new Direction(s);
			} catch (IllegalArgumentException e){
				this.dir=null;
				this.addComment("Bauen: Strassenrichtung nicht erkannt: " + s);
				this.doNotConfirmOrders();
				return;
			}
			if (this.dir.getDir()==Direction.DIR_INVALID){
				this.dir=null;
				this.addComment("Bauen: Strassenrichtung nicht erkannt: " + s);
				this.doNotConfirmOrders();
				return;
			}
			
		}
		
		// Prioritäten
		// prioAnpassungen..Defaults setzen
		this.prioAdaption();
		// komplette Prio wird gesetzt
		int i = OP.getOptionInt("prio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioEisen = i;
				this.prioHolz = i;
				this.prioSilber = i;
				this.prioSteine = i;
			} else {
				this.addComment("Bauen: Prio nicht erkannt: " + i);
				this.doNotConfirmOrders();
			}
		}
		// Silberprio
		i = OP.getOptionInt("silberprio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioSilber = i;
			} else {
				this.addComment("Bauen: SilberPrio nicht erkannt: " + i);
				this.doNotConfirmOrders();
			}
		}
		// Eisenprio
		i = OP.getOptionInt("eisenprio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioEisen = i;
			} else {
				this.addComment("Bauen: EisenPrio nicht erkannt: " + i);
				this.doNotConfirmOrders();
			}
		}
		// Holzprio
		i = OP.getOptionInt("holzprio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioHolz = i;
			} else {
				this.addComment("Bauen: HolzPrio nicht erkannt: " + i);
				this.doNotConfirmOrders();
			}
		}
		// Steinprio
		i = OP.getOptionInt("steinprio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioSteine = i;
			} else {
				this.addComment("Bauen: SteinPrio nicht erkannt: " + i);
				this.doNotConfirmOrders();
			}
		}
		
		// Transporterspecs für Steine
		this.steinSpec = OP.getOptionString("steinspec");
		
		// min Auslastung
		this.minAuslastung = OP.getOptionInt("minAuslastung",this.minAuslastung);
		
		// lernTalent
		this.lernTalent = OP.getOptionString("Talent");
		
		// Burg + Building können eine Nummer mitbekommen haben....checken
		String s = OP.getOptionString("nummer");
		if (s.length()>0 && (this.actTyp==Bauen.BUILDING || this.actTyp==Bauen.BURG)){
			// OK, wir haben eine Nummer
			// schauen, ob wir da was finden
			boolean foundNummber = false;
			if (this.scriptUnit.getUnit().getRegion().buildings() != null){
				for (Iterator<Building> iter = this.scriptUnit.getUnit().getRegion().buildings().iterator();iter.hasNext();){
					Building actBuilding = (Building)iter.next();
					if (actBuilding.getID().toString().equalsIgnoreCase(s)){
						// Treffer
						this.buildungNummer = s;
						foundNummber = true;
					}
				}
			}
			if (!foundNummber){
				// problem
				this.addComment("Bauen: " + s + " kann nicht gefunden werden.");
				this.doNotConfirmOrders();
				return;
			}
		}
		
		this.scriptUnit.findScriptClass("RequestInfo");
		
		this.parseOK = true;
		
		// eigentlicher Ablauf nu getrennt
		switch (this.actTyp){
			case Bauen.BUILDING: this.vorMP_Building();break;
			case Bauen.BURG: this.vorMP_Burg();break;
			case Bauen.STRASSE: this.vorMP_Strasse();break;
		}
	}	
	
	/**
	 * Building suchen
	 */
	private void vorMP_Building(){
		int numberOfFinishedBuildings = 0;
		if (this.buildungNummer.length()<1){
			// noch keine Nummer bekannt
			int maxFoundSize=0;
			for (Iterator<Building> iter = this.scriptUnit.getUnit().getRegion().buildings().iterator();iter.hasNext();){
				Building actB = (Building)iter.next();
				if (actB.getBuildingType().equals(this.buildingType)){
					// Das oder ein zielobject
					if (actB.getSize()>maxFoundSize && this.numberOfBuildings==1){
						maxFoundSize = actB.getSize();
						this.buildungNummer = actB.getID().toString();
						this.actSize = actB.getSize();
					}
					if (this.numberOfBuildings>1){
						if (actB.getSize()==this.targetSize){
							numberOfFinishedBuildings++;
						}
						if (actB.getSize()>maxFoundSize && actB.getSize()<this.targetSize){
							maxFoundSize = actB.getSize();
							this.buildungNummer = actB.getID().toString();
							this.actSize = actB.getSize();
						}
					}
				}
			}
			if (this.numberOfBuildings>1 && numberOfFinishedBuildings>=this.numberOfBuildings){
				// nix mehr zu tun
				this.addComment("Bauen: Fertig " + this.numberOfBuildings + " " + this.buildingType.getName());
				this.fertig=true;
				return;
			}
		}
		if (this.buildungNummer.length()<1){
			// Neubau
			this.addComment("Bauen: Neubau " + this.buildingType.getName() + " geplant.");
			this.actSize=0;
		} else {
			// checken der Grösse
			if (this.actSize<this.targetSize){
				this.addComment("Bauen: Weiterbau von " + (this.targetSize-this.actSize) + " Stufen an " + this.buildingType.getName() + "(" + this.buildungNummer + ") geplant.");
			} else {
				// schon fertig
				// haben wir eventuell mehrere? Dann vielleicht doch neubau?
				this.addComment("Bauen: Fertig " + this.buildingType.getName() + "(" + this.buildungNummer + ")");
				this.fertig=true;
				return;
			}
		}
		
		int anzahl = this.targetSize - this.actSize;
		int prio = 0;
		// Notwendigen Ressourcen anfragen und MPRs ablegen
		for (Iterator<Item> iter = this.buildingType.getRawMaterials().iterator();iter.hasNext();){
			Item actItem = (Item)iter.next();
			prio = 10; // Default
			// Prio anpassen
			if (actItem.getItemType().getName().equalsIgnoreCase("Stein")){prio=this.prioSteine;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Eisen")){prio=this.prioEisen;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Laen")){prio=this.prioEisen;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Holz")){prio=this.prioHolz;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Mallorn")){prio=this.prioHolz;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Silber")){prio=this.prioSilber;}
			MatPoolRequest MPR = new MatPoolRequest(this,anzahl * actItem.getAmount(),actItem.getItemType().getName(),prio,"Bauen: " + this.buildingType.getName());
			// steinSpec
			if (actItem.getItemType().getName().equalsIgnoreCase("Stein") && this.steinSpec.length()>0){
				MPR.addSpec(this.steinSpec);
			}
			// hier hinzufügen
			this.addMPR(actItem, MPR);
			// MatPool
			this.addMatPoolRequest(MPR);
			
			if (this.numberOfBuildings>1){
				// allgemein noch fertigzustellen
				int todoAnzahl = this.numberOfBuildings - numberOfFinishedBuildings;
				// dat nächste gerade angefordert mit voller prio
				todoAnzahl-=1;
				if (todoAnzahl>0){
					for (int x = 1;x<=todoAnzahl;x++){
						MatPoolRequest MPR2 = new MatPoolRequest(MPR);
						MPR2.setPrio((int)((double)MPR2.getPrio()*(double)0.75));
						MPR2.setPrioTM((int)((double)MPR2.getPrioTM()*(double)0.75));
						MPR2.setKommentar(MPR2.getKommentar().concat(" - Vorplanung " + x));
						this.addMatPoolRequest(MPR2);
					}
				}
				
			}
			
		}
	}
	
	private void vorMP_Strasse(){
		// actSize ermitteln - gibt es schon eine strasse in diese richtung?
		int actProz = 0;
		for (Iterator<Border> iter=this.scriptUnit.getUnit().getRegion().borders().iterator();iter.hasNext();){
			Border actBorder = (Border)iter.next();
			if (actBorder.getType().equalsIgnoreCase("Straße") && actBorder.getDirection()==this.dir.getDir()){
				actProz = actBorder.getBuildRatio();
			}
		}
		this.targetSize = this.scriptUnit.getUnit().getRegion().getRegionType().getRoadStones();
		if (actProz>0){
			// es gibt eine...fehlende Steine berechnen
			this.actSize = (int)Math.floor(((double)actProz/(double)100) * (double)this.targetSize);
		}
		
		if (this.actSize<this.targetSize){
			// Strasse noch zu machen
			this.addComment("Bauen: noch " + (this.targetSize - this.actSize) + " Steine für Strasse nach " + this.dir.toString() + " einzubauen.");
			this.steinRequest = new MatPoolRequest(this,(this.targetSize - this.actSize),"Stein",this.prioSteine,"Strassenbau " + this.dir.toString());
			if (this.steinSpec.length()>0){
				this.steinRequest.addSpec(this.steinSpec);
			}
			this.addMatPoolRequest(this.steinRequest);
		} else {
			// Strasse fertig
			this.addComment("Bauen: Strasse nach " + this.dir.toString() + " fertig.");
			this.fertig = true;
			return;
		}
	}
	
	private void vorMP_Burg(){
		if (this.buildungNummer.length()<1){
			// noch keine Nummer bekannt
			Building actB = FFToolsRegions.getBiggestCastle(this.region());
			if (actB!=null){
				this.buildungNummer = actB.getID().toString();
				this.actSize = actB.getSize();
			}
		}
		if (this.buildungNummer.length()<1){
			// Neubau
			this.addComment("Bauen: Neubau einer Burg geplant.");
			this.actSize=0;
		} else {
			// checken der Grösse
			if (this.actSize<this.targetSize){
				this.addComment("Bauen: Weiterbau von " + (this.targetSize-this.actSize) + " Stufen an Burg (" + this.buildungNummer + ") geplant.");
			} else {
				// schon fertig
				this.addComment("Bauen: Fertig Burg (" + this.buildungNummer + ")");
				this.fertig=true;
				return;
			}
		}
		int anzahl = this.targetSize - this.actSize;
		
		this.steinRequest = new MatPoolRequest(this,anzahl,"Stein",this.prioSteine,"Burgenbau");
		if (this.steinSpec.length()>0){
			this.steinRequest.addSpec(this.steinSpec);
		}
		this.addMatPoolRequest(this.steinRequest);
		
	}
	
	/**
	 * Zweiter Lauf nach dem ersten(?) MatPool
	 */
	
	private void nachMatPool(){
		if (!this.parseOK){return;}
		if (this.fertig){return;}
		// Behandlung gleich nach Typ
		switch (this.actTyp){
			case Bauen.BUILDING: this.nachMP_Building();break;
			case Bauen.BURG: this.nachMP_Burg();break;
			case Bauen.STRASSE: this.nachMP_Strasse();break;
		}
		
	}
	
	private void nachMP_Building(){
		// Step 1 was könnte ich maximal nach Ressourcen bauen?
		int anzRes = this.targetSize - this.actSize;
		for (Iterator<Item> iter = this.buildingType.getRawMaterials().iterator();iter.hasNext();){
			// in actItem die benötigte Anzahl pro Grössenpunkt
			Item actItem = (Item)iter.next();
			// im MPR das Ergebnis des MPs
			MatPoolRequest MPR = this.mprTable.get(actItem);
			int actAnz = (int)Math.floor((double)MPR.getBearbeitet()/(double)actItem.getAmount());
			if (actAnz<anzRes){anzRes=actAnz;}
		}
		this.addComment("Bauen: Ressourcen für " + anzRes + " Stufen bei " + this.buildingType.getName() + " verfügbar.");
		
		// Step 2 was könnten wir maximal nach Talentstand der Einheit bauen?
		SkillType bauType = this.gd_Script.rules.getSkillType("Burgenbau",false);
		int anzTalPoints = 0;
		if (bauType!=null){
			Skill bauSkill = this.scriptUnit.getUnit().getModifiedSkill(bauType);
			if (bauSkill!=null){
				anzTalPoints = bauSkill.getLevel() * this.scriptUnit.getUnit().getModifiedPersons();
			}
		}
		
		// Schaffenstrunk?
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
			anzTalPoints *= 2;
			scriptUnit.addComment("Trankeffekt berücksichtigt");
		}
		
		// RdF ?
		if (FFToolsGameData.hasRdfFEffekt(this.scriptUnit)){
			anzTalPoints *= 2;
			scriptUnit.addComment("RdfF berücksichtigt");
		}
		
		
		
		
		int anzTal=0;
		if (this.buildingType.getBuildSkillLevel()>0){
			anzTal = (int)Math.floor((double)anzTalPoints/(double)this.buildingType.getBuildSkillLevel());
		}
		this.addComment("Bauen: Einheit ist fähig für " + anzTal + " Stufen bei " + this.buildingType.getName());
		
		int actAnz = Math.min(anzTal,anzRes);
		
		// wird gebäude fertig?
		boolean complete = false;
		if (actAnz + this.actSize>=this.targetSize){
			complete=true;
		}
		
		// wird Mindestauslastung eingehalten
		boolean okAusl = false;
		int Auslastung = (int)Math.floor(((double)actAnz/(double)anzTal) * 100);
		this.addComment("Bauen: Auslastung bei " + this.buildingType.getName() + ": " + Auslastung + "% (min:" + this.minAuslastung + "%)");
		if (Auslastung>=this.minAuslastung){
			okAusl=true;
		}
		
		// Entscheidung
		if (okAusl || complete){
			// bauen, wenn nicht schon anderes Bauscript vorhanden
			if (this.actSize>0){
				this.bauBefehl = "MACHEN " + actAnz + " BURG " + this.buildungNummer;
			} else {
				this.bauBefehl = "MACHEN " + actAnz + " " + this.buildingType.getName();
			}
			
		} else {
			// nicht bauen
			this.addComment("Bauen: " + this.buildingType.getName() + " wird diese Runde nicht weitergebaut.");
			this.bauBefehl = "";
		}	
	}
	
	private void nachMP_Burg(){
		// Step 1 was könnte ich maximal nach Ressourcen bauen?
		int anzRes = this.steinRequest.getBearbeitet();
		
		
		// Step 2 was könnten wir maximal nach Talentstand der Einheit bauen?
		SkillType bauType = this.gd_Script.rules.getSkillType("Burgenbau",false);
		int anzTalPoints = 0;
		if (bauType!=null){
			Skill bauSkill = this.scriptUnit.getUnit().getModifiedSkill(bauType);
			if (bauSkill!=null){
				anzTalPoints = bauSkill.getLevel() * this.scriptUnit.getUnit().getModifiedPersons();
			}
		}
		
		// Schaffenstrunk?
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
			anzTalPoints *= 2;
			scriptUnit.addComment("Trankeffekt berücksichtigt");
		}
		
		// RdF ?
		if (FFToolsGameData.hasRdfFEffekt(this.scriptUnit)){
			anzTalPoints *= 2;
			scriptUnit.addComment("RdfF berücksichtigt");
		}
		
		
		int anzTal=0;
		
		anzTal = (int)Math.floor((double)anzTalPoints/(double)FFToolsGameData.getCastleSizeBuildSkillLevel(this.actSize));
		
		this.addComment("Bauen: Einheit ist fähig für " + anzTal + " Stufen bei der Burg");
		
		int actAnz = Math.min(anzTal,anzRes);
		
		// wird gebäude fertig?
		boolean complete = false;
		if (actAnz + this.actSize>=this.targetSize){
			complete=true;
		}
		
		// wird Mindestauslastung eingehalten
		boolean okAusl = false;
		int Auslastung = (int)Math.floor(((double)actAnz/(double)anzTal) * 100);
		this.addComment("Bauen: Auslastung bei Burgenbau: " + Auslastung + "% (min:" + this.minAuslastung + "%)");
		if (Auslastung>=this.minAuslastung){
			okAusl=true;
		}
		
		// Entscheidung
		if (okAusl || complete){
			// bauen, wenn nicht schon anderes Bauscript vorhanden
			if (this.actSize>0){
				this.bauBefehl = "MACHEN " + actAnz + " BURG " + this.buildungNummer;
			} else {
				this.bauBefehl = "MACHEN " + actAnz + " BURG";
			}
			
		} else {
			// nicht bauen
			this.addComment("Bauen: Burg wird diese Runde nicht weitergebaut.");
			this.bauBefehl = "";
		}	
	}
	
	private void nachMP_Strasse(){
		
		if (this.isFertig()){
			return;
		}
		
		// Step 1 was könnte ich maximal nach Ressourcen bauen?
		int anzRes = this.steinRequest.getBearbeitet();
		
		
		// Step 2 was könnten wir maximal nach Talentstand der Einheit bauen?
		SkillType bauType = this.gd_Script.rules.getSkillType("Strassenbau",false);
		int anzTalPoints = 0;
		if (bauType!=null){
			Skill bauSkill = this.scriptUnit.getUnit().getModifiedSkill(bauType);
			if (bauSkill!=null){
				anzTalPoints = bauSkill.getLevel() * this.scriptUnit.getUnit().getModifiedPersons();
			}
		}
		int anzTal=anzTalPoints;
		
		this.addComment("Bauen: Einheit ist fähig für " + anzTal + " Stufen bei der Strasse");
		
		int actAnz = Math.min(anzTal,anzRes);
		
		// wird Strasse fertig?
		boolean complete = false;
		if (actAnz + this.actSize>=this.targetSize){
			complete=true;
		}
		
		// wird Mindestauslastung eingehalten
		boolean okAusl = false;
		int Auslastung = (int)Math.floor(((double)actAnz/(double)anzTal) * 100);
		this.addComment("Bauen: Auslastung bei Strassenbau: " + Auslastung + "% (min:" + this.minAuslastung + "%)");
		if (Auslastung>=this.minAuslastung){
			okAusl=true;
		}
		
		// Entscheidung
		if (okAusl || complete){
			this.bauBefehl = "MACHEN STRASSE " + this.dir.toString();
		} else {
			// nicht bauen
			this.addComment("Bauen: Strasse (" + this.dir.toString() + ") wird diese Runde nicht weitergebaut.");
			this.bauBefehl = "";
		}	
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}	
	
	private void prioAdaption(){
		if (this.actTyp==Bauen.STRASSE){
			this.prioSteine = 600;
		}
		if (this.actTyp == Bauen.BURG){
			if (this.targetSize==1){this.prioSteine=580;}
			if (this.targetSize==2){this.prioSteine=570;}
			if (this.targetSize>2){this.prioSteine=560;}
			if (this.targetSize>10){this.prioSteine=550;}
			if (this.targetSize>50){this.prioSteine=540;}
			if (this.targetSize>250){this.prioSteine=530;}
			if (this.targetSize>1250){this.prioSteine=520;}
			if (this.targetSize>6250){this.prioSteine=510;}
		}
		if (this.actTyp == Bauen.BUILDING){
			String s = this.buildingType.getName();
			if (s.equalsIgnoreCase("Leuchtturm")){this.prioSteine = 590;}
			if (s.equalsIgnoreCase("Magierturm")){this.prioSteine = 610;}
			if (s.equalsIgnoreCase("Akademie")){this.prioSteine = 620;}
			if (s.equalsIgnoreCase("Bergwerk")){this.prioSteine = 630;}
			if (s.equalsIgnoreCase("Schmiede")){this.prioSteine = 640;}
			if (s.equalsIgnoreCase("Sägewerk")){this.prioSteine = 650;}
		}
		
	}
	
	private void addMPR(Item actItem, MatPoolRequest MPR){
		if (this.mprTable==null){
			this.mprTable = new Hashtable<Item, MatPoolRequest>();
		}
		mprTable.put(actItem,MPR);
	}


	/**
	 * @return the bauBefehl
	 */
	public String getBauBefehl() {
		return bauBefehl;
	}


	/**
	 * @return the fertig
	 */
	public boolean isFertig() {
		return fertig;
	}


	/**
	 * @return the lernTalent
	 */
	public String getLernTalent() {
		return lernTalent;
	}


	/**
	 * @return the actSize
	 */
	public int getActSize() {
		return actSize;
	}


	/**
	 * @return the targetSize
	 */
	public int getTargetSize() {
		return targetSize;
	}


	/**
	 * @return the prioSteine
	 */
	public int getPrioSteine() {
		return prioSteine;
	}
	
	
}
