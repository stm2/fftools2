package com.fftools.scripts;

import java.util.Iterator;

import magellan.library.Unit;




public class Auramaxwarning extends Script{
	
	
	private static final int Durchlauf = 159;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Auramaxwarning() {
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
		Unit u = this.scriptUnit.getUnit();
		if (u.getAuraMax()>0) {
			if (u.getAura()==u.getAuraMax() && !zaubert()){
				/* Maximum erreicht */
				this.doNotConfirmOrders();
				this.addComment("AuraMaxWarning: Einheit hat Maximum an Aura!");
			} else {
				/* Maximum noch nicht erreicht */
				this.addComment("AuraMaxWarning: Einheit regeneriert weiter Aura");
			}
		} else {
			this.addComment("-> Der Scriptbefehl AuraMaxWarning ist für diese Einheit sinnlos");
		}
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
	
	/**
	 * prüft, ob ZAUBERE gesetzt ist
	 * @return
	 */
	private boolean zaubert(){
		
		if (this.scriptUnit.getUnit().getOrders()==null || this.scriptUnit.getUnit().getOrders().size()==0){
			return false;
		}
		for(Iterator<String> iter = this.scriptUnit.getUnit().getOrders().iterator(); iter.hasNext();) {
			String s = (String) iter.next();
			if (s.toUpperCase().startsWith("ZAUBERE") || s.toUpperCase().startsWith("@ZAUBERE")){
				this.addComment("auramax: Einheit zaubert.");
				return true;
			}
		}
		this.addComment("auramax: Einheit zaubert nicht.");
		return false;
	}
	
	
}
