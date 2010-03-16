package com.fftools.pools.bau;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Bauen;


/**
 *Verwaltet Bauen-scripte
 *
 */
public class BauManager implements OverlordRun,OverlordInfo{
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static final int Durchlauf1 = 105;
	
	
	private int[] runners = {Durchlauf1};
	
	// private ScriptMain scriptMain = null;
	
	// merken wir uns zu jeder ScriptUnit doch einfach die bauScripte
	private Hashtable<ScriptUnit, ArrayList<Bauen>> bauScripte = null;
	

	public BauManager (ScriptMain _scriptMain){
		// this.scriptMain = _scriptMain;
	}
	
	
	
	public void informUs(){
		
	}
	
	
	/**
	 * Checkt den Status der bauscripte
	 *
	 */
	
	public void run(int durchlauf){
		if (this.bauScripte==null){return;}
		
		BauScriptComparator bauC = new BauScriptComparator();
		
		for (Iterator<ScriptUnit> iter = this.bauScripte.keySet().iterator();iter.hasNext();){
			ScriptUnit actUnit = (ScriptUnit)iter.next();
			
			// debug
			if (actUnit.getUnitNumber().equalsIgnoreCase("y9jl")){
				int i=0;
				i++;
			}
			
			boolean allFertig = true;
			String lernTalent = "";
			boolean hasCommand = false;
			Bauen actBauen = null;
			ArrayList<Bauen> actList = this.bauScripte.get(actUnit);
			if (actList!=null && actList.size()>0){
				// sortieren
				Collections.sort(actList,bauC);
				for (Iterator<Bauen> iter2 = actList.iterator();iter2.hasNext();){
					actBauen = (Bauen)iter2.next();
					if (!actBauen.isFertig()){
						allFertig = false;
						if (actBauen.getLernTalent().length()>0){
							lernTalent=actBauen.getLernTalent();
						}
						if (actBauen.getBauBefehl().length()>0){
							// Baubefehl
							hasCommand = true;
							actBauen.addOrder(actBauen.getBauBefehl(), true);
							break;
						}
					}
				}
			}
			if (allFertig){
				// alle bauaufträge fertig
				actUnit.addComment("Alle Bauaufträge erledigt");
				actUnit.doNotConfirmOrders();
			}
			
			if (!hasCommand){
				// soll Lernen
				if (lernTalent.length()>0 && actBauen!=null){
					// alles fein
					actBauen.addComment("Keine Bautätigkeit. Einheit soll Lernen.");
					actBauen.lerneTalent(lernTalent,true);
				} else {
					// kann nicht lernen
					actUnit.addComment("!!!Bauen: Unit soll Lernen, kann aber nicht!");
					actUnit.doNotConfirmOrders();
				}
			}
		}
	}
	
	
	public void addBauScript(Bauen bauen){
		if (this.bauScripte==null){
			this.bauScripte = new Hashtable<ScriptUnit, ArrayList<Bauen>>();
		}
		ArrayList<Bauen> actList = this.bauScripte.get(bauen.scriptUnit);
		if (actList==null){
			actList = new ArrayList<Bauen>();
		} 
		if (!actList.contains(bauen)){
			actList.add(bauen);
			this.bauScripte.put(bauen.scriptUnit,actList);
		}
	}
	
	
	public int[] runAt(){
		return runners;
	}
	
}
