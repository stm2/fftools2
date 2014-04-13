package com.fftools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import magellan.client.Client;
import magellan.client.event.UnitOrdersEvent;
import magellan.library.Item;
import magellan.library.Order;
import magellan.library.Region;
import magellan.library.Ship;
import magellan.library.Skill;
import magellan.library.StringID;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.gamebinding.MovementEvaluator;
import magellan.library.io.cr.CRParser;
import magellan.library.rules.ItemType;
import magellan.library.rules.Race;
import magellan.library.rules.RegionType;
import magellan.library.rules.SkillType;

import com.fftools.overlord.Overlord;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.relations.MatPoolOffer;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Rekrutieren;
import com.fftools.scripts.Script;
import com.fftools.scripts.Setlernplan;
import com.fftools.scripts.Settrankorder;


/**
 * 
 * @author Fiete
 *
 * wird angelegt fuer jede durch FfTools bearbeitet Einheit
 * merkt sich u.a., ob bereits "alte" orderzeilen geloescht worden sind
 */
public class ScriptUnit {
	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	private Unit unit = null;
	private boolean NotNeededOrdersDeleted = false;
	private ArrayList<String> originalScriptOrders = null;
	
	private boolean builtfoundScriptList = false;
	private ArrayList<Script> foundScriptList = null;
	
	// workaround, da nach rekrutiere nicht meht modified persons neu berechnet werden
	private int recruitedPersons = 0;
	
	// wird von addComment und addOrder auf true gesetzt
	// und dann wird das orderchangeevent gefeuert beim letzten durchlauf
	private boolean orders_changed = false;
	
	// jedes script hat die chance, diesen wert auf false zu setzen
	// dann wird diese unit NICHT bestaetigt
	private boolean unitOrders_may_confirm = true;
	
	// jedes script, welches einen Befehl vollst�ndig erfolgreich umsetzt
	// kann diesen Wert auf true setzen
	// ist nach allen Scripten der Wert immer noch false
	// hat gar kein script gegriffen und die units sollte NICHT
	// best�tigt werden
	// wird bei addOrder immer gesetzt
	private boolean unitOrders_adjusted = false;
	
	// um auf die Pools zuzugreifen brauchen wir eine Referenz auf ScriptMain
	private ScriptMain scriptMain = null;
	
	// Referenz auf den Client
	private Client c = null; 

	// Sondereinsetllung f�r MatPool: darf Einheit was von sich weg geben?
	private boolean gibNix = false;
	
	
	
	// wenn scripte andere scripte aufrufen, k�nnen die nicht 
	// sofort in die scriptLsite eingetragen werden..da l�uft ja
	// bei runscript der Iterator dr�ber
	// gibt eine java.util.ConcurrentModificationException
	// daher hier eine private List..die nach dem durchlauf angef�gt wird
	// und vor dem Durchlauf gecleart wird
	private ArrayList<Script> addScriptList = null;
	
	private ArrayList<Script> delScriptList = null;
	
	/**
	 * Freie Kapazit�t der Unit, wird von MatPool genutzt
	 */
	private int originalFreeKapaFood = -1;
	private int originalFreeKapaHorse = -1;
	private int originalFreeKapaUser = -1;
	private int originalFreeKapaUserWeight = -1;
	private int originalModifiedLoad = 0;
	private int usedKapa = 0;
	private int setKapaPolicy = MatPoolRequest.KAPA_unbenutzt;
	
	/**
	 * Ausschliesslich f�r Kapit�ne mit gewicht=schiff
	 * Dann wird auch dass Gewicht von Einheiten mit // onboard bzw // Crew
	 * ber�cksichtigt
	 */
	private boolean includeSailorsWeight = false;
	
	// MatPool2
	/**
	 * enth�lt die Items nach gesch�tzten Ordern und Matpoollauf/l�ufen
	 */
	private HashMap<ItemType, Item> modifiedItemsMatPool2 = null;
	
	
	private boolean isDepotUnit = false;
	private boolean setDepotStatus = false;
	
	public ScriptUnit(Unit _u,ScriptMain _scriptMain) {
		this.unit = _u;
		this.scriptMain = _scriptMain;
	}
	
	/**
	 * Liefert true, wenn nicht benoetigte Orderzeilen bereits geloescht worden, sonst false
	 * 
	 * @return
	 */
	
	public boolean areNotNeededOrdersDeleted() {
		return NotNeededOrdersDeleted;
	}
	
	public void setClient(Client _c){
		this.c = _c;
	}
	
	
	/**
	 * 
	 * Loescht alle orderzeilen bis auf scriptzeilen (mit //)
	 * und welche mit do_not_touch
	 * und welche mit @ vorne
	 * 
	 * @return int Anzahl der geloeschten orderzeilen
	 */
	
