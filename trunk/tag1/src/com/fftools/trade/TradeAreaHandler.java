package com.fftools.trade;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import magellan.library.GameData;
import magellan.library.ID;
import magellan.library.Region;
import magellan.library.rules.ItemType;
import magellan.library.rules.RegionType;
import magellan.library.utils.Regions;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.transport.TransportRequest;



/**
 * 
 * a class to handle TradeAreas
 * shortform TAH
 * @author Fiete
 *
 */

public class TradeAreaHandler implements OverlordInfo{
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	
	/**
	 * List of TradeAreas
	 */
	private ArrayList<TradeArea> tradeAreas = null;
	
	/**
	 * enthält alle Regionen und passene tradeRegionen
	 * wird benutzt, um die Information abzulegen, ob eine
	 * Region automatisch einem TA zugeordnet werden kann
	 * oder manuell zugeordnet wurde (TradeArea = Name)
	 */
	private Hashtable<Region,TradeRegion> tradeRegions = null;
	
	/**
	 * enthält die Scriptunits, die als Trader erkannt worden sind
	 * und die entsprechend angelegten <Trader>
	 */
	private Hashtable<ScriptUnit,Trader> traders = null;
	
	

	
	
	/**
	 * the Game Data...needed for getting the rules for pathfinder
	 */
	private GameData data = null;
	
	/**
	 * keep reference to *all*
	 */
	private ScriptMain scriptMain = null;
	
	/**
	 * constructs a new TAH
	 * @param GameData  the game data
	 */
	public TradeAreaHandler(ScriptMain _scriptMain) {
		this.scriptMain = _scriptMain;
		this.data = _scriptMain.gd_ScriptMain;
	}
	
	
	/**
	 * gives the TradeArea for the given Trader
	 * @param t
	 * @param allowNew
	 * @return
	 */
	public TradeArea getTradeArea(Trader t,boolean allowNew){
		TradeRegion tR = this.getTradeRegion(t.getScriptUnit().getUnit().getRegion());
		
		if (t.isAreaOriginSetter()){
			tR.setSetAsTradeAreaOrigin(true);
			tR.setTradeAreaName(t.getSetAreaName());
		}
		return this.getTradeArea(tR, allowNew);
	}
	
	
	
	/**
	 * Gets the TradeArea for the given TradeRegion
	 * if the region is not in one TA, a new may
	 * be set up, if allowNew = true
	 * 
	 * @param r
	 * @return
	 */
	public TradeArea getTradeArea(TradeRegion tradeRegion,boolean allowNew){
		Region r = tradeRegion.getRegion();
		TradeArea tA = null;
		// no TAs present..create List and add the first TA
		if (this.tradeAreas==null){
			this.tradeAreas = new ArrayList<TradeArea>();
			tA = new TradeArea(tradeRegion,this.scriptMain.getOverlord());
			
			if (tradeRegion.isSetAsTradeAreaOrigin()){
				tA.setName(tradeRegion.getTradeAreaName());
			}
			this.addTradeArea(tA);
			return tA;
		}
		
		// if we have tradeAreas designed and a units has a 
		// option TradeArea activated...another procedure is used..
		if (tradeRegion.isSetAsTradeAreaOrigin()){
			// first...is there already such an TA?
			tA = this.getTradeAreaByName(tradeRegion.getTradeAreaName());
			if (tA!=null){
				tA.addRegion(tradeRegion);
				return tA;
			}
			// ok...nothing to find..create new one...
			// if we may...
			if (!allowNew){
				return null;
			}
			// lets do it
			tA = new TradeArea(tradeRegion,this.scriptMain.getOverlord());
			this.addTradeArea(tA);
			tA.setName(tradeRegion.getTradeAreaName());
			
			// with this addition other regions may be now
			// in another TA...
			// we have to check all regions again...
			// will be done after in calling proc
			return tA;
		}
		
		// we have to go through our TAs to find the right one
		// here we check only, if the region is already present
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			tA = (TradeArea)iter.next();
			if (tA.contains(tradeRegion)) {
				return tA;
			}
		}
		
