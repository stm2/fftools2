package com.fftools.scripts;

import java.util.Iterator;

import magellan.library.Building;
import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;

/**
 * 
 * Eine erste abgespeckte Version zur Produktion
 * Waffen und R�stungen
 * @author Fiete
 *
 */

public class Schmieden extends MatPoolScript{
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf_vorMatPool = 50;
	private int Durchlauf_nachMatPool = 100;
	
	private int[] runners = {Durchlauf_vorMatPool,Durchlauf_nachMatPool};
	
	/**
	 * herzustellendes Object
	 */
	private ItemType itemType = null;
	
	/**
	 * basisPrio zur Anforderung von Eisen = Laen 
	 */
	private int eisenPrio = 500;
	/**
	 * basisPrio zur Anforderung von Holz
	 */
	private int holzPrio = 500;
	
	/**
	 * Anzahl Vorratsrunden
	 */
	private int vorratsRunden = 3;
	
	/**
	 * minimale Auslastung
	 */
	private int minAuslastung = 75;
	
	/**
	 * prioAnforderungsbonus f�r Schmiedeinsassen
	 */
	private int schmiedeBonus = 25;
	
	/**
	 * Eisen/Laen Request f�r diese Runde
	 */
	private MatPoolRequest eisenRequest = null;
	
	/**
	 * Holzrequest f�r diese Runde
	 */
	private MatPoolRequest holzRequest = null;
	
	/**
	 * SkillType ben�tigt f�r Herstellung der Ware
	 */
	private SkillType neededSkillType = null;
	
	/**
	 * vor MatPool maximale Produktionsmenge
	 */
	private int maxProduction = 0;
	
	boolean isInSchmiede = false;
	
	// Konstruktor
	public Schmieden() {
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
		// FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Schmieden");
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// Optionen lesen und Pr�fen
		// Ware
		String warenName = OP.getOptionString("Ware");
		ItemType actItemType = this.gd_Script.rules.getItemType(warenName, false);
		if (actItemType!=null){
			this.itemType = actItemType;
		} else {
			this.addComment("!!! Schmieden: Ware nicht erkannt (" + warenName + ") !");
			outText.addOutLine("!!! Schmieden: Ware nicht erkannt (" + warenName + ") !)" + this.unitDesc(), true);
			this.scriptUnit.doNotConfirmOrders();
		}
		
		// Prios
		int prio = OP.getOptionInt("Prio",0);
		if (prio>0) {
			this.eisenPrio = prio;
			this.holzPrio = prio;
		}
		
		prio=OP.getOptionInt("holzPrio",0);
		if (prio>0){
			this.holzPrio=prio;
		}
		prio=OP.getOptionInt("eisenPrio",0);
		if (prio>0){
			this.eisenPrio=prio;
		}
		
		// Vorratsrunden
		int setVorrat = OP.getOptionInt("vorratsrunden",-1);
		if (setVorrat>-1){
			this.vorratsRunden = setVorrat;
		}
		
		// minAuslastung
		int setMinAuslastung = OP.getOptionInt("minAuslastung", -1);
		if (setMinAuslastung>-1){
			if (setMinAuslastung<=100){
				this.minAuslastung = setMinAuslastung;
			} else {
				// Fehler
				this.addComment("!!! Schmieden: minAuslastung fehlerhaft!");
				outText.addOutLine("!!! Schmieden: minAuslastung fehlerhaft!" + this.unitDesc(), true);
				this.scriptUnit.doNotConfirmOrders();
			}
		}
		
		// Skilltype ermitteln
		if (this.itemType!=null){
			this.neededSkillType = this.itemType.getMakeSkill().getSkillType();
		}
		int prodPoints = 0;
		Skill neededSkill=null;
		int actSkillLevel = 0;
		if (this.neededSkillType!=null){
			neededSkill = this.scriptUnit.getUnit().getModifiedSkill(this.neededSkillType);
			if (neededSkill!=null){
				// actSkillLevel = neededSkill.getModifiedLevel(this.scriptUnit.getUnit(), true); 
				actSkillLevel = FFToolsUnits.getModifiedSkillLevel(neededSkill,this.scriptUnit.getUnit(), true);
				prodPoints = actSkillLevel * this.scriptUnit.getUnit().getModifiedPersons();
			}
		}
		
		if (prodPoints==0){
			this.addComment("Keine Produktion m�glich - kein Talent.");
			this.scriptUnit.doNotConfirmOrders();
			return;
		}
		
		// Schaffenstrunk oder RdF verdoppeln prodPoints 
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
			prodPoints *= 2;
			this.addComment("Schmieden: Einheit nutzt Schaffenstrunk. Produktion verdoppelt.");
		} 
		
		
		// Maximale Anzahl an Ware ermitteln
		int maxMachenWare = 0;
		int warenLevel = 0;
		if (this.itemType!=null){
			warenLevel = this.itemType.getMakeSkill().getLevel();
		}
		if (warenLevel>0){
			maxMachenWare = (int)Math.floor((double)prodPoints/(double)warenLevel);
		} else {
			this.addComment("Keine Produktion m�glich - kann ben�tigten Level nicht finden.");
			this.scriptUnit.doNotConfirmOrders();
			return;
		}
	

