package com.fftools.scripts;

import com.fftools.utils.FFToolsOptionParser;


/**
 * 
 * Schreibt die angegebenen Parameter in die Befehle und die Einheit KANN durch
 * andere Skripte best�tigt werden. confirm=$Runde best�tigt bis $Runde
 * confirm=immer wird die Einheit immer best�tigt.
 * 
 * 
 * @author Marc
 * 
 */

public class Ack extends Script {

	private int Durchlauf_vorMatpool = 1;

	/**
	 * Parameterloser Constructor Drinne Lassen fuer die Instanzierung des
	 * Objectes
	 */

	public Ack() {
		super.setRunAt(Durchlauf_vorMatpool);
	}

	/**
	 * Hier ruft SkriptMain auf...
	 */

	public void runScript(int scriptDurchlauf) {

		if (scriptDurchlauf == Durchlauf_vorMatpool) {
			this.vorMatpool();
		}

	}

	// Hier passiert es dann!

	private void vorMatpool() {
		
		
		String Orderline = "";
		// Default null zeigt Unver�ndertheit an!
		int confirmUntil = 0;

		// Gibt es �berhaupt Argumente?
		if (super.getArgCount() > 0) {

			FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
			OP.addOptionList(this.getArguments());
			
			if (OP.getOptionString("Laufzeit").equalsIgnoreCase("immer") || OP.getOptionInt("Laufzeit", -1)>500){
				// ab 1 iterieren!! nicht null!
				for (int i = 1; i <= (super.getArgCount() - 1); i++) {
					String actArg = super.getArgAt(i);
					if (!actArg.toLowerCase().startsWith("laufzeit") && !actArg.toLowerCase().startsWith("start")) {
						Orderline = Orderline + " " + actArg;
					}
				}
				int actGameRunde = super.scriptUnit.getScriptMain().gd_ScriptMain.getDate().getDate();
				int startRunde = OP.getOptionInt("Start", actGameRunde);
				if (OP.getOptionInt("Laufzeit", -1)>500 && startRunde<= actGameRunde) {
					confirmUntil = OP.getOptionInt("Laufzeit", -1);
					// Ist die Laufzeit der ACK noch g�ltig?
					if (confirmUntil >= actGameRunde) {
						// Setze Befehle... und damit auch adjusted auf true;
						this
						.addMyOrder(
								Orderline,
										"ACK noch "
										+ (confirmUntil	- actGameRunde + 1)
										+ " Runde(n) Laufzeit");
						
					} else {
						// Wenn confirmUnitl >= null war
						// der confirm-parameter eine g�ltige zahl.
						if (confirmUntil >= 0) {
							super
									.addComment("Unbest�tigt durch Script ACK: Laufzeit abgelaufen");
							super.scriptUnit.doNotConfirmOrders();
						}

					}
				}

				// es scheint wohl doch immer zu sein....
				else {
					// alt: super.addOrder(Orderline + " ; ACK Zombie!", true);
					if (startRunde>actGameRunde) {
						// noch nicht gestartet
						super.addComment("ACK zuk�nftig: " + Orderline);
					} else {
						this.addMyOrder(Orderline, "ACK Zombie!");
						super
								.addComment("Warnung: Diese Einheit wird durch ACK immer best�tigt!");
						super.addOutLine("Einheit "
								+ super.scriptUnit.getUnit().toString(false)
								+ " nutzt ACK mit Laufzeit=immer");
					}
				}

			}
			// ok, alle parameter sind wohl f�r die Einheit! Oder steckt
			// vielleicht ein versauter Parameter drin?

			else {
				// Alle Argumente werden zu einem String geaddet, der vorher
				// "" gesetzt wird.
				Orderline = "";
				for (int i = 0; i <= (super.getArgCount() - 1); i++) {
					Orderline = Orderline + " " + super.getArgAt(i);
				}

				// check, ob Laufzeit angegeben wurde, dann Fehler
				if (Orderline.toLowerCase().indexOf("laufzeit")>0){
					//	ein = bedeutet einen Tippfehler in Laufzeit=Parameter
					super.scriptUnit.doNotConfirmOrders();
					super.addComment("Einheit unbest�tigt durch ACK: unbekannte Laufzeit.");
				} else {
					// Jetzt setze die Order!
					// alt: super.addOrder(Orderline + " ; ACK", true);
					this.addMyOrder(Orderline, "ACK");
					super.addComment("Hinweis: Einheit wird durch ACK alleine nicht best�tigt");
					super.scriptUnit.setUnitOrders_adjusted(false);
				}
			}
		}
		// Tja ein Leere ACK solls ja auch geben..
		else {
			super.addOutLine("Skript ACK ohne Parameter: "
					+ super.scriptUnit.getUnit().getID());
			super.addComment("Unbest�tigt durch ACK: Keine Parameter!");
			super.scriptUnit.doNotConfirmOrders();
		}

	}
	
	/**
	 * erg�nzt unterst�tzung f�r scriptbefehle als argument
	 * @param orderline
	 * @param comment
	 */
	private void addMyOrder(String orderline,String comment){
		if (orderline.substring(0,1).equalsIgnoreCase(" ")){
			orderline = orderline.substring(1);
		}
		if (orderline.toLowerCase().startsWith("script")){
			// ok, wir sollen ein script basteln...erstmal als komment anzeigen
			super.addComment(orderline + " ; " + comment, true);
			// orderline aufsplitten....test: versehentliche spaces eleminieren
			orderline = orderline.replace("   ", " ");
			orderline = orderline.replace("  ", " ");
			String[] s = orderline.split(" ");
			// [0] -> "script"
			// [1] -> scriptname
			// [2+] -> parameter
			String newOrderLine = "";
			if (s.length>2){
				for (int i = 2;i<s.length;i++){
					newOrderLine = newOrderLine.concat(s[i] + " ");
				}
			}
			super.scriptUnit.findScriptClass(s[1], newOrderLine,true);
		} else {
			// alles normal...Vorgehen wie sonst auch
			super.addOrder(orderline + " ; " + comment, true);
		}
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