		// check, if we have in TA in Range
		tA = this.getTAinRange(r);
		if (tA!=null){
			// yes...lets add the region 
			tA.addRegion(tradeRegion);
			// and return the tA
			return tA;
		}
		
		
		// nothing found, if I cannot make one...i must give up
		if (!allowNew){
			return null;
		}
		
		// still nothing found....making a new one
		tA = new TradeArea(tradeRegion,this.scriptMain.getOverlord());
		this.addTradeArea(tA);
		return tA;
	}
	
	/**
	 * checks the known TAs (list must be non null) if there
	 * is a landpath betwenn r and originRegion
	 * if so..the region is added and the TA returned
	 * 
	 * FF made public and checked non null tradeareas
	 * 
	 * @param r Region to search a TradeArea for
	 * @return the TradeArea if found, or null
	 */
	public TradeArea getTAinRange(Region r){
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(this.data.rules);
		Region actOriginRegion = null;
		
		if (this.tradeAreas==null){
			return null;
		}
		
		Hashtable<TradeArea,Integer> matches = new Hashtable<TradeArea, Integer>(1);
		for (Iterator<TradeArea> iter=this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = ((TradeArea)iter.next());
			actOriginRegion = tA.getOriginRegion();
			
			// einschub: in origin
			if (actOriginRegion.equals(r)){
				return tA;
			}
			
			if (TradeUtils.onSameIsland(r, actOriginRegion, this.data)) {
				String path = Regions.getDirections(this.data.regions(), actOriginRegion.getID() , r.getID(), excludeMap);
				if (path!=null && path.length()>0) {
					// weg gefunden
					// länge bestimmen = Anzahl der richtungen
					String[] zeichen = path.split(" ");
					int i = zeichen.length;
					matches.put(tA, Integer.valueOf(i));
				}
			}
		}
		
		// if matches is empty -> nothing was found...returning null		
		if (matches.isEmpty()){
			return null;
		}
		
		// ok..so we found something, at least one
		// i don´t want to sort the matches...using the old way
		TradeArea maxEntf = null;
		int actMaxEntf = Integer.MAX_VALUE;
		for (Iterator<TradeArea> iter = matches.keySet().iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			Integer actEntf = matches.get(tA);
			if (actEntf.intValue()<actMaxEntf){
				// ok...we have a new good tA
				maxEntf = tA;
				actMaxEntf = actEntf.intValue();
			}
		}
		
		// in maxEntf now the resulting TA
		return maxEntf;
	}
	
	public void informUs(){
		// if (outText.getTxtOut()==null) {return;}
		
		outText.setFile("TradeAreas");
		
		outText.addOutLine("******TradeAreaHandlerInfo******");
		if (this.tradeAreas!=null){
			outText.addOutLine("I have " + this.tradeAreas.size() + " Trade Areas (TAs) registered");
		} else {
			outText.addOutLine("I have no Trade Areas registered");
			return;
		}
		outText.addOutLine("calling now the TradeAreas for their info...");
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			tA.informUs();
		}
		outText.setFile("TradeAreas");
		outText.addOutLine("***EndOF TadeAreaHandlerInfo***");
		
		this.informInterTARelations();
		
		outText.setFileStandard();
		
	}
	
	/**
	 * informiert exclusiv über die Requests (Wer verlangt was mit welcher Prio)
	 *
	 */
	
	public void informUsTransportRequests(){
		// if (outText.getTxtOut()==null) {return;}
		outText.setFile("TAH - TransportRequests");
		
		outText.addOutLine("******TradeArea - Transport Requests******");
		if (this.tradeAreas!=null){
			outText.addOutLine("I have " + this.tradeAreas.size() + " Trade Areas (TAs) registered");
		} else {
			outText.addOutLine("I have no Trade Areas registered");
			return;
		}
		outText.addOutLine("calling now the TradeAreas for their info...");
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			tA.informUsTradeTransportRequests(this.scriptMain.getOverlord());
		}
		outText.addOutLine("***EndOF TadeAreaTransportRequestInfo***");
		
		outText.setFileStandard();
		
	}
	
	/**
	 * liefert alle TransportRequests der TradeAreas
	 * @return
	 */
	public ArrayList<TransportRequest> getTransportRequests(){
		ArrayList<TransportRequest> erg = new ArrayList<TransportRequest>();
		if (this.tradeAreas==null){
			return erg;
		}
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			ArrayList<TransportRequest> actList = tA.getTransportRequests(this.scriptMain.getOverlord());
			if (actList==null) {
				outText.addOutLine("??? TradeArea " + tA.getName() + " hat keine Transportaufträge");
			} else {
				erg.addAll(actList);
			}
		}
		return erg;
	}
	
	
	
	
	/**
	 * beim pre-parsen der units auf einen Händler gestossen
	 * parst optionen
	 * fügt Region dem TA hinzu bzw legt ein TA neu an
	 * @param u
	 */
	public Trader addTrader(ScriptUnit u){
		
		if (this.traders==null){
			this.traders = new Hashtable<ScriptUnit,Trader>();
		}		
		Trader t = this.traders.get(u);
		if (t==null) {
			t = new Trader(u);
			this.traders.put(u,t);
		}
		
		TradeArea tA = this.getTradeArea(t, true);
		if (tA==null){
			outText.addOutLine("!!! TradeArea not created for unit (handel): " +  u.getUnit().toString(true));
			return null;
		}
		tA.addTrader(t);
		
		// wenn ein trader sein TA manuell setzt, müssen bekannte
		// TradeRegions untersucht werden, ob sie nicht nun einem
		// anderen TA zugeordnet werden müssen...
		if (t.isAreaOriginSetter()){
			this.recalcTradeAreas();
		}
		return t;
	}
	
	private TradeArea getTradeAreaByName(String name){
		if (this.tradeAreas==null){return null;}
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			if (tA.getName().equalsIgnoreCase(name)){
				return tA;
			}
		}
		return null;
	}
	
	private void addTradeArea(TradeArea _tA){
		if (this.tradeAreas==null){
			this.tradeAreas = new ArrayList<TradeArea>();
		}
		this.tradeAreas.add(_tA);
	}
	
	public TradeRegion getTradeRegion(Region r){
		if (this.tradeRegions==null){
			this.tradeRegions = new Hashtable<Region,TradeRegion>();
		}
		if (this.tradeRegions.containsKey(r)){
			return this.tradeRegions.get(r);
		} else {
			TradeRegion nTR = new TradeRegion(r);
			this.tradeRegions.put(r, nTR);
			return nTR;
		}
	}
	
	
	/**
	 * löscht alle Regionen aus den TAs bis auf die Regionen, die 
	 * manuell TAs gesetzt haben
	 * fügt dann neu alle anderen Regionen wieder hinzu
	 */
	public void recalcTradeAreas_old(){
		if (this.tradeAreas==null){return;}
		// aus allen TAs die automatisch zugeordneten löschen...
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			tA.removeNonManualOrigins();
		}
		// komplett leere TAs löschen?
		ArrayList<TradeArea> newList = null;
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			if (tA.getTradeRegions()!=null){
				if (newList == null) {
					newList = new ArrayList<TradeArea>();
				}
				newList.add(tA);
			}
		}
		this.tradeAreas = newList;
		
		// alle automatischen neu zuordnen
		for (Iterator<TradeRegion> iter = this.tradeRegions.values().iterator();iter.hasNext();){
			TradeRegion tR = (TradeRegion)iter.next();
			if (!tR.isSetAsTradeAreaOrigin()){
				// automatisch...
				TradeArea tA = this.getTradeArea(tR, true);
				if (tA==null){
					outText.addOutLine("!!! TradeArea not created for TradeRegion (recalc): " + tR.getRegion().toString());
					return;
				}
			}
		}
	}
	
	/**
	 * löscht alle TAs und baut aus tradeRegions wieder auf
	 */
	public void recalcTradeAreas(){
		if (this.tradeRegions==null){return;}
		if (this.tradeAreas!=null){
			this.tradeAreas.clear();
		}
		//	alle mauellen zuordnen
		for (Iterator<TradeRegion> iter = this.tradeRegions.values().iterator();iter.hasNext();){
			TradeRegion tR = (TradeRegion)iter.next();
			if (tR.isSetAsTradeAreaOrigin()){
				TradeArea tA = this.getTradeArea(tR, true);
				if (tA==null){
					outText.addOutLine("!!! TradeArea not created for TradeRegion (recalc): " + tR.getRegion().toString());
					return;
				}
			}
		}
		// alle automatischen zuordnen
		for (Iterator<TradeRegion> iter = this.tradeRegions.values().iterator();iter.hasNext();){
			TradeRegion tR = (TradeRegion)iter.next();
			if (!tR.isSetAsTradeAreaOrigin()){
				TradeArea tA = this.getTradeArea(tR, true);
				if (tA==null){
					outText.addOutLine("!!! TradeArea not created for TradeRegion (recalc): " + tR.getRegion().toString());
					return;
				}
			}
		}
		
		// traders wieder zuordnen
		if (this.traders!=null) {
			for (Iterator<ScriptUnit> iter = this.traders.keySet().iterator();iter.hasNext();){
				ScriptUnit u = (ScriptUnit)iter.next();
				Region r = u.getUnit().getRegion();
				TradeArea TA = this.getTAinRange(r);
				if (TA!=null){
						Trader T = this.traders.get(u);
						TA.addTrader(T);
				} else {
					outText.addOutLine("!!!Fehler bei recalcTradeAreas: für " + u.unitDesc() + " kein TA gefunden.");
				}		
			}
		}
		
		// int i = 0;
	}
	
	/**
	 * wird nicht bei einem bestimmten durchlauf aufgerufen
	 */
	public int[] runAt(){
		return null;
	}


	/**
	 * @return the tradeAreas
	 */
	public ArrayList<TradeArea> getTradeAreas() {
		return tradeAreas;
	}
	
	
	private void informInterTARelations(){
		outText.setFile("Inter_TA_Relation");
		
		if (this.tradeAreas==null || this.tradeAreas.size()==0){
			outText.addOutLine("no trade areas known");
			return;
		}
		
		// Liste aller Luxusgüter bekommen
		ArrayList<ItemType> lux = new ArrayList<ItemType>();
		lux.add(data.rules.getItemType("Weihrauch", false));
		lux.add(data.rules.getItemType("Öl", false));
		lux.add(data.rules.getItemType("Myrrhe", false));
		lux.add(data.rules.getItemType("Juwel", false));
		lux.add(data.rules.getItemType("Gewürz", false));
		lux.add(data.rules.getItemType("Balsam", false));
		lux.add(data.rules.getItemType("Seide", false));
		
		
		outText.addOutChars("Lux|", 10);
		for (TradeArea tA : this.tradeAreas){
			outText.addOutChars(tA.getName()+"|", 16);
		}
		outText.addNewLine();
		
		outText.addOutChars("Balance|", 10);
		for (TradeArea tA : this.tradeAreas){
			outText.addOutChars("normal|", 8);
			outText.addOutChars("max|", 8);
		}
		outText.addNewLine();
		
		for (ItemType itemType : lux){
			outText.addOutChars(itemType.getName()+"|",10);
			for (TradeArea tA : this.tradeAreas){
				int normalBuy = tA.getAreaBuyAmount(itemType);
				int maxBuy = tA.getAreaBuyMaxAmount(itemType);
				int normalSell = tA.getAreaSellAmount(itemType);
				outText.addOutChars((normalBuy-normalSell)+"|", 8);
				outText.addOutChars((maxBuy-normalSell)+"|", 8);
				// outText.addOutChars("A", 16);
			}
			outText.addNewLine();
		}
		
		
	}
	
}
