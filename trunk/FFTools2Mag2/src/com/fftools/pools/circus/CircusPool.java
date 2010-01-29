package com.fftools.pools.circus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import magellan.library.Region;

import com.fftools.ScriptUnit;


/**
 * Klasse die Unterhalter einer Region koordiniert und selbst 
 * von CircusPoolManager verwaltet wird
 * @author Marc
 *
 */

public class CircusPool {
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	// der PoolManager sammelt die CirusPools 
	public CircusPoolManager circusPoolManager;
	
	// Region f�r die der Pool arbeiten soll
	public Region region = null;
	
	// Liste mit den CircusPoolRelations
	private ArrayList <CircusPoolRelation> listOfRelations = null;
	
	private int regionMaxUnterhaltung;
	
	// Wird vom Pool runtergez�hlt bis alles abgesch�pft ist.
	private int remainingUnterhaltung;	
	
	private int regionsVerdienst=0;
	
	// falls Region nicht komplett von Monopol bewirtschaftet wird
	private int limit = 100000;
	
	/**
	 * Konstruktor
	 * @param _cpm
	 * @param _region
	 */
	public CircusPool(CircusPoolManager _cpm, Region _region ){
		circusPoolManager = _cpm;
		
		region = _region;
		regionMaxUnterhaltung = region.maxEntertain();
		remainingUnterhaltung = regionMaxUnterhaltung;
		// zun�chst gleich Max, damit mehrfache Aenderungen auffallen!
		limit = regionMaxUnterhaltung;
	}
	
	
	/**
	 * F�gt dem Circuspool eine CircusPoolRelation hinzu
	 * @param _cpr CircusPoolRelation 
	 */
	public void addCircusPoolRelation(CircusPoolRelation _cpr){
		// gibt es schon eine liste mit Relations? Falls nicht anlegen!
		if (listOfRelations == null){
	    	listOfRelations = new ArrayList <CircusPoolRelation>();
		}
	    
		listOfRelations.add(_cpr);	
	}
	
	/**
	 * Zentrale Methode zum CircusPool anstossen
	 *
	 */
	public void runPool(){
		
		
		
		// Check ob es Relations gibt
		if (this.listOfRelations != null){
			 
			// Sortiert Relations nach Talentwert
			// sortieren hinter != null gelegt, damit nix leeres sortiert wird
			Collections.sort(this.listOfRelations);
			
			//Durchiterieren der cpr   
			for (Iterator<CircusPoolRelation> iter1 = this.listOfRelations.iterator();iter1.hasNext();){
			     CircusPoolRelation cpr = (CircusPoolRelation) iter1.next();
	             
			     // Ist Silber der Bauern verschleudert?
			     if (remainingUnterhaltung <= 0){
			    	 
			    	 // Sende Vorzeichen-Signal an Relation und damit an Script
			    	 cpr.setDoUnterhaltung(-1);
			     }
			     
                  //   Es ist Silber da aber zu wenig f�r die Leistung der Einheit.
			     if ((remainingUnterhaltung > 0) && (cpr.getVerdienst() > remainingUnterhaltung)){
			    	 
			    	 cpr.setDoUnterhaltung(remainingUnterhaltung);
			    	 regionsVerdienst = regionsVerdienst + remainingUnterhaltung;
			    	 // Silber der Bauern ist aufgebraucht...
			    	 remainingUnterhaltung = 0;
			     }
			     
			     
			     // Check: Noch Silber zu verdienen AND Verdienst geringer als Restsilber?
			     if ((remainingUnterhaltung > 0) && (cpr.getVerdienst()<= remainingUnterhaltung)){
			    	 // Unterhalte soviel wie geht!
			    	 cpr.setDoUnterhaltung(cpr.getVerdienst());
			    	 regionsVerdienst = regionsVerdienst + cpr.getVerdienst();
			    	 // Freies Silber reduzieren um Unterhalungsverm�gen der Einheit 
			    	 remainingUnterhaltung = remainingUnterhaltung - cpr.getVerdienst();
			       
			     }
			    
			
		     }
		
		}	
	
	}

/**
 * 
 * Gibt den Verdienst aller Unterhalter in der Region zur�ck
 */	
public int getRegionsVerdienst(){
	return regionsVerdienst;
}

public int getRemainingUnterhalt(){
	return remainingUnterhaltung;
}
	
public int getUnterhaltungslimit(){
	return limit;
}

/**
 * 
 * Setzt limit und korrigiert remainigUnterhalung
 * 
 */
public void setUnterhaltungslimit(int _lim){
	limit = _lim;
	if (limit < remainingUnterhaltung){
		remainingUnterhaltung = limit;
	}
}

/**
 * Gibt maximale Unterhaltung in PoolRegion zur�ck
 */
public int getRegionMaxUnterhaltung(){
	return regionMaxUnterhaltung;
}

/**
 * 
 * Gibt null wenn nichts gefunden wird.
 * 
 */
public CircusPoolRelation getCircusPoolRelation(ScriptUnit _su){
	boolean go_on= true;
	CircusPoolRelation cpr = null;
	for (Iterator<CircusPoolRelation> iter2 = this.listOfRelations.iterator();(iter2.hasNext()&&(go_on));){
	    // casten, vergesse das zu gern!
		cpr = (CircusPoolRelation) iter2.next();
	    if (cpr.getSkriptUnit() == _su){
	    	go_on=false;
	    	return cpr;
	    }
	}
	return cpr;  
}


/**
 * @return the listOfRelations
 */
public ArrayList<CircusPoolRelation> getListOfRelations() {
	return listOfRelations;
}



}