		// RdF
		ItemType rdfType=this.gd_Script.rules.getItemType("Ring der flinken Finger",false);
		if (rdfType!=null){
			Item rdfItem = this.scriptUnit.getModifiedItem(rdfType);
			if (rdfItem!=null && rdfItem.getAmount()>0){
				
				// RDF vorhanden...
				// produktion pro mann ausrechnen
				int prodProMann = (int)Math.floor((double)maxMachenWare/(double)this.scriptUnit.getUnit().getModifiedPersons());
				int oldMaxMachenWare = maxMachenWare;
				for (int i = 1;i<=rdfItem.getAmount();i++){
					if (i<=this.scriptUnit.getUnit().getModifiedPersons()){
						maxMachenWare -= prodProMann;
						maxMachenWare += (prodProMann * 10);
					} else {
						// �berz�hliger ring
						this.addComment("Schmieden: zu viele RdF!",false);
					}
				}
				this.addComment("Schmieden: " + rdfItem.getAmount() + " RdF. Prod von " + oldMaxMachenWare + " auf " + maxMachenWare + " erh�ht.");
			} 
		} else {
			this.addComment("Schmieden: RdF ist noch v�llig unbekannt.");
		}
		
		
		
		if (maxMachenWare<=0){
			this.addComment("Schmieden: Keine Produktion m�glich - ungen�gendes Talent.");
			this.scriptUnit.doNotConfirmOrders();
			return;
		} else {
			this.addComment("Schmieden: Maximal m�gliche Anzahl:" + maxMachenWare + " " + this.itemType.getName());
		}
		
		this.maxProduction = maxMachenWare;
		
		// die prios Festlegen
		Building actB = this.scriptUnit.getUnit().getModifiedBuilding();
		if (actB!=null){
			if (actB.getBuildingType().getName().equalsIgnoreCase("Schmiede")){
				this.isInSchmiede=true;
				this.addComment("Schmieden: Einheit erh�lt Schmiedebonus. (Prio +" + this.schmiedeBonus + ")");
			}
		}
		
		int actEisenPrio = this.eisenPrio + actSkillLevel;
		if (this.isInSchmiede){
			actEisenPrio += this.schmiedeBonus;
		}
		int actHolzPrio = this.holzPrio + actSkillLevel;
		if (this.isInSchmiede){
			actHolzPrio += this.schmiedeBonus;
		}
		
