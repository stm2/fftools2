package com.fftools.pools.ausbildung;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Order;
import magellan.library.StringID;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.utils.FFToolsOptionParser;

/**
 * Verwaltet Objekte vom Typ <ttt>Lernplan</ttt>
 * @author Fiete
 *
 */
public class LernplanHandler implements OverlordInfo {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	/**
	 * Die Map der Lernpl�ne, Key ist der Lernplan-Name -> toLowerCase
	 * Values sind Objekte des Types Lernplan
	 */
	private Hashtable<StringID,Lernplan> lernPlanMap = null; 
	
	/**
	 * Liste aller bereist erfassten units, um keine doppelt zu erfassen
	 */
	private ArrayList<ScriptUnit> scriptedUnits = null;
	
	private final String scriptIdentifier = "setLernplan";
	public static final String lernplanDetailSeparator = "%"; 
	
	
	/**
	 * Konstruktor
	 *
	 */
	public LernplanHandler(){
		
	}
	
	/**
	 * liefert den Lernplan zu einem Namen (String) oder null, wenn noch nicht
	 * bekannt und allowNew=false, anderenfalls einen neuen Lernplan
	 * 
	 * u: an wen sollen eventuelle Fehlermeldungen ausgegeben werden ?!
	 * 
	 * @param lernPlanName
	 * @return
	 */
	public Lernplan getLernplan(ScriptUnit u,String lernPlanName,boolean allowNew){
		if ((this.lernPlanMap==null || this.lernPlanMap.size()==0) && !allowNew){
			return null;
		}
		
		if (this.lernPlanMap==null && allowNew){
			this.lernPlanMap = new Hashtable<StringID, Lernplan>();
		}
		
		String lowName = lernPlanName.toLowerCase();
		StringID actStringID = StringID.create(lowName);
		
		Lernplan actLP = this.lernPlanMap.get(actStringID);
		
		if (actLP==null && allowNew){
			actLP = new Lernplan(u,lernPlanName);
			this.lernPlanMap.put(actStringID, actLP);
		}
		return actLP;
	}
	
	/**
	 * Service f�r die Scripts : parst die Orders einer ScriptUnit komplett
	 * nach Definitionen zum LernPlan
	 * @param u
	 */
	public void parseOrders(ScriptUnit u){
		if (u==null){
			return;
		}
		if (this.scriptedUnits==null){
			this.scriptedUnits = new ArrayList<ScriptUnit>();
		}
		if (this.scriptedUnits.contains(u)){
			// schon mal gescripted
			return;
		}
		// wir werden jetzt parsen, also gleich dazu
		this.scriptedUnits.add(u);
		
		// neuen Optionparser
		FFToolsOptionParser OP = new FFToolsOptionParser(u);
		ArrayList<String> orders = new ArrayList<String>();
		for (Order o:u.getUnit().getOrders2()){
			String order = o.getText();
			if (order.toLowerCase().indexOf(scriptIdentifier.toLowerCase())>0){
				orders.add(order);
			}
		}
		
		if (orders.size()>0){
			for (String order:orders){
				// diese orderzeile passt in unser beuteschema
				OP.reset();
				OP.addOptionString(order);
				this.processOptionParser(u,OP);
			}
		}
	}
	
	/**
	 * findet passenden Lernplan und �bergibt dann dem den OP
	 * wir schleppen u mit f�r eventuelle parsefehler ->anzeigen, wo sie
	 * auftreten
	 * @param OP Optionparser mit Optionen einer Zeile
	 */
	private void processOptionParser(ScriptUnit u,FFToolsOptionParser OP){
		String name = OP.getOptionString("name");
		if (name.length()<2){
			// name nicht vorhanden oder 1 Zeichen lang -> ablehnen
			outText.addOutLine("!!! LernplanName nicht erkannt! " + u.unitDesc() , true);
			u.addComment("!!! Lernplanname nicht erkannt");
			u.doNotConfirmOrders();
			return;
		}
		
		Lernplan actLP = this.getLernplan(u,name, true);
		// �bergibt an den Lernplan
		actLP.parseOptionLine(u,OP);
	}
	
	
	/**
	 * liefert die Ausbildungsrelation zur Benutzung mit den Ausbildungspool
	 * @param u Die betroffene Scriptunit
	 * @param LernplanName Der Name des Lernplanes, eventuell noch mit % Parameter
	 * @return die fertige Ausbildungsrelation oder null
	 */
	public AusbildungsRelation getAusbildungsrelation(ScriptUnit u,String lernplanName){
		// LernplanNamen checken...
		// wurden abgelegt inkl %
		Lernplan actLP = this.getLernplan(u, lernplanName, false);
		if (actLP==null){
			// nix dazu gefunden....
			outText.addOutLine("!!! Lernplan mit diesem Namen nicht gefunden! " + u.unitDesc() , true);
			u.addComment("!!! Lernplan mit diesem Namen nicht gefunden! ");
			u.doNotConfirmOrders();
			return null;
		}
		
		AusbildungsRelation retVal = null;
		
		// String masterPlanName = lernplanName;
		String levelString = "";
		// maxLevel herausfinden
		int maxLevel = Lernplan.level_unset;
		if (lernplanName.indexOf(LernplanHandler.lernplanDetailSeparator)>0){
			// hier wird ein bestimmter Level angestrebt
			String[] paare = lernplanName.split(LernplanHandler.lernplanDetailSeparator);
			// masterPlanName = paare[0];
			levelString = paare[1];
			try {
				Integer i = Integer.parseInt(levelString);
				maxLevel = i.intValue();
				if (maxLevel<1){
					// dumm
					outText.addOutLine("!!! Lernplan: gew�nschte Stufe fehlerhaft: " + u.unitDesc() , true);
					u.addComment("!!! Lernplan: gew�nschte Stufe fehlerhaft");
					u.doNotConfirmOrders();
					return null;
				}
			} catch (NumberFormatException e){
				// dumm....
				outText.addOutLine("!!! Lernplan: gew�nschte Stufe fehlerhaft: " + u.unitDesc() , true);
				u.addComment("!!! Lernplan: gew�nschte Stufe fehlerhaft");
				u.doNotConfirmOrders();
				return null;
			}
		}
		
		// jetzt eigentlich die vorhandenen Level checken, wenn nicht erf�llt,
		// dann dem ersten nicht erf�llten die Lernsettings setzen
		// herausfinden, ob wir �berhaupt einen maximalen Level haben.
		int maxPlanLevel = Lernplan.level_unset;
		for (Iterator<Lernplan> iter=this.lernPlanMap.values().iterator();iter.hasNext();){
			Lernplan actLernplan = (Lernplan)iter.next();
			if (actLernplan.getLernPlanLevel()!=Lernplan.level_unset 
					&& actLernplan.getLernPlanLevel()>maxPlanLevel
					&& actLernplan.isSameMasterLernplan(actLP)){
				maxPlanLevel = actLernplan.getLernPlanLevel();
			}
		}
		
		if (maxPlanLevel!=Lernplan.level_unset){
			// OK, wir haben einen maxPlanLevel
			retVal = this.getAusbildungsRelationMultipleLernplan(u, actLP,maxPlanLevel,maxLevel);
		} else {
			// keinen maxPlanLevel...wir gehen davon aus, wir haben nur 
			// einen passenden LernPlan
			retVal = this.getAusbildungsRelationSingleLernplan(u, actLP);
		}
		return retVal;
	}
	
