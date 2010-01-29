package com.fftools.scripts;




public class Abrunde extends Script{
	
	
	private static final int Durchlauf = 1;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Abrunde() {
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
		// erwartetes format
		// script abrunde XXX befehl ...
		// script abrunde XXX script befehl ...
		
		String rundenString = super.getArgAt(0);
		int sollRunde = 0;
		try {
			sollRunde = Integer.parseInt(rundenString);
		} catch (NumberFormatException e) {
			// pech gehabt
			this.addComment("!!! Fehlerhafte Rundenerkennung bei: " + this.unitDesc());
			outText.addOutLine("Fehlerhafte Rundenerkennung bei: " + this.unitDesc());
			this.scriptUnit.doNotConfirmOrders();
			return;
		}
		
		if (sollRunde<=0){
			// pech gehabt
			this.addComment("!!! Fehlerhafte Rundenerkennung bei: " + this.unitDesc());
			outText.addOutLine("Fehlerhafte Rundenerkennung bei: " + this.unitDesc());
			this.scriptUnit.doNotConfirmOrders();
			return;
		}
		
		int actRunde = this.gd_Script.getDate().getDate();

		if (actRunde>=sollRunde){
			// gleiche runde!
			String keyWord = super.getArgAt(1);
			if (keyWord.equalsIgnoreCase("script")){
				// scriptbefehl dahinter
				if (super.getArgCount()>2) {
					// ok, in dieser Region soll ein script aufgerufen werden
					// eigentlich checken, ob dass von den scriptdurchl�ufen her passt
					// ansonsten parametersatz bauen und erg�nzen....
					String newOrderLine = "";
					for (int i = 3;i<super.getArgCount();i++){
						newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
					}
					
					newOrderLine = newOrderLine.trim();
					
					super.scriptUnit.findScriptClass(super.getArgAt(2), newOrderLine,true);
					
					// diese Zeile aus den orders entfernen
					this.scriptUnit.removeScriptOrder("abrunde " + sollRunde + " script " + super.getArgAt(2) + " " + newOrderLine);
					
					// neue script zeile schreiben
					this.addOrder("// script " + super.getArgAt(2) + " " + newOrderLine, false);
					this.addComment("abrunde erg�nzt: // script " + super.getArgAt(2) + " " + newOrderLine + " ;dnt", false);
					
				} else {
					// die befehlszeile endet mit dem keyWord script
					super.addComment("Unerwartetes Ende der Befehlszeile (script)", true);
					super.addComment("Unit wurde durch abrunde NICHT bestaetigt", true);
					super.scriptUnit.doNotConfirmOrders();
					addOutLine("X....Unerwartetes Ende der Befehlszeile (abrunde):" + this.unitDesc());
					this.scriptUnit.doNotConfirmOrders();
				}
			} else {
				// kein scriptbefehl dahinter
				// kein script Befehl...alles was jetzt kommt als Order verpacken...
				// inkl des ersten wortes
				String newOrderLine = "";
				for (int i = 1;i<super.getArgCount();i++){
					newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
				}
				if (newOrderLine.length()>0){
					// nun denn ... fertig und irgendetwas zu schreiben
					super.addOrder(newOrderLine,true);
				}
			}
		}
		// wenn sollrunde in der Zukunft: nix machen
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
	
}
