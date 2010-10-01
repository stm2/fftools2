package com.fftools.scripts;


import magellan.library.CoordinateID;
import magellan.library.Ship;
import magellan.library.Unit;

import com.fftools.utils.FFToolsRegions;

public class Route extends Script{
	
	private static final int Durchlauf = 3;
	
	// Parameterloser constructor
	public Route() {
		super.setRunAt(Durchlauf);
	}
	
	public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf!=Durchlauf){return;}
		
		// hier code fuer Route
		// addOutLine("....start Route mit " + super.getArgCount() + " Argumenten");
		if (super.getArgCount()<2) {
			super.addComment("Ein Ziel fehlt beim Aufruf von ROUTE!",true);
			super.addComment("Unit wurde durch ROUTE NICHT bestaetigt", true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("X....fehlendes ROUTE Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		} else {
			// wir haben zumindest ein Ziel
			// TH: Pr�fen, ob Koordinate oder Regionsname, falls Komma in Regionsangabe sind es wohl Koordinaten...
			CoordinateID actDest = null;
			if (super.getArgAt(0).indexOf(',') > 0) {
				actDest = CoordinateID.parse(super.getArgAt(0),",");
			} else {
			// Keine Koordinaten, also Region in Koordinaten konvertieren
				actDest = FFToolsRegions.getRegionCoordFromName(this.gd_Script, super.getArgAt(0));
			}
			if (actDest!=null){
				// wir haben ein Ziel...sind wir da?
				CoordinateID actRegCoordID = super.scriptUnit.getUnit().getRegion().getCoordinate();
				if (actRegCoordID.equals(actDest)){
					// yep, wir sind da
					// es gibt weitere Ziele, zumindest eines
					// aktuelles Ziel aus der Liste nehmen und hinten anf�gen
					// aktuelle GOTO erstellen und an scriptList anf�gen
					// neuen Path berechnen -> macht GOTO
					
					// neue ROUTE bilden
					String newROUTE = "ROUTE ";
					for (int i = 1;i<super.getArgCount();i++){
						newROUTE = newROUTE.concat(super.getArgAt(i) + " ");
					}
					// noch hinten den bisher ersten wieder dranne
					newROUTE = newROUTE.concat(super.getArgAt(0) + " ");
					
					// ersetzen
					if (super.scriptUnit.replaceScriptOrder(newROUTE, "ROUTE ".length())) {
						// OK...soweit alles klar
						// neues Ziel setzen; TH: wieder mit Unterscheidung Coordinate vs. Region
						if (super.getArgAt(1).indexOf(',') > 0) {
							actDest = CoordinateID.parse(super.getArgAt(1),",");
						} else {
						// Keine Koordinaten, also Region in Koordinaten konvertieren
							actDest = FFToolsRegions.getRegionCoordFromName(this.gd_Script, super.getArgAt(1));
						}
						if (actDest == null) {
							zielParseFehler();
						} else {
							// fein 
							setGOTO(actDest);
							super.addComment("Routenpunkt erreicht. N�chster Routenpunkt ausgew�hlt.",true);
						}
		 			} else {
		 				// irgendetwas beim ersetzen ist schief gegangen
		 				super.addComment("Fehler beim setzen der n�chsten // script GOTO Anweisung",true);
		 				super.addComment("Unit wurde durch GOTO NICHT bestaetigt", true);
		 				super.scriptUnit.doNotConfirmOrders();
		 				addOutLine("X....Fehler beim setzen der n�chsten // script GOTO Anweisung bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		 			}
						
					 
				} else {
					// nope, da m�ssen wir noch hin
					setGOTO(actDest);
				}
			} else {
				// fehler beim parsen des Ziels
				zielParseFehler();
			}
		}
	}
	
	private void zielParseFehler() {
		super.addComment("Ungueltiges Ziel beim Aufruf von ROUTE!",true);
		super.addComment("Unit wurde durch ROUTE NICHT bestaetigt", true);
		super.scriptUnit.doNotConfirmOrders();
		addOutLine("X....ung�ltiges ROUTE Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	private void setGOTO(CoordinateID dest){
		// lediglich ein GOTO script zum dest erzeugen...wird in sp?terem Lauf dort bearbeitet
		// naja...wenn Kapit�n, dann ein SAILTO....
		if (this.checkShip()){
			super.scriptUnit.findScriptClass("Sailto",dest.toString(","));
		} else {
			super.scriptUnit.findScriptClass("Goto",dest.toString(","));
		}
	}
	
	/**
	 * ?berpr?ft, ob einheit Kapit�n eines schiffes
	 * versucht, das shiff zu setzen (wird f�r pathbuilding en?tigt)
	 * wenn beides OK -> true, sonst false
	 * @return
	 */
	private boolean checkShip(){
		// schiff checken
		Ship myS = super.scriptUnit.getUnit().getShip();
		if (myS==null){
			return false;
		}
		Ship ship = myS;
		
		// Kapt�n checken
		Unit captn = ship.getOwnerUnit();
		if (captn==null){return false;}
		
		// ist es unsere unit?
		if (!captn.equals(super.scriptUnit.getUnit())){
			return false;
		}
		return true;
	}
	
}
