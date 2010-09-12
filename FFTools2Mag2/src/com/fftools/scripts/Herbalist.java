package com.fftools.scripts;

import java.util.Iterator;
import java.util.List;

import magellan.library.Item;
import magellan.library.Unit;
import magellan.library.rules.ItemType;

import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

/**
 * 
 * Job "Herbalist": Collects herbs and passes them to transporters
 * Call: // script Herbalist [Menge=<int> [, Ziel=<UnitID>]]
 * - Parameter Menge: Maximum amount of herbs to stock up
 * - Parameter Ziel: Unit to give the herbs to
 * - Description: The unit collects the local herbs until it has a stock of <Menge>, 
 *   then it learns stealth. If the unit with ID <UnitID> is in the region, the 
 *   herbalist gives all the herbs to that unit and collects more.     
 * - Required Items: none
 * - Optional Items: Kr�uterbeutel 
 * 
 * @author Torsten
 *
 */

public class Herbalist extends MatPoolScript{

	// Durchlauf vor Lernfix, wg. Lernplan
	// TODO: TH: Brauchen wir wirklich einen Lernplan daf�r? Erstmal wird nur Tarnung gelernt.
	private static final int Durchlauf = 5;

	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Herbalist() {
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
		
		// 1. Parameter auslesen und Rahmenbedingungen ermitteln 
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		int targetHerbAmount = OP.getOptionInt("menge", 0);
		int currentHerbAmount = 0;
		int lastWeekHerbAmount = 0;
		String targetUnit = OP.getOptionString("ziel");

		// Welches Kraut w�chst hier?
		ItemType herb = this.scriptUnit.getUnit().getRegion().getHerb();
		
		// Wieviele dieser Kr�uter hat der Herbalist aktuell vorr�tig?
		Item actItem = this.scriptUnit.getUnit().getItem(herb);
		if (actItem!=null){
			currentHerbAmount = actItem.getAmount();
		}

		// Und wieviele waren es in der Vorwoche (aus der privaten Beschreibung)?
		List<String> comments = this.scriptUnit.getUnit().getComments();
		// Wenn noch keine Vorwoche bekannt, negativ initialisieren, damit man's merkt!
		if (comments==null) {
				lastWeekHerbAmount = -1;
		} else {
			for (Iterator<String> iter = comments.iterator();iter.hasNext();){
				String comment = iter.next();
				if (comment.indexOf("krautvorrat=") > -1) {
					lastWeekHerbAmount = Integer.parseInt(comment.substring(comment.indexOf("FFT-ARGS: ")+10));
				}
			}
		}

		// 2. Wie lief das Pfl�cken?
		int makeMore = 1;
		int diff = currentHerbAmount-lastWeekHerbAmount;
		// Ist das die erste Pfl�ck-Woche? Falls ja ist Null-Differenz irrelevant 
		if (lastWeekHerbAmount == -1) diff=999;
		// Differenz negativ? - Meldung; Differenz umdrehen
		if (diff < 0) {
			this.addComment("WARNUNG: Kr�utervorrat erniedrigt. �bergabe? Verfall?");
			diff = lastWeekHerbAmount+diff;
		}
		// Differenz 0-2 - Warnung, noconfirm, nicht mehr MACHEN
		if (diff <= 2) {
			this.addOrder("FORSCHEN KR�UTER", false);
			this.addComment("WARNUNG: Sehr wenig geerntet. Region pr�fen, auf Forschen umgestellt!");
			this.doNotConfirmOrders();
			makeMore = 0;
		}
		
		// 3. Ist ein Abholer definiert, und falls ja, ist er schon eingetroffen?
		if (targetUnit!=null) {
			Unit u = FFToolsGameData.getUnitInRegion(this.region(),targetUnit);
			if (u!=null) {
				this.scriptUnit.getUnit().addOrders("GIB " + u.toString(false) + " KR�UTER ;dnt",false);
				// und damit haben wir keine Kr�uter mehr vorr�tig!
				currentHerbAmount=0;
			} else {
				// Transporter ist ausdr�cklich gew�nscht, das Depot bekommt nix!
				this.scriptUnit.setGibNix(true);
			}
		}
		
		// 4. Dann mach mal mehr Kr�uter, oder auch nicht... falls nicht, lerne Tarnung
		int makeHerbAmount = targetHerbAmount - currentHerbAmount;
		if (makeMore==1) {
			if (makeHerbAmount>0) {
				this.scriptUnit.getUnit().addOrders("MACHEN " + makeHerbAmount + " KR�UTER", false);
			} else {
				this.scriptUnit.findScriptClass("Lernfix", "Talent=Tarnung");
			}
		}

		// 5. Und zum Schlu� merken wir uns, wieviel Kr�uter wir aktuell haben 
		this.scriptUnit.getUnit().addOrders("BESCHREIBEN PRIVAT \"krautvorrat=" + currentHerbAmount + "\"", false);
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
}
