package com.fftools.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import magellan.library.CoordinateID;
import magellan.library.Region;
import magellan.library.utils.Islands;

import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsOptionParser;




public class Islandinfo extends Script{
	
	
	private static final int Durchlauf = 30;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Islandinfo() {
		super.setRunAt(Durchlauf);
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
	
	public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf){
			this.scriptStart();
		}
	}
	
	/*
	 * liefert die IslandInfo als Kommentare
	 * je nach Parameter beim Aufruf
	 */
	private void scriptStart(){
		// regionsliste bauen
		// entweder die TradeRegions oder alle Regionen der Insel, je nach setting
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		
		ArrayList<Region> myRegions = new ArrayList<Region>();
		
		if (OP.getOptionBoolean("onlyScriptRegions", true)) {
			// nur die Regionen des TradeAreas
			TradeArea TA = this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTAinRange(this.region());
			if (TA == null){
				this.addComment("! Fehler bei IslandInfo: diese Region gehört zu keinem TradeArea");
				return;
			}
			List<TradeRegion> tRegions = TA.getTradeRegions();
			if (tRegions==null || tRegions.size()==0){
				this.addComment("! Fehler bei IslandInfo: das TradeArea ist leer");
				return;
			}
			for (TradeRegion TR:tRegions){
				myRegions.add(TR.getRegion());
			}
		} else {
			// alle Regionen der Insel
			// Map <CoordinateID,Region> allRegions=Regions.getAllNeighbours(this.scriptUnit.getScriptMain().gd_ScriptMain.regions(), this.scriptUnit.getUnit().getRegion().getID(), 20, FFToolsRegions.getOceanRegionTypes(this.scriptUnit.getScriptMain().gd_ScriptMain.rules));
			Map <CoordinateID,Region> allRegions = Islands.getIsland(this.scriptUnit.getScriptMain().gd_ScriptMain.rules,this.scriptUnit.getScriptMain().gd_ScriptMain.regions(),this.scriptUnit.getUnit().getRegion());
			for (Region R:allRegions.values()){
				myRegions.add(R);
			}
		}
		this.addComment("IslandInfo: Betrachtete Regionen: " + myRegions.size());
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
}
