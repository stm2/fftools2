package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.Unit;

import com.fftools.utils.FFToolsOptionParser;




public class Auramaxwarning extends Script{
	
	
	private static final int Durchlauf = 1;

	
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
		
		// Optionen parsen - bei mode=auto ver�ndertes Verhalten!
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Auramaxwarning");
		boolean automode = OP.isOptionString("mode", "auto");
		if (automode){
			this.addComment("Auramaxwarning - AutoMode erkannt.");
		}
		Unit u = this.scriptUnit.getUnit();
		if (u.getAuraMax()>0) {
			if (u.getAura()==u.getAuraMax() && !zaubert()){
				/* Maximum erreicht */
				this.addComment("AuraMaxWarning: Einheit hat Maximum an Aura!");
				if (automode){
					automode_AuraMax();
				} else {
					this.doNotConfirmOrders();
				}
			} else {
				/* Maximum noch nicht erreicht */
				this.addComment("AuraMaxWarning: Einheit regeneriert weiter Aura");
				if (automode){
					automode_noAuraMax();
				}
			}
		} else {
			this.addComment("-> Der Scriptbefehl AuraMaxWarning ist f�r diese Einheit sinnlos");
		}
	}
	
	
	/**
	 * Untersucht, ob Befehle im Automode definiert worden sind
	 */
	private void automode_noAuraMax(){
		int givenOrders = setGivenOrders("IfNoAuraMax:");
		if (givenOrders==0){
			this.addComment("!!!Obwohl diese Einheit im automode ist, hat sie keine IfNoAuraMax:-Befehle!");
			this.doNotConfirmOrders();
		} else {
			this.addComment(givenOrders + " Befehle wurden der Einheit gegeben.");
		}
	}
	
	/**
	 * Untersucht, ob Befehle im Automode definiert worden sind
	 */
	private void automode_AuraMax(){
		int givenOrders = setGivenOrders("IfAuraMax:");
		if (givenOrders==0){
			this.addComment("!!!Obwohl diese Einheit im automode ist, hat sie keine IfAuraMax:-Befehle!");
			this.doNotConfirmOrders();
		} else {
			this.addComment(givenOrders + " Befehle wurden der Einheit gegeben.");
		}
	}
	
	/**
	 * Setzt die �bergebenen Werte als befehle ein, liefert Anzahl der gefundenen Zeilen zur�ck
	 * hinter keyword k�nnen eressea oder script befehle stehen
	 * @param keyword
	 * @return
	 */
	private int setGivenOrders(String keyword){
		int anzahl = 0;
		String searchPhrase = "// " + keyword;
		ArrayList<String> L = new ArrayList<String>(); 
		L.addAll(this.scriptUnit.getUnit().getOrders());
		for (String s:L){
			if (s.toLowerCase().startsWith(searchPhrase.toLowerCase())){
				anzahl++;
				s = s.substring(searchPhrase.length() + 1);
				String[] params = s.split(" ");
				String keyWord = params[0];
				if (keyWord.equalsIgnoreCase("script")) {
					if (params.length > 2) {
						// ok, in dieser Region soll ein script aufgerufen werden
						// eigentlich checken, ob dass von den scriptdurchl�ufen her passt
						// ansonsten parametersatz bauen und erg�nzen....
						String newOrderLine = "";
						for (int i = 2;i<params.length;i++){
							newOrderLine = newOrderLine.concat(params[i] + " ");
						}
						super.scriptUnit.findScriptClass(params[1], newOrderLine,true);
						this.addComment("Auramaxwarning - invoked stript " + params[1] + " with param " + newOrderLine + " ... ");
					} else {
						// die befehlszeile endet mit dem keyWord script
						super.addComment("Unerwartetes Ende der Befehlszeile (script)", true);
						super.addComment("Unit wurde durch " + keyword + " NICHT bestaetigt", true);
						super.scriptUnit.doNotConfirmOrders();
						addOutLine("X....Unerwartetes Ende der Befehlszeile (" + keyword + "): " + this.unitDesc());
					}
				} else {
					// kein script Befehl...alles was jetzt kommt als Order verpacken...
					// inkl des ersten wortes
					String newOrderLine = "";
					for (int i = 0;i<params.length;i++){
						newOrderLine = newOrderLine.concat(params[i] + " ");
					}
					if (newOrderLine.length()>0){
						// nun denn ... fertig und irgendetwas zu schreiben
						newOrderLine = newOrderLine.concat(" ;script Auramaxwarning - " + keyword);
						super.addOrder(newOrderLine,false);
						super.addComment("Unit wurde durch Auramaxwarning bestaetigt", true);
					}
				}
			}
		}
		return anzahl;
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
	
	/**
	 * pr�ft, ob ZAUBERE gesetzt ist
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
