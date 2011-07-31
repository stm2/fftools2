package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Border;
import magellan.library.Building;
import magellan.library.CoordinateID;
import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.rules.BuildingType;
import magellan.library.rules.SkillType;
import magellan.library.utils.Direction;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

/**
 * 
 * Dat Bauscript - f�r Bauarbeiter, und solche, die nur matreial f�r einen Bau anfordern sollen
 * @author Fiete
 *
 */

public class Bauen extends MatPoolScript implements Cloneable{
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	private int Durchlauf_Baumanager = 8;
	private int Durchlauf_vorMatPool = 10;
	private int Durchlauf_nachMatPool = 100;
	
	private int[] runners = {Durchlauf_Baumanager,Durchlauf_vorMatPool,Durchlauf_nachMatPool};
	
	private boolean parseOK = false;
	
	/**
	 * zum besseren Verst�ndnis
	 */
	public static final int STRASSE=1;
	public static final int BURG=2;
	public static final int BUILDING=3;
	
	public static final int minReitLevel=-1;
	public static final int minDefaultBurgenbauTalent=3;  // Burgenbau
	public static final int minDefaultStrassenbauTalent=2; 
	
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
	 * aktuelle Gr�sse des (gr�ssten) passenden Objektes in der Region
	 */
	private int actSize = 0;
	
	/**
	 * bei bedarf gw�nschte Gr�sse des Buildings / der Burg
	 */
	private int targetSize = 0;
	
	/**
	 * bei Strasse, Richtung
	 */
	private Direction dir = null;
	
	/**
	 * Ort der MPRs, Items sind die rawMaterials f�r den Burgentyp
	 */
	private Hashtable<Item,MatPoolRequest> mprTable = null;
	
	/**
	 * wenn dieses Bauscript an der Reihe ist, dann mit diesem Befehl
	 */
	private String bauBefehl = "";
	
	/**
	 * wenn Typ gr�ssenbegrenzt, mehrere Geb�ude pro Region m�glich, z.B. Akademie
	 * dann hier anzahl der geb�ude pro Region
	 * paremeter: anzahl=1
	 */
	private int numberOfBuildings = 1;
	
	private String lernTalent = "Burgenbau";
	
	private int minBurgenbauTalent=Bauen.minDefaultBurgenbauTalent;
	private int minStassenbauTalent=Bauen.minDefaultStrassenbauTalent;
	private int spec = Bauen.BURG;
	private boolean isLearning=false;
	
	private String statusInfo = "";
	private String finalStatusInfo = "";
	
	// falls in PLanungsmodus...dann eventuell sp�terer zieleinheit �berhelfen
	private ArrayList<MatPoolRequest> vorPlanungsMPR = null;
	
	public void setFinalStatusInfo(String finalStatusInfo) {
		this.finalStatusInfo += finalStatusInfo;
	}


	public String getFinalStatusInfo() {
		return finalStatusInfo;
	}


	/**
	 * wenn in planingMode, dann nur feststellen des Bedarfes
	 * kein MPRs etc
	 * genutzt von Bauauftrag
	 */
	private boolean planingMode=false;
	
	/**
	 * mode=auto
	 * Steuerung vom Baumanager
	 */
	private boolean automode=false;
	
	/**
	 * wird vom TA-Baumanager genutzt
	 */
	private boolean automode_hasPlan = false;
	
	/**
	 * wird vom TA-Baumanager genutzt
	 */
	private boolean hasGotoOrder = false;
	
	/**
	 * kann gesetzt werden als Heimatbasis
	 */
	private CoordinateID homeDest = null;
	
	/**
	 * 
	 * @param homeDest
	 */
	private boolean originatedFromBauMAnger = false;
	
	public void setHomeDest(CoordinateID homeDest) {
		this.addComment("Home Region zentral �bernommen: " + homeDest.toString());
		this.homeDest = homeDest;
	}


	public boolean isHasGotoOrder() {
		return hasGotoOrder;
	}


	public void setHasGotoOrder(boolean hasGotoOrder) {
		this.hasGotoOrder = hasGotoOrder;
	}


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
		
	
		if (scriptDurchlauf==Durchlauf_Baumanager){
			this.BauManager();
		}
	
		if (scriptDurchlauf==Durchlauf_vorMatPool){
			this.vorMatPool();
		}
        

