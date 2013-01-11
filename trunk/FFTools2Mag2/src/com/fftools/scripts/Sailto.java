package com.fftools.scripts;


import java.util.List;

import magellan.library.CoordinateID;
import magellan.library.Region;
import magellan.library.utils.Regions;

import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.FFToolsUnits;

public class Sailto extends Script{
	private static final int Durchlauf = 42;
	
	// Parameterloser constructor
	public Sailto() {
		super.setRunAt(Durchlauf);
	}
	
	public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf!=Durchlauf){return;}
		// hier code fuer Sailto
		// addOutLine("....start SAILTO mit " + super.getArgCount() + " Argumenten");
		if (FFToolsUnits.checkShip(this)){
			if (super.getArgCount()<1) {
				super.addComment("Das Ziel fehlt beim Aufruf von SAILTO!",true);
				super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
				super.scriptUnit.doNotConfirmOrders();
				addOutLine("X....fehlendes SAILTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
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
						if (super.getArgCount()>1) {
							// es gibt weitere Ziele
							// aktuelles Ziel aus der Liste nehmen..
							// neue script order erstellen und anf�gen
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
				 				super.addComment("Fehler beim setzen der n�chsten // script SAILTO Anweisung",true);
				 				super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
				 				super.scriptUnit.doNotConfirmOrders();
				 				addOutLine("X....Fehler beim setzen der n�chsten // script SAILTO Anweisung bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				 			}
							
						} else {
							// das wars...Ziel erreicht und gut
							super.addComment("SAILTO: Einheit hat Ziel erreicht, daher NICHT best�tigt.",true);
							super.scriptUnit.doNotConfirmOrders();
						}
					} else {
						// nope, da m�ssen wir noch hin
						makeOrderNACH(actRegCoordID,actDest);
						
						
					}
				} else {
					// fehler beim parsen des Ziels
					zielParseFehler();
				}
			}
		} else {
			// kein Kaptn oder nicht auf dem schiff
			super.addComment("Einheit nicht als Kapit�n eines Schiffes erkannt.",true);
			super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("X....Einheit nicht als Kapit�n eines Schiffes erkannt: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
	}
	
	private void zielParseFehler() {
		super.addComment("Ungueltiges Ziel beim Aufruf von SAILTO!",true);
		super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
		super.scriptUnit.doNotConfirmOrders();
		addOutLine("X....ung�ltiges SAILTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	private void makeOrderNACH(CoordinateID act,CoordinateID dest){

		// FF 20070103: eingebauter check, ob es actDest auch gibt?!
		if (!com.fftools.utils.FFToolsRegions.isInRegions(this.gd_Script.getRegions(), dest)){
			// Problem  actDest nicht im CR -> abbruch
			super.addComment("Sailto Ziel nicht im CR",true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("!!! Sailto Ziel nicht im CR: " + this.unitDesc());
			return;
		} 

		int speed = super.gd_Script.getGameSpecificStuff().getGameSpecificRules().getShipRange(this.scriptUnit.getUnit().getModifiedShip());
		List<Region> pathL = Regions.planShipRoute(this.scriptUnit.getUnit().getModifiedShip(),super.gd_Script, dest);
		
		String path = null;
		if (pathL!=null){
			path = Regions.getDirections(pathL);
		}
		if (path!=null && path.length()>0) {
			// path gefunden
			// Reisezeitinfo
			this.addComment("Distance to target: " + (pathL.size()-1) + ", ETA in " + ((int)Math.ceil(((double)pathL.size()-1)/speed)) + " turns.");
			// NACH-Order	
			super.addOrder("NACH " + path, true);
			super.addComment("Einheit durch SAILTO best�tigt.",true);
		} else {
			// path nicht gefunden
			super.addComment("Einheit durch SAILTO NICHT best�tigt.",true);
			super.addComment("Es konnte kein Weg gefunden werden.",true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("X....kein Weg gefunden f�r " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
	}
	
	
}
