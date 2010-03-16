package com.fftools.utils;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import magellan.library.Border;
import magellan.library.Building;
import magellan.library.CoordinateID;
import magellan.library.GameData;
import magellan.library.ID;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Rules;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.rules.CastleType;
import magellan.library.rules.ItemType;
import magellan.library.rules.RegionType;
import magellan.library.rules.SkillType;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;
import magellan.library.utils.Umlaut;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;

/**
 * Regionenhandling analog zu com.eressea.utils.Regions
 * @author Fiete
 *
 */
public class FFToolsRegions {
	public static final OutTextClass outText = OutTextClass.getInstance();
	
	private static long cntCacheHits = 0;
	private static long cntCacheRequests = 0;
	
	
	/**
	 * a cache for the calls to getPathDistLand with no(!) nextstepInfo!
	 */
	private static HashMap<PathDistLandInfo,Integer> pathDistCache = null;
	
	/**
	 * Gibt es eine Koordinate in den übergebenen Regionen?
	 * @param regions die übliche magellan map <ID,Region>
	 * @param c zu prüfende Region Coord (zu gebrauchen wenn bspw. geparst
	 * @return
	 */
	public static boolean isInRegions(Map<CoordinateID,Region> regions,CoordinateID c){
		if (regions==null || c==null) {return false;}
		for (Iterator<Region> iter = regions.values().iterator();iter.hasNext();){
			Region r = (Region)iter.next();
			if (r.getCoordinate().equals(c)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * liefert den Abstand zweier Regionen in Anzahl Runden
	 * berücksichtigt dabei Strassen
	 * @param regions Zur Verfügung stehende Regionen...
	 * @param von Startregion
	 * @param nach Zielregion
	 * @param reitend  reitet die Einheit oder zu Fuss?
	 * @param nextHold wenn Übergeben, wird darin CoordinateID des nächsten Halts der Unit abgelegt
	 * @return Anzahl benötigter Runden
	 */
	public static int getPathDistLand(GameData data, CoordinateID von, CoordinateID nach,boolean reitend,CoordinateID nextHold){
		if (!data.regions().containsKey(von)){
			return -1;
		}
		if (!data.regions().containsKey(nach)){
			return -1;
		}
		if (von.equals(nach)){
			return 0;
		}
		
		// Path organisieren
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(data.rules);
		String path = Regions.getDirections(data.regions(), von, nach, excludeMap);
		if (path==null || path.length()==0) {
			return -1;
		}
		
		int bewegungsPunkte = 4;
		if (reitend){
			bewegungsPunkte = 6;
		}
		
		int anzRunden = 1;
		int restBewegunspunkte = bewegungsPunkte;
		String[] dirs = path.split(" ");
		int step = 0;
		CoordinateID lastCoord = null;
		CoordinateID actCoord = null;
		try {
			lastCoord = (CoordinateID)von.clone();
			actCoord = (CoordinateID)von.clone();
		} catch (CloneNotSupportedException e){
			
		}
		boolean reached = false;
		boolean nextHoldSet = false;
		while (!reached){
			String actDir = dirs[step];
			int actDirInt = Direction.toInt(actDir);
			CoordinateID moveCoord = Direction.toCoordinate(actDirInt);
			actCoord.translate(moveCoord);
			
			
			
			int notwBewPunkte = 3;
			Region r1 = (Region)data.regions().get(lastCoord);
			Region r2 = (Region)data.regions().get(actCoord);
			if(r1==null || r2==null){
				return -1;
			}
			if (Regions.isCompleteRoadConnection(r1, r2)){
				notwBewPunkte = 2;
			}
			restBewegunspunkte-=notwBewPunkte;
			if (restBewegunspunkte<0) {
				anzRunden++;
				restBewegunspunkte = bewegungsPunkte-notwBewPunkte;
				nextHoldSet = true;
			} else {
				if (!nextHoldSet && nextHold!=null){
					nextHold.x = actCoord.x;
					nextHold.y = actCoord.y;
					nextHold.z = actCoord.z;
				}
			}
			
			if (actCoord.equals(nach)){
				reached = true;
			} else {
				// schieben
				lastCoord.x = actCoord.x;
				lastCoord.y = actCoord.y;
				lastCoord.z = actCoord.z;
				step++;
			}
		}
		
		return anzRunden;
	}
	
	
	/**
	 * liefert den Abstand zweier Regionen in Anzahl Runden
	 * wenn über See gefahren wird
	 * @param regions Zur Verfügung stehende Regionen...
	 * @param von Startregion
	 * @param nach Zielregion
	 * @param shipmovement
	 * @param nextHold wenn Übergeben, wird darin CoordinateID des nächsten Halts der Unit abgelegt
	 * @return Anzahl benötigter Runden
	 */
	public static int getPathDistOcean(GameData data, CoordinateID von, CoordinateID nach,int shipMovement,CoordinateID nextHold){
		if (!data.regions().containsKey(von)){
			return -1;
		}
		if (!data.regions().containsKey(nach)){
			return -1;
		}
		if (von.equals(nach)){
			return 0;
		}
		
		// Path organisieren
		Map<ID,RegionType> excludeMap = FFToolsRegions.getNonOceanRegionTypes(data.rules);
		String path = Regions.getDirections(data.regions(), von, nach, excludeMap);
		if (path==null || path.length()==0) {
			return -1;
		}
		
		int bewegungsPunkte = shipMovement;
		
		int anzRunden = 1;
		int restBewegunspunkte = bewegungsPunkte;
		String[] dirs = path.split(" ");
		int step = 0;
		CoordinateID lastCoord = null;
		CoordinateID actCoord = null;
		try {
			lastCoord = (CoordinateID)von.clone();
			actCoord = (CoordinateID)von.clone();
		} catch (CloneNotSupportedException e){
			
		}
		boolean reached = false;
		boolean nextHoldSet = false;
		while (!reached){
			String actDir = dirs[step];
			int actDirInt = Direction.toInt(actDir);
			CoordinateID moveCoord = Direction.toCoordinate(actDirInt);
			actCoord.translate(moveCoord);
			int notwBewPunkte = 1;
			restBewegunspunkte-=notwBewPunkte;
			if (restBewegunspunkte<0) {
				anzRunden++;
				restBewegunspunkte = bewegungsPunkte-notwBewPunkte;
				nextHoldSet = true;
			} else {
				if (!nextHoldSet && nextHold!=null){
					nextHold.x = actCoord.x;
					nextHold.y = actCoord.y;
					nextHold.z = actCoord.z;
				}
			}
			
			if (actCoord.equals(nach)){
				reached = true;
			} else {
				// schieben
				lastCoord.x = actCoord.x;
				lastCoord.y = actCoord.y;
				lastCoord.z = actCoord.z;
				step++;
			}
		}
		
		return anzRunden;
	}
	
	/**
	 * liefert komplette GotoInfo
	 * berücksichtigt dabei Strassen
	 * @param regions Zur Verfügung stehende Regionen...
	 * @param von Startregion
	 * @param nach Zielregion
	 * @param reitend  reitet die Einheit oder zu Fuss?
	 * @return GotoInfo
	 */
	public static GotoInfo getPathDistLandGotoInfo(GameData data, CoordinateID von, CoordinateID nach,boolean reitend){
		if (!data.regions().containsKey(von)){
			return null;
		}
		if (!data.regions().containsKey(nach)){
			return null;
		}
		GotoInfo erg = new GotoInfo();
		erg.setDestRegion(data.getRegion(nach));
		if (von.equals(nach)){
			erg.setAnzRunden(0);
			return erg;
		}
		
		// Path organisieren
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(data.rules);
		String path = Regions.getDirections(data.regions(), von, nach, excludeMap);
		if (path==null || path.length()==0) {
			return null;
		}
		erg.setPath(path);
		int bewegungsPunkte = 4;
		if (reitend){
			bewegungsPunkte = 6;
		}
		
		int anzRunden = 1;
		int restBewegunspunkte = bewegungsPunkte;
		String[] dirs = path.split(" ");
		int step = 0;
		CoordinateID lastCoord = null;
		CoordinateID actCoord = null;
		try {
			lastCoord = (CoordinateID)von.clone();
			actCoord = (CoordinateID)von.clone();
		} catch (CloneNotSupportedException e){
			
		}
		CoordinateID nextHold = new CoordinateID(0,0);
		Region lastHoldRegion = data.getRegion(von);
		
		boolean reached = false;
		boolean nextHoldSet = false;
		while (!reached){
			String actDir = dirs[step];
			int actDirInt = Direction.toInt(actDir);
			CoordinateID moveCoord = Direction.toCoordinate(actDirInt);
			actCoord.translate(moveCoord);

			int notwBewPunkte = 3;
			Region r1 = (Region)data.regions().get(lastCoord);
			Region r2 = (Region)data.regions().get(actCoord);
			if(r1==null || r2==null){
				return null;
			}
			if (Regions.isCompleteRoadConnection(r1, r2)){
				notwBewPunkte = 2;
			}
			restBewegunspunkte-=notwBewPunkte;
			if (restBewegunspunkte<0) {
				erg.setPathElement(anzRunden-1, lastHoldRegion, data.getRegion(lastCoord));
				lastHoldRegion = data.getRegion(lastCoord);
				anzRunden++;
				restBewegunspunkte = bewegungsPunkte-notwBewPunkte;
				nextHoldSet = true;
				erg.setNextHold(data.getRegion(nextHold));
			} else {
				if (!nextHoldSet && nextHold!=null){
					nextHold.x = actCoord.x;
					nextHold.y = actCoord.y;
					nextHold.z = actCoord.z;
				}
			}
			
			if (actCoord.equals(nach)){
				reached = true;
				erg.setPathElement(anzRunden-1, lastHoldRegion, data.getRegion(actCoord));
			} else {
				// schieben
				lastCoord.x = actCoord.x;
				lastCoord.y = actCoord.y;
				lastCoord.z = actCoord.z;
				step++;
			}
		}
		erg.setAnzRunden(anzRunden);
		return erg;
	}
	
	
	/**
	 * liefert komplette GotoInfo
	 * @param regions Zur Verfügung stehende Regionen...
	 * @param von Startregion
	 * @param nach Zielregion
	 * @param reitend  reitet die Einheit oder zu Fuss?
	 * @return GotoInfo
	 */
	public static GotoInfo getPathDistOceanGotoInfo(GameData data, CoordinateID von, CoordinateID nach,int shipMovement){
		if (!data.regions().containsKey(von)){
			return null;
		}
		if (!data.regions().containsKey(nach)){
			return null;
		}
		GotoInfo erg = new GotoInfo();
		erg.setDestRegion(data.getRegion(nach));
		if (von.equals(nach)){
			erg.setAnzRunden(0);
			return erg;
		}
		
		// Path organisieren
		Map<ID,RegionType> excludeMap = FFToolsRegions.getNonOceanRegionTypes(data.rules);
		String path = Regions.getDirections(data.regions(), von, nach, excludeMap);
		if (path==null || path.length()==0) {
			return null;
		}
		erg.setPath(path);
		int bewegungsPunkte = 7;
				int anzRunden = 1;
		int restBewegunspunkte = bewegungsPunkte;
		String[] dirs = path.split(" ");
		int step = 0;
		CoordinateID lastCoord = null;
		CoordinateID actCoord = null;
		try {
			lastCoord = (CoordinateID)von.clone();
			actCoord = (CoordinateID)von.clone();
		} catch (CloneNotSupportedException e){
			
		}
		CoordinateID nextHold = new CoordinateID(0,0);
		Region lastHoldRegion = data.getRegion(von);
		
		boolean reached = false;
		boolean nextHoldSet = false;
		while (!reached){
			String actDir = dirs[step];
			int actDirInt = Direction.toInt(actDir);
			CoordinateID moveCoord = Direction.toCoordinate(actDirInt);
			actCoord.translate(moveCoord);

			int notwBewPunkte = 3;
			Region r1 = (Region)data.regions().get(lastCoord);
			Region r2 = (Region)data.regions().get(actCoord);
			if(r1==null || r2==null){
				return null;
			}
			if (Regions.isCompleteRoadConnection(r1, r2)){
				notwBewPunkte = 2;
			}
			restBewegunspunkte-=notwBewPunkte;
			if (restBewegunspunkte<0) {
				erg.setPathElement(anzRunden-1, lastHoldRegion, data.getRegion(lastCoord));
				lastHoldRegion = data.getRegion(lastCoord);
				anzRunden++;
				restBewegunspunkte = bewegungsPunkte-notwBewPunkte;
				nextHoldSet = true;
				erg.setNextHold(data.getRegion(nextHold));
			} else {
				if (!nextHoldSet && nextHold!=null){
					nextHold.x = actCoord.x;
					nextHold.y = actCoord.y;
					nextHold.z = actCoord.z;
				}
			}
			
			if (actCoord.equals(nach)){
				reached = true;
				erg.setPathElement(anzRunden-1, lastHoldRegion, data.getRegion(actCoord));
			} else {
				// schieben
				lastCoord.x = actCoord.x;
				lastCoord.y = actCoord.y;
				lastCoord.z = actCoord.z;
				step++;
			}
		}
		erg.setAnzRunden(anzRunden);
		return erg;
	}
	
	/**
	 * Aufruf ohne CoordinateID für ersten Aufenthalt
	 * @param data
	 * @param von
	 * @param nach
	 * @param reitend
	 * @return
	 */
	public static int getPathDistLand(GameData data, CoordinateID von, CoordinateID nach,boolean reitend){
		// cache check
		cntCacheRequests++;
		if (pathDistCache==null){
			pathDistCache = new HashMap<PathDistLandInfo, Integer>();
		}
		// schon vorhanden ?
		for (Iterator<PathDistLandInfo> iter = pathDistCache.keySet().iterator();iter.hasNext();){
			PathDistLandInfo info = (PathDistLandInfo)iter.next();
			if (info.is(data, von, nach) || info.is(data, nach, von)){
				// Treffer
				Integer actI = pathDistCache.get(info);
				cntCacheHits++;
				return actI.intValue();
			}
		}
		
		int retVal =  getPathDistLand(data, von, nach, reitend,null);
		// in den Cache
		PathDistLandInfo neueInfo = new PathDistLandInfo(data,von,nach);
		Integer cacheValue = Integer.valueOf(retVal);
		pathDistCache.put(neueInfo,cacheValue);
		return retVal;
	}
	
	
	public static GotoInfo makeOrderNACH(ScriptUnit u,CoordinateID act,CoordinateID dest,boolean writeOrders){
		
		//	FF 20070103: eingebauter check, ob es actDest auch gibt?!
		if (!isInRegions(u.getScriptMain().gd_ScriptMain.regions(), dest)){
			// Problem  actDest nicht im CR -> abbruch
			u.addComment("Goto Ziel nicht im CR",true);
			u.doNotConfirmOrders();
			outText.addOutLine("!!! Goto Ziel nicht im CR: " + u.unitDesc());
			return null;
		} 
		
		GotoInfo erg = new GotoInfo();
		
		erg.setDestRegion(u.getScriptMain().gd_ScriptMain.getRegion(dest));
		
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(u.getScriptMain().gd_ScriptMain.rules);
		String path = Regions.getDirections(u.getScriptMain().gd_ScriptMain.regions(), act, dest, excludeMap);
		if (path!=null && path.length()>0) {
			// path gefunden
			if (writeOrders){
				u.addOrder("NACH " + path, true);
				u.addComment("Einheit durch GOTO bestätigt.",true);
			}
			erg.setPath(path);
			
			// Testing anzRunden
			boolean reitend = false;
			if (u.getPayloadOnHorse()>=0){
				reitend = true;
			}
			CoordinateID nextHold = new CoordinateID(act);
			int _anzRunden = FFToolsRegions.getPathDistLand(u.getScriptMain().gd_ScriptMain, act, dest, reitend,nextHold);
			if (_anzRunden>0){
				Region _nextHoldRegion = u.getScriptMain().gd_ScriptMain.getRegion(nextHold);
				if (_nextHoldRegion!=null){
					if (writeOrders){
					  u.addComment("Nächster Halt in " + _nextHoldRegion.toString(),true);
					}
					erg.setNextHold(_nextHoldRegion);
				}
				if (writeOrders){
					u.addComment("Erwartete Ankunft am EndZiel in " + _anzRunden + " Runden",true);
				}
				erg.setAnzRunden(_anzRunden);
			} else {
				if (writeOrders){
					u.addComment("Anzahl Runden: " + _anzRunden + " (?)",true);
				}
			}
		} else {
			// path nicht gefunden
			u.addComment("Einheit durch GOTO NICHT bestätigt.",true);
			u.addComment("Es konnte kein Weg gefunden werden.",true);
			u.doNotConfirmOrders();
			outText.addOutLine("X....kein Weg gefunden für " + u.unitDesc());
		}
		return erg;
	}
	
	public static void informUs(){
		String erg = "PathDistCache:";
		
		if (pathDistCache==null || pathDistCache.size()==0){
			erg += " unbenutzt";
		} else {
			erg += " " + pathDistCache.size() + " Datensätze";
			erg += ", Hits:" + cntCacheHits + "/" + cntCacheRequests;
		}
		outText.addOutLine(erg);
	}
	
	/**
	 * Liefert die Anzahl von ScriptPersonen in einer Region
	 * @param scriptUnits
	 * @param r
	 * @return
	 */
	public static int countScriptPersons(Hashtable<Unit,ScriptUnit> scriptUnits,Region r){
		int erg = 0;
		if (scriptUnits==null || scriptUnits.size()==0 || r==null){
			return 0;
		}
		
		for (Iterator<Unit> iter = scriptUnits.keySet().iterator();iter.hasNext();){
			Unit u = (Unit)iter.next();
			if (u.getRegion().equals(r)){
				erg+=u.getModifiedPersons();
			}
		}
		
		return erg;
	}
	
	/**
	 * liefert die fehlende Steinanzahl für angegebene Richtung
	 * in angegebener Region
	 * @param r Region
	 * @param direction int 
	 * @param data GameData
	 * @return
	 */
	public static int getMissingStreetStones(Region r, int direction,GameData data){
		
		// Anzahl Sollsteine ermitteln
		
		int anzahlSollSteine = ((RegionType)r.getType()).getRoadStones();
		
		if (anzahlSollSteine<=0){
			// kein Strassenbau möglich ?!
			return anzahlSollSteine;
		}
		
		// Grenze finden
		Border b = null;
		boolean borderfind = false;
		for(Iterator<Border> iter = r.borders().iterator(); iter.hasNext();) {
			b = (Border) iter.next();
			if(Umlaut.normalize(b.getType()).equals("STRASSE") &&
					   (b.getDirection() == direction)) {
				borderfind = true;
				break;
			}
		}
		
		if (!borderfind){
			// keine Grenze vorhanden
			return anzahlSollSteine;
		}
		// Gefundene Grenze in b
		int fertigProzent = b.getBuildRatio();
		if (fertigProzent==-1){
			// fehler im CR ?!
			return -2;
		}
		
		if (fertigProzent==100){
			// abkürzen
			return 0;
		}
		
		// berechnen
		double verbauteSteineDBL = ((double)fertigProzent/(double)100)* (double)anzahlSollSteine;
		// abrunden
		int verbauteSteine = (int)Math.floor(verbauteSteineDBL);
		return anzahlSollSteine - verbauteSteine;
	}
	
	
	/**
	 * liefert aus einer Region das passende Buildungm wenn vorhanden
	 * @param r
	 * @param number
	 * @return
	 */
	public static Building getBuilding(Region r,String number){
		Building b=null;
		if (r.buildings()!=null && r.buildings().size()>0){
			for (Iterator<Building> iter = r.buildings().iterator();iter.hasNext();){
				Building actB = (Building)iter.next();
				if (actB.getID().toString().equalsIgnoreCase(number)){
					return actB;
				}
			}
		}
		
		return b;
	}
	
	/**
	 * liefert Größte Burg der Region oder null, wenn es gar keine gibt.
	 * @param r
	 * @return
	 */
	public static Building getBiggestCastle(Region r){
		int maxFoundSize=0;
		Building actB=null;
		for (Iterator<Building> iter = r.buildings().iterator();iter.hasNext();){
			Building b = (Building)iter.next();
			if (b.getBuildingType() instanceof CastleType) {
				// Das oder ein zielobject
				if (b.getSize()>maxFoundSize){
					maxFoundSize = b.getSize();
					actB=b;
				}
			}
		}
		return actB;
	}
	
	
	/**
	 * Returns a map of all RegionTypes that are flagged as <tt>ocean</tt>.
	 *
	 * @param rules Rules of the game
	 *
	 * @return map of all non ocean RegionTypes
	 */
	public static Map<ID,RegionType> getNonOceanRegionTypes(Rules rules) {
		Map<ID,RegionType> ret = new Hashtable<ID, RegionType>();

		for(Iterator<RegionType> iter = rules.getRegionTypeIterator(); iter.hasNext();) {
			RegionType rt = (RegionType) iter.next();

			if(!rt.isOcean()) {
				ret.put(rt.getID(), rt);
			}
		}

		return ret;
	}
	
	
	/**
	 * Returns a map of all RegionTypes that are flagged as <tt>ocean</tt>.
	 *
	 * @param rules Rules of the game
	 *
	 * @return map of all non ocean RegionTypes
	 */
	public static Map<ID,RegionType> getOceanRegionTypes(Rules rules) {
		Map<ID,RegionType> ret = new Hashtable<ID, RegionType>();

		for(Iterator<RegionType> iter = rules.getRegionTypeIterator(); iter.hasNext();) {
			RegionType rt = (RegionType) iter.next();

			if(rt.isOcean()) {
				ret.put(rt.getID(), rt);
			}
		}

		return ret;
	}
	
	
	
	/**
	 * liefert Wert eines zusätzlich verbauten Steines in die Burg der Region bis zur 
	 * Erreichung des nächsten Levels in Silberstücken
	 * @param r
	 * @return
	 */
	public static double getValueOfBuiltStones(Region r){
		
		if (r.getModifiedPeasants()<=0){
			return 0;
		}
		int stones = 0;
		Building b = FFToolsRegions.getBiggestCastle(r);
		if (b!=null){
			// Es gibt eines....Steine bis zum nächsten Level?
			int actSize = b.getSize();
			
			if (actSize<10){
				stones = 10-actSize;
			} else if (actSize<50){
				stones = 50-actSize;
			} else if (actSize<250){
				stones = 250-actSize;
			} else if (actSize<1250){
				stones = 1250-actSize;
			} else if (actSize<6250){
				stones = 6250-actSize;
			}
			if (stones<=0){
				return 0;
			}
		} else {
			// Neubau -> bis 10
			// Anzahl Bauern durch 10 Steine
			stones = 10;
		}
		return ((double)r.getModifiedPeasants()/stones);
	}
	
	/**
	 * Anzahl von ItemType bei allen script(!)units der Region
	 * @param r
	 * @param type
	 * @return
	 */
	public static int getNumberOfItemsInRegion(Region r, ItemType type, ScriptMain scriptMain){
		if (r.units()==null){
			return 0;
		}
		int erg = 0;
		for (Unit u:r.units()){
			ScriptUnit su = scriptMain.getScriptUnit(u);
			if (su!=null){
				Item item=u.getModifiedItem(type);
				if (item!=null){
					erg+=item.getAmount();
				}
			}
		}
		return erg;
	}
	
	/**
	 * Anzahl von SkillType bei allen script(!)units der Region
	 * @param r
	 * @param type
	 * @return
	 */
	public static int getNumberOfTalentInRegion(Region r, SkillType type, ScriptMain scriptMain){
		if (r.units()==null){
			return 0;
		}
		int erg = 0;
		for (Unit u:r.units()){
			ScriptUnit su = scriptMain.getScriptUnit(u);
			if (su!=null){
				Skill  skill=u.getModifiedSkill(type);
				if (skill!=null){
					erg+=(skill.getLevel() * u.getModifiedPersons());
				}
			}
		}
		return erg;
	}
	
}
