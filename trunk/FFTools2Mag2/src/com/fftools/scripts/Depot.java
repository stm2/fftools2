package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import magellan.library.Building;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.rules.BuildingType;
import magellan.library.rules.ItemType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;

public class Depot extends TransportScript{
	
	private static final int Duchlauf_main=2;
	private static final int Durchlauf_nachLetztemMP = 152; //nur Info
	
	private int[] runsAt = {Duchlauf_main,Durchlauf_nachLetztemMP};
	
	public static int default_request_prio = 1;
	private String default_request_kommentar = "Depot";
	
	private int default_runden_silbervorrat = 3;
	private int default_silbervorrat_maxPrio = 100;
	private int default_silbervorrat_minPrio = 10;
	
	private int used_silbervorrat_maxPrio = 0;
	
	private String default_silbervorrat_kommentar = "SilberDepot";

	private LinkedList<MatPoolRequest> silberDepotMPRs = null;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Depot() {
		super.setRunAt(this.runsAt);
	}
	
	
	public void runScript(int scriptDurchlauf){
		
		switch (scriptDurchlauf){
		
			case Duchlauf_main:this.runDepot(scriptDurchlauf);break;

			case Durchlauf_nachLetztemMP:this.runNachMP(scriptDurchlauf);break;
			
		
		}
		
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
	
	public void runDepot(int scriptDurchlauf){
		
		
		// debug me
		if(this.scriptUnit.getUnit().toString(false).equalsIgnoreCase("k5Lr")){
			int i = 1;
			i++;
		}
		
		// "registrierung" beim TransportManager, damit der später nicht
		// seine Depots = Lieferanten zusammensuchen muss
		this.getOverlord().getTransportManager().addDepot(this.scriptUnit);
		
		// Falls KEIN Handler in der Region ist, diese Region trotzdem
		// beim zuständigen TAH anmelden
		TradeRegion tR = this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTradeRegion(this.scriptUnit.getUnit().getRegion());
		// und damit sie auch beim TA landet, den aktuellen TA anfordern
		// TradeArea tA = this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTradeArea(tR, true);
		this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTradeArea(tR, true);
		// Depotunit auch beim MatPool setzen
		this.getOverlord().getMatPoolManager().getRegionsMatPool(this.scriptUnit).setDepotUnit(this.scriptUnit);
		
		// also selber alles zusammensuchen....
		// Region getItem funzt wohl...also nur ne Liste der ItemTypes erstellen in der Region
		ArrayList<ItemType> itemTypes = new ArrayList<ItemType>();
		Region r = this.scriptUnit.getUnit().getRegion();
		for (Iterator<Unit> iter = r.units().iterator();iter.hasNext();){
			Unit u = (Unit) iter.next();
			for (Iterator<Item> iter2 = u.getItems().iterator();iter2.hasNext();){
				Item item = (Item)iter2.next();
				ItemType itemType = item.getItemType();
				if (!itemTypes.contains(itemType)){
					itemTypes.add(itemType);
				}
			}
		}
		for (Iterator<ItemType> iter = itemTypes.iterator();iter.hasNext();) {
			ItemType itemType = (ItemType)iter.next();
			// Request doch mit "alles"
			this.addMatPoolRequest(new MatPoolRequest(this,Integer.MAX_VALUE,itemType.getName(),Depot.default_request_prio,this.default_request_kommentar));
		}
		
		// fertig (?)  dass depot ist soooo einfach? da muss noch was kommen...
		
		//ja: der Lernbefehl...standardmässig tarnung
		//neu: optional:
		// noch neuer: Lernpool wird verwendet
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Depot");
		if (OP.getOptionBoolean("Lernen", true)){
			super.lerneTalent("Tarnung", true);
		}
		
		// Silbervorrat für die Region...für Region/Insel/Report gesetzt ?
		if (reportSettings.getOptionBoolean("DepotSilber", this.region())){
			int anzahl_silber_runden = this.default_runden_silbervorrat;
			this.addComment("DEBUG: DepotSilber ist aktiviert");
			// reportweite settings
			int reportRunden = reportSettings.getOptionInt("DepotSilberRunden", this.region());
			if (reportRunden>0){
				anzahl_silber_runden = reportRunden;
				this.addComment("DEBUG: Reportsettings -> depotsilberrunden = " + reportRunden);
			} else {
				this.addComment("DEBUG: Reportsettings -> keine Info");
			}
			
			// aus den Optionen
			int optionRunden = OP.getOptionInt("DepotSilberRunden", -1);
			if (optionRunden>0){
				anzahl_silber_runden = optionRunden;
				this.addComment("DEBUG: Optionen -> depotsilberrunden = " + optionRunden);
			} else {
				this.addComment("DEBUG: Optionen -> keine Info");
			}
			// maxSilberrequestPrio...gleiches Spiel
			int silberRequestPrio = this.default_silbervorrat_maxPrio;
			int reportSilberDepotPrio = reportSettings.getOptionInt("DepotSilberPrio", this.region());
			if (reportSilberDepotPrio>0){
				silberRequestPrio = reportSilberDepotPrio;
			}
			int optionenPrio = OP.getOptionInt("DepotSilberPrio", -1);
			if (optionenPrio>0){
				silberRequestPrio = optionenPrio;
			}
			
			if (anzahl_silber_runden>0){
				super.setPrioParameter(silberRequestPrio-this.default_silbervorrat_minPrio,-0.5,0,this.default_silbervorrat_minPrio);
				this.used_silbervorrat_maxPrio = silberRequestPrio;
				int kostenProRunde = this.getKostenProRunde();
			
				this.silberDepotMPRs = new LinkedList<MatPoolRequest>();
				
				int prioTM = reportSettings.getOptionInt("DepotSilberPrioTM", this.region());
				
				// los gehts
				for (int i = 1;i<=anzahl_silber_runden;i++){
					// regionsinterne Prio
					super.setPrioParameter(silberRequestPrio-this.default_silbervorrat_minPrio,-0.5,0,this.default_silbervorrat_minPrio);
					int actPrio = super.getPrio(i-1);
					// TM Prio
					int actPrioTM=actPrio;
					if (prioTM>0){
						super.setPrioParameter(prioTM-this.default_silbervorrat_minPrio,-0.5,0,this.default_silbervorrat_minPrio);
						actPrioTM = super.getPrio(i-1);
					}
					
					MatPoolRequest actMPR = new MatPoolRequest(this,kostenProRunde,"Silber",actPrio,this.default_silbervorrat_kommentar);
					if (prioTM>0){
						actMPR.setPrioTM(actPrioTM);
					}
					this.addMatPoolRequest(actMPR);
					this.silberDepotMPRs.add(actMPR);
				}
			}
			
		}
		
		
		// requestInfo
		this.scriptUnit.findScriptClass("RequestInfo");
		
	}
	
	private int getKostenProRunde(){
		int erg=0;
		
		
		// debug
		if (this.scriptUnit.getUnitNumber().equalsIgnoreCase("e4ft")){
			int i= 0;
			i++;
		}
		
		// ScriptPersonen * 10
		erg += FFToolsRegions.countScriptPersons(this.getOverlord().getScriptMain().getScriptUnits(), this.region()) * 10;
		
		// Gebäudekosten
		Region r = this.region();
		if (r.buildings()!=null && r.buildings().size()>0){
			for (Iterator<Building> iter = r.buildings().iterator();iter.hasNext();){
				Building b = (Building) iter.next();
				BuildingType bT = b.getBuildingType();
				if (bT.getMaintenanceItems()!=null){
					for (Iterator<Item> iter2 = bT.getMaintenanceItems().iterator();iter2.hasNext();){
						Item item = (Item)iter2.next();
						if (item.getName().equalsIgnoreCase("Silber")){
							erg+=item.getAmount();
						}
					}
				}
			}
		}
		
		// SilberSockel
		int sockel = reportSettings.getOptionInt("DepotSilberSockel", this.region());
		if (sockel>0){
			erg+=sockel;
		}
		this.addComment("DEBUG: Reportsettings -> DepotSilberSockel = " + sockel);
		return erg;
	}
	
	/**
	 * rein informativ für die unit
	 * @param scriptDurchlauf
	 */
	public void runNachMP(int scriptDurchlauf){
		if (this.silberDepotMPRs==null || this.silberDepotMPRs.size()==0){
			return;
		}
		for (Iterator<MatPoolRequest> iter = this.silberDepotMPRs.iterator();iter.hasNext();){
			MatPoolRequest MPR = (MatPoolRequest)iter.next();
			/**
			String erg = "DepotSilber gefordert:";
			erg += MPR.getOriginalGefordert() + "(Prio " + MPR.getPrio() + ")";
			erg += ",bearbeitet:" + MPR.getBearbeitet();
			this.addComment(erg);
			**/
			// Problem
			if (MPR.getPrio()==this.used_silbervorrat_maxPrio && MPR.getBearbeitet()<MPR.getOriginalGefordert()){
				// max prio und nicht erfüllt ?!
				this.scriptUnit.doNotConfirmOrders();
				this.addComment("!!! DepotSilber ungenügend !!! (" + MPR.getBearbeitet() + "/" + MPR.getOriginalGefordert() + ")");
				outText.addOutLine("!!! DepotSilber ungenügend (" + MPR.getBearbeitet() + "/" + MPR.getOriginalGefordert() + "): " + this.unitDesc(),true);
			}
			
		}
	}
	
	
}