		if (scriptDurchlauf==Durchlauf_nachMatPool){
			this.nachMatPool();
		}
		
	}
	
	/** lediglich registrierung und ob auto
	 * 
	 */
	private void BauManager(){
		if (!this.isInPlaningMode()){
			super.addVersionInfo();
			
			// eintragen
			this.getBauManager().addBauScript(this);
			
		}
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());

		// automode
		if (OP.getOptionString("mode").equalsIgnoreCase("auto")){
			this.setAutomode(true);
		}
		
		// wants info
		if (OP.getOptionBoolean("info", false)){
			this.getBauManager().addInformationListener(this.scriptUnit);
		}
		
		// home
		String homeString=OP.getOptionString("home");
		if (homeString.length()>2){
			CoordinateID actDest = null;
			if (homeString.indexOf(',') > 0) {
				actDest = CoordinateID.parse(homeString,",");
			} else {
			// Keine Koordinaten, also Region in Koordinaten konvertieren
				actDest = FFToolsRegions.getRegionCoordFromName(this.gd_Script, homeString);
			}
			if (actDest!=null){
				this.homeDest=actDest;
			} else {
				this.addComment("!!! HOME Angabe nicht erkannt!");
				this.doNotConfirmOrders();
			}
		}
		
		// spec
		if (OP.getOptionString("spec").equalsIgnoreCase("Strassenbau") || OP.getOptionString("spec").equalsIgnoreCase("Strasse")){
			this.spec=Bauen.STRASSE;
		}
		
		// minTalent
		this.minBurgenbauTalent=OP.getOptionInt("minTalent",this.minBurgenbauTalent);
		this.minBurgenbauTalent=OP.getOptionInt("minBurgenbau",this.minBurgenbauTalent);
		this.minStassenbauTalent = OP.getOptionInt("minStrassenbau", this.minStassenbauTalent);
		
		if (this.spec==Bauen.STRASSE){
			if (this.scriptUnit.getSkillLevel("Strassenbau")<this.minStassenbauTalent){
				this.lernTalent="Strassenbau";
				this.setFinalStatusInfo("Min Strassenbau. ");
				this.setAutomode_hasPlan(true);
				isLearning=true;
			}
		} else {
			if (this.scriptUnit.getSkillLevel("Burgenbau")<this.minBurgenbauTalent){
				this.lernTalent="Burgenbau";
				this.setFinalStatusInfo("Min Burgenbau. ");
				this.setAutomode_hasPlan(true);
				isLearning=true;
			}
		}
		
		
	}


	public void vorMatPool(){
		
		if (this.isAutomode()){
			return;
		}
		
		if (this.isLearning){
			return;
		}
		
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
			// Typ muss Geb�udeType enthalten
			String s = OP.getOptionString("Typ");
			if (s.length()<2){
				s = OP.getOptionString("Type");
			}
			this.buildingType = this.gd_Script.rules.getBuildingType(s,false);
			if (this.buildingType==null){
				// Abbruch
				this.addComment("Bauen: unbekanntes Geb�ude: " + s);
				statusInfo+="Fehler: unbekanntes Geb�ude: " + s;
				this.doNotConfirmOrders();
				return;
			}
			
			// wenn keine maxGr�sse angegeben im Type wird gr�sse erwartet
			if (this.buildingType.getMaxSize()>0){
				// Gr�sse ist vorgegeben
				this.targetSize = this.buildingType.getMaxSize();
			} else {
				// Gr�sse MUSS angegeben werden
				int i = OP.getOptionInt("ziel",0);
				if (i==0){
					i = OP.getOptionInt("size",0);
				}
				if (i>0) {
					// alles OK
					this.targetSize=i;
				} else {
					// nix ist OK
					this.addComment("Bauen: bei " + this.buildingType.getName() + " MUSS eine Zielgr�sse angegeben werden! (ziel=X)");
					statusInfo+="Fehler: bei " + this.buildingType.getName() + " MUSS eine Zielgr�sse angegeben werden! (ziel=X)";
					this.doNotConfirmOrders();
					return;
				}
			}
			// Anzahl vomBuildings
			this.numberOfBuildings=OP.getOptionInt("anzahl", 1);
		}
		if (this.actTyp == Bauen.BURG){
			// Gr�sse MUSS angegeben sein.
			int i = OP.getOptionInt("ziel",0);
			if (i==0){
				i = OP.getOptionInt("size",0);
			}
			if (i>0) {
				// alles OK
				this.targetSize=i;
			} else {
				// nix ist OK
				this.addComment("Bauen: beim Burgenbau MUSS eine Zielgr�sse angegeben werden! (ziel=X)");
				statusInfo+="Fehler: bei " + this.buildingType.getName() + " MUSS eine Zielgr�sse angegeben werden! (ziel=X)";
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
				statusInfo+="Fehler: Strassenrichtung nicht erkannt: " + s;
				this.doNotConfirmOrders();
				return;
			}
			if (this.dir.getDir()==Direction.DIR_INVALID){
				this.dir=null;
				this.addComment("Bauen: Strassenrichtung nicht erkannt: " + s);
				statusInfo+="Fehler: Strassenrichtung nicht erkannt: " + s;
				this.doNotConfirmOrders();
				return;
			}
			
		}
		
		// Priorit�ten
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
				statusInfo+="Fehler: Prio nicht erkannt: " + i;
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
		
		// Transporterspecs f�r Steine
		this.steinSpec = OP.getOptionString("steinspec");
		
		// min Auslastung
		this.minAuslastung = OP.getOptionInt("minAuslastung",this.minAuslastung);
		
		// lernTalent
		// this.lernTalent = OP.getOptionString("Talent");
		if (OP.getOptionString("Talent").length()>2){
			this.lernTalent = OP.getOptionString("Talent");
		}
		
		
		
		// Burg + Building k�nnen eine Nummer mitbekommen haben....checken
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
				statusInfo+="Fehler: " + s + " kann nicht gefunden werden.";
				this.doNotConfirmOrders();
				return;
			}
		}
		
		if (!this.isInPlaningMode()){
			this.scriptUnit.findScriptClass("RequestInfo");
		}
		
		this.parseOK = true;
		
		// eigentlicher Ablauf nu getrennt
		switch (this.actTyp){
			case Bauen.BUILDING: this.vorMP_Building();break;
			case Bauen.BURG: this.vorMP_Burg();break;
			case Bauen.STRASSE: this.vorMP_Strasse();break;
		}
		
		if (this.isAutomode()){
			statusInfo+=";automode";
		}
		if (this.isInPlaningMode()){
			statusInfo+="(planing)";
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
				String s = "Bauen: Fertig " + this.numberOfBuildings + " " + this.buildingType.getName();
				this.addComment(s);
				statusInfo+=s;
				this.fertig=true;
				return;
			}
		}
		if (this.buildungNummer.length()<1){
			// Neubau
			String s = "Bauen: Neubau " + this.buildingType.getName();
			if (this.targetSize>0){
				s += "(" + this.targetSize + ")";
			}
			s += " geplant.";
			this.addComment(s);
			statusInfo+=s;
			this.actSize=0;
		} else {
			// checken der Gr�sse
			if (this.actSize<this.targetSize){
				String s = "Bauen: Weiterbau von " + (this.targetSize-this.actSize) + " Stufen an " + this.buildingType.getName() + "(" + this.buildungNummer + ") geplant.";
				statusInfo+=s;
				this.addComment(s);
				
			} else {
				// schon fertig
				// haben wir eventuell mehrere? Dann vielleicht doch neubau?
				String s = "Bauen: Fertig " + this.buildingType.getName() + "(" + this.buildungNummer + ")";
				this.addComment(s);
				statusInfo+=s;
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
			if (!this.isInPlaningMode()){
				// hier hinzuf�gen
				this.addMPR(actItem, MPR);
				// MatPool
				this.addMatPoolRequest(MPR);
			} else {
				this.addComment("Bauauftrag-Reuqest: " + MPR.toString());
				this.addPlanungsMPR(MPR);
			}
			
			if (this.numberOfBuildings>1){
				// allgemein noch fertigzustellen
				int todoAnzahl = this.numberOfBuildings - numberOfFinishedBuildings;
				// dat n�chste gerade angefordert mit voller prio
				todoAnzahl-=1;
				if (todoAnzahl>0){
					for (int x = 1;x<=todoAnzahl;x++){
						MatPoolRequest MPR2 = new MatPoolRequest(MPR);
						MPR2.setPrio((int)((double)MPR2.getPrio()*(double)0.75));
						MPR2.setPrioTM((int)((double)MPR2.getPrioTM()*(double)0.75));
						MPR2.setKommentar(MPR2.getKommentar().concat(" - Vorplanung " + x));
						MPR2.setOriginalGefordert(this.targetSize * actItem.getAmount());
						if (!this.isInPlaningMode()){
							this.addMatPoolRequest(MPR2);
						} else {
							this.addComment("Bauauftrag-Reuqest: " + MPR2.toString());
							this.addPlanungsMPR(MPR2);
						}
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
			if (actBorder.getType().equalsIgnoreCase("Stra�e") && actBorder.getDirection()==this.dir.getDir()){
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
			String s = "Bauen: noch " + (this.targetSize - this.actSize) + " Steine f�r Strasse nach " + this.dir.toString() + " einzubauen.";
			this.addComment(s);
			statusInfo+=s;
			this.steinRequest = new MatPoolRequest(this,(this.targetSize - this.actSize),"Stein",this.prioSteine,"Strassenbau " + this.dir.toString());
			if (this.steinSpec.length()>0){
				this.steinRequest.addSpec(this.steinSpec);
			}
			if (!this.isInPlaningMode()){
				this.addMatPoolRequest(this.steinRequest);
			} else {
				this.addComment("Bauauftrag-Reuqest: " + this.steinRequest.toString());
				this.addPlanungsMPR(this.steinRequest);
			}
		} else {
			// Strasse fertig
			String s = "Bauen: Strasse nach " + this.dir.toString() + " fertig.";
			this.addComment(s);
			statusInfo+=s;
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
			String s = "Bauen: Neubau einer Burg";
			if (this.targetSize>0){
				s+="(" + this.getTargetSize() + ")";
			}
			s += " geplant.";
			this.addComment(s);
			statusInfo+=s;
			this.actSize=0;
		} else {
			// checken der Gr�sse
			if (this.actSize<this.targetSize){
				String s = "Bauen: Weiterbau von " + (this.targetSize-this.actSize) + " Stufen an Burg (" + this.buildungNummer + ") geplant.";
				this.addComment(s);
				statusInfo+=s;
			} else {
				// schon fertig
				String s = "Bauen: Fertig Burg (" + this.buildungNummer + ")";
				this.addComment(s);
				statusInfo+=s;
				this.fertig=true;
				return;
			}
		}
		int anzahl = this.targetSize - this.actSize;
		
		this.steinRequest = new MatPoolRequest(this,anzahl,"Stein",this.prioSteine,"Burgenbau");
		if (this.steinSpec.length()>0){
			this.steinRequest.addSpec(this.steinSpec);
		}
		
		// this.addMatPoolRequest(this.steinRequest);
		if (!this.isInPlaningMode()){
			this.addMatPoolRequest(this.steinRequest);
		} else {
			this.addComment("Bauauftrag-Reuqest: " + this.steinRequest.toString());
			this.addPlanungsMPR(this.steinRequest);
		}
		
	}
	
	/**
	 * Zweiter Lauf nach dem ersten(?) MatPool
	 */
	
	private void nachMatPool(){
		if (!this.parseOK){return;}
		if (this.fertig){return;}
		if (this.isInPlaningMode()){
			return;
		}
		if (this.isAutomode()){
			return;
		}
		if (this.isLearning){
			return;
		}
		// Behandlung gleich nach Typ
		switch (this.actTyp){
			case Bauen.BUILDING: this.nachMP_Building();break;
			case Bauen.BURG: this.nachMP_Burg();break;
			case Bauen.STRASSE: this.nachMP_Strasse();break;
		}
		
	}
	
	private void nachMP_Building(){
		// Step 1 was k�nnte ich maximal nach Ressourcen bauen?
		int anzRes = this.targetSize - this.actSize;
		for (Iterator<Item> iter = this.buildingType.getRawMaterials().iterator();iter.hasNext();){
			// in actItem die ben�tigte Anzahl pro Gr�ssenpunkt
			Item actItem = (Item)iter.next();
			// im MPR das Ergebnis des MPs
			MatPoolRequest MPR = this.mprTable.get(actItem);
			int actAnz = (int)Math.floor((double)MPR.getBearbeitet()/(double)actItem.getAmount());
			if (actAnz<anzRes){anzRes=actAnz;}
		}
		this.addComment("Bauen: Ressourcen f�r " + anzRes + " Stufen bei " + this.buildingType.getName() + " verf�gbar.");
		
		// Step 2 was k�nnten wir maximal nach Talentstand der Einheit bauen?
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
			scriptUnit.addComment("Trankeffekt ber�cksichtigt");
		}
		
		// RdF ?
		if (FFToolsGameData.hasRdfFEffekt(this.scriptUnit)){
			anzTalPoints *= 2;
			scriptUnit.addComment("RdfF ber�cksichtigt");
		}
		
		
		
		
		int anzTal=0;
		if (this.buildingType.getBuildSkillLevel()>0){
			anzTal = (int)Math.floor((double)anzTalPoints/(double)this.buildingType.getBuildSkillLevel());
		}
		this.addComment("Bauen: Einheit ist f�hig f�r " + anzTal + " Stufen bei " + this.buildingType.getName());
		
		int actAnz = Math.min(anzTal,anzRes);
		
		// wird geb�ude fertig?
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
			this.setFinalStatusInfo("baut " + actAnz + " " + this.buildingType.getName());
			
		} else {
			// nicht bauen
			this.addComment("Bauen: " + this.buildingType.getName() + " wird diese Runde nicht weitergebaut.");
			this.bauBefehl = "";
			this.setFinalStatusInfo("wartet auf Geb�ude.");
		}	
	}
	
	private void nachMP_Burg(){
		// Step 1 was k�nnte ich maximal nach Ressourcen bauen?
		int anzRes = this.steinRequest.getBearbeitet();
		
		
		// Step 2 was k�nnten wir maximal nach Talentstand der Einheit bauen?
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
			scriptUnit.addComment("Trankeffekt ber�cksichtigt");
		}
		
		// RdF ?
		if (FFToolsGameData.hasRdfFEffekt(this.scriptUnit)){
			anzTalPoints *= 2;
			scriptUnit.addComment("RdfF ber�cksichtigt");
		}
		
		
		int anzTal=0;
		
		anzTal = (int)Math.floor((double)anzTalPoints/(double)FFToolsGameData.getCastleSizeBuildSkillLevel(this.actSize));
		
		this.addComment("Bauen: Einheit ist f�hig f�r " + anzTal + " Stufen bei der Burg");
		
		int actAnz = Math.min(anzTal,anzRes);
		
		// wird geb�ude fertig?
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
			this.setFinalStatusInfo("baut " + actAnz + " Burg");
		} else {
			// nicht bauen
			this.addComment("Bauen: Burg wird diese Runde nicht weitergebaut.");
			this.setFinalStatusInfo("wartet auf Burgenbau. ");
			this.bauBefehl = "";
		}	
	}
	
	private void nachMP_Strasse(){
		
		if (this.isFertig()){
			return;
		}
		
		// Step 1 was k�nnte ich maximal nach Ressourcen bauen?
		int anzRes = this.steinRequest.getBearbeitet();
		
		
		// Step 2 was k�nnten wir maximal nach Talentstand der Einheit bauen?
		SkillType bauType = this.gd_Script.rules.getSkillType("Strassenbau",false);
		int anzTalPoints = 0;
		if (bauType!=null){
			Skill bauSkill = this.scriptUnit.getUnit().getModifiedSkill(bauType);
			if (bauSkill!=null){
				anzTalPoints = bauSkill.getLevel() * this.scriptUnit.getUnit().getModifiedPersons();
			}
		}
		int anzTal=anzTalPoints;
		
		this.addComment("Bauen: Einheit ist f�hig f�r " + anzTal + " Stufen bei der Strasse");
		
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
			this.setFinalStatusInfo("baut Strasse");
		} else {
			// nicht bauen
			this.addComment("Bauen: Strasse (" + this.dir.toString() + ") wird diese Runde nicht weitergebaut.");
			this.bauBefehl = "";
			this.setFinalStatusInfo("wartet auf Strassenbau");
		}	
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
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
			if (s.equalsIgnoreCase("S�gewerk")){this.prioSteine = 650;}
		}
		
	}
	
	private void addMPR(Item actItem, MatPoolRequest MPR){
		if (this.mprTable==null){
			this.mprTable = new Hashtable<Item, MatPoolRequest>();
		}
		mprTable.put(actItem,MPR);
	}

	
	private void addPlanungsMPR(MatPoolRequest MPR){
		if (this.vorPlanungsMPR==null){
			this.vorPlanungsMPR = new ArrayList<MatPoolRequest>();
		}
		if (!this.vorPlanungsMPR.contains(MPR)){
			this.vorPlanungsMPR.add(MPR);
		}
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


	/**
	 * @return the planingMode
	 */
	public boolean isInPlaningMode() {
		return planingMode;
	}


	/**
	 * @param planingMode the planingMode to set
	 */
	public void setPlaningMode(boolean planingMode) {
		this.planingMode = planingMode;
	}


	/**
	 * @return the automode
	 */
	public boolean isAutomode() {
		return automode;
	}


	/**
	 * @param automode the automode to set
	 */
	private void setAutomode(boolean automode) {
		this.automode = automode;
	}
	
	
	public String toString(){
		String erg = "";
		if (statusInfo==""){
			erg = "Bauen ohne besonderen Status bei " + this.unitDesc();
		} else {
			erg = statusInfo;
			if (this.getPrioSteine()>0){
				erg = "(Prio " + this.getPrioSteine() + ") " + erg;
			}
		}
		return erg;
	}
	
	
	public String getUnitBauInfo(){
		String erg =this.unitDesc();
		SkillType sT = this.gd_Script.rules.getSkillType("Burgenbau",false);
		Skill s = this.scriptUnit.getUnit().getModifiedSkill(sT);
		if (s==null){
			erg += ",kein Burgenbau";
		} else {
			int tp = this.scriptUnit.getUnit().getModifiedPersons() * s.getLevel();
			erg += ", " + tp + " Burgenbau (" + this.scriptUnit.getUnit().getModifiedPersons() + "x" + s.getLevel()+")";
		}
		sT = this.gd_Script.rules.getSkillType("Stra�enbau",false);
		s = this.scriptUnit.getUnit().getModifiedSkill(sT);
		if (s==null){
			erg += ",kein Strassenbau";
		} else {
			int tp = this.scriptUnit.getUnit().getModifiedPersons() * s.getLevel();
			erg += ", " + tp + " Strassenbau (" + this.scriptUnit.getUnit().getModifiedPersons() + "x" + s.getLevel()+")";
		}

		return erg;
	}


	/**
	 * @return the automode_hasPlan
	 */
	public boolean hasPlan() {
		return automode_hasPlan;
	}


	/**
	 * @param automode_hasPlan the automode_hasPlan to set
	 */
	public void setAutomode_hasPlan(boolean automode_hasPlan) {
		this.automode_hasPlan = automode_hasPlan;
	}


	/**
	 * @return the actTyp
	 */
	public int getActTyp() {
		return actTyp;
	}


	/**
	 * @return the buildingType
	 */
	public BuildingType getBuildingType() {
		return buildingType;
	}
	
	
	public Bauen clone(){
		try {
			return (Bauen)super.clone();
		}
	      catch(CloneNotSupportedException e) {
	      }
	     return null;
	}
	
	
	/**
	 * wird aufgerufen, wenn in Automode und keinen Auftrag erhalten
	 */
	public void autoLearn(){
		this.addComment("Keine Auftr�ge vom Baumanager erhalten.");
		if (this.scriptUnit.getSkillLevel("Reiten")<Bauen.minReitLevel){
			this.addComment("Mindestreitlevel unterschritten. Lerne Reiten");
			// this.addOrder("LERNEN Reiten ;mindestReitlevel", true);
			this.lerneTalent("Reiten", false);
			this.finalStatusInfo="Mindestreitlevel";
			return;
		}
		
		// wenn home gesetzt, dann dorthin laufen
		if (this.homeDest!=null){
			// sind wir da?
			CoordinateID actPos = super.scriptUnit.getUnit().getRegion().getCoordinate();
			if (!actPos.equals(this.homeDest)){
				// wir sind noch nicht da
				// ja, hinreiten und pferde requesten
				GotoInfo gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.region().getCoordinate(), this.homeDest, true);
				this.addComment("unterwegs in die HOME-Region");
				this.addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
				// Pferde requesten...
				if (this.scriptUnit.getSkillLevel("Reiten")>0){
					MatPoolRequest MPR = new MatPoolRequest(this,this.scriptUnit.getUnit().getModifiedPersons(), "Pferd", 21, "Bauarbeiter unterwegs" );
					this.addMatPoolRequest(MPR);
				}
				this.finalStatusInfo="going HOME";
				return;
			} else {
				this.addComment("bereits in dier HOME-Region");
			}
		}
		
		
		// this.addOrder("LERNEN " + this.lernTalent + " ;unbesch�ftigt", true);
		this.lerneTalent(this.lernTalent, true);
		this.finalStatusInfo="ABM: LERNEN";
	}


	public boolean isOriginatedFromBauMAnger() {
		return originatedFromBauMAnger;
	}


	public void setOriginatedFromBauMAnger(boolean originatedFromBauMAnger) {
		this.originatedFromBauMAnger = originatedFromBauMAnger;
	}
	
	
	public void transferPlanungsMPR(){
		if (this.vorPlanungsMPR!=null && this.vorPlanungsMPR.size()>0){
			for (MatPoolRequest MPR:this.vorPlanungsMPR){
				this.addMatPoolRequest(MPR);
			}
		}
	}
	
}