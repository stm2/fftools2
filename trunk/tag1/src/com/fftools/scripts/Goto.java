package com.fftools.scripts;


import magellan.library.CoordinateID;

import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

public class Goto extends Script implements WithGotoInfo{
	
	private int Durchlauf1 = 4;
	
	private int[] runners = {Durchlauf1};
	
	private GotoInfo gotoInfo= null;
	
	// Parameterloser constructor
	public Goto() {
		super.setRunAt(this.runners);
	}
	
	public void runScript(int scriptDurchlauf){		
		// hier code fuer GoTo
		// addOutLine("....start GoTo mit " + super.getArgCount() + " Argumenten");
		if (super.getArgCount()<1) {
			super.addComment("Das Ziel fehlt beim Aufruf von GOTO!",true);
			super.addComment("Unit wurde durch GOTO NICHT bestaetigt", true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("X....fehlendes GOTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
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
						// neue script order erstellen und anf�gen
						// neuen Path berechnen
						
						// neue GOTO bilden
						String newGOTO = "GOTO ";
						for (int i = 1;i<super.getArgCount();i++){
							newGOTO = newGOTO.concat(super.getArgAt(i) + " ");
						}
						// ersetzen
						if (super.scriptUnit.replaceScriptOrder(newGOTO, "GOTO ".length())) {
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
			 				super.addComment("Fehler beim setzen der n�chsten // script GOTO Anweisung",true);
			 				super.addComment("Unit wurde durch GOTO NICHT bestaetigt", true);
			 				super.scriptUnit.doNotConfirmOrders();
			 				addOutLine("X....Fehler beim setzen der n�chsten // script GOTO Anweisung bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			 			}
						
					} else {
						// das wars...Ziel erreicht und gut
						super.addComment("GOTO: Einheit hat Ziel erreicht, daher NICHT best�tigt.",true);
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
	}
	
	private void zielParseFehler() {
		super.addComment("Ungueltiges Ziel beim Aufruf von GOTO!",true);
		super.addComment("Unit wurde durch GOTO NICHT bestaetigt", true);
		super.scriptUnit.doNotConfirmOrders();
		addOutLine("X....ung�ltiges GOTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	private void makeOrderNACH(CoordinateID act,CoordinateID dest){		
		this.gotoInfo = new GotoInfo();
		this.gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, act, dest,true);
		
	}

	public GotoInfo getGotoInfo(){
		return this.gotoInfo;
	}
	
	
}