		// ben�tigte Materialien erfassen
		for (Iterator<Item> iter = this.itemType.getResources();iter.hasNext();){
			Item actItem = (Item)iter.next();
			boolean isKnownItem = false;
			int anzahl = actItem.getAmount() * maxMachenWare;
			// f�r diese Runde
			if (actItem.getItemType().getName().equalsIgnoreCase("Eisen") || actItem.getItemType().getName().equalsIgnoreCase("Laen")){
				// wenn in Schmiede, Anzahl halbieren (Schmiedebonus)
				if (this.isInSchmiede){
					anzahl = (int)Math.ceil((double)anzahl/(double)2);
				}
				this.eisenRequest = new MatPoolRequest(this,anzahl,actItem.getItemType().getName(),actEisenPrio,"Schmieden diese Runde");
				this.addMatPoolRequest(this.eisenRequest);
				isKnownItem=true;
			}
			if (actItem.getItemType().getName().equalsIgnoreCase("Holz") || actItem.getItemType().getName().equalsIgnoreCase("Mallorn")){
				this.holzRequest = new MatPoolRequest(this,anzahl,actItem.getItemType().getName(),actHolzPrio,"Schmieden diese Runde");
				this.addMatPoolRequest(this.holzRequest);
				isKnownItem=true;
			}
			if (!isKnownItem){
				this.addComment("Schmieden: unbekannte Zutat: " +  actItem.getItemType().getName());
				this.scriptUnit.doNotConfirmOrders();
			}
		}
		
		// falls Vorrat gew�nscht, dann obige beiden Prio + 
		// Kommentar anpassen und neue MPR anlegen
		
		if (this.vorratsRunden>0){
			for (int i = 1;i<=this.vorratsRunden;i++){
				if (this.eisenRequest!=null){
					// Eisen
					this.setPrioParameter(this.eisenRequest.getPrio(), -0.5, 0,1);
					MatPoolRequest myMPR = new MatPoolRequest(this.eisenRequest);
					myMPR.setPrio(this.getPrio(i));
					myMPR.setKommentar("Schmiederessource in " + i);
					this.addMatPoolRequest(myMPR);
				}
				if (this.holzRequest!=null){
					// Holz
					this.setPrioParameter(this.holzRequest.getPrio(), -0.5, 0,1);
					MatPoolRequest myMPR = new MatPoolRequest(this.holzRequest);
					myMPR.setPrio(this.getPrio(i));
					myMPR.setKommentar("Schmiederessource in " + i);
					this.addMatPoolRequest(myMPR);
				}
			}
		}
		
		this.scriptUnit.findScriptClass("RequestInfo");
		
	}	
	
	/**
	 * Zweiter Lauf nach dem MatPool
	 */
	private void nachMatPool(){
		// f�r diese Runde Erf�llung der entscheidenden Requests 
		// ermitteln
		// actAnzahl berechnen (was ist tats�chlich m�glich)
		// Auslastung ermitteln und pr�fen
		// entweder machen oder Lernen
		
		if (this.itemType==null){
			return;
		}
		
		if (this.maxProduction==0){
			return;
		}
		
//		 ben�tigte Materialien erfassen
		int actProduction = this.maxProduction;
		for (Iterator<Item> iter = this.itemType.getResources();iter.hasNext();){
			Item actItem = (Item)iter.next();
			int resAnzahl = 0;
			// f�r diese Runde
			if (actItem.getItemType().getName().equalsIgnoreCase("Eisen") || actItem.getItemType().getName().equalsIgnoreCase("Laen")){
				resAnzahl = this.eisenRequest.getBearbeitet();
				if (this.isInSchmiede){
					resAnzahl *= 2;
				}
				int detProd = (int)Math.floor((double)resAnzahl/(double)actItem.getAmount());
				if (detProd<actProduction){
					actProduction = detProd;
				}
			}
			if (actItem.getItemType().getName().equalsIgnoreCase("Holz") || actItem.getItemType().getName().equalsIgnoreCase("Mallorn")){
				resAnzahl = this.holzRequest.getBearbeitet();
				int detProd = (int)Math.floor((double)resAnzahl/(double)actItem.getAmount());
				if (detProd<actProduction){
					actProduction = detProd;
				}
			}
		}
		
		// prozentsatz
		int Auslastung = (int)Math.ceil(((double)actProduction/(double)this.maxProduction)*100);
		this.addComment("Schmieden: " + actProduction + " von " + this.maxProduction + " m�glich (" + Auslastung + "%, min:" + this.minAuslastung + "%)");
		if (Auslastung>=this.minAuslastung){
			// machen
			this.addOrder("MACHEN " + actProduction + " " + this.itemType.getName(), true);
		} else {
			// lernen
			this.lerneTalent(this.itemType.getMakeSkill().getSkillType().getName(), true);
		}
	}
			
		
}
