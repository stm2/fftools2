package com.fftools.scripts;

import java.util.Iterator;

import magellan.library.Item;
import magellan.library.Unit;
import magellan.library.rules.ItemType;

import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.GibDetails;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;



/**
 * zur einbindung externen empfänger von waren
 * 
 * @author Fiete
 *
 */
public class Liefere extends Script{
	
	/**
	 * sollte ganz zum schluss laufen
	 */
	private static final int Durchlauf = 160;
	
	private int menge = -1;
	
	private ItemType itemType = null;
	
	private String ziel = null;
	
	private boolean notSuccessACK = false;
	
	private boolean weniger = false;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Liefere() {
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
	
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		String nameGut = OP.getOptionString("Ware");
		if (nameGut.length()<2) {
			this.addComment("Liefere: Ware nicht angegeben.");
			outText.addOutLine("!!!Liefere: Ware nicht angegeben. " + this.unitDesc());
			return;
		}
		nameGut = nameGut.replace("_", " ");
		this.itemType = this.gd_Script.rules.getItemType(nameGut);
		if (this.itemType==null){
			this.addComment("Liefere: Ware nicht erkannt.");
			outText.addOutLine("!!!Liefere: Ware nicht erkannt. " + this.unitDesc());
			return;
		}
		this.menge = OP.getOptionInt("Menge", -1);
		if (this.menge<1){
			this.addComment("Liefere: Summe nicht erkannt.");
			outText.addOutLine("!!!Liefere: Summe nicht erkannt. " + this.unitDesc());
			return;
		}
		this.ziel = OP.getOptionString("Ziel");
		if (ziel.length()<2){
			this.addComment("Liefere: Ziel nicht angegeben.");
			outText.addOutLine("!!!Liefere: Ziel nicht angegeben. " + this.unitDesc());
			return;
		}
		
		this.notSuccessACK = OP.getOptionBoolean("notSuccessACK",false);
		
		this.weniger = OP.getOptionBoolean("weniger", false);
		
		// OK..dann mal los
		// Menge checken
		Item item = this.scriptUnit.getModifiedItem(this.itemType);
		if (item==null || item.getAmount()==0){
			this.addComment("Liefere: kein " + this.itemType.getName() + " vorhanden.");
			return;
		}
		if (item.getAmount()<this.menge && !this.weniger){
			this.addComment("Liefere: nicht ausreichend " + this.itemType.getName() + " vorhanden.");
			if (!this.notSuccessACK){
				this.scriptUnit.doNotConfirmOrders();
			}
			return;
		}
		// wir haben genug
		// ziele durchgehen und checken
		String[] targets = this.ziel.split(",");
		int gesamtAmount = item.getAmount();
		for (int i=0;i<targets.length;i++){
			String s2 = targets[i];
			s2 = s2.replace("_", " ");
			Unit u = FFToolsGameData.getUnitInRegion(this.region(),s2);
			if (u!=null){
				// Ziel gefunden
				if ((gesamtAmount >= this.menge && !this.weniger) || gesamtAmount>0){
					// ausreichend zeug sollte da sein
					int zumTarget = this.menge;
					int vonGib = this.holDasZeug(u);
					gesamtAmount -= vonGib;
					zumTarget -= vonGib;
					
					if (zumTarget>0 && gesamtAmount>0){
						// dann eigene Vorräte angreifen..
						String newOrder = "GIB ";
						newOrder += u.toString(false) + " ";
						newOrder += zumTarget + " ";
						newOrder += "\"" + this.itemType.getName() + "\" ;";
						newOrder += "Liefere";
						this.addOrder(newOrder,false);
						gesamtAmount -= zumTarget;
					} else {
						if (zumTarget>0) {
							this.addComment("Liefere: (->" + u.toString(false) + ") nicht ausreichend " + this.itemType.getName() + " vorhanden. (" + zumTarget + " fehlen)");
						}
					}
				} else {
					// nicht mehr genügend da
					this.addComment("Liefere: (->" + u.toString(false) + ") nicht ausreichend " + this.itemType.getName() + " vorhanden.");
					if (!this.notSuccessACK){
						this.scriptUnit.doNotConfirmOrders();
					}
				}
			} else {
				this.addComment("Liefere: target not found: " + s2);
			}
		}
	}
	
	/**
	 * Geht die GIB orders durch und splittet, wo passend
	 * @param u
	 * @return die anzahl durch splitting umgeleiteten güter
	 */
	private int holDasZeug(Unit u){
		// in u die zielunit in der region
		// einheiten der region durchlaufen, die 
		// an diese scriptunit was übergeben..diese umbiegen
		int actMenge = this.menge;
		boolean changedOrders = false;
		int worstCaseCounter = 0;
		for (Iterator<Unit> iter = this.region().units().iterator();iter.hasNext();){
			Unit actUnit = (Unit)iter.next();
			ScriptUnit actScriptUnit = this.scriptUnit.getScriptMain().getScriptUnit(actUnit.toString(false));
			if (actScriptUnit!=null){
				changedOrders	= true;
				while (changedOrders){
					changedOrders = false;
					worstCaseCounter = 0;
					for (Iterator<String> iter2 = actUnit.getOrders().iterator();iter2.hasNext();){
						String actOrder = (String)iter2.next();
						GibDetails GD=new GibDetails(actScriptUnit,actOrder);
						if (GD.getItemType()!=null && GD.getItemType().equals(this.itemType)){
							if (GD.getTargetScriptUnit()!=null && GD.getTargetScriptUnit().equals(this.scriptUnit)){
								// Bingo
								int Transfer = Math.min(actMenge,GD.getAmount());
								GD.SplitOrder(u, Transfer, "Liefere");
								actMenge -= Transfer;
								if (actMenge<=0){
									return this.menge;
								}
								changedOrders = true;
								worstCaseCounter++;
								if (worstCaseCounter>2000){
									// endless loop?!
									outText.addOutLine("!!!* endless loop in Liefere...exiting holDasZeug",true);
									return 0;
								}
								
								break;
							}
						}
					}
				}
			}
		}
		return this.menge-actMenge;
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
	
}