	public int deleteNotNeededOrders(){
		int cnt =0;
		if (this.unit == null) {NotNeededOrdersDeleted = false;return -1;}
		ArrayList<Order> newOrders = new ArrayList<Order>(1);
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.startsWith("//")){
				// Kommentare beibehalten
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toLowerCase().indexOf("do_not_touch")>0 || s.toLowerCase().indexOf(";dnt")>0) {
				// nicht anfassen!
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.startsWith("@")){
				// permanente orders...
				newOrders.add(this.getUnit().createOrder(s));
			} else {
				cnt++;
			}
		}
		this.unit.setOrders2(newOrders);
		NotNeededOrdersDeleted = true;
		
		// this.unit.getRegion().refreshUnitRelations(true);
		
		// hat diese Unit durch ein Script (rekrutieren) TEMPs angelegt?
		if (this.unit.tempUnits()!=null && this.unit.tempUnits().size()>0){
			ArrayList<TempUnit> Temps4Deletion = new ArrayList<TempUnit>();
			for (TempUnit t:this.unit.tempUnits()){
				if (t.getOrders2()!=null && t.getOrders2().size()>0){
					for (Order o:t.getOrders2()){
						String s = o.getText();
						if (s.equals(Rekrutieren.scriptCreatedTempMark)){
							// bingo!!
							Temps4Deletion.add(t);
							break;
						}
					}
				}
			}
			
			// Jetzt l�schen
			if (Temps4Deletion.size()>0){
				for (TempUnit t:Temps4Deletion){
					this.addComment("L�sche vorher angelegte Temp Unit: " + t.getID().toString());
					this.unit.deleteTemp(t.getID(), this.scriptMain.gd_ScriptMain);
					ScriptUnit suDel = this.scriptMain.getScriptUnit(t);
					if (suDel!=null){
						this.getOverlord().deleteScriptUnit(suDel);
					} else {
						this.addComment("!!! Zu l�schende Unit nicht in ScriptUnis!!!(" + t.getID().toString() + ")(" + this.unitDesc()+")");
					}
				}
			}
			
			
		}
		return cnt;
	}
	
	/**
	 * 
	 * Loescht alle orderzeilen bis auf scriptzeilen (mit //)
	 * und welche mit do_not_touch
	 * und welche mit @ vorne
	 * und beschr�nkt sich auf orders, die mit ordersStartWith beginnen
	 * 
	 * @return int Anzahl der geloeschten orderzeilen
	 */
	
	public int deleteSomeOrders(String ordersStartWith){
		int cnt =0;
		if (this.unit == null) {NotNeededOrdersDeleted = false;return -1;}
		ArrayList<Order> newOrders = new ArrayList<Order>(1);
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.startsWith("//")){
				// Kommentare beibehalten
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toLowerCase().indexOf("do_not_touch")>0 || s.toLowerCase().indexOf(";dnt")>0) {
				// nicht anfassen!
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toLowerCase().indexOf(";script")>0) {
				// nicht anfassen!
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.startsWith("@")){
				// permanente orders...
				newOrders.add(this.getUnit().createOrder(s));
			} else if (!(s.toLowerCase().startsWith(ordersStartWith.toLowerCase()))){
				// orders, die nicht mit ordersStartWith beginnen
				newOrders.add(this.getUnit().createOrder(s));
			} else {
				cnt++;
			}
		}
		if (cnt>0){
			this.unit.setOrders2(newOrders);
		}
		// this.unit.getRegion().refreshUnitRelations(true);
		return cnt;
	}
	
	
	/**
	 * 
	 * Loescht alle orderzeilen bis auf scriptzeilen (mit //)
	 * und welche mit do_not_touch
	 * und welche mit @ vorne
	 * und beschr�nkt sich auf orders, die mit ordersStartWith beginnen
	 * 
	 * @return int Anzahl der geloeschten orderzeilen
	 */
	
	public boolean hasOrder(String orderStartWith){
		if (this.unit.getOrders2()==null || this.unit.getOrders2().size()==0){
			return false;
		}
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if ((s.length()>=orderStartWith.length()) &&  s.substring(0, orderStartWith.length()).equalsIgnoreCase(orderStartWith)){
				return true;
			} 
		}
		return false;
	}
	
	
	public void saveOriginalScriptOrders(){
		if (!NotNeededOrdersDeleted) {deleteNotNeededOrders();}
		originalScriptOrders = new ArrayList<String>(1);

		// sichern der urspruenglichen // script orders falls durch scripte selbst
		// welche hinzugefuegt werden...die sollen nicht abgearbeitet werden
		// (Erg�nzung: in dieser runDurchlauf-runde)
		// und wuerden ausserdem den Iterator ueber u.orders stoeren...
		
		for(Iterator<Order>iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script ")) {
				String s2 = s.substring(10);
				originalScriptOrders.add(s2);
			}
		}
	}
	
	/**
	 * 
	 * Stellt sicher, dass andere Orders als // geloescht worden sind
	 * 
	 * Stellt sicher, dass die OriginalScriptorders gesichert sind
	 * und das die scriptListe erstellt worden ist
	 * 
	 * durchlaeuft die scripte der unit und ruft sie mit dem entsprechenden
	 * Durchlauf int auf
	 * 
	 * @param scriptDurchlauf
	 */
	
	public void runScripts(int scriptDurchlauf){
		
		if (this.unit==null){
			return;
		}
		if (this.getOverlord().isDeleted(this)){
			return;
		}
		if (!NotNeededOrdersDeleted){deleteNotNeededOrders();}
		if (this.getOverlord().isDeleted(this)){
			return;
		}
		if (originalScriptOrders==null){saveOriginalScriptOrders();}
		if (!builtfoundScriptList) {builtScriptList();}
		
		
		
		if (foundScriptList==null){
			outText.addOutLine("keine Scripte gefunden: " + unit.toString(true));
			return;
		}
		
		for (Iterator<Script> iter = this.foundScriptList.iterator();iter.hasNext();){
			Script sc = (Script)iter.next();
			if (isInRegionSelected(this.unit.getRegion()) || isAllwaysRunScript(sc)){
			   if (sc.shouldRun(scriptDurchlauf)){	
				   sc.runScript(scriptDurchlauf);
			   }
			}
		}
		
		// Iterator ist durch..jetzt eventuelle script anh�ngen
		if (this.addScriptList!=null && this.addScriptList.size()>0){
			this.foundScriptList.addAll(this.addScriptList);
			this.addScriptList.clear();
		}
		
		this.checkOrderRefresh();
	}
	
	/**
	 * �berpr�ft, ob dieses script ausgef�hrt werden soll, egal ob
	 * in selektierten regionen oder nicht
	 * @param s
	 * @return
	 */
	private boolean isAllwaysRunScript(Script s){
		boolean erg=false;
		if (s.getClass().equals(Setlernplan.class)){
			return true;
		}
		if (s.getClass().equals(Settrankorder.class)){
			return true;
		}
		
		return erg;
	}
	
	/**
	 * f�gt ein weiteres script der scriptliste hinzu
	 * gedacht f�r aufrufe aus anderen scripten
	 * @param s
	 */
	public void addAScript(Script s){
		
		if (this.addScriptList==null){
			this.addScriptList = new ArrayList<Script>(1);
		}
		this.addScriptList.add(s);
	}
	
	/**
	 * f�gt ein weiteres script der scriptliste hinzu
	 * gedacht f�r aufrufe aus anderen scripten
	 * Scripte dieser Unit d�rfen nicht gerade abgearbeitet werden!
	 * @param s
	 */
	public void addAScriptNow(Script s){
		this.foundScriptList.add(s);
	}
	
	/**
	 * entfernt ein script der scriptliste
	 * gedacht f�r aufrufe aus anderen scripten
	 * @param s
	 */
	public void delAScript(Script s){
		
		if (this.delScriptList==null){
			this.delScriptList = new ArrayList<Script>(1);
		}
		this.delScriptList.add(s);
	}
	
	
	/**
	 * falls sich orders irgendwann seit letztem Refresh geaendert haben
	 * und ein client - refernz vorhanden ist
	 * feuert das OrdersChangeEvent fuer die unit
	 */
	
	public void checkOrderRefresh() {
		if (this.orders_changed && this.c!=null) {
			this.c.getMagellanContext().getEventDispatcher().fire(new UnitOrdersEvent(this, this.unit));
			this.orders_changed = false;
		}
	}

	/**
	 * Baut die Liste der erkannten scripte
	 *
	 */
	public void builtScriptList(){
		ArrayList<String> arguments = null;
		String scriptName = null;
		if (this.unit==null){
			return;
		}
		if (originalScriptOrders==null){saveOriginalScriptOrders();}
		for (Iterator<String> i2 = originalScriptOrders.iterator();i2.hasNext();){
			String order = (String)i2.next();
			scriptName = null;
			arguments=null;
			StringTokenizer st = new StringTokenizer(order);
			int i = 0;
		     while (st.hasMoreTokens()) {
		         // println(st.nextToken());
		    	 if (i==0) {
		    		 // nach // script steht der scriptname
		    		 scriptName = st.nextToken();
		    	 } else {
		    		 // ansonsten folgen Parameter
		    		 if (arguments==null){arguments = new ArrayList<String>(1);}
		    		 arguments.add(st.nextToken());
		    	 }
		    	 i++;
		     }
		     if (scriptName!=null){
		    	 // wenn keine Ausnahme, script finden
		    	 boolean noScript = false;
		    	 
		    	 if (scriptName.equalsIgnoreCase("setprio")){
		    		 noScript = true;
		    	 }
		    	 
		    	 if (scriptName.equalsIgnoreCase("setscripteroption")){
		    		 noScript = true;
		    	 }
		    	 if (scriptName.equalsIgnoreCase("setscripteroption2")){
		    		 noScript = true;
		    	 }
		    	 
		    	 if (!noScript){
			    	 // Spannung...classe finden
			    	 this.findScriptClass(scriptName, arguments,false);
		    	 }
		     } else {
		    	 // es kam nuescht hinter script
		    	 // Fehlermeldung
		    	 outText.addOutLine("Kein Scriptbefehl erkannt");
		     }
		}
		
		
		// extra f�r Marc..Lohn hinzuf�gen
		// ist nur erfolgreich, wenn nicht bereits explizit
		// Lohnscript in den befehlen angefordert wurde
		if (reportSettings.getOptionBoolean("Lohn", this.unit.getRegion())) {
		  	
			this.findScriptClass("Lohn","",false);
		}
		
		// Geb�udeunterhalt benutzen k�nnen
		
		if (reportSettings.getOptionBoolean("Gebaeudeunterhalt", this.unit.getRegion())) {
		     
			   this.findScriptClass("Gebaeudeunterhalt","",false);
	    }
		
		
		
		builtfoundScriptList = true;
	}
	
	public void doNotConfirmOrders() {
		this.unitOrders_may_confirm = false;
	}
	
	public void ordersHaveChanged() {
		this.orders_changed=true;
	}
	
	/**
	 * ueberprueft, ob eines der scripte 
	 * unitOrders_may_confirm via public void doNotConfirmOrders()
	 * auf false gesetzt hat, anderenfalls wird die einheit bestaetigt
	 * anderenfalls wird sie NICHT bestaetigt
	 * 
	 */
	public void setFinalConfim() {
		// gibts lange befehle mit ;dnt
		// nur pr�fen wenn notwendig // immer!
		// if (!this.unitOrders_adjusted){
			this.checkForLongOrder();
		// }
		if (this.unitOrders_adjusted) {
			this.unit.setOrdersConfirmed(this.unitOrders_may_confirm);
		} else {
			this.unit.setOrdersConfirmed(false);
		}
	}
	
	/**
	 * �berpr�ft, ob ein langer befehl �berhaupt vorhanden ist
	 *
	 */
	private void checkForLongOrder(){
		boolean may_confirm = false;
		// alle order durchlaufen
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order actOrder = (Order)iter.next();
			if (actOrder.isLong()){
				// jo..lange order
				may_confirm = true;
				// schleife kann verlassen werden, ein treffer reicht
				break;
			}
		}
		
		
		// Ozeancheck
		if (!may_confirm){
			// trifft nur zu, wenn auf Ozean und nicht MM
			Race mmRace = this.scriptMain.gd_ScriptMain.rules.getRace("Meermenschen", false);
			RegionType ozeanRegionType = this.scriptMain.gd_ScriptMain.rules.getRegionType("Ozean",false);
			Ship ship = this.unit.getShip();
			boolean isCaptain = false;
			if (ship!=null && ship.getOwnerUnit().equals(this.unit)){
				isCaptain=true;
			}
			if (mmRace != null && 
				  !mmRace.equals(this.unit.getRace()) &&
				  ozeanRegionType != null &&
				  ozeanRegionType.equals(this.unit.getRegion().getRegionType())
				  && !isCaptain){
				// kein MM
				// und auf dem Ozean
				// darf eh keinen langen Befehl ausf�hren
				// darf best�tigt werden
				may_confirm = true;
				this.addComment("Nicht-MM auf Ozean: best�tigt.");
			}
		}
		
		if (may_confirm){
			this.setUnitOrders_adjusted(true);
		} else {
			// im Zweifel hier abbrechen
			this.addComment("KEIN langer Befehl erkannt!");
			this.doNotConfirmOrders();
		}
	}
	
	
	
	public Unit getUnit(){
		if (this.unit==null) {return null;} else {return this.unit;}
	}
	
	
	// Vollstaendigkeit halber 
	public void setScriptMain(ScriptMain _scriptMain){
		this.scriptMain = _scriptMain;
	}
	
	public ScriptMain getScriptMain() {
		return this.scriptMain;
	}
	
	/**
	 * parst die orders nochmal und sucht nach optionalen konfig eintr�gen
	 * 
	 * 
	 */
	
	public void readReportSettings(){
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script setitemgroup")) {
				String s2 = s.substring(23);
				// ToDo: in ReportSettings verschieben
			
				
				
				StringTokenizer st = new StringTokenizer(s2);
				int i = 0;
				String catName = "";
				String itemTypeName = "";
				String stringPrio = "";
				 while (st.hasMoreTokens()) {
				     // println(st.nextToken());
					 switch (i){
					 case 0:catName = st.nextToken();break;
					 case 1:itemTypeName = st.nextToken();break;
					 case 2:stringPrio = st.nextToken();break;
					 }
					 i++;
				 }
			     int intPrio = Integer.parseInt(stringPrio);
			     
			     // platzhalter _ durch space ersetzen
				 itemTypeName = itemTypeName.replace("_", " ");
	 
			     reportSettings.setData(catName, itemTypeName, intPrio);
			}
			if (s.toLowerCase().startsWith("// script setscripteroption")) {
				String s2 = s.substring(28);
				reportSettings.parseOption(s2,this.getUnit(),true);
			}
			String findKey = "// script setscripteroption2 ";
			if (s.toLowerCase().startsWith(findKey)) {
				String s2 = s.substring(findKey.length());
				reportSettings.parseOption(s2,this.getUnit(),false);
			}
		}
	}
	/**
	 * �berpr�ft, ob angegebe Unit als gescriptet erkannt wird
	 * nur dann wird sie Grundlage einer ScriptUnit
	 * 
	 * @param u Die Unit
	 * @return wahr, wenn unit als gescriptet erkannt
	 */
	public static boolean isScriptUnit(Unit u){
		// orders nach // script eintr�gen durchsuchen
		for(Iterator<Order> iter = u.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script ")) { return true;}
		}

		return false;
	}
	
	/**
	 * �berpr�ft, ob angegebe Unit als Depot erkannt wird
	 * 
	 * @param u Die Unit
	 * @return wahr, wenn unit als gescriptet erkannt
	 */
	public static boolean isDepot(Unit u){
		// orders nach // script Depot eintr�gen durchsuchen
		for(Iterator<Order> iter = u.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script depot")) { return true;}
		}
		return false;
	}
	
	public boolean isDepot(){
		if (!this.setDepotStatus){
			this.isDepotUnit=ScriptUnit.isDepot(this.unit);
			this.setDepotStatus = true;
		}
		return this.isDepotUnit; 
	}
	
	
	
	/**
	 * Ersetzt umlaute
	 * alle buchstaben klein bis auf den ersten
	 * @param s
	 * @return
	 */
	private String cleanReportString(String s){
		
		if (s == null){
			return "";
		}
		
		String erg = s;
		//		 alles klein schreiben
	   	 erg = erg.toLowerCase();
	   	 // umlaute ersetzen
	   	 erg = erg.replaceAll("�","oe");
	   	 erg = erg.replaceAll("�","ue");
	   	 erg = erg.replaceAll("�","ae");
	   	 erg = erg.replaceAll("�","ss");
	   	 // erster Buchstabe gross
	   	 if (erg.length()>0){
	   		 erg = erg.substring(0,1).toUpperCase() + erg.substring(1);
	   	 }
		return erg;
	}
	/**
	 * Vereinfachung f�r den Aufruf aus anderen Scripten
	 * keine Parameter
	 * while running = true...
	 * @param scriptName
	 */
	public void findScriptClass(String scriptName){
		this.findScriptClass(scriptName, "");
	}
	
	
	/**
	 * Vereinfachung f�r den Aufruf aus anderen Scripten...
	 * Die Parameter k�nnen einfach �bergeben werdeb
	 * ArrayList wird erstellt....
	 * @param scriptName
	 * @param args
	 */
	public void findScriptClass(String scriptName,String args){
		StringTokenizer st = new StringTokenizer(args);
		ArrayList<String> arguments = null;
	     while (st.hasMoreTokens()) {
	    	 if (arguments==null){
	    		 arguments = new ArrayList<String>(1);
	    	 }
    		 arguments.add(st.nextToken());
	     }
	     this.findScriptClass(scriptName, arguments,true);
	}
	
	/**
	 * Vereinfachung f�r den Aufruf aus anderen Scripten...
	 * Die Parameter k�nnen einfach �bergeben werdeb
	 * ArrayList wird erstellt....
	 * @param scriptName
	 * @param args
	 */
	public void findScriptClass(String scriptName,String args,boolean whileRunning){
		StringTokenizer st = new StringTokenizer(args);
		ArrayList<String> arguments = null;
	     while (st.hasMoreTokens()) {
	    	 if (arguments==null){
	    		 arguments = new ArrayList<String>(1);
	    	 }
    		 arguments.add(st.nextToken());
	     }
	     this.findScriptClass(scriptName, arguments,whileRunning);
	}
	
	/**
	 * Versucht, angegebenes script als Klasse zu laden und 
	 * an die scriptList anzuf�gen
	 * 
	 * @param scriptName
	 */
	public void findScriptClass(String _scriptName,ArrayList<String> arguments,boolean whileRunning){
		
         // hier nun ne kleine Umsetzer-liste
		 // oder auch namenstransformation
		 // aktuelles Beispiel:
		 // tags(RegionDepot) soll auf // script Depot
		 // konvertiert werden
		 // machen wir mal ganz einfach..erstmal
		
		 if (_scriptName.equals("RegionDepot")){_scriptName = "Depot";}
	
		 // na dann mal paar Scriptsachen rausnehmen, die gar keine scriptsachen sind

		 if (_scriptName.equalsIgnoreCase("setitemgroup")) {return;}
		 if (_scriptName.equalsIgnoreCase("setScripterOption")) {return;}
		 
		 // zur Sicherheit nochmal den Namen clearen...
		 String scriptName = this.cleanReportString(_scriptName);
		 
		 Class<?> t = null;
	   	 Object o = null;
	   	 try {
	   		 t = Class.forName("com.fftools.scripts." + scriptName);
	   	 } catch (ClassNotFoundException e) {
	   		 outText.addOutLine("Klasse nicht gefunden: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   	 } catch (ExceptionInInitializerError e) {
	   		 outText.addOutLine("Klasse nicht initialisiert: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   	 } catch (LinkageError e) {
	   		 outText.addOutLine("Klasse nicht gelinkt: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   	 }
	   	 if (t!=null){
	   		 try {
	   			 o = t.newInstance();
	   		 } catch (IllegalAccessException e) {
	   			 outText.addOutLine("Konstruktor der Klasse nicht verfuegbar: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   		 } catch (InstantiationException e) {
	   			 outText.addOutLine("Klasse konnte nicht instanziert werden: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   		 }
	   	 }
	   	 if (o!=null) {
	   		 
	   		 // OK, wir haben einen Treffer .. ;-)
	   		 // Das Object auf Script.java casten
	   		 Script sc = (Script) o;
	   		 
	   		 // FF 20070103 beim Overlord registrieren
	   		 this.scriptMain.getOverlord().addOverlordInfo(sc);
	   		 
	   		 // checken, ob auf doppelte zu checken ist
	   		 if (!sc.allowMultipleScripts()){
	   			 // ok, wir m�ssen checken
	   			 // normale liste
	   			 if (this.foundScriptList!=null){
		   			 for (Iterator<Script> iter = this.foundScriptList.iterator();iter.hasNext();){
		   				 Script actSc = (Script) iter.next();
		   				 if (actSc.getClass().getName().equalsIgnoreCase(sc.getClass().getName())){
		   					 // Treffer...sowas haben wir schon
		   					 if (sc.errorMsgIfNotAllowedAddedScript()){
		   						 outText.addOutLine("info: nicht zugelassen weiterer eintrag von: " + sc.getClass().getName() + " bei " + this.getUnit().toString(true));
		   					 }
		   					 return;
						} 
		   			 }
	   			 }
	   			 // die besondere Liste addScr
	   			 if (this.addScriptList!=null){
		   			 for (Iterator<Script> iter = this.addScriptList.iterator();iter.hasNext();){
		   				 Script actSc = (Script) iter.next();
		   				 if (actSc.getClass().getName().equalsIgnoreCase(sc.getClass().getName())){
		   					 // Treffer...sowas haben wir schon
		   					 if (sc.errorMsgIfNotAllowedAddedScript()){
		   						 outText.addOutLine("info: nicht zugelassen weiterer eintrag von: " + sc.getClass().getName() + " bei " + this.getUnit().toString(true));
		   					 }	
		   					 return;
						} 
		   			 }
	   			 }
	   		 }
	   		 
	   		 // wichtige Daten uebergeben
	   		 sc.setScriptUnit(this);
	   		 // inkl Client, wenn wir einen haben
	   		 if (this.c!=null){sc.setClient(c);}
	   		 // aber GameData brauchen wir schon
	   		 if (this.scriptMain.gd_ScriptMain!=null){
	   			 sc.setGameData(this.scriptMain.gd_ScriptMain);
	   		 }
	   		 if (arguments!=null) {sc.setArguments(arguments);}
	   		 // Und nu anhaengen 
	   		 if (!whileRunning){
	   			if (foundScriptList==null) {
		   			 foundScriptList = new ArrayList<Script>(1);
		   		 }
	   			 foundScriptList.add(sc);
	   		 } else {
	   			 this.addAScript(sc);
	   		 }
	   	 }
	}

	

	/**
	 * @param unitOrders_adjusted the unitOrders_adjusted to set
	 */
	public void setUnitOrders_adjusted(boolean unitOrders_adjusted) {
		this.unitOrders_adjusted = unitOrders_adjusted;
	}
	
	/**
	 * Ersetzt gezielt eine script order durch die �bergeben
	 * wirksam erst bei n�chstem aufruf, da die script liste ja bereits gebaut worden ist
	 * Ob eine script order passend ist wird durch einen vergleich mit
	 * der new order erreicht, dazu wird angegeben, wieviele Zeichen verglichen werden sollen
	 * minimum daf�r ist "X".length
	 * @param newOrder
	 * @param compareLength
	 */
	public boolean replaceScriptOrder(String newOrder,int compareLength){
		// vorhandenen script eintrag l�schen
		if (this.removeScriptOrder(newOrder.substring(0,compareLength))) {
			// l�schen war erfolgreich
			// neuen Bauen...via order
			this.getUnit().addOrder("// script " + newOrder,false, 1);
			// this.getUnit().reparseOrders();
			this.ordersHaveChanged();
			this.setUnitOrders_adjusted(true);
			// fertig.
			return true;
		} else {
			// l�schen war nicht erfolgreich....zur Sicherheit false zur�ckgeben
			// und nicht erg�nzen
			return false;
		}
	}
	
	/**
	 * l�scht ein ein script order aus den orders der unit
	 * gel�scht werden alle orders, die nach // script  mit matchingChars beginnen
	 * not case sensitive
	 * @param matchingChars
	 */
	public boolean removeScriptOrder(String matchingChars){
		ArrayList<Order> newOrders = new ArrayList<Order>(1);
		ArrayList<Order> newComments = new ArrayList<Order>(1);
		boolean didSomething = false;
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			boolean toDelete = false;
			if (s.startsWith("// script ")){
				String checkS = s.substring("// script ".length()).toLowerCase();
				String checkMatchS = matchingChars.toLowerCase();
				if (checkS.startsWith(checkMatchS)){
					toDelete = true;
					didSomething = true;
				}
			}
			if (!toDelete){
				newOrders.add(this.getUnit().createOrder(s));
			} else {
				// kleiner Kommentar, dass gel�scht?
				newComments.add(this.getUnit().createOrder("; script order entfernt: " + s + "  do_not_touch"));
			}
		}
		if (didSomething){
			newOrders.addAll(newComments);
			this.unit.setOrders2(newOrders);
			// this.unit.reparseOrders();
		}
		return didSomething;
	}
	
	/**
	 * Findet in unit in der gleichen Region und gibt sie zur�ck
	 * bzw null, wenn nicht gefunden
	 * @param unitDesc Numer der Unit (kein Temp...(?))
	 * @return
	 */
	public Unit findUnitInSameRegion(String unitDesc){
        //	Die Region mal holen
		Region actR = this.getUnit().getRegion();
		boolean found = false;
		Unit actU = null;
		// units durchlaufen
		for (Iterator<Unit> iter = actR.units().iterator();iter.hasNext();){
			actU = (Unit)iter.next();
			// und pr�fen
			if (actU.toString(false).equalsIgnoreCase(unitDesc)){
				// erg�nzung...und sich selbst ausschliessen
				if (!this.getUnit().toString(false).equals(actU.toString(false))) {
					found = true;
					break;
				}
			}
		}
		
		if (found){
			return actU;
		} else {
			return null;
		}
	}
	
	/**
	 * liefert freie Kapa, keine Angabe einer Benutzer-Kapa
	 * @param kapaPolicy
	 * @return
	 */
	public int getFreeKapa(int kapaPolicy) {
		return this.getFreeKapa(kapaPolicy, 0);
	}
	
	/**
	 * 
	 * @return the freeKapa
	 */
	public int getFreeKapa(int kapaPolicy, int userKapa) {
		
		if (this.originalFreeKapaFood==-1){
			// kapa war noch nicht gesetzt
			
			MovementEvaluator ME = scriptMain.gd_ScriptMain.getGameSpecificStuff().getMovementEvaluator(); 
			this.originalFreeKapaFood = (int)Math.floor((ME.getPayloadOnFoot(this.getUnit()) - ME.getModifiedLoad(this.getUnit()))/100);
			this.originalFreeKapaHorse = (int)Math.floor((ME.getPayloadOnHorse(this.getUnit()) - ME.getModifiedLoad(this.getUnit()))/100);
			this.originalFreeKapaUser = userKapa;
			this.originalModifiedLoad = (int)Math.ceil(ME.getModifiedLoad(this.getUnit())/100);
		}
			// test debug:
			// this.unit.getRegion().refreshUnitRelations(true);
			// nur die unit m�sste reichen
			// this.unit.refreshRelations();
		switch (kapaPolicy){
		case MatPoolRequest.KAPA_unbenutzt:
			// eigentlich ein Fehler..wenn ich kapa nicht benutze
			// wozu brauche ich dann diese Angabe
			return -1;
		case MatPoolRequest.KAPA_max_zuFuss:
			int freeFood = this.originalFreeKapaFood - this.usedKapa;
			return Math.max(0, freeFood);
		case MatPoolRequest.KAPA_max_zuPferd:
			int freeHorse = this.originalFreeKapaHorse - this.usedKapa;
			return Math.max(0, freeHorse);
		case MatPoolRequest.KAPA_benutzer:
			int freeUser = this.originalFreeKapaUser - (this.originalModifiedLoad + this.usedKapa);
			return Math.max(0, freeUser);
		default:
			return -1;
		}
	}

	/**
	 * liefert wahr, wenn diese scriptUnit aktuell die 
	 * freeKapa schin berechnet hat
	 * @return
	 */
	public boolean freeKapaUsed(){
		boolean erg = false;
		if (this.originalFreeKapaFood!=-1){
			erg=true;
		}
		return erg;
	}
	
	
	
	/**
	 * reduziert die freie Kapa um den entsprechenden Betrag
	 * @param betrag
	 */
	public void decFreeKapa(int betrag){
		
		
		// erh�ht die genutzte Kapa
		this.usedKapa+=betrag;
	}


	public void resetFreeKapa(){
		
		
		this.originalFreeKapaFood = -1;
		this.usedKapa = 0;
		// kapaPolicy auch auf unbenutzt setzen?! (vermutlich nicht...)
		
	}
	
	/**
	 * F�gt einen Befehl zur Scriptunit hinzu
	 * und markiert, dass sie durch FFTools best�tigt werden kann
	 * @param s der Befehl
	 * @param no_doubles wenn wahr, wird nur hinzugef�gt, wenn nicht bereits eingleichlautender Befehl bei der Einheit existiert
	 */
	public void addOrder(String s,boolean no_doubles) {
		addOrder(s, no_doubles, true);
	}
	
	/**
	 * F�gt einen Befehl zur Scriptunit hinzu
	 * @param s der Befehl
	 * @param no_doubles wenn wahr, wird nur hinzugef�gt, wenn nicht bereits eingleichlautender Befehl bei der Einheit existiert
	 * @param unit_may_confirm wenn wahr wird das Flag gesetzt, dass die Unit duch FfTools best�tigt werden KANN
	 */
	public void addOrder(String s,boolean no_doubles,boolean unit_may_confirm) {
		boolean add_ok = true;
		
		if (no_doubles){
			for (Iterator<Order> i1 = this.getUnit().getOrders2().iterator();(i1.hasNext() && add_ok);){
				Order o = (Order)i1.next();
				String s_old = o.getText();
				if (s_old.equalsIgnoreCase(s)) {add_ok = false;}
			}
		}
		if (add_ok){
			// this.getUnit().addOrder(s,false, 1);
			this.getUnit().addOrder(s);
			// this.getUnit().reparseOrders();
			this.ordersHaveChanged();
			if (unit_may_confirm){
				this.setUnitOrders_adjusted(true);
			}			
		}
	}

	
	public void addComment(String s){
		addComment(s, true);
	}
	
	public void addComment(String s,boolean no_doubles){
		boolean add_ok = true;
		
		if (no_doubles){
			for (Iterator<Order> i1 = this.getUnit().getOrders2().iterator();(i1.hasNext() && add_ok);){
				Order o = (Order)i1.next();
				String s_old = o.getText();
				if (s_old.equalsIgnoreCase("; " + s)) {add_ok = false;}
			}
		}
		if (add_ok){
			// this.getUnit().addOrder("; " + s,false, 1);
			this.getUnit().addOrder("; " + s);
			// this.getUnit().reparseOrders();
			this.ordersHaveChanged();
		}
	}

	/**
	 * @return the foundScriptList
	 */
	public ArrayList<Script> getFoundScriptList() {
		return foundScriptList;
	}
	
	public String unitDesc(){
		return this.getUnit().toString(true) + " in " + this.getUnit().getRegion().toString(); 
	}

	public Overlord getOverlord(){
		return this.scriptMain.getOverlord();
	}
	
	public String toString(){
		return this.unitDesc();
	}
	
	public String getUnitNumber(){
		return this.getUnit().toString(false);
	}
	
	/**
	 * liefert die Anzahl der Effekte oder 0
	 * @param Name
	 * @return
	 */
	public int getEffekte(String Name){
		int erg = 0;
		Unit u = this.getUnit();
		if (u.getEffects()!=null && u.getEffects().size()>0){
			for (Iterator<String> iter = u.getEffects().iterator();iter.hasNext();){
				String effect = (String)iter.next();
				String[] pairs = effect.split(" ");
				if (pairs.length>0 &&  pairs[1].equalsIgnoreCase(Name)){
					try {
						Integer am = Integer.parseInt(pairs[0]);
						return am.intValue();
					} catch (NumberFormatException e){
						this.addComment("!*!unerwarteter Wert f�r Trank im CR");
						outText.addOutLine("!*!unerwarteter Wert f�r Trank im CR:" + this.unitDesc(),true);
						return 0;
					}
					
				}
			}
		}
		return erg;
	}
	
	/**
	 * 
	 * @param TagName  Name des Tags
	 * @param TagValue Wert des Tags
	 */
	public void putTag(String TagName,String TagValue){
		this.getUnit().putTag(TagName, TagValue);
	}
	
	/**
	 * liefert (erstes) vorkommen eines scriptes
	 * einer bestimmten Klasse
	 * wird null �bergeben, wird erstes (beliebiges)
	 * script zur�ckgegeben
	 * @param searchClass
	 * @return
	 */
	public Object getScript(Class<?> searchClass){
		// normale liste
		 if (this.foundScriptList!=null){
			 for (Iterator<Script> iter = this.foundScriptList.iterator();iter.hasNext();){
				 Script actSc = (Script) iter.next();
				 if (searchClass==null){
					 return actSc;
				 }
				 if (searchClass.isInstance(actSc)){
					 return actSc;
				} 
			 }
		  }
		 // die besondere Liste addScr
		 if (this.addScriptList!=null){
			 for (Iterator<Script> iter = this.addScriptList.iterator();iter.hasNext();){
				 Script actSc = (Script) iter.next();
				 if (searchClass==null){
					 return actSc;
				 }
				 if (searchClass.isInstance(actSc)){
					 return actSc;
				} 
			 }
		 } 
		return null;
	}

	/**
	 * @return the modifiedItemsMatPool2
	 */
	public HashMap<ItemType, Item> getModifiedItemsMatPool2() {
		return modifiedItemsMatPool2;
	}

	/**
	 * @param modifiedItemsMatPool2 the modifiedItemsMatPool2 to set
	 */
	public void setModifiedItemsMatPool2(
			HashMap<ItemType, Item> modifiedItemsMatPool2) {
		this.modifiedItemsMatPool2 = modifiedItemsMatPool2;
	}
	
	/**
	 * analog unit.getModifiedItem
	 * @param type : which ItemType
	 * @return the modified Item or NULL
	 */
	public Item getModfiedItemMatPool2(ItemType type){
		if (this.modifiedItemsMatPool2==null || this.modifiedItemsMatPool2.size()==0){
			return null;
		}
		return this.modifiedItemsMatPool2.get(type);
	}
	
	
	/**
	 * Fiete 20080305 taken from EresseaMovementElevator
	 * used by MatPool2 only!
	 * @return
	 */
	public int getPayloadOnHorse() {
		int capacity = 0;
		int horses = 0;
		Item i = null;
		/**
        Item i = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_HORSE));
			
		if(i != null) {
			horses = i.getAmount();
		}
		*/
		horses = countActHorses();
		if(horses <= 0) {
			return -1;
		}

		int skillLevel = 0;
		Skill s = this.unit.getModifiedSkill(new SkillType(StringID.create("Reiten")));

		if(s != null) {
			skillLevel = s.getLevel();
		}

		if(horses > (skillLevel * this.unit.getModifiedPersons() * 2)) {
			return -2;
		}

		int carts = 0;
		i = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_CART));

		if(i != null) {
			carts = i.getAmount();
		}

		int horsesWithoutCarts = horses - (carts * 2);

		Race race = getRace(this.unit);

		if(horsesWithoutCarts >= 0) {
			capacity = (((carts * 140) + (horsesWithoutCarts * 20)) * 100) -
					   (((int) ((race.getWeight()) * 100)) * unit.getModifiedPersons());
		} else {
			int cartsWithoutHorses = carts - (horses / 2);
			horsesWithoutCarts = horses % 2;
			capacity = (((((carts - cartsWithoutHorses) * 140) + (horsesWithoutCarts * 20)) -
					   (cartsWithoutHorses * 40)) * 100) -
					   (((int) ((race.getWeight()) * 100)) * unit.getModifiedPersons());
		}
		// Fiete 20070421 (Runde 519)
		// GOTS not active when riding! (tested)
		// return respectGOTS(unit, capacity);
		return capacity;
	}

	
	/**
	 * f�r den Matpool:
	 * anzahl modified
	 * wenn Depot in Region: minus noch offerierte Pferde
	 * Nur f�r MatPool2 !
	 * @return
	 */
	private int countActHorses(){
		int erg = 0;
		ItemType horseType = this.getScriptMain().gd_ScriptMain.rules.getItemType("Pferd",false);
		Item i = this.getModfiedItemMatPool2(horseType);
		if(i != null) {
			erg = i.getAmount();
		}
		MatPool MP = this.getOverlord().getMatPoolManager().getRegionsMatPool(this);
		if (MP!=null){
			ArrayList<MatPoolOffer> offers = MP.getOffers(this);
			if (offers!=null && offers.size()>0){
				for (MatPoolOffer actOffer : offers){
					if (actOffer.getItemType().equals(horseType)) {
						erg -=actOffer.getAngebot();
					}
				}
			}
		}
		return erg;
	}
	
	
	/**
	 * Returns the maximum payload in GE  100 of this unit when it travels on foot. Horses, carts
	 * and persons are taken into account for this calculation. If the unit has a sufficient skill
	 * in horse riding but there are too many carts for the horses, the weight of the additional
	 * carts are also already considered. The calculation also takes into account that trolls can
	 * tow carts.
	 *
	 * @param unit TODO: DOCUMENT ME!
	 *
	 * @return the payload in GE  100, CAP_UNSKILLED if the unit is not sufficiently skilled in
	 * 		   horse riding to travel on horseback.
	 */
	@SuppressWarnings("deprecation")
	public int getPayloadOnFoot() {
		int capacity = 0;
		int horses = 0;
		Item i = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_HORSE));

		if(i != null) {
			horses = i.getAmount();
		}

		if(horses < 0) {
			horses = 0;
		}

		int skillLevel = 0;
		Skill s = this.unit.getModifiedSkill(new SkillType(StringID.create("Reiten")));

		if(s != null) {
			skillLevel = s.getLevel();
		}

		if(horses > ((skillLevel * this.unit.getModifiedPersons() * 4) + this.unit.getModifiedPersons())) {
			// too many horses
			return -1;
		}

		int carts = 0;
		i = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_CART));

		if(i != null) {
			carts = i.getAmount();
		}

		if(carts < 0) {
			carts = 0;
		}

		int horsesWithoutCarts = 0;
		int cartsWithoutHorses = 0;

		if(skillLevel == 0) {
			// can't use carts!!!
			horsesWithoutCarts = horses;
			cartsWithoutHorses = carts;
		} else if(carts > (horses / 2)) {
			// too many carts
			cartsWithoutHorses = carts - (horses / 2);
		} else {
			// too many horses (or exactly right number)
			horsesWithoutCarts = horses - (carts * 2);
		}

		Race race = getRace(this.unit);

		if((race == null) || (race.getID().equals(EresseaConstants.R_TROLLE) == false)) {
			capacity = (((((carts - cartsWithoutHorses) * 140) + (horsesWithoutCarts * 20)) -
					   (cartsWithoutHorses * 40)) * 100) +
					   (((int) (race.getCapacity() * 100)) * this.unit.getModifiedPersons());
		} else {
			int horsesMasteredPerPerson = (skillLevel * 4) + 1;
			int trollsMasteringHorses = horses / horsesMasteredPerPerson;

			if((horses % horsesMasteredPerPerson) != 0) {
				trollsMasteringHorses++;
			}

			int cartsTowedByTrolls = Math.min((this.unit.getModifiedPersons() - trollsMasteringHorses) / 4,
											  cartsWithoutHorses);
			int trollsTowingCarts = cartsTowedByTrolls * 4;
			int untowedCarts = cartsWithoutHorses - cartsTowedByTrolls;
			capacity = (((((carts - untowedCarts) * 140) + (horsesWithoutCarts * 20)) -
					   (untowedCarts * 40)) * 100) +
					   (((int) (race.getCapacity() * 100)) * (this.unit.getModifiedPersons() -
					   trollsTowingCarts));
		}

		return respectGOTS(this.unit, capacity);
	}

	private int respectGOTS(Unit unit, int capacity) {
		Item gots = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_GOTS));

		if(gots == null) {
			return capacity;
		}

		int multiplier = Math.max(0, Math.min(unit.getModifiedPersons(), gots.getAmount()));
		Race race = getRace(unit);

		if((multiplier == 0) || (race == null)) {
			return capacity;
		}

		// increase capacity by 49*unit.race.capacity per GOTS
		return capacity + (multiplier * (49 * (int) (race.getCapacity() * 100)));
	}

	private Race getRace(Unit unit) {
		Race race = unit.getRace();

		if(unit.getDisguiseRace() != null) {
			race = unit.getDisguiseRace();
		}

		return race;
	}
	
	/**
	 * liefert freie Kapa, keine Angabe einer Benutzer-Kapa
	 * @param kapaPolicy
	 * @return
	 */
	public int getFreeKapaMatPool2(int kapaPolicy) {
		return this.getFreeKapaMatPool2(kapaPolicy, 0);
	}
	
	
	
	public int getModifiedLoad() {
		int load = this.getLoad();

		// also take care of passengers
		Collection<Unit> passengers = this.unit.getPassengers();
		MovementEvaluator ME = scriptMain.gd_ScriptMain.getGameSpecificStuff().getMovementEvaluator();

		for(Iterator<Unit> iter = passengers.iterator(); iter.hasNext();) {
			Unit passenger = (Unit) iter.next();
			// checken, ob das auch ne scriptUnit ist
			ScriptUnit passengerSU = this.getScriptMain().getScriptUnit(passenger);
			if (passengerSU==null){
				load += ME.getModifiedWeight(passenger);
			} else {
				load += passengerSU.getWeight();
			}
		}

		return load;
	}
	
	@SuppressWarnings("deprecation")
	private int getLoad() {
		int load = 0;
		ItemType horse = this.unit.getRegion().getData().rules.getItemType(EresseaConstants.I_HORSE);
		ItemType cart = this.unit.getRegion().getData().rules.getItemType(EresseaConstants.I_CART);
		if (this.modifiedItemsMatPool2!=null && this.modifiedItemsMatPool2.size()>0) {
			for(Iterator<Item> iter = this.modifiedItemsMatPool2.values().iterator(); iter.hasNext();) {
				Item i = (Item) iter.next();
	
				if(!i.getItemType().equals(horse) && !i.getItemType().equals(cart)) {
					// pavkovic 2003.09.10: only take care about (possibly) modified items with positive amount
					if(i.getAmount() > 0) {
						load += (((int) (i.getItemType().getWeight() * 100)) * i.getAmount());
					}
				}
			}
		}
		return load;
	}
	
	@SuppressWarnings("deprecation")
	private int getWeight(){
		int load = 0;
		// Pferde und Wagen erg�nzen
		ItemType horse = this.unit.getRegion().getData().rules.getItemType(EresseaConstants.I_HORSE);
		ItemType cart = this.unit.getRegion().getData().rules.getItemType(EresseaConstants.I_CART);
		if (this.modifiedItemsMatPool2!=null && this.modifiedItemsMatPool2.size()>0) {
			for(Iterator<Item> iter = this.modifiedItemsMatPool2.values().iterator(); iter.hasNext();) {
				Item i = (Item) iter.next();
				if(i.getItemType().equals(horse) || i.getItemType().equals(cart)) {
					// pavkovic 2003.09.10: only take care about (possibly) modified items with positive amount
					if(i.getAmount() > 0) {
						load += (((int) (i.getItemType().getWeight() * 100)) * i.getAmount());
					}
				}
			}
		}
		// andere Items
		load+=this.getLoad();
		// Gewicht der Personen
		int raceWeight =(int)((this.unit.getDisguiseRace() != null) ? this.unit.getDisguiseRace().getWeight() : this.unit.getRace().getWeight());
		load+=raceWeight *  this.unit.getModifiedPersons() * 100;
		
		return load;
	}
	
	
	/**
	 * 
	 * @return the freeKapa
	 */
	public int getFreeKapaMatPool2(int kapaPolicy, int userKapa) {	
		
		if (this.getSetKapaPolicy()!=MatPoolRequest.KAPA_unbenutzt){
			kapaPolicy=this.getSetKapaPolicy();
		}
		
		
		if (this.originalFreeKapaUser<0 && userKapa>=0 && kapaPolicy==MatPoolRequest.KAPA_benutzer){
			this.originalFreeKapaUser = userKapa;
		}
		if (this.originalFreeKapaUserWeight<0 && userKapa>=0 && kapaPolicy==MatPoolRequest.KAPA_weight){
			this.originalFreeKapaUserWeight = userKapa;
		}
		
		
		switch (kapaPolicy){
		case MatPoolRequest.KAPA_unbenutzt:
			// eigentlich ein Fehler..wenn ich kapa nicht benutze
			// wozu brauche ich dann diese Angabe
			return -1;
		case MatPoolRequest.KAPA_max_zuFuss:
			// int freeFood = this.originalFreeKapaFood - this.usedKapa;
			return (int)Math.floor(((double)this.getPayloadOnFoot() - (double)this.getModifiedLoad())/100);
		case MatPoolRequest.KAPA_max_zuPferd:
			return (int)Math.floor(((double)this.getPayloadOnHorse() - (double)this.getModifiedLoad())/100);
		case MatPoolRequest.KAPA_benutzer:
			return this.originalFreeKapaUser - (int)Math.ceil((double)this.getModifiedLoad()/100);
		case MatPoolRequest.KAPA_weight:
			if (!this.isIncludeSailorsWeight()){
				return this.originalFreeKapaUserWeight - (int)Math.ceil((double)this.getWeight()/100);
			} else {
				// Gewicht von weiteren Insassen auf dem Schiff miberechnen
				int erg = this.originalFreeKapaUserWeight;
				// wie gehabt, unser Gewicht abziehen
				erg -=  (int)Math.ceil((double)this.getWeight()/100);
				// Gewicht der anderen abziehen
				erg -= calcWeightOfSailors();
				return erg;
			}
		default:
			return -1;
		}
	}
	
	/**
	 * Berechnet das Gewicht der als Mitfahrer deklarierten Einheiten
	 * (// onboard oder // crew)
	 * @return
	 */
	private int calcWeightOfSailors(){
		int erg = 0;
		Ship ship = this.getUnit().getModifiedShip();
		if (ship==null){
			this.addComment("!!! Insassen des Schiffes sollen ber�cksichtigt werden, aber wir sind nicht auf einem Schiff!!");
			this.doNotConfirmOrders();
			return 0;
		}
		// Liste der Insassen
		Collection<Unit> insassen = ship.modifiedUnits();
		if (insassen==null || insassen.isEmpty()){
			this.addComment("!!! Insassen des Schiffes sollen ber�cksichtigt werden, aber es gibt keine Insassen!!");
			this.doNotConfirmOrders();
			return 0;
		}
		for (Unit u:insassen){
			// eigene Unit ausschliessen
			if (!u.equals(this.getUnit())){
				// orders durchgehen und die richtige finden
				boolean isCrew = false;
				for (Order o:u.getOrders2()){
					String s = o.getText();
					if (s.toLowerCase().startsWith("// onboard")){
						isCrew=true;
						break;
					}
					if (s.toLowerCase().startsWith("// crew")){
						isCrew=true;
						break;
					}
				}
				if (isCrew){
					// scriptUnit finden
					ScriptUnit otherSC = this.scriptMain.getScriptUnit(u);
					if (otherSC==null){
						// ok, wir haben keine ScriptUnit...dann das Gewicht so
						// abziehen
						MovementEvaluator EME=this.scriptMain.gd_ScriptMain.getRules().getGameSpecificStuff().getMovementEvaluator();
						int unitWeight = (int)Math.ceil(EME.getModifiedWeight(u)/100);
						erg+=unitWeight;
						this.addComment("Crew " + u.toString(true) + " ist keine ScriptUnit, das Gewicht wurde mit " + unitWeight + " GE calculiert.");
					} else {
						// ganz normale ScriptUnit
						int unitWeight = (int)Math.ceil((double)otherSC.getWeight()/100);
						erg+=unitWeight;
						this.addComment("Crew " + u.toString(true) + " wurde mit " + unitWeight + " GE calculiert.");
						otherSC.addComment("als Crew von " + ship.toString() + " ber�cksichtigt.");
					}
				} else {
					// Wir haben eine Unit an Board, die nicht Crew ist...
					// dass kann schief gehen
					this.addComment("!!! Hinweis: " + u.toString(true) + " ist an Bord und wurde nicht bei Kapa-Berechnung ber�cksichtigt.");
					this.doNotConfirmOrders();
				}
			}
		}
		return erg;
	}
	
	
	/**
	 * Setzt eine �nderung der Items um
	 * @param type Welcher ItemType
	 * @param amount Positiv: zunahme, Negativ: abnahme
	 */
	public void changeModifiedItemsMatPools(ItemType type, int amount){
		if (this.modifiedItemsMatPool2==null){
			if (amount>0){
				this.modifiedItemsMatPool2 = new HashMap<ItemType, Item>();
			} else {
				// hier wohl eigentlich bl�der fehler
				// nix da und trotzdem abziehen
				outText.addOutLine("!!!Versuch, einen nicht vorhanden Typ in der Menge zu reduzieren: " + unitDesc(), true);
				return;
			}
		}
		Item item = this.modifiedItemsMatPool2.get(type);
		if (item==null){
			// noch nicht vorhanden
			if (amount<0){
				// problem!
				outText.addOutLine("!!!Versuch, einen nicht vorhanden Typ in der Menge zu reduzieren: " + unitDesc(), true);
			} else {
				this.modifiedItemsMatPool2.put(type,new Item(type,amount));
			}
		} else {
			item.setAmount(item.getAmount() + amount);
		}
	}
	
	/**
	 * je nach MatPoolversion wird modifiedItems geliefert
	 * @param itemType
	 * @return
	 */
	public Item getModifiedItem(ItemType itemType){
		return this.getModfiedItemMatPool2(itemType);
	}
	
	private boolean isInRegionSelected(Region r){
		boolean erg = true;
		
		if (this.scriptMain.client==null){
			return true;
		}
		
		Client c = this.scriptMain.client;
		
		if (c.getSelectedRegions()!=null && c.getSelectedRegions().size()>0){
			if (!c.getSelectedRegions().values().contains(r)){
				erg=false;
			}
		}
		
		return erg;
	}

	/**
	 * @return the setKapaPolicy
	 */
	public int getSetKapaPolicy() {
		return setKapaPolicy;
	}

	/**
	 * @param setKapaPolicy the setKapaPolicy to set
	 */
	public void setSetKapaPolicy(int setKapaPolicy) {
		this.setKapaPolicy = setKapaPolicy;
	}

	public boolean isGibNix() {
		return gibNix;
	}

	public void setGibNix(boolean gibNix) {
		this.gibNix = gibNix;
	}
	
	/**
	 * 
	 * @param TalentName
	 * @return
	 */
	public int getSkillLevel(String TalentName){
		int erg=0;
		
		SkillType sT = this.scriptMain.gd_ScriptMain.rules.getSkillType(TalentName, false);
		if (sT!=null){
			Skill skill = this.unit.getModifiedSkill(sT);
			if (skill!=null){
				erg=skill.getLevel();
			}
		}
		return erg;
	}
	
	/**
	 * pr�ft, ob Tags f�r die Scripte definiert sind und setzt diese ggf, wenn
	 * noch nicht vorhanden
	 */
	public void autoTags(){
		if (reportSettings.getOptionBoolean("useReportTags",this.unit.getRegion())){
			for (Script s:this.foundScriptList){
				String searchOption="tag1_" + s.getClass().getSimpleName().toLowerCase();
				String reportOptionString = reportSettings.getOptionString(searchOption,this.unit.getRegion());
				if (reportOptionString!=null){
					// erster Buchstabe gro�
					reportOptionString = reportOptionString.substring(0,1).toUpperCase() + reportOptionString.substring(1).toLowerCase();
					String actTag = this.unit.getTag(CRParser.TAGGABLE_STRING);
					if (actTag==null || !actTag.equals(reportOptionString)){
						this.unit.putTag(CRParser.TAGGABLE_STRING, reportOptionString);
						this.addComment("autoTag gesetzt auf :" + reportOptionString);
					} else {
						this.addComment("autoTag ist bereits auf:" + reportOptionString);
					}
				} else {
					// this.addComment("kein autotag eintrag f�r:" + searchOption);
				}
			}
		} else {
			// this.addComment("autoTag deactiviert");
		}
	}
	
	/**
	 * setzt die DELs von der delScriptList um
	 */
	public void processScriptDeletions(){
		if (this.delScriptList==null || this.delScriptList.size()==0){
			return;
		}
		for (Script s : this.delScriptList){
			this.foundScriptList.remove(s);
		}
		this.delScriptList.clear();
	}
	
	
	/**
	 * 
	 * @return mainDurchlauf beim Overlord
	 */
	public int getMainDurchlauf(){
		return this.getOverlord().getMainDurchlauf();
	}

	/**
	 * @return the includeSailorsWeight
	 */
	public boolean isIncludeSailorsWeight() {
		return includeSailorsWeight;
	}

	/**
	 * @param includeSailorsWeight the includeSailorsWeight to set
	 */
	public void setIncludeSailorsWeight(boolean includeSailorsWeight) {
		this.includeSailorsWeight = includeSailorsWeight;
	}

	public int getRecruitedPersons() {
		return recruitedPersons;
	}

	public void setRecruitedPersons(int recruitedPersons) {
		this.recruitedPersons = recruitedPersons;
	}
	
	public void incRecruitedPersons(int recruitedPersons) {
		this.recruitedPersons += recruitedPersons;
	}
	
}
