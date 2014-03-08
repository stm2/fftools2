package com.fftools.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
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
import magellan.library.gamebinding.EresseaMapMetric;
import magellan.library.rules.CastleType;
import magellan.library.rules.ItemType;
import magellan.library.rules.RegionType;
import magellan.library.rules.SkillType;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;
import magellan.library.utils.Umlaut;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
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
	
	private static boolean notUseRegionNames = false;
	
	/**
	 * a cache for the calls to getPathDistLand with no(!) nextstepInfo!
	 */
	private static HashMap<PathDistLandInfo,Integer> pathDistCache = null;
	
	/**
	 * TH: A translation map of region names to region coordinates
	 */

	private static HashMap<String,CoordinateID> regionMap = null;
	
	/**
	 * Gibt es eine Koordinate in den �bergebenen Regionen?
	 * @param regions die �bliche magellan map <ID,Region>
	 * @param c zu pr�fende Region Coord (zu gebrauchen wenn bspw. geparst
	 * @return
	 */
	public static boolean isInRegions(Collection<Region> regions,CoordinateID c){
		if (regions==null || c==null) {return false;}
		for (Region r:regions){
			if (r.getCoordinate().equals(c)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * liefert den Abstand zweier Regionen in Anzahl Runden
	 * ber�cksichtigt dabei Strassen
	 * @param regions Zur Verf�gung stehende Regionen...
	 * @param von Startregion
	 * @param nach Zielregion
	 * @param reitend  reitet die Einheit oder zu Fuss?
	 * @param nextHold wenn �bergeben, wird darin FFToolsCoordinateID des n�chsten Halts der Unit abgelegt
	 * @return Anzahl ben�tigter Runden
	 */
	public static int getPathDistLand(GameData data, CoordinateID von, CoordinateID nach,boolean reitend,FFToolsCoordinateID nextHold){
		if (!data.getRegions().contains(data.getRegion(von))){
			return -1;
		}
		if (!data.getRegions().contains(data.getRegion(nach))){
			return -1;
		}
		if (von.equals(nach)){
			return 0;
		}
		
		// Path organisieren
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(data.rules);
		RegionType Feuerwand = Regions.getFeuerwandRegionType(data);
		excludeMap.put(Feuerwand.getID(), Feuerwand);
		// String path = Regions.getDirections(data, von, nach, excludeMap,1);
		String path =  Regions.getDirections(Regions.getLandPath(data, von, nach, excludeMap,2, 3));
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
		lastCoord = (CoordinateID)von.clone();
		actCoord = (CoordinateID)von.clone();
		boolean reached = false;
		boolean nextHoldSet = false;
		while (!reached){
			String actDir = dirs[step];
			Direction actDirInt = FFToolsRegions.getDirectionFromString(actDir);
			CoordinateID moveCoord = CoordinateID.create(actDirInt.toCoordinate());
			actCoord = actCoord.translate(moveCoord);
			
			int notwBewPunkte = 3;
			Region r1 = (Region)data.getRegion(lastCoord);
			Region r2 = (Region)data.getRegion(actCoord);
			
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
					nextHold.setToCoordinateID(actCoord);
				}
			}
			
			if (actCoord.equals(nach)){
				reached = true;
			} else {
				// schieben
				lastCoord = CoordinateID.create(actCoord);
				step++;
			}
		}
		
		return anzRunden;
	}
	
	/**
	 * ersetzt alte Funktion in magellan.Direction
	 * @param s
	 * @return
	 */
	public static Direction getDirectionFromString(String s){
		Direction erg = Direction.INVALID;
		
		if (s.equalsIgnoreCase("NW")){
			erg = EresseaMapMetric.NW;
		}
		if (s.equalsIgnoreCase("NO") || s.equalsIgnoreCase("NE")){
			erg = EresseaMapMetric.NE;
		}
		if (s.equalsIgnoreCase("O") || s.equalsIgnoreCase("E")){
			erg = EresseaMapMetric.E;
		}
		if (s.equalsIgnoreCase("SO") || s.equalsIgnoreCase("SE")){
			erg = EresseaMapMetric.SE;
		}
		if (s.equalsIgnoreCase("SW")){
			erg = EresseaMapMetric.SW;
		}
		if (s.equalsIgnoreCase("W")){
			erg = EresseaMapMetric.W;
		}
		return erg;
	}
	
	
	/**
	 * liefert komplette GotoInfo
	 * ber�cksichtigt dabei Strassen
	 * @param regions Zur Verf�gung stehende Regionen...
	 * @param von Startregion
	 * @param nach Zielregion
	 * @param reitend  reitet die Einheit oder zu Fuss?
	 * @return GotoInfo
	 */
	public static GotoInfo getPathDistLandGotoInfo(GameData data, CoordinateID von, CoordinateID nach,boolean reitend){
		if (!data.getRegions().contains(data.getRegion(von))){
			return null;
		}
		if (!data.getRegions().contains(data.getRegion(nach))){
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
		RegionType Feuerwand = Regions.getFeuerwandRegionType(data);
		excludeMap.put(Feuerwand.getID(), Feuerwand);
		// String path = Regions.getDirections(data, von, nach, excludeMap,1);
		String path =  Regions.getDirections(Regions.getLandPath(data, von, nach, excludeMap,2, 3));
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
		lastCoord = (CoordinateID)von.clone();
		actCoord = (CoordinateID)von.clone();
		CoordinateID nextHold = CoordinateID.create(0, 0);
		Region lastHoldRegion = data.getRegion(von);
		
		boolean reached = false;
		boolean nextHoldSet = false;
		while (!reached){
			String actDir = dirs[step];
			Direction actDirInt = FFToolsRegions.getDirectionFromString(actDir);
			CoordinateID moveCoord = CoordinateID.create(actDirInt.toCoordinate());
			actCoord = actCoord.translate(moveCoord);

			int notwBewPunkte = 3;
			Region r1 = (Region)data.getRegion((lastCoord));
			Region r2 = (Region)data.getRegion((actCoord));
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
					nextHold = CoordinateID.create(actCoord);
				}
			}
			
			if (actCoord.equals(nach)){
				reached = true;
				erg.setPathElement(anzRunden-1, lastHoldRegion, data.getRegion(actCoord));
			} else {
				// schieben
				lastCoord = CoordinateID.create(actCoord);
				step++;
			}
		}
		erg.setAnzRunden(anzRunden);
		return erg;
	}
	
	
	/**
	 * Aufruf ohne CoordinateID f�r ersten Aufenthalt
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
	
	
	public static GotoInfo makeOrderNACH(ScriptUnit u,CoordinateID act,CoordinateID dest,boolean writeOrders,String originInfo){
		
		//	FF 20070103: eingebauter check, ob es actDest auch gibt?!
		if (!isInRegions(u.getScriptMain().gd_ScriptMain.getRegions(), dest)){
			// Problem  actDest nicht im CR -> abbruch
			u.addComment("Goto Ziel nicht im CR",true);
			u.doNotConfirmOrders();
			outText.addOutLine("!!! Goto Ziel nicht im CR: " + u.unitDesc());
			return null;
		} 
		
		GotoInfo erg = new GotoInfo();
		
		erg.setDestRegion(u.getScriptMain().gd_ScriptMain.getRegion(dest));
		
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(u.getScriptMain().gd_ScriptMain.rules);
		RegionType Feuerwand = Regions.getFeuerwandRegionType(u.getScriptMain().gd_ScriptMain);
		excludeMap.put(Feuerwand.getID(), Feuerwand);
		// String path = Regions.getDirections(u.getScriptMain().gd_ScriptMain, act, dest, excludeMap,2);
		String path =  Regions.getDirections(Regions.getLandPath(u.getScriptMain().gd_ScriptMain, act, dest, excludeMap,2, 3));
		if (path!=null && path.length()>0) {
			// path gefunden
			if (writeOrders){
				u.addOrder("NACH " + path, true);
				String info = "Einheit durch GOTO best�tigt.";
				if ((originInfo.length())>1) {
					info = info + "(" + originInfo + ")";
				}
				
				u.addComment(info ,true);
			}
			erg.setPath(path);
			
			// Testing anzRunden
			boolean reitend = false;
			if (u.getPayloadOnHorse()>=0){
				reitend = true;
			}
			FFToolsCoordinateID nextHold = FFToolsCoordinateID.create(0,0,0);
			int _anzRunden = FFToolsRegions.getPathDistLand(u.getScriptMain().gd_ScriptMain, act, dest, reitend,nextHold);
			if (_anzRunden>0){
				CoordinateID _nextRegionCoord = CoordinateID.create(nextHold.getX(),nextHold.getY(),nextHold.getZ());
				Region _nextHoldRegion = u.getScriptMain().gd_ScriptMain.getRegion(_nextRegionCoord);
				if (_nextHoldRegion!=null){
					if (writeOrders){
					  u.addComment("N�chster Halt in " + _nextHoldRegion.toString(),true);
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
			u.addComment("Einheit durch GOTO NICHT best�tigt.",true);
			u.addComment("Es konnte kein Weg gefunden werden.",true);
			u.doNotConfirmOrders();
			outText.addOutLine("X....kein Weg gefunden f�r " + u.unitDesc());
		}
		return erg;
	}
	
	public static void informUs(){
		String erg = "PathDistCache:";
		
		if (pathDistCache==null || pathDistCache.size()==0){
			erg += " unbenutzt";
		} else {
			erg += " " + pathDistCache.size() + " Datens�tze";
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
	 * liefert die fehlende Steinanzahl f�r angegebene Richtung
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
			// kein Strassenbau m�glich ?!
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
			// abk�rzen
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
	 * liefert Gr��te Burg der Region oder null, wenn es gar keine gibt.
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
	 * @return map of all ocean RegionTypes
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
	 * liefert n�chsth�chste Ausbaustufe 
	 * @param actSize
	 * @return
	 */
	public static int getNextCastleSize(int actSize){
		int nextS = 6250;
		if (actSize<10){
			nextS = 10;
		} else if (actSize<50){
			nextS = 50;
		} else if (actSize<250){
			nextS = 250;
		} else if (actSize<1250){
			nextS = 1250;
		} else if (actSize<6250){
			nextS = 6250;
		}
		
		return nextS;
	}
	
	/**
	 * liefert n�chste Ausbaustufe der Region
	 * @param r
	 * @return
	 */
	public static int getNextCastleSize(Region r){
		int erg=0;
		Building b = FFToolsRegions.getBiggestCastle(r);
		if (b!=null){
			// Es gibt eines....Steine bis zum n�chsten Level?
			int actSize = b.getSize();
			erg = getNextCastleSize(actSize);
		} else {
			// Neubau -> bis 10
			erg = 10;
		}
		return erg;
	}
	
	
	/**
	 * liefert Wert eines zus�tzlich verbauten Steines in die Burg der Region bis zur 
	 * Erreichung des n�chsten Levels in Silberst�cken
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
			// Es gibt eines....Steine bis zum n�chsten Level?
			int actSize = b.getSize();
			stones = getNextCastleSize(actSize)-actSize;
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

	/**
	 * TODO Inserted by TH
	 * Returns coordinates of the region whose name was passed as argument, or NULL if not found
	 * @param data
	 * @param regionName
	 * @return
	 */
	public static CoordinateID getRegionCoordFromName (GameData data, String regionName) {
		String currentName = null;
		CoordinateID currentCoord = null;
		
		// usage of RegionNames as switch via reportSettiungs
		ReportSettings reportSettings = ReportSettings.getInstance();
		if (reportSettings.getOptionBoolean("notUseRegionNames")){
			notUseRegionNames=true;
		} else {
			notUseRegionNames=false;
		}
		
		
		if (notUseRegionNames){
			return null;
		}
		
		// be aware of spaces in the name
		// FFTools wide consept: use '_' for spaces (Ring_der_Unsichtbarkeit)
		// we have the correct names in the map - we need to replace '_' with ' ' 
		// in  the search string
		regionName = regionName.replace('_',' ');
		
		
		// If the translation map has not yet been initialized, do it:
		if (regionMap==null) {
			outText.addOutLine("Erzeuge neue Map RegionNames");
			System.out.println("Erzeuge neue Map RegionNames\n");
			regionMap = new HashMap<String, CoordinateID>();
			for (Region r:data.getRegions()){
				currentName = r.getName();
				currentCoord = r.getCoordinate();
				regionMap.put(currentName, currentCoord);
			}
			outText.addOutLine("Map RegionNames erzeugt");
			System.out.println("Map RegionNames erzeugt\n");
		}
		// Translation map has been initialized, now return the coordinate for the name
		// CAREFUL WITH THE RESULT: May be NULL if region name cannot be found!
		return regionMap.get(regionName.replace("_", " "));
	}

	public static boolean isNotUseRegionNames() {
		return notUseRegionNames;
	}

	public static void setNotUseRegionNames(boolean useRegionNames) {
		FFToolsRegions.notUseRegionNames = useRegionNames;
	}
	
	/**
	 * simuliert eine Schiffsbewegung, liefert die Entfernung
	 * @param from
	 * @param to
	 * @return
	 */
	public static int getShipPathSize_Virtuell(CoordinateID from, CoordinateID to,GameData data, int speed){
		
		Region fromR = data.getRegion(from);
		if (fromR==null){return -1;}
		
		List<Region> pathL = Regions.planShipRoute(data,from,Direction.INVALID,to,speed);
		if (pathL==null || pathL.size()<=0){
			return -1;
		}
		int dist = pathL.size()-1;
		
		return dist;
	}
	
	/**
	 * simuliert eine Schiffsbewegung, liefert die Rundenanzahl
	 * @param from
	 * @param to
	 * @return
	 */
	public static int getShipPathSizeTurns_Virtuell(CoordinateID from, CoordinateID to,GameData data, int speed){
		int turns = getShipPathSize_Virtuell(from,to,data,speed);
		if (turns<=0){
			return -1;
		}
		turns = (int)Math.ceil(((double)turns)/speed); 
		return turns;
	}
}
