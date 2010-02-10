package com.fftools.scripts;


import java.util.List;

import magellan.library.CoordinateID;
import magellan.library.Region;
import magellan.library.Ship;
import magellan.library.StringID;
import magellan.library.Unit;
import magellan.library.rules.BuildingType;
import magellan.library.utils.Regions;

public class Sailto extends Script{
	private Ship ship = null;
	
	private static final int Durchlauf = 4;
	
	// Parameterloser constructor
	public Sailto() {
		super.setRunAt(Durchlauf);
	}
	
	public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf!=Durchlauf){return;}
		
		
		if (checkShip()){
		
			if (super.getArgCount()<1) {
				super.addComment("Das Ziel fehlt beim Aufruf von SAILTO!",true);
				super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
				super.scriptUnit.doNotConfirmOrders();
				addOutLine("X....fehlendes SAILTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			} else {
				// wir haben zumindest ein Ziel
				CoordinateID actDest = CoordinateID.parse(super.getArgAt(0),",");
				if (actDest!=null){
					// wir haben ein Ziel...sind wir da?
					CoordinateID actRegCoordID = super.scriptUnit.getUnit().getRegion().getCoordinate();
					if (actRegCoordID.equals(actDest)){
						// yep, wir sind da
						if (super.getArgCount()>1) {
							// es gibt weitere Ziele
							// aktuelles Ziel aus der Liste nehmen..
							// neue script order erstellen und anfügen
							// neuen Path berechnen
							
							// neue SAILTO bilden
							String newSAILTO = "SAILTO ";
							for (int i = 1;i<super.getArgCount();i++){
								newSAILTO = newSAILTO.concat(super.getArgAt(i) + " ");
							}
							// ersetzen
							if (super.scriptUnit.replaceScriptOrder(newSAILTO, "SAILTO ".length())) {
								// OK...soweit alles klar
								// neues Ziel setzen
								actDest = CoordinateID.parse(super.getArgAt(1),",");
								if (actDest == null) {
									zielParseFehler();
								} else {
									// fein 
									makeOrderNACH(actRegCoordID,actDest);
								}
				 			} else {
				 				// irgendetwas beim ersetzen ist schief gegangen
				 				super.addComment("Fehler beim setzen der nächsten // script SAILTO Anweisung",true);
				 				super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
				 				super.scriptUnit.doNotConfirmOrders();
				 				addOutLine("X....Fehler beim setzen der nächsten // script SAILTO Anweisung bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				 			}
							
						} else {
							// das wars...Ziel erreicht und gut
							super.addComment("SAILTO: Einheit hat Ziel erreicht, daher NICHT bestätigt.",true);
							super.scriptUnit.doNotConfirmOrders();
						}
					} else {
						// nope, da müssen wir noch hin
						makeOrderNACH(actRegCoordID,actDest);
						
						
					}
				} else {
					// fehler beim parsen des Ziels
					zielParseFehler();
				}
			}
		} else {
			// kein Kaptn oder nicht auf dem schiff
			super.addComment("Einheit nicht als Kapitän eines Schiffes erkannt.",true);
			super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("X....Einheit nicht als Kapitän eines Schiffes erkannt: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
	}
	
	private void zielParseFehler() {
		super.addComment("Ungueltiges Ziel beim Aufruf von SAILTO!",true);
		super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
		super.scriptUnit.doNotConfirmOrders();
		addOutLine("X....ungültiges SAILTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	private void makeOrderNACH(CoordinateID act,CoordinateID dest){
		
		
		
		// FF 20070103: eingebauter check, ob es actDest auch gibt?!
		if (!com.fftools.utils.FFToolsRegions.isInRegions(this.gd_Script.regions(), dest)){
			// Problem  actDest nicht im CR -> abbruch
			super.addComment("Sailto Ziel nicht im CR",true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("!!! Sailto Ziel nicht im CR: " + this.unitDesc());
			return;
		} 

		// int speed = super.gd_Script.getGameSpecificStuff().getGameSpecificRules().getShipRange(this.ship);
		
		// BuildingType harbour = super.gd_Script.rules.getBuildingType(StringID.create("Hafen"));

		// List<Region> pathL = Regions.planShipRoute(this.ship, dest,super.gd_Script.regions(), harbour, speed);
		List<Region> pathL = Regions.planShipRoute(this.ship, super.gd_Script,dest);
		
		String path = null;
		if (pathL!=null){
			path = Regions.getDirections(pathL);
		}
		if (path!=null && path.length()>0) {
			// path gefunden
			super.addOrder("NACH " + path, true);
			super.addComment("Einheit durch SAILTO bestätigt.",true);
		} else {
			// path nicht gefunden
			super.addComment("Einheit durch SAILTO NICHT bestätigt.",true);
			super.addComment("Es konnte kein Weg gefunden werden.",true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("X....kein Weg gefunden für " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
	}
	/**
	 * überprüft, ob einheit Kapitän eines schiffes
	 * versucht, das shiff zu setzen (wird für pathbuilding enötigt)
	 * wenn beides OK -> true, sonst false
	 * @return
	 */
	private boolean checkShip(){
		// schiff checken
		Ship myS = super.scriptUnit.getUnit().getShip();
		if (myS==null){
			return false;
		}
		this.ship = myS;
		
		// Kaptän checken
		Unit captn = this.ship.getOwnerUnit();
		if (captn==null){return false;}
		
		// ist es unsere unit?
		if (!captn.equals(super.scriptUnit.getUnit())){
			return false;
		}
		
		return true;
	}
	
}
