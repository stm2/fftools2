package com.fftools;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JTextArea;

import magellan.client.Client;
import magellan.library.Faction;
import magellan.library.GameData;
import magellan.library.Order;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.event.GameDataEvent;
import magellan.library.gamebinding.EresseaRelationFactory;

import com.fftools.overlord.Overlord;
import com.fftools.utils.FFToolsTags;


/**
 * 
 * @author Fiete
 *
 * Oberklasse der Scriptverarbeitung...verwaltet ScriptUnit, durchlaeufe etc
 * 
 */


public class ScriptMain {
	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private Hashtable<Unit,ScriptUnit> scriptUnits = null;
	
	private ArrayList<Region> scriptRegions = null;
	
	public Client client = null;
	public GameData gd_ScriptMain = null;
	private Properties settings=null;
	
	private final static String FFTools2_ignoreFactions = "FFTools2.ignoreFactions";
	
	
	/**
	 * zentrale verwaltung aller objecte, die in den durchlaufen
	 * aufgerufen werden können: scripte und manager (handler)
	 */
	private Overlord overlord = null;

	/**
	 * Table of factions with changed Trustlevels und old Trustlevel
	 */
	private Hashtable<Faction,Integer> changedTrustlevels = null;
	
	
	public ScriptMain(){
		// nuescht zu tun.... ?
		// outline KANN noch nicht funktionieren...
		
		outText.addOutLine("new Script Main initialized");
	}
	
	public ScriptMain(GameData _gd){
		this.gd_ScriptMain = _gd;
		outText.addOutLine("new Script Main initialized with a GameData object..parsing units");
		extractScriptUnits();
	}
	
	public ScriptMain(Client _client,JTextArea _txtOutput){
		super();
		outText.setTxtOut(_txtOutput);
		this.client = _client;
		this.gd_ScriptMain = client.getData();
		this.setSettings(_client.getProperties());
		outText.addOutLine("new Script Main initialized (client)");
	}
	
	/**
	 * Eine als scriptunit erkannte unit wird der Map scriptUnits hinzugefuegt
	 * falls aus irgendeinem Grund eine unit 2x registriert werden soll, verhindert
	 * die Map dass, weil unit als key dient und nur 1x vorkommen kann
	 * 
	 * @param u Die zu addende Unit
	 * 
	 */
	
	public void addUnit(Unit u){
		// ist die Map schon angelegt worden?
		if (scriptUnits == null){
			scriptUnits = new Hashtable<Unit, ScriptUnit>();
		}
		
		// neue ScriptUnit anlegen
		ScriptUnit new_su = new ScriptUnit(u,this);
		// falls wir einen client context haben...
		if (this.client!=null) {new_su.setClient(this.client);}
		// hinzufuegen
		scriptUnits.put(u,new_su);
		// regionen hinzufügen
		Region r = u.getRegion();
		if (this.scriptRegions==null){
			this.scriptRegions = new ArrayList<Region>();
		}
		if (!this.scriptRegions.contains(r)){
			this.scriptRegions.add(r);
		}
		
		// outText.addOutLine("..zu ScriptMain hinzugefuegt: " + u.toString(true));
	}
	
	public int getNumberOfScriptUnits(){
		if (scriptUnits==null){return 0;} else {return scriptUnits.size();}
	}
	
	/**
	 * HERE WE GO
	 * hier wird festgeleget, was in welcher Reihenfolge passiert
	 * Idee: durchlauf 1+2, dann MatPool und andere, dann durchlauf 3...
	 */
	
	public void runScripts(){
		long startT = System.currentTimeMillis();
		if (scriptUnits==null){
			outText.addOutLine("runScripts nicht moeglich: keine scriptUnits");
			return;
		}
		if (this.gd_ScriptMain==null){
			outText.addOutLine("runScripts nicht moeglich: kein GameData Object");
			return;
		}
		reportSettings.setGameData(this.gd_ScriptMain);
		reportSettings.reset();
		reportSettings.setScriptMain(this);
		
		
		this.stopRelationUpdates();
		
		
		this.readReportSettings();
		
		// not the the console, but to the logfile...
		outText.setScreenOut(false);
		
		// gettimg some info from reportsettings...
		reportSettings.informUs();
		
		// gettimg some info from TRade Areas...
		this.getOverlord().getTradeAreaHandler().informUs();
		
		outText.setScreenOut(true);
		
		// verlagere die abarbeitung zum overlord
		this.getOverlord().run();

		// zeige info, was wan gelaufen ist/sein könnte ;)
		this.getOverlord().informUs();
		
		// TM info
		this.getOverlord().getTransportManager().informUs();
		// MP getInfo
		outText.setScreenOut(false);
		this.getOverlord().getMatPoolManager().informUs();
		// gettimg some info from TRade Areas...
		this.getOverlord().getTradeAreaHandler().informUs();
		outText.setScreenOut(true);
		
		
		outText.addOutLine("unit final confirm");
		// als Vorletztes den confirm-status der units setzen
		// und autoTags setzen
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			scrU.setFinalConfim();
			// autoTags
			scrU.autoTags();
		}
		