	/**
	 * Liefert AusbRel aus genau einem Lernplan oder null, wenn der nicht mehr greift
	 * @param u
	 * @param LP
	 * @return
	 */
	private AusbildungsRelation getAusbildungsRelationSingleLernplan(ScriptUnit u,Lernplan LP){
		AusbildungsRelation retVal = new AusbildungsRelation(u,null,null);
		if (!LP.appendAusbildungsRelation(u, retVal)){
			// nichts erg�nzt worden...keine Lernziele mehr verf�gbar!
			retVal=null;
		}
		return retVal;
	}
	
	
	/**
	 * liefert AusbRel aus dem Lernplan, der noch nicht durch die unit erf�llt wird
	 * @param u
	 * @param LP
	 * @param maxPlanLevel
	 * @return
	 */
	private AusbildungsRelation getAusbildungsRelationMultipleLernplan(ScriptUnit u,Lernplan LP,int maxPlanLevel, int userSetMaxPlanLevel){
		String masterName = LP.getName();
		AusbildungsRelation AB = new AusbildungsRelation(u,null,null);
		int posI = masterName.indexOf(LernplanHandler.lernplanDetailSeparator);
		if (posI>0){
			masterName = masterName.substring(0,posI);
		}
		boolean hasMatched = false;
		int actMaxPlanLevel = maxPlanLevel;
		// anpassen, wenn user gesetzt
		if (userSetMaxPlanLevel!=Lernplan.level_unset){
			actMaxPlanLevel = userSetMaxPlanLevel;
		}
		
		
		for (int i = 1;i<=actMaxPlanLevel;i++){
			String neuS = masterName + LernplanHandler.lernplanDetailSeparator + i;
			StringID ID = StringID.create(neuS);
			Lernplan actLP = this.lernPlanMap.get(ID);
			hasMatched = false;
			if (actLP!=null){
				hasMatched = actLP.appendAusbildungsRelation(u, AB);
			}
			if (hasMatched){
				// da hat ein Lernplan zugeschlagen...
				break;
			}
		}
		
		if (!hasMatched && userSetMaxPlanLevel==Lernplan.level_unset){
			// abschliessend nach Lernplan ohne % als Bonus suchen..
			StringID ID = StringID.create(masterName);
			Lernplan actLP = this.lernPlanMap.get(ID);
			if (actLP!=null){
				hasMatched = actLP.appendAusbildungsRelation(u, AB);
				if (hasMatched){
					AB.setActLernplanLevel(Lernplan.level_afterSets);
				}
			}
		}
		
		
		if (!hasMatched){
			// gar nix geliefert...also auch null zur�ckgeben
			AB = null;
		}
		return AB;
	}
	
	
	/**
	 * wird nicht bei einem bestimmten durchlauf aufgerufen
	 * f�r OverlordInfo
	 */
	public int[] runAt(){
		return null;
	}
	
	  //inner class //
	  private class LernplanComparator implements Comparator<Lernplan> {
	    public int compare(Lernplan arg0, Lernplan arg1) {
	      return arg0.getName().compareToIgnoreCase(arg1.getName());
	    }
	  }
	
	  
	  /**
	   * Liefert sortierte Liste der bekannten Lernpl�ne
	   * @return
	   */
	  public ArrayList<Lernplan> getSortedLernPl�ne(){
		  if (this.lernPlanMap==null || this.lernPlanMap.size()==0){
		     return null;	  
		  }
		  ArrayList<Lernplan> erg = new ArrayList<Lernplan>();
		  erg.addAll(this.lernPlanMap.values());
		  Collections.sort(erg, new LernplanComparator());
		  return erg;  
	  }
	  
	  public void reset(){
		  this.scriptedUnits = null;
		  this.lernPlanMap = null;
	  }
	
	
}