		outText.addOutLine("setting tags");
		// finally..tags FF 20070531
		FFToolsTags.AllOrders2Tags(this.gd_ScriptMain);
		
		// FF 20080804 : trustlevels
		this.resetFactionTrsutLevel();
		
		// und zum schluss refreshen
		// natuerlich nur, wenn wir nen client haben..
		outText.addOutLine("refreshing client");
		
		this.restartRelationUpdates();
		
		this.refreshClient();
		
		long endT = System.currentTimeMillis();
		
		if (this.client!=null && this.client.getSelectedRegions()!=null && this.client.getSelectedRegions().size()>0){
			outText.addOutLine("!Achtung. Nur selektierte Regionen wurden bearbeitet. Anzahl: " + this.client.getSelectedRegions().size());
		}
		
		outText.addOutLine("runScripts benötigte " + (endT-startT) + " ms.");

	}
	
	
	public void refreshClient(){
		if (this.client!=null){
			// durchläuft alle registrierten Scriptunits
			// diese checken selsbt, ob orders geändert wurden und wenn ja
			// (und auch diese einen client haben)
			// wird OrdersChangeEvent gefeuert
			/**
			for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
				ScriptUnit scrU = (ScriptUnit)iter.next();
				scrU.refreshClient();
			}
			*/
			outText.addOutLine("refreshing client regions");
			// neuer Anlauf...regionen refreshen
			/*
			for (Region r:this.scriptRegions){
				r.refreshUnitRelations(true);
			}
			*/
			outText.addOutLine("refreshing client regions...done");
			outText.addOutLine("refreshing GameData");
			this.client.getMagellanContext().getEventDispatcher().fire(new GameDataEvent(this, this.client.getData()));
			outText.addOutLine("refreshing GameData...done");
		}
	}
	
	
	/**
	 * durchläuft sämtliche scriptunits und checked, ob in den orders
	 * optionale konfigs vorliegen
	 * 
	 * wird aufgebohrt für alle Sachen, die VOR den eigentlichen scripts
	 * über "ALLES" laufen sollen.
	 */
	private void readReportSettings(){
		
		if (scriptUnits==null || scriptUnits.size()==0){
			System.out.println("aborting readReportSettings - no script Units");
			return;
		}
		
		
		// ok durch 2 durchläufe
		// weil für prepare Handel schon reportsettungs notwendig
		// Lauf 1
		// System.out.println("ReadReportSettings 1");
		outText.addOutLine("ReadReportSettings 1");
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			// auf reportsetting einträge prüfen
			if (!this.getOverlord().isDeleted(scrU)){
				scrU.readReportSettings();
				outText.addPoint();
			}
			
		}
		// Lauf 2
		// System.out.println("ReadReportSettings 2");
		outText.addOutLine("ReadReportSettings 2");
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			// auf Handel prüfen (TradeArea bauen)
			if (!this.getOverlord().isDeleted(scrU)){
				this.prepareHandel(scrU);
				// gleich mal eben die scriptlisten bauen
				scrU.builtScriptList();
				outText.addPoint();
			}
		}
		
		// aus den settings resultierende Paramter usw
		
		
		// TM anstossen
		// System.out.println("TM");
		outText.addOutLine("TM-presetup");
		this.getOverlord().getTransportManager().initReportSettings(reportSettings);
		
		
		// Jetzt alle scriptunits bei den matpools anmelden..
		// System.out.println("MP-Anmeldungen");
		outText.addOutLine("Units to Matpools");
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			if (!this.getOverlord().isDeleted(scrU)){
			// MatPool mp = this.overlord.getMatPoolManager().getRegionsMatPool(scrU);
				this.overlord.getMatPoolManager().getRegionsMatPool(scrU);
			}
		}
		// System.out.println("finished ReadReportSettings\n");
		outText.addOutLine("Removing deletet units");
		// Entfernte ScriptUnits tatsächlich entfernen
		ArrayList<ScriptUnit> removeIt = new ArrayList<ScriptUnit>();
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			if (this.getOverlord().isDeleted(scrU)){
				removeIt.add(scrU);
			}
		}
		for (ScriptUnit su:removeIt){
			this.scriptUnits.remove(su.getUnit());
		}
	}
	
	public void extractScriptUnits(){
		if (gd_ScriptMain == null) {
			return;
		}
		int numberOfRegions = gd_ScriptMain.getRegions().size();
		int numberOfUnits = gd_ScriptMain.getUnits().size();
		outText.addOutLine("Overall: found " + numberOfRegions + " Regions and " + numberOfUnits + " Units.");
		
		// ignore Factions ermittlen
		ArrayList<String> ignoreList=null;
		String s = null;
		if (getSettings()!=null){
			s = getSettings().getProperty(ScriptMain.FFTools2_ignoreFactions, null);
		}
		if (s!=null){
			String[] splitter = s.split(",");
			for (String ss : splitter){
				if (ignoreList==null){
					ignoreList = new ArrayList<String>();
				}
				ignoreList.add(ss);
				outText.addOutLine("Faction to be ignored (from ini file: " + ss);
			}
		}
		
		// Neu: ignore List aus befehle ergänzen
		// -> // private final static String FFTools2_ignoreFactions = "FFTools2.ignoreFactions";
		// durch die Regionen wandern..
		String ids = "// " + ScriptMain.FFTools2_ignoreFactions + " ";
		for (Region r: gd_ScriptMain.getRegions()){
			if (r.getUnits()!=null && r.getUnits().size()>0){
				for (Unit u:r.getUnits().values()){			
					if (u.getOrders2()!=null && u.getOrders2().size()>0){
						for (Order ord:u.getOrders2()){
							String so = ord.getText();
							if (so.startsWith(ids)){
								String factionToAdd =  so.substring(ids.length());
								if (factionToAdd.length()>1){
									if (ignoreList==null){
										ignoreList = new ArrayList<String>();
									}
									ignoreList.add(factionToAdd);
									outText.addOutLine("Faction to be ignored (from unitorder): " + factionToAdd + " (" + u.toString() + ")");
								}
							}
						}
					}
				}
			}
		}
		
		
		
		// durch die Regionen wandern..
		for (Region r:gd_ScriptMain.getRegions()){
			if (r.getUnits()!=null && r.getUnits().size()>0){
				for (Unit u:r.getUnits().values()){
					boolean ignore = false;
					if (ignoreList != null){	
						for (String ss : ignoreList){
							if (u.getFaction().getID().toString().equalsIgnoreCase(ss)){
								ignore=true;
								break;
							}
						}
					}
					
					
					if (!ignore && ScriptUnit.isScriptUnit(u)){
						this.addUnit(u);
						this.setFactionTrustlevel(u.getFaction());
					}
				}
			}
		}
		
		// Gleich die Orders einmal saven = clearen
		outText.addOutLine("removing unprotected orders");
		for (ScriptUnit su: this.scriptUnits.values()){
			su.saveOriginalScriptOrders();
		}
		
		// einmal updaten
		outText.addOutLine("refreshing regions after adding the scriptunits and removing unprotected orders");
		this.refreshScripterRegions();
		
		outText.addOutLine("Scripter enthaelt " + getNumberOfScriptUnits() + " units...starte scripter\n");
	}
	
	private void setFactionTrustlevel(Faction f){
		if (!f.isPrivileged()){
			if (this.changedTrustlevels==null){
				this.changedTrustlevels = new Hashtable<Faction, Integer>(0);
			}
			
			Integer knownTrustLevel = this.changedTrustlevels.get(f);
			if (knownTrustLevel==null){
				knownTrustLevel = new Integer(f.getTrustLevel());
				this.changedTrustlevels.put(f,knownTrustLevel);
			}
			f.setTrustLevel(100);
			f.setTrustLevelSetByUser(true);
		}
	}
	
	
	private void resetFactionTrsutLevel(){
		if (this.changedTrustlevels==null || this.changedTrustlevels.size()==0){
			return;
		}
		for (Iterator<Faction> i = this.changedTrustlevels.keySet().iterator();i.hasNext();){
			Faction f = (Faction) i.next();
			Integer trustLevel = this.changedTrustlevels.get(f);
			f.setTrustLevel(trustLevel.intValue());
			f.setTrustLevelSetByUser(true);
		}
	}
	
	/**
	 * checked, ob unit einen Händler enthält
	 * wenn ja wird TadeArea gehandelt...
	 * festsetzung // script handeln
	 * @param u
	 */
	private void prepareHandel(ScriptUnit u){
		
		// Einschub: jede scriptunit bei ihrem MaterialPool anmelden
		// MatPool mp = this.getOverlord().getMatPoolManager().getRegionsMatPool(u);
		
		boolean isHandel = false;
		for(Order o:u.getUnit().getOrders2()) {
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script handeln")){
				isHandel=true;
				break;
			}
		}
		if (!isHandel){
			// kein Händler
			return;
		} else {
			// alles weitere in TAH
			this.getOverlord().getTradeAreaHandler().addTrader(u);	
		}
	}
	

	
	


	
	/**
	 * wir müssen tatsächlich die relations refreshen...eventuell nur für console...
	 * 
	 */
	public void refreshScripterRegions(){
		if (this.scriptUnits==null){return;}
		if (this.scriptRegions==null) {return;}
		EresseaRelationFactory ERF = ((EresseaRelationFactory) gd_ScriptMain.getGameSpecificStuff().getRelationFactory());
		boolean updaterStopped = ERF.isUpdaterStopped();
		if (!updaterStopped){
			ERF.stopUpdating();
		}
		for (Iterator<Region> iter = this.scriptRegions.iterator();iter.hasNext();){
			Region r = (Region)iter.next();
			// r.refreshUnitRelations(true);
			// gd_ScriptMain.getGameSpecificStuff().getRelationFactory().createRelations(r);
			
			ERF.processRegionNow(r);
		}
		if (!updaterStopped){
			ERF.restartUpdating();
		}
	}

	
	public void stopRelationUpdates(){
		EresseaRelationFactory ERF = ((EresseaRelationFactory) gd_ScriptMain.getGameSpecificStuff().getRelationFactory());
		ERF.stopUpdating();
	}
			
	public void restartRelationUpdates(){
		EresseaRelationFactory ERF = ((EresseaRelationFactory) gd_ScriptMain.getGameSpecificStuff().getRelationFactory());
		ERF.restartUpdating();
	}

  public Overlord getOverlord(){
	  if (this.overlord==null){
		  this.overlord = new Overlord(this);
	  }
	  return this.overlord;
  }

	/**
	 * @return the scriptUnits
	 */
	public Hashtable<Unit, ScriptUnit> getScriptUnits() {
		return scriptUnits;
	}
	
	/**
	 * liefert zu einer einheitennummer die scriptunit, wenn vorhanden
	 * @param unitID
	 * @return
	 */
	public ScriptUnit getScriptUnit(String unitID){
		ScriptUnit erg = null;
		for (Iterator<ScriptUnit> iter=this.scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit actUnit = (ScriptUnit)iter.next();
			if (actUnit.getUnit().toString(false).equalsIgnoreCase(unitID)){
				return actUnit;
			}
		}
		return erg;
	}
	
	/**
	 * liefert zu einer Unit die ScriptUnit oder Null
	 * falls nicht vorhanden
	 * @param u
	 * @return
	 */
	public ScriptUnit getScriptUnit(Unit u){
		ScriptUnit erg = null;
		if (this.scriptUnits!=null){
			return this.scriptUnits.get(u);
		}
		return erg;
	}
	
	
/**
 * Liefert Hashtable der ScriptUnits einer Region oder Null
 * @param Region _region
 * @return
 */
	public Hashtable <Unit, ScriptUnit> getScriptUnits(Region _region){
		Hashtable <Unit, ScriptUnit> regionsScriptUnits =null;
		// alle ScriptUnits durchiterieren...und auf region checken
		for (Iterator<ScriptUnit> iter = this.scriptUnits.values().iterator(); iter.hasNext();){
			ScriptUnit kandidat = (ScriptUnit) iter.next();
			if (kandidat.getUnit().getRegion().equals(_region)){
				// Table erzeugen
				if (regionsScriptUnits == null){
					regionsScriptUnits = new Hashtable <Unit, ScriptUnit> ();
				}
				regionsScriptUnits.put(kandidat.getUnit(), kandidat);
			}
		}
		return regionsScriptUnits;
		
	}

/**
 * @return the settings
 */
public Properties getSettings() {
	return settings;
}

/**
 * @param settings the settings to set
 */
public void setSettings(Properties settings) {
	this.settings = settings;
}
	
	
}